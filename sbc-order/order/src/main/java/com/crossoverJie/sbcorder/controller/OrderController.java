package com.crossoverJie.sbcorder.controller;

import com.crossoverJie.order.api.OrderService;
import com.crossoverJie.order.vo.req.CreateOrderReqVO;
import com.crossoverJie.order.vo.req.OrderNoReqVO;
import com.crossoverJie.order.vo.req.OrderQueryReqVO;
import com.crossoverJie.order.vo.req.UserOrderQueryReqVO;
import com.crossoverJie.order.vo.res.CreateOrderResVO;
import com.crossoverJie.order.vo.res.OrderNoResVO;
import com.crossoverJie.request.check.anotation.CheckReqNo;
import com.crossoverJie.sbcorder.common.enums.StatusEnum;
import com.crossoverJie.sbcorder.common.exception.SBCException;
import com.crossoverJie.sbcorder.common.res.BaseResponse;
import com.crossoverJie.sbcorder.common.util.DateUtil;
import com.crossoverJie.sbcorder.model.Order;
import com.crossoverjie.distributed.annotation.CommonLimit;
import com.crossoverjie.distributed.annotation.ControllerLimit;
import com.crossoverjie.distributed.limit.RedisLimit;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Function:order控制器
 * @author crossoverJie
 * Date: 2017/6/7 下午11:55
 * @since JDK 1.8
 */
@RestController
@Api(value = "orderApi", description = "订单API", tags = {"订单服务"})
public class OrderController implements OrderService{
    private final static Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private RedisLimit redisLimit ;

    /** 订单号生成器 (基于时间戳 + 自增序号保证唯一) */
    private static final AtomicLong ORDER_SEQ = new AtomicLong(0);

    /** 订单存储: orderNo -> Order */
    private static final Map<String, Order> ORDER_STORE = new ConcurrentHashMap<>();

    /** 请求号去重缓存: reqNo -> 已创建的订单响应 (复用 sbc-request-check 去重思想) */
    private static final Map<String, BaseResponse<CreateOrderResVO>> REQ_NO_CACHE = new ConcurrentHashMap<>();

    /** 请求号维度锁: 保证同一 reqNo 并发只有一个线程执行创建 */
    private static final ConcurrentHashMap<String, Object> REQ_LOCKS = new ConcurrentHashMap<>();

    @Override
    @CheckReqNo
    public BaseResponse<OrderNoResVO> getOrderNo(@RequestBody OrderNoReqVO orderNoReq) {
        BaseResponse<OrderNoResVO> res = new BaseResponse();

        //限流
        boolean limit = redisLimit.limit();
        if (!limit){
            res.setCode(StatusEnum.REQUEST_LIMIT.getCode());
            res.setMessage(StatusEnum.REQUEST_LIMIT.getMessage());
            return res ;
        }

        res.setReqNo(orderNoReq.getReqNo());
        if (null == orderNoReq.getAppId()){
            throw new SBCException(StatusEnum.FAIL);
        }
        OrderNoResVO orderNoRes = new OrderNoResVO() ;
        orderNoRes.setOrderId(DateUtil.getLongTime());
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        res.setDataBody(orderNoRes);
        return res ;
    }

    @Override
    @ControllerLimit
    public BaseResponse<OrderNoResVO> getOrderNoLimit(@RequestBody OrderNoReqVO orderNoReq) {
        BaseResponse<OrderNoResVO> res = new BaseResponse();
        res.setReqNo(orderNoReq.getReqNo());
        if (null == orderNoReq.getAppId()){
            throw new SBCException(StatusEnum.FAIL);
        }
        OrderNoResVO orderNoRes = new OrderNoResVO() ;
        orderNoRes.setOrderId(DateUtil.getLongTime());
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        res.setDataBody(orderNoRes);
        return res ;
    }

    @Override
    @CommonLimit
    public BaseResponse<OrderNoResVO> getOrderNoCommonLimit(@RequestBody OrderNoReqVO orderNoReq) {
        BaseResponse<OrderNoResVO> res = new BaseResponse();
        res.setReqNo(orderNoReq.getReqNo());
        if (null == orderNoReq.getAppId()){
            throw new SBCException(StatusEnum.FAIL);
        }
        OrderNoResVO orderNoRes = new OrderNoResVO() ;
        orderNoRes.setOrderId(DateUtil.getLongTime());
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        res.setDataBody(orderNoRes);
        return res ;
    }

    /**
     * 创建订单
     * 复用 sbc-request-check 去重思想: 同一 reqNo 重复提交返回同一订单
     */
    @Override
    public BaseResponse<CreateOrderResVO> createOrder(@RequestBody CreateOrderReqVO req) {
        LOGGER.info("createOrder request: {}", req);

        // 请求号校验 (复用 sbc-request-check 模块的去重逻辑)
        String reqNo = req.getReqNo();
        if (reqNo == null || reqNo.trim().isEmpty()) {
            throw new SBCException(StatusEnum.REPEAT_REQUEST.getCode(), "请求号不能为空");
        }

        // 快速路径: 无锁读已完成的缓存
        BaseResponse<CreateOrderResVO> cached = REQ_NO_CACHE.get(reqNo);
        if (cached != null) {
            LOGGER.info("重复请求 reqNo={}, 返回已有订单", reqNo);
            return cached;
        }

        // 参数校验 (无状态, 放在锁外)
        if (req.getUserId() == null) {
            throw new SBCException(StatusEnum.VALIDATION_FAIL.getCode(), "用户ID不能为空");
        }
        if (req.getProductName() == null || req.getProductName().trim().isEmpty()) {
            throw new SBCException(StatusEnum.VALIDATION_FAIL.getCode(), "商品名称不能为空");
        }
        if (req.getUnitPrice() == null || req.getUnitPrice() <= 0) {
            throw new SBCException(StatusEnum.VALIDATION_FAIL.getCode(), "商品单价必须大于0");
        }
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new SBCException(StatusEnum.VALIDATION_FAIL.getCode(), "购买数量必须大于0");
        }

