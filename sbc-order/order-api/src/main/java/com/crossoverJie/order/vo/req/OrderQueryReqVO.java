package com.crossoverJie.order.vo.req;

import com.crossoverJie.sbcorder.common.req.BaseRequest;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

/**
 * Function: 按订单号查询请求VO
 *
 * @author crossoverJie
 * Date: 2018/10/14
 * @since JDK 1.8
 */
public class OrderQueryReqVO extends BaseRequest {

    @NotNull(message = "订单号不能为空")
    @ApiModelProperty(required = true, value = "订单号", example = "1697000000")
    private String orderNo;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    @Override
    public String toString() {
        return "OrderQueryReqVO{" +
                "orderNo='" + orderNo + '\'' +
                '}';
    }
}
