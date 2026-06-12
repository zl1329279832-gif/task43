package com.crossoverJie.sbcorder.controller;

import com.crossoverJie.order.api.OrderService;
import com.crossoverJie.order.vo.req.CreateOrderReqVO;
import com.crossoverJie.order.vo.req.OrderNoReqVO;
import com.crossoverJie.order.vo.req.OrderQueryReqVO;
import com.crossoverJie.order.vo.res.OrderDetailResVO;
import com.crossoverJie.order.vo.res.OrderNoResVO;
import com.crossoverJie.request.check.anotation.CheckReqNo;
import com.crossoverJie.sbcorder.common.enums.StatusEnum;
import com.crossoverJie.sbcorder.common.exception.SBCException;
import com.crossoverJie.sbcorder.common.res.BaseResponse;
import com.crossoverJie.sbcorder.common.util.DateUtil;
import com.crossoverJie.sbcorder.common.util.StringUtil;
import com.crossoverjie.distributed.annotation.CommonLimit;
import com.crossoverjie.distributed.annotation.ControllerLimit;
import com.crossoverjie.distributed.limit.RedisLimit;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    private static final AtomicLong ORDER_SEQ = new AtomicLong(0);

    /** 订单存储：orderNo -> OrderDetailResVO */
    private final ConcurrentHashMap<String, OrderDetailResVO> orderStore = new ConcurrentHashMap<>();

    /** 请求号去重映射：reqNo -> orderNo，用于重复提交返回同一订单 */
    private final ConcurrentHashMap<String, String> reqNoIndex = new ConcurrentHashMap<>();

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

    @Override
    public BaseResponse<OrderDetailResVO> createOrder(@RequestBody CreateOrderReqVO createOrderReq) {
        BaseResponse<OrderDetailResVO> res = new BaseResponse<>();

        String reqNo = createOrderReq.getReqNo();

        // 请求号缺失校验
        if (StringUtil.isEmpty(reqNo)) {
            throw new SBCException(StatusEnum.REQ_NO_MISSING);
        }

        // 重复提交检测：相同reqNo返回同一订单（复用sbc-request-check去重思路）
        String existingOrderNo = reqNoIndex.get(reqNo);
        if (existingOrderNo != null) {
            OrderDetailResVO existingOrder = orderStore.get(existingOrderNo);
            if (existingOrder != null) {
                LOGGER.info("重复请求reqNo={}，返回已有订单orderNo={}", reqNo, existingOrderNo);
                res.setReqNo(reqNo);
                res.setCode(StatusEnum.SUCCESS.getCode());
                res.setMessage("重复提交，返回已有订单");
                res.setDataBody(existingOrder);
                return res;
            }
        }

        // 生成订单号
        String orderNo = generateOrderNo();

        // 构建订单
        OrderDetailResVO order = new OrderDetailResVO();
        order.setOrderNo(orderNo);
        order.setUserId(createOrderReq.getUserId());
        order.setProductName(createOrderReq.getProductName());
        order.setProductCount(createOrderReq.getProductCount());
        order.setPrice(createOrderReq.getPrice());
        order.setStatus("CREATED");
        order.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        // 存储订单
        orderStore.put(orderNo, order);
        reqNoIndex.put(reqNo, orderNo);

        LOGGER.info("创建订单成功，orderNo={}，userId={}，reqNo={}", orderNo, createOrderReq.getUserId(), reqNo);

        res.setReqNo(reqNo);
        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        res.setDataBody(order);
        return res;
    }

    @Override
    public BaseResponse<OrderDetailResVO> getOrderByOrderNo(@RequestBody OrderQueryReqVO orderQueryReq) {
        BaseResponse<OrderDetailResVO> res = new BaseResponse<>();
        res.setReqNo(orderQueryReq.getReqNo());

        String orderNo = orderQueryReq.getOrderNo();
        if (StringUtil.isEmpty(orderNo)) {
            throw new SBCException(StatusEnum.FAIL.getCode(), "订单号不能为空");
        }

        OrderDetailResVO order = orderStore.get(orderNo);
        if (order == null) {
            throw new SBCException(StatusEnum.ORDER_NOT_FOUND);
        }

        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        res.setDataBody(order);
        return res;
    }

    @Override
    public BaseResponse<List<OrderDetailResVO>> getOrdersByUserId(@RequestBody OrderQueryReqVO orderQueryReq) {
        BaseResponse<List<OrderDetailResVO>> res = new BaseResponse<>();
        res.setReqNo(orderQueryReq.getReqNo());

        Long userId = orderQueryReq.getUserId();
        if (userId == null) {
            throw new SBCException(StatusEnum.FAIL.getCode(), "用户ID不能为空");
        }

        List<OrderDetailResVO> userOrders = new ArrayList<>();
        for (OrderDetailResVO order : orderStore.values()) {
            if (userId.equals(order.getUserId())) {
                userOrders.add(order);
            }
        }

        res.setCode(StatusEnum.SUCCESS.getCode());
        res.setMessage(StatusEnum.SUCCESS.getMessage());
        res.setDataBody(userOrders);
        return res;
    }

    private String generateOrderNo() {
        long seq = ORDER_SEQ.incrementAndGet();
        return "ORD" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + String.format("%06d", seq);
    }
}