        // 按 reqNo 粒度加锁, 保证同一 reqNo 并发只落一笔
        Object lock = REQ_LOCKS.computeIfAbsent(reqNo, k -> new Object());
        synchronized (lock) {
            // double-check: 另一个线程可能已经创建完成
            cached = REQ_NO_CACHE.get(reqNo);
            if (cached != null) {
                LOGGER.info("重复请求 reqNo={}, 返回已有订单(并发)", reqNo);
                return cached;
            }

            // 生成订单号
            String orderNo = DateUtil.getLongTime() + "" + ORDER_SEQ.incrementAndGet();

            // 构建订单
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(req.getUserId());
            order.setProductName(req.getProductName());
            order.setUnitPrice(req.getUnitPrice());
            order.setQuantity(req.getQuantity());
            order.setTotalPrice(req.getUnitPrice() * req.getQuantity());
            order.setStatus("COMPLETED");
            order.setCreateTime(DateUtil.getDateStr(DateUtil.getLongTime() * 1000));
            order.setReqNo(reqNo);

            // 存储订单
            ORDER_STORE.put(orderNo, order);

            // 构建响应
            CreateOrderResVO resVO = new CreateOrderResVO();
            resVO.setOrderNo(order.getOrderNo());
            resVO.setUserId(order.getUserId());
            resVO.setProductName(order.getProductName());
            resVO.setUnitPrice(order.getUnitPrice());
            resVO.setQuantity(order.getQuantity());
            resVO.setTotalPrice(order.getTotalPrice());
            resVO.setStatus(order.getStatus());
            resVO.setCreateTime(order.getCreateTime());

            BaseResponse<CreateOrderResVO> response = BaseResponse.createSuccess(resVO, "订单创建成功");
            response.setReqNo(reqNo);

            // 缓存结果，用于重复提交返回同一订单
            REQ_NO_CACHE.put(reqNo, response);

            LOGGER.info("createOrder success: orderNo={}, reqNo={}", orderNo, reqNo);
            return response;
        }
    }

    /**
     * 按订单号查询订单
     */
    @Override
    public BaseResponse<CreateOrderResVO> getOrderByOrderNo(@RequestBody OrderQueryReqVO req) {
        LOGGER.info("getOrderByOrderNo request: {}", req);

        if (req.getOrderNo() == null || req.getOrderNo().trim().isEmpty()) {
            throw new SBCException(StatusEnum.VALIDATION_FAIL.getCode(), "订单号不能为空");
        }

        Order order = ORDER_STORE.get(req.getOrderNo());

        BaseResponse<CreateOrderResVO> response = new BaseResponse<>();
        response.setReqNo(req.getReqNo());

        if (order != null) {
            CreateOrderResVO resVO = convertToResVO(order);
            response.setDataBody(resVO);
            response.setCode(StatusEnum.SUCCESS.getCode());
            response.setMessage("查询成功");
        } else {
            response.setCode(StatusEnum.FAIL.getCode());
            response.setMessage("订单不存在");
        }

        return response;
    }

    /**
     * 按用户ID查询订单列表
     */
    @Override
    public BaseResponse<List<CreateOrderResVO>> getOrdersByUserId(@RequestBody UserOrderQueryReqVO req) {
        LOGGER.info("getOrdersByUserId request: {}", req);

        if (req.getUserId() == null) {
            throw new SBCException(StatusEnum.VALIDATION_FAIL.getCode(), "用户ID不能为空");
        }

        List<CreateOrderResVO> orders = new ArrayList<>();
        for (Order order : ORDER_STORE.values()) {
            if (req.getUserId().equals(order.getUserId())) {
                orders.add(convertToResVO(order));
            }
        }

        BaseResponse<List<CreateOrderResVO>> response = new BaseResponse<>();
        response.setReqNo(req.getReqNo());
        response.setDataBody(orders);
        response.setCode(StatusEnum.SUCCESS.getCode());
        response.setMessage("查询成功，共" + orders.size() + "条订单");

        return response;
    }

    /**
     * Order 实体转 CreateOrderResVO
     */
    private CreateOrderResVO convertToResVO(Order order) {
        CreateOrderResVO resVO = new CreateOrderResVO();
        resVO.setOrderNo(order.getOrderNo());
        resVO.setUserId(order.getUserId());
        resVO.setProductName(order.getProductName());
        resVO.setUnitPrice(order.getUnitPrice());
        resVO.setQuantity(order.getQuantity());
        resVO.setTotalPrice(order.getTotalPrice());
        resVO.setStatus(order.getStatus());
        resVO.setCreateTime(order.getCreateTime());
        return resVO;
    }
}
