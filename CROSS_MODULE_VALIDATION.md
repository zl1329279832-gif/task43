# 用户下单与订单查询 - 跨模块验证说明

## 一、功能概述

本功能实现了用户通过 `sbc-user` 发起下单请求，经 `sbc-gateway-zuul` 网关透传后，由 `sbc-user` 通过 Feign 调用 `sbc-order` 服务完成订单创建与查询的完整跨服务流程。

### 调用链路

```
客户端 → sbc-gateway-zuul (8383)
       → sbc-user (8080)          [用户校验 + Feign调用]
       → sbc-order (8181)         [订单创建/查询]
```

### 涉及模块

| 模块 | 职责 |
|------|------|
| `sbc-common` | 基础类：BaseRequest（含reqNo）、BaseResponse、StatusEnum、SBCException |
| `sbc-gateway-zuul` | 网关：token校验、reqNo/userId透传、服务路由 |
| `sbc-user` | 用户服务：用户存在性校验、reqNo校验、通过Feign调用订单服务 |
| `sbc-order` | 订单服务：订单创建（含去重）、按订单号查询、按用户查询订单列表 |
| `sbc-request-check` | 请求去重插件：基于Redis的reqNo去重AOP切面 |

---

## 二、接口清单

### 用户服务 (sbc-user)

| 接口 | 路径 | 说明 |
|------|------|------|
| 用户下单 | `POST /userService/createOrder` | 接收userId、商品信息、reqNo，通过Feign调用订单服务 |

### 订单服务 (sbc-order)

| 接口 | 路径 | 说明 |
|------|------|------|
| 创建订单 | `POST /orderService/createOrder` | 创建订单，支持reqNo去重 |
| 按订单号查询 | `POST /orderService/getOrderByOrderNo` | 根据orderNo查询单个订单 |
| 按用户查询列表 | `POST /orderService/getOrdersByUserId` | 根据userId查询该用户的所有订单 |

### 网关路由 (sbc-gateway-zuul)

| 路由 | 目标服务 |
|------|----------|
| `/api/user/**` | sbc-user |
| `/api/order/**` | sbc-order |

---

## 三、异常场景验证

### 场景1：重复提交返回同一订单

**触发条件：** 使用相同的 `reqNo` 多次调用 `POST /userService/createOrder`

**处理机制：**
- 订单服务 `OrderController.createOrder()` 内维护 `reqNoIndex`（ConcurrentHashMap），将 reqNo 映射到 orderNo
- 首次请求：创建订单 → 存入 orderStore 和 reqNoIndex → 返回新订单
- 重复请求：检测到 reqNo 已存在于 reqNoIndex → 从 orderStore 取出同一订单 → 直接返回
- 与 `sbc-request-check` 模块中 `@CheckReqNo` 的 Redis 去重机制思路一致，但此处返回已有订单而非抛异常

**验证方式：**
```json
// 第一次请求
POST /userService/createOrder
{"reqNo":"REQ001","userId":1,"productName":"iPhone","productCount":1,"price":999900}
// 返回: code=9000, orderNo="ORD20170716..."

// 第二次请求（相同reqNo）
POST /userService/createOrder
{"reqNo":"REQ001","userId":1,"productName":"iPhone","productCount":1,"price":999900}
// 返回: code=9000, message="重复提交，返回已有订单", 同一个orderNo
```

### 场景2：订单服务超时降级（Hystrix）

**触发条件：** sbc-order 服务不可用或响应超时

**处理机制：**
- `OrderServiceClient` 配置了 `fallbackFactory = OrderServiceFallbackFactory.class`
- `OrderServiceFallbackFactory` 记录降级原因日志后返回 `BaseResponse`，code 为 `8000`（FALLBACK）
- 用户服务收到降级响应后直接透传给调用方

**验证方式：**
1. 停止 sbc-order 服务
2. 调用 `POST /userService/createOrder`
3. 预期返回: `{"code":"8000","message":"FALL_BACK","reqNo":"...","dataBody":null}`

