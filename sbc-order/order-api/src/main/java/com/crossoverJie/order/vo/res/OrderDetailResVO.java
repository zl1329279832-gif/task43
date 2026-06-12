package com.crossoverJie.order.vo.res;

import java.io.Serializable;

/**
 * Function: 订单详情响应
 *
 * @author crossoverJie
 *         Date: 2017/7/16 19:48
 * @since JDK 1.8
 */
public class OrderDetailResVO implements Serializable {

    private String orderNo;

    private Long userId;

    private String productName;

    private Integer productCount;

    private Long price;

    private String status;

    private String createTime;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
