package com.crossoverJie.sbcuser.controller;

import com.alibaba.fastjson.JSON;
import com.crossoverJie.order.feign.api.OrderServiceClient;
import com.crossoverJie.order.vo.req.CreateOrderReqVO;
import com.crossoverJie.order.vo.req.OrderNoReqVO;
import com.crossoverJie.order.vo.res.CreateOrderResVO;
import com.crossoverJie.order.vo.res.OrderNoResVO;
import com.crossoverJie.sbcorder.common.enums.StatusEnum;
import com.crossoverJie.sbcorder.common.exception.SBCException;
import com.crossoverJie.sbcorder.common.res.BaseResponse;
import com.crossoverJie.sbcuser.req.OrderNoReq;
import com.crossoverJie.sbcuser.res.UserRes;
import com.crossoverJie.user.api.UserService;
import com.crossoverJie.user.vo.req.UserCreateOrderReqVO;
import com.crossoverJie.user.vo.req.UserReqVO;
import com.crossoverJie.user.vo.res.UserCreateOrderResVO;
import com.crossoverJie.user.vo.res.UserResVO;
import com.google.common.util.concurrent.RateLimiter;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Function:user控制器
 *
 * @author crossoverJie
 *         Date: 2017/6/7 下午11:55
 * @since JDK 1.8
 */
@RestController
@Api(value = "userApi", description = "用户API", tags = {"用户服务"})
public class UserController implements UserService {
    private final static Logger logger = LoggerFactory.getLogger(UserController.class);

    //@Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OrderServiceClient orderServiceClient;

    private final static int COUNT = 10;

    /** 模拟用户数据库: 已知用户ID集合 */
    private static final Set<Long> KNOWN_USER_IDS = new HashSet<>();
    static {
        KNOWN_USER_IDS.add(1001L);
        KNOWN_USER_IDS.add(1002L);
        KNOWN_USER_IDS.add(1003L);
        KNOWN_USER_IDS.add(123L);
    }

    /** 模拟用户名称映射 */
    private static final java.util.Map<Long, String> USER_NAMES = new java.util.HashMap<>();
    static {
        USER_NAMES.put(1001L, "张三");
        USER_NAMES.put(1002L, "李四");
        USER_NAMES.put(1003L, "王五");
        USER_NAMES.put(123L, "张三");
    }

    @Resource(name = "concurrentTestThread")
    private ExecutorService executorService;


    @Override
    public BaseResponse<UserResVO> getOrderNo(@RequestBody UserReqVO userReq) {
        OrderNoReq req = new OrderNoReq();
        req.setReqNo("1213");
        //调用远程服务
        ResponseEntity<Object> res = restTemplate.postForEntity("http://sbc-order/order/getOrderNo", req, Object.class);
        logger.info("res=" + JSON.toJSONString(res));

        logger.debug("入参=" + JSON.toJSONString(userReq));
        UserRes userRes = new UserRes();
        userRes.setUserId(123);
        userRes.setUserName("张三");

        userRes.setReqNo(userReq.getReqNo());
        userRes.setCode(StatusEnum.SUCCESS.getCode());
        userRes.setMessage("成功");

        return userRes;
    }

    @Override
    public BaseResponse<UserResVO> getUserByFeign(@RequestBody UserReqVO userReq) {
        //调用远程服务
        OrderNoReqVO vo = new OrderNoReqVO();
        vo.setAppId(1L);
        vo.setReqNo(userReq.getReqNo());

        for (int i = 0; i < 10; i++) {
            executorService.execute(new Worker(vo, orderServiceClient));
        }

        UserRes userRes = new UserRes();
        userRes.setUserId(123);
        userRes.setUserName("张三");

        userRes.setReqNo(userReq.getReqNo());
        userRes.setCode(StatusEnum.SUCCESS.getCode());
        userRes.setMessage("成功");

        return userRes;
    }

    @Override
    public BaseResponse<UserResVO> getUserByFeignBatch(@RequestBody UserReqVO userReqVO) {
        //调用远程服务
        OrderNoReqVO vo = new OrderNoReqVO();
        vo.setReqNo(userReqVO.getReqNo());
        vo.setAppId(1L);

        RateLimiter limiter = RateLimiter.create(2.0);
        //批量调用
        for (int i = 0; i < COUNT; i++) {
            double acquire = limiter.acquire();
            logger.debug("获取令牌成功!,消耗=" + acquire);
            BaseResponse<OrderNoResVO> orderNo = orderServiceClient.getOrderNo(vo);
            logger.debug("远程返回:" + JSON.toJSONString(orderNo));
        }

        UserRes userRes = new UserRes();
        userRes.setUserId(123);
        userRes.setUserName("张三");

        userRes.setReqNo(userReqVO.getReqNo());
        userRes.setCode(StatusEnum.SUCCESS.getCode());
        userRes.setMessage("成功");

        return userRes;
    }


