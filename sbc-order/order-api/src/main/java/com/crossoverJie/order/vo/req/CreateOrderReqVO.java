package com.crossoverJie.order.vo.req;

import com.crossoverJie.sbcorder.common.req.BaseRequest;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

/**
 * Function: 创建订单请求VO
 *
 * @author crossoverJie
 * Date: 2018/10/14
 * @since JDK 1.8
 */
public class CreateOrderReqVO extends BaseRequest {

    @NotNull(message = "用户ID不能为空")
    @ApiModelProperty(required = true, value = "用户ID", example = "1001")
    private Long userId;

    @NotNull(message = "商品名称不能为空")
    @ApiModelProperty(required = true, value = "商品名称", example = "iPhone 15")
    private String productName;

    @NotNull(message = "商品单价不能为空")
    @ApiModelProperty(required = true, value = "商品单价(元)", example = "5999.00")
    private Double unitPrice;

    @NotNull(message = "购买数量不能为空")
    @ApiModelProperty(required = true, value = "购买数量", example = "2")
    private Integer quantity;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CreateOrderReqVO{" +
                "userId=" + userId +
                ", productName='" + productName + '\'' +
                ", unitPrice=" + unitPrice +
                ", quantity=" + quantity +
                '}';
    }
}
