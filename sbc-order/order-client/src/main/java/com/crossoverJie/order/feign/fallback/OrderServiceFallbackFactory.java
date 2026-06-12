package com.crossoverJie.order.feign.fallback;

import com.crossoverJie.order.feign.api.OrderServiceClient;
import com.crossoverJie.order.vo.req.CreateOrderReqVO;
import com.crossoverJie.order.vo.req.OrderNoReqVO;
import com.crossoverJie.order.vo.req.OrderQueryReqVO;
import com.crossoverJie.order.vo.req.UserOrderQueryReqVO;
import com.crossoverJie.order.vo.res.CreateOrderResVO;
import com.crossoverJie.order.vo.res.OrderNoResVO;
import com.crossoverJie.sbcorder.common.enums.StatusEnum;
import com.crossoverJie.sbcorder.common.res.BaseResponse;
import feign.hystrix.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.List;

/**
 * Function:查看fallback原因
 *
 * @author crossoverJie
 *         Date: 2017/9/4 00:45
 * @since JDK 1.8
 */
public class OrderServiceFallbackFactory implements FallbackFactory<OrderServiceClient>{

    private final static Logger LOGGER = LoggerFactory.getLogger(OrderServiceFallbackFactory.class);


    @Override
    public OrderServiceClient create(Throwable throwable) {

        return new OrderServiceClient() {
            @Override
            public BaseResponse<OrderNoResVO> getOrderNo(@RequestBody OrderNoReqVO orderNoReq) {
                LOGGER.error("fallback:" + throwable);

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
            public BaseResponse<CreateOrderResVO> createOrder(@RequestBody CreateOrderReqVO req) {
                LOGGER.error("createOrder fallback:" + throwable);
                BaseResponse<CreateOrderResVO> baseResponse = new BaseResponse<>();
                baseResponse.setCode(StatusEnum.FALLBACK.getCode());
                baseResponse.setMessage("订单服务降级，请稍后重试");
                return baseResponse;
            }

            @Override
            public BaseResponse<CreateOrderResVO> getOrderByOrderNo(@RequestBody OrderQueryReqVO req) {
                LOGGER.error("getOrderByOrderNo fallback:" + throwable);
                BaseResponse<CreateOrderResVO> baseResponse = new BaseResponse<>();
                baseResponse.setCode(StatusEnum.FALLBACK.getCode());
                baseResponse.setMessage("订单服务降级，请稍后重试");
                return baseResponse;
            }

            @Override
            public BaseResponse<List<CreateOrderResVO>> getOrdersByUserId(@RequestBody UserOrderQueryReqVO req) {
                LOGGER.error("getOrdersByUserId fallback:" + throwable);
                BaseResponse<List<CreateOrderResVO>> baseResponse = new BaseResponse<>();
                baseResponse.setCode(StatusEnum.FALLBACK.getCode());
                baseResponse.setMessage("订单服务降级，请稍后重试");
                baseResponse.setDataBody(Collections.emptyList());
                return baseResponse;
            }
        };
    }
}