    @Override
    public BaseResponse<OrderNoResVO> getUserByHystrix(@RequestBody UserReqVO userReqVO) {

        OrderNoReqVO vo = new OrderNoReqVO();
        vo.setAppId(123L);
        vo.setReqNo(userReqVO.getReqNo());
        BaseResponse<OrderNoResVO> orderNo = orderServiceClient.getOrderNo(vo);
        return orderNo;
    }

    /**
     * 用户下单: 校验用户 -> Feign 调用订单服务创建订单 -> 返回统一 BaseResponse
     *
     * 处理场景:
     * 1. 请求号缺失 -> 抛 SBCException(REPEAT_REQUEST)
     * 2. 用户不存在 -> 抛 SBCException(FAIL, "用户不存在")
     * 3. 订单服务超时/异常 -> Hystrix fallback 降级 (code=8000)
     * 4. 重复提交同一 reqNo -> 订单服务返回同一订单 (幂等)
     */
    @Override
    public BaseResponse<UserCreateOrderResVO> createOrderForUser(@RequestBody UserCreateOrderReqVO req) {
        logger.info("createOrderForUser request: {}", JSON.toJSONString(req));

        // 1. 请求号校验
        String reqNo = req.getReqNo();
        if (reqNo == null || reqNo.trim().isEmpty()) {
            throw new SBCException(StatusEnum.REPEAT_REQUEST.getCode(), "请求号不能为空");
        }

        // 2. 用户存在性校验
        Long userId = req.getUserId();
        if (userId == null || !KNOWN_USER_IDS.contains(userId)) {
            throw new SBCException(StatusEnum.FAIL.getCode(), "用户不存在, userId=" + userId);
        }

        // 3. 构建订单服务请求
        CreateOrderReqVO orderReq = new CreateOrderReqVO();
        orderReq.setReqNo(reqNo);
        orderReq.setUserId(userId);
        orderReq.setProductName(req.getProductName());
        orderReq.setUnitPrice(req.getUnitPrice());
        orderReq.setQuantity(req.getQuantity());

        // 4. Feign 调用订单服务 (Hystrix 自动降级)
        logger.info("calling order service createOrder, reqNo={}", reqNo);
        BaseResponse<CreateOrderResVO> orderResponse = orderServiceClient.createOrder(orderReq);
        logger.info("order service response: code={}, message={}", orderResponse.getCode(), orderResponse.getMessage());

        // 5. 判断订单服务是否降级
        if (StatusEnum.FALLBACK.getCode().equals(orderResponse.getCode())) {
            BaseResponse<UserCreateOrderResVO> fallbackRes = new BaseResponse<>();
            fallbackRes.setReqNo(reqNo);
            fallbackRes.setCode(StatusEnum.FALLBACK.getCode());
            fallbackRes.setMessage("订单服务暂时不可用，请稍后重试");
            return fallbackRes;
        }

        // 6. 组装用户下单响应
        CreateOrderResVO orderData = orderResponse.getDataBody();
        UserCreateOrderResVO userRes = new UserCreateOrderResVO();
        if (orderData != null) {
            userRes.setOrderNo(orderData.getOrderNo());
            userRes.setUserId(orderData.getUserId());
            userRes.setProductName(orderData.getProductName());
            userRes.setUnitPrice(orderData.getUnitPrice());
            userRes.setQuantity(orderData.getQuantity());
            userRes.setTotalPrice(orderData.getTotalPrice());
            userRes.setStatus(orderData.getStatus());
            userRes.setCreateTime(orderData.getCreateTime());
        }
        // 补充用户名称
        userRes.setUserName(USER_NAMES.getOrDefault(userId, "未知用户"));

        BaseResponse<UserCreateOrderResVO> response = BaseResponse.createSuccess(userRes, "下单成功");
        response.setReqNo(reqNo);

        logger.info("createOrderForUser success: orderNo={}, userId={}",
                userRes.getOrderNo(), userId);
        return response;
    }


    private static class Worker implements Runnable {

        private OrderNoReqVO vo;
        private OrderServiceClient orderServiceClient;

        public Worker(OrderNoReqVO vo, OrderServiceClient orderServiceClient) {
            this.vo = vo;
            this.orderServiceClient = orderServiceClient;
        }

        @Override
        public void run() {

            BaseResponse<OrderNoResVO> orderNo = orderServiceClient.getOrderNoCommonLimit(vo);
            logger.info("远程返回:" + JSON.toJSONString(orderNo));

        }
    }

}
