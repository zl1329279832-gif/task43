package com.crossoverJie.order.vo.req;

import com.crossoverJie.sbcorder.common.req.BaseRequest;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

/**
 * Function: 创建订单请求
 *
 * @author crossoverJie
 *         Date: 2017/7/16 19:48
 * @since JDK 1.8
 */
public class CreateOrderReqVO extends BaseRequest {

    @NotNull(message = "用户ID不能为空")
    @ApiModelProperty(required = true, value = "用户ID", example = "1001")
    private Long userId;

    @NotNull(message = "商品名称不能为空")
    @ApiModelProperty(required = true, value = "商品名称", example = "iPhone")
    private String productName;

    @NotNull(message = "商品数量不能为空")
    @ApiModelProperty(required = true, value = "商品数量", example = "1")
    private Integer productCount;

    @NotNull(message = "商品单价不能为空")
    @ApiModelProperty(required = true, value = "商品单价(分)", example = "999900")
    private Long price;

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

    public Integer getProductCount() {
        return productCount;
    }

    public void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }
}
