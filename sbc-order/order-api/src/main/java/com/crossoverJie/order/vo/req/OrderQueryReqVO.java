package com.crossoverJie.order.vo.req;

import com.crossoverJie.sbcorder.common.req.BaseRequest;
import io.swagger.annotations.ApiModelProperty;

/**
 * Function: 订单查询请求
 *
 * @author crossoverJie
 *         Date: 2017/7/16 19:48
 * @since JDK 1.8
 */
public class OrderQueryReqVO extends BaseRequest {

    @ApiModelProperty(required = false, value = "订单号", example = "ORD20170716000001")
    private String orderNo;

    @ApiModelProperty(required = false, value = "用户ID", example = "1001")
    private Long userId;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
