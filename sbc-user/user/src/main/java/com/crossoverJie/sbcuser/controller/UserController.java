package com.crossoverJie.sbcuser.controller;

import com.alibaba.fastjson.JSON;
import com.crossoverJie.order.feign.api.OrderServiceClient;
import com.crossoverJie.order.vo.req.CreateOrderReqVO;
import com.crossoverJie.order.vo.req.OrderNoReqVO;
import com.crossoverJie.order.vo.res.OrderDetailResVO;
import com.crossoverJie.order.vo.res.OrderNoResVO;
import com.crossoverJie.sbcorder.common.enums.StatusEnum;
import com.crossoverJie.sbcorder.common.exception.SBCException;
import com.crossoverJie.sbcorder.common.res.BaseResponse;
import com.crossoverJie.sbcorder.common.util.StringUtil;
import com.crossoverJie.sbcuser.req.OrderNoReq;
import com.crossoverJie.sbcuser.res.UserRes;
import com.crossoverJie.user.api.UserService;
import com.crossoverJie.user.vo.req.CreateOrderUserReqVO;
import com.crossoverJie.user.vo.req.UserReqVO;
import com.crossoverJie.user.vo.res.UserResVO;
import com.google.common.util.concurrent.RateLimiter;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Function:user控制器
 *
 * @author crossoverJie
 *         Date: 2017/6/7 下午11:55
 * @since JDK 1.8
 */
@RestController
@Api(value = "userApi", description = "用户API", tags = {"用户服务"})
public class UserController implements UserService {
    private final static Logger logger = LoggerFactory.getLogger(UserController.class);

    //@Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OrderServiceClient orderServiceClient;

    private final static int COUNT = 10;

    @Resource(name = "concurrentTestThread")
    private ExecutorService executorService;

    /** 模拟用户存储 */
    private static final Map<Long, String> USER_STORE = new HashMap<>();
    static {
        USER_STORE.put(1L, "张三");
        USER_STORE.put(2L, "李四");
        USER_STORE.put(3L, "王五");
        USER_STORE.put(1001L, "赵六");
        USER_STORE.put(1002L, "孙七");
    }


    @Override
    public BaseResponse<UserResVO> getOrderNo(@RequestBody UserReqVO userReq) {
        OrderNoReq req = new OrderNoReq();
        req.setReqNo("1213");
        //调用远程服务
        ResponseEntity<Object> res = restTemplate.postForEntity("http://sbc-order/order/getOrderNo", req, Object.class);
        logger.info("res=" + JSON.toJSONString(res));

        logger.debug("入参=" + JSON.toJSONString(userReq));
        UserRes userRes = new UserRes();
        userRes.setUserId(123);
        userRes.setUserName("张三");

        userRes.setReqNo(userReq.getReqNo());
        userRes.setCode(StatusEnum.SUCCESS.getCode());
        userRes.setMessage("成功");

        return userRes;
    }

    @Override
    public BaseResponse<UserResVO> getUserByFeign(@RequestBody UserReqVO userReq) {
        //调用远程服务
        OrderNoReqVO vo = new OrderNoReqVO();
        vo.setAppId(1L);
        vo.setReqNo(userReq.getReqNo());

        for (int i = 0; i < 10; i++) {
            executorService.execute(new Worker(vo, orderServiceClient));
        }

        UserRes userRes = new UserRes();
        userRes.setUserId(123);
        userRes.setUserName("张三");

        userRes.setReqNo(userReq.getReqNo());
        userRes.setCode(StatusEnum.SUCCESS.getCode());
        userRes.setMessage("成功");

        return userRes;
    }

    @Override
    public BaseResponse<UserResVO> getUserByFeignBatch(@RequestBody UserReqVO userReqVO) {
        //调用远程服务
        OrderNoReqVO vo = new OrderNoReqVO();
        vo.setReqNo(userReqVO.getReqNo());
        vo.setAppId(1L);

        RateLimiter limiter = RateLimiter.create(2.0);
        //批量调用
        for (int i = 0; i < COUNT; i++) {
            double acquire = limiter.acquire();
            logger.debug("获取令牌成功!,消耗=" + acquire);
            BaseResponse<OrderNoResVO> orderNo = orderServiceClient.getOrderNo(vo);
            logger.debug("远程返回:" + JSON.toJSONString(orderNo));
        }

        UserRes userRes = new UserRes();
        userRes.setUserId(123);
        userRes.setUserName("张三");

        userRes.setReqNo(userReqVO.getReqNo());
        userRes.setCode(StatusEnum.SUCCESS.getCode());
        userRes.setMessage("成功");

        return userRes;
    }


    @Override
    public BaseResponse<OrderNoResVO> getUserByHystrix(@RequestBody UserReqVO userReqVO) {

        OrderNoReqVO vo = new OrderNoReqVO();
        vo.setAppId(123L);
        vo.setReqNo(userReqVO.getReqNo());
        BaseResponse<OrderNoResVO> orderNo = orderServiceClient.getOrderNo(vo);
        return orderNo;
    }

    @Override
    public BaseResponse<OrderDetailResVO> createOrder(@RequestBody CreateOrderUserReqVO createOrderUserReqVO) {
        logger.info("用户下单请求，userId={}，reqNo={}", createOrderUserReqVO.getUserId(), createOrderUserReqVO.getReqNo());

        // 请求号校验
        if (StringUtil.isEmpty(createOrderUserReqVO.getReqNo())) {
            throw new SBCException(StatusEnum.REQ_NO_MISSING);
        }

        // 用户存在性校验
        Long userId = createOrderUserReqVO.getUserId();
        if (userId == null || !USER_STORE.containsKey(userId)) {
            throw new SBCException(StatusEnum.USER_NOT_FOUND);
        }

        // 构建订单服务请求
        CreateOrderReqVO orderReq = new CreateOrderReqVO();
        orderReq.setReqNo(createOrderUserReqVO.getReqNo());
        orderReq.setUserId(userId);
        orderReq.setProductName(createOrderUserReqVO.getProductName());
        orderReq.setProductCount(createOrderUserReqVO.getProductCount());
        orderReq.setPrice(createOrderUserReqVO.getPrice());

        // 通过Feign调用订单服务（Hystrix降级由fallbackFactory处理）
        BaseResponse<OrderDetailResVO> orderRes = orderServiceClient.createOrder(orderReq);

        logger.info("订单服务返回：{}", JSON.toJSONString(orderRes));

        // 透传reqNo
        if (orderRes != null) {
            orderRes.setReqNo(createOrderUserReqVO.getReqNo());
        }

        return orderRes;
    }


    private static class Worker implements Runnable {

        private OrderNoReqVO vo;
        private OrderServiceClient orderServiceClient;

        public Worker(OrderNoReqVO vo, OrderServiceClient orderServiceClient) {
            this.vo = vo;
            this.orderServiceClient = orderServiceClient;
        }

        @Override
        public void run() {

            BaseResponse<OrderNoResVO> orderNo = orderServiceClient.getOrderNoCommonLimit(vo);
            logger.info("远程返回:" + JSON.toJSONString(orderNo));

        }
    }

}
