package com.crossoverJie.sbcuser.config;

import com.crossoverJie.order.feign.api.OrderServiceClient;
import com.crossoverJie.order.vo.req.CreateOrderReqVO;
import com.crossoverJie.order.vo.res.CreateOrderResVO;
import com.crossoverJie.sbcorder.common.enums.StatusEnum;
import com.crossoverJie.sbcorder.common.res.BaseResponse;
import com.crossoverJie.sbcorder.common.util.DateUtil;
import org.powermock.api.mockito.PowerMockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Function: 本地环境 Feign 客户端 mock 配置
 *
 * @author crossoverJie
 *         Date: 2018/10/14 20:51
 * @since JDK 1.8
 */
@Component
public class OrderMockServiceConfig implements CommandLineRunner {

    private final static Logger logger = LoggerFactory.getLogger(OrderMockServiceConfig.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${excute.env}")
    private String env;

    @Override
    public void run(String... strings) throws Exception {

        // 非本地环境不做处理
        if ("dev".equals(env) || "test".equals(env) || "pro".equals(env)) {
            return;
        }

        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        OrderServiceClient orderServiceClient = defaultListableBeanFactory.getBean(OrderServiceClient.class);
        logger.info("======orderServiceClient {}=====", orderServiceClient.getClass());

        defaultListableBeanFactory.removeBeanDefinition(OrderServiceClient.class.getCanonicalName());

        OrderServiceClient mockOrderApi = PowerMockito.mock(OrderServiceClient.class, invocation -> {
            String methodName = invocation.getMethod().getName();
            logger.info("mock method: {}", methodName);

            if ("createOrder".equals(methodName)) {
                CreateOrderReqVO req = (CreateOrderReqVO) invocation.getArguments()[0];
                CreateOrderResVO resVO = new CreateOrderResVO();
                resVO.setOrderNo(DateUtil.getLongTime() + "");
                resVO.setUserId(req.getUserId());
                resVO.setProductName(req.getProductName());
                resVO.setUnitPrice(req.getUnitPrice());
                resVO.setQuantity(req.getQuantity());
                resVO.setTotalPrice(req.getUnitPrice() * req.getQuantity());
                resVO.setStatus("COMPLETED");
                resVO.setCreateTime(DateUtil.getDateStr(DateUtil.getLongTime() * 1000));

                BaseResponse<CreateOrderResVO> response = BaseResponse.createSuccess(resVO, "mock 下单成功");
                response.setReqNo(req.getReqNo());
                return response;
            }

            if ("getOrderByOrderNo".equals(methodName)) {
                BaseResponse<CreateOrderResVO> response = new BaseResponse<>();
                response.setCode(StatusEnum.SUCCESS.getCode());
                response.setMessage("mock 查询成功");
                return response;
            }

            if ("getOrdersByUserId".equals(methodName)) {
                BaseResponse<java.util.List<CreateOrderResVO>> response = new BaseResponse<>();
                response.setCode(StatusEnum.SUCCESS.getCode());
                response.setMessage("mock 查询成功");
                response.setDataBody(Collections.emptyList());
                return response;
            }

            // 默认: 原有 getOrderNo 系列方法的 mock 响应
            return BaseResponse.createSuccess(DateUtil.getLongTime() + "", "mock orderNo success");
        });

        defaultListableBeanFactory.registerSingleton(OrderServiceClient.class.getCanonicalName(), mockOrderApi);

        logger.info("======mockOrderApi {}=====", mockOrderApi.getClass());
    }
}
