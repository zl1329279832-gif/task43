package com.crossoverJie.order.vo.req;

import com.crossoverJie.sbcorder.common.req.BaseRequest;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

/**
 * Function: 按用户ID查询订单列表请求VO
 *
 * @author crossoverJie
 * Date: 2018/10/14
 * @since JDK 1.8
 */
public class UserOrderQueryReqVO extends BaseRequest {

    @NotNull(message = "用户ID不能为空")
    @ApiModelProperty(required = true, value = "用户ID", example = "1001")
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "UserOrderQueryReqVO{" +
                "userId=" + userId +
                '}';
    }
}
