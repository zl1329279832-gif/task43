package com.crossoverJie.order.feign.fallback;

import com.crossoverJie.order.feign.api.OrderServiceClient;
import com.crossoverJie.order.vo.req.CreateOrderReqVO;
import com.crossoverJie.order.vo.req.OrderNoReqVO;
import com.crossoverJie.order.vo.req.OrderQueryReqVO;
import com.crossoverJie.order.vo.res.OrderDetailResVO;
import com.crossoverJie.order.vo.res.OrderNoResVO;
import com.crossoverJie.sbcorder.common.enums.StatusEnum;
import com.crossoverJie.sbcorder.common.res.BaseResponse;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.List;

/**
 * Function:
 *
 * @author crossoverJie
 *         Date: 2017/9/3 20:52
 * @since JDK 1.8
 */
public class OrderServiceFallBack implements OrderServiceClient {
    @Override
    public BaseResponse<OrderNoResVO> getOrderNo(@RequestBody OrderNoReqVO orderNoReq) {
        BaseResponse<OrderNoResVO> baseResponse = new BaseResponse<>() ;
        OrderNoResVO vo = new OrderNoResVO() ;
        vo.setOrderId(123456L);
        baseResponse.setDataBody(vo);
        baseResponse.setMessage(StatusEnum.FALLBACK.getMessage());
        baseResponse.setCode(StatusEnum.FALLBACK.getCode());
        return baseResponse;
    }

    @Override
    public BaseResponse<OrderNoResVO> getOrderNoLimit(@RequestBody OrderNoReqVO orderNoReq) {
        return null;
    }

    @Override
    public BaseResponse<OrderNoResVO> getOrderNoCommonLimit(@RequestBody OrderNoReqVO orderNoReq) {
        return null;
    }

    @Override
    public BaseResponse<OrderDetailResVO> createOrder(@RequestBody CreateOrderReqVO createOrderReq) {
        BaseResponse<OrderDetailResVO> baseResponse = new BaseResponse<>();
        baseResponse.setReqNo(createOrderReq.getReqNo());
        baseResponse.setMessage(StatusEnum.FALLBACK.getMessage());
        baseResponse.setCode(StatusEnum.FALLBACK.getCode());
        return baseResponse;
    }

    @Override
    public BaseResponse<OrderDetailResVO> getOrderByOrderNo(@RequestBody OrderQueryReqVO orderQueryReq) {
        BaseResponse<OrderDetailResVO> baseResponse = new BaseResponse<>();
        baseResponse.setReqNo(orderQueryReq.getReqNo());
        baseResponse.setMessage(StatusEnum.FALLBACK.getMessage());
        baseResponse.setCode(StatusEnum.FALLBACK.getCode());
        return baseResponse;
    }

    @Override
    public BaseResponse<List<OrderDetailResVO>> getOrdersByUserId(@RequestBody OrderQueryReqVO orderQueryReq) {
        BaseResponse<List<OrderDetailResVO>> baseResponse = new BaseResponse<>();
        baseResponse.setReqNo(orderQueryReq.getReqNo());
        baseResponse.setDataBody(Collections.<OrderDetailResVO>emptyList());
        baseResponse.setMessage(StatusEnum.FALLBACK.getMessage());
        baseResponse.setCode(StatusEnum.FALLBACK.getCode());
        return baseResponse;
    }
}