### 场景3：用户不存在

**触发条件：** 请求中 `userId` 对应的用户在系统中不存在

**处理机制：**
- `UserController.createOrder()` 通过 `USER_STORE` 校验用户是否存在
- 若用户不存在，抛出 `SBCException(StatusEnum.USER_NOT_FOUND)`
- `GlobalExceptionHandler` 捕获异常，返回统一 `BaseResponse`，code 为 `7000`

**验证方式：**
```json
POST /userService/createOrder
{"reqNo":"REQ002","userId":9999,"productName":"iPhone","productCount":1,"price":999900}
// 返回: {"code":"7000","message":"用户不存在","dataBody":null}
```

### 场景4：请求号缺失

**触发条件：** 请求中 `reqNo` 为空或未传

**处理机制：**
- 用户服务层 `UserController.createOrder()` 首先校验 reqNo
- 订单服务层 `OrderController.createOrder()` 二次校验 reqNo
- 任一层检测到缺失，抛出 `SBCException(StatusEnum.REQ_NO_MISSING)`
- 返回统一 `BaseResponse`，code 为 `7001`

**验证方式：**
```json
POST /userService/createOrder
{"userId":1,"productName":"iPhone","productCount":1,"price":999900}
// 返回: {"code":"7001","message":"请求号不能为空","dataBody":null}
```

### 场景5：网关拦截失败（缺少token）

**触发条件：** 通过网关访问但未携带 `token` 参数

**处理机制：**
- `RequestFilter`（Zuul pre filter）检查请求中的 `token` 参数
- 若 token 缺失，设置 `sendZuulResponse(false)` 并返回 HTTP 401
- 请求不会被路由到下游服务

**验证方式：**
```
# 不带token（被拦截）
POST http://localhost:8383/api/user/userService/createOrder
// 返回: HTTP 401

# 带token（正常透传）
POST http://localhost:8383/api/user/userService/createOrder?token=xxx
// 正常路由到 sbc-user
```

---

## 四、模块复用说明

### 4.1 Feign + Hystrix 降级

- `OrderServiceClient` 继承 `OrderService` API 接口，通过 `@FeignClient` 实现远程调用
- 配置 `fallbackFactory = OrderServiceFallbackFactory.class`，当 sbc-order 不可用时自动降级
- `OrderConfig` 通过 `@Bean` 注册 `OrderServiceFallbackFactory`
- sbc-user 的 `SbcUserApplication` 通过 `@EnableHystrix` 和 `@EnableFeignClients` 启用

### 4.2 sbc-request-check 去重

- `sbc-request-check` 模块通过 `spring.factories` 自动配置，`@CheckReqNo` 注解基于 Redis 拦截重复 reqNo
- 订单服务的 `getOrderNo` 接口使用 `@CheckReqNo` 注解进行 Redis 级别的去重
- `createOrder` 接口采用应用级去重（ConcurrentHashMap），同一 reqNo 返回同一订单而非抛异常
- 两者复用 `BaseRequest.reqNo` 字段作为去重标识

### 4.3 BaseResponse 统一响应

- 所有接口统一返回 `BaseResponse<T>`，包含 `code`、`message`、`reqNo`、`dataBody`
- 异常通过 `GlobalExceptionHandler` 统一转为 `BaseResponse<NULLBody>` 返回
- 状态码定义在 `StatusEnum` 中，覆盖成功（9000）、降级（8000）、校验失败（3000）、业务失败（4000）、重复请求（5000）、限流（6000）、用户不存在（7000）、请求号缺失（7001）、订单不存在（7002）

### 4.4 网关透传

- `RequestFilter` 将请求中的 `reqNo` 和 `userId` 通过 `addZuulRequestHeader` 透传到下游服务
- 支持从 URL 参数或 HTTP Header 中获取这两个标识
- 路由配置同时覆盖 `sbc-user` 和 `sbc-order` 两个服务
