package com.crossoverJie.order.feign.api;

import com.crossoverJie.order.api.OrderService;
import com.crossoverJie.order.feign.config.OrderConfig;
import com.crossoverJie.order.feign.fallback.OrderServiceFallbackFactory;
import com.crossoverJie.order.vo.req.CreateOrderReqVO;
import com.crossoverJie.order.vo.req.OrderNoReqVO;
import com.crossoverJie.order.vo.req.OrderQueryReqVO;
import com.crossoverJie.order.vo.res.OrderDetailResVO;
import com.crossoverJie.order.vo.res.OrderNoResVO;
import com.crossoverJie.sbcorder.common.res.BaseResponse;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * Function:
 *
 * @author crossoverJie
 *         Date: 2017/7/16 19:00
 * @since JDK 1.8
 */
@RequestMapping(value="/orderService")
@FeignClient(name="sbc-order",
        fallbackFactory = OrderServiceFallbackFactory.class,
        configuration = OrderConfig.class
)
@RibbonClient
public interface OrderServiceClient extends OrderService{


    /**
     * 获取订单号
     * @param orderNoReq
     * @return
     */
    @Override
    @ApiOperation("获取订单号")
    @RequestMapping(value = "/getOrderNo", method = RequestMethod.POST)
    BaseResponse<OrderNoResVO> getOrderNo(@RequestBody OrderNoReqVO orderNoReq) ;



    /**
     * 限流获取订单号
     * @param orderNoReq
     * @return
     */
    @Override
    @ApiOperation("限流获取订单号")
    @RequestMapping(value = "/getOrderNoLimit", method = RequestMethod.POST)
    BaseResponse<OrderNoResVO> getOrderNoLimit(@RequestBody OrderNoReqVO orderNoReq) ;


    /**
     * 通用限流获取订单号
     * @param orderNoReq
     * @return
     */
    @Override
    @ApiOperation("通用限流获取订单号")
    @RequestMapping(value = "/getOrderNoCommonLimit", method = RequestMethod.POST)
    BaseResponse<OrderNoResVO> getOrderNoCommonLimit(@RequestBody OrderNoReqVO orderNoReq) ;

    /**
     * 创建订单
     * @param createOrderReq
     * @return
     */
    @Override
    @ApiOperation("创建订单")
    @RequestMapping(value = "/createOrder", method = RequestMethod.POST)
    BaseResponse<OrderDetailResVO> createOrder(@RequestBody CreateOrderReqVO createOrderReq) ;

    /**
     * 按订单号查询订单
     * @param orderQueryReq
     * @return
     */
    @Override
    @ApiOperation("按订单号查询订单")
    @RequestMapping(value = "/getOrderByOrderNo", method = RequestMethod.POST)
    BaseResponse<OrderDetailResVO> getOrderByOrderNo(@RequestBody OrderQueryReqVO orderQueryReq) ;

    /**
     * 按用户ID查询订单列表
     * @param orderQueryReq
     * @return
     */
    @Override
    @ApiOperation("按用户ID查询订单列表")
    @RequestMapping(value = "/getOrdersByUserId", method = RequestMethod.POST)
    BaseResponse<List<OrderDetailResVO>> getOrdersByUserId(@RequestBody OrderQueryReqVO orderQueryReq) ;
}
