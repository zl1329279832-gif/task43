package com.crossoverjie.gateway.zuul.filter;

import com.crossoverJie.sbcorder.common.util.StringUtil;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Function: 请求拦截
 *
 * @author crossoverJie
 *         Date: 2017/11/20 00:33
 * @since JDK 1.8
 */
public class RequestFilter extends ZuulFilter {
    private Logger logger = LoggerFactory.getLogger(RequestFilter.class) ;
    /**
     * 请求路由之前被拦截 实现 pre 拦截器
     * @return
     */
    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {

        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();
        String token = request.getParameter("token");
        if (StringUtil.isEmpty(token)){
            logger.warn("need token");
            //过滤请求
            currentContext.setSendZuulResponse(false);
            currentContext.setResponseStatusCode(401);
            return null ;
        }
        logger.info("token ={}",token) ;

        // 透传请求号reqNo到下游服务
        String reqNo = request.getParameter("reqNo");
        if (reqNo == null) {
            reqNo = request.getHeader("reqNo");
        }
        if (!StringUtil.isEmpty(reqNo)) {
            currentContext.addZuulRequestHeader("reqNo", reqNo);
            logger.info("透传reqNo={}", reqNo);
        }

        // 透传用户标识userId到下游服务
        String userId = request.getParameter("userId");
        if (userId == null) {
            userId = request.getHeader("userId");
        }
        if (!StringUtil.isEmpty(userId)) {
            currentContext.addZuulRequestHeader("userId", userId);
            logger.info("透传userId={}", userId);
        }

        return null;
    }
}
