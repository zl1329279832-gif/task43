package com.crossoverjie.gateway.zuul.filter;

import com.crossoverJie.sbcorder.common.util.StringUtil;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Function: 请求头透传过滤器
 * 将请求号 (reqNo) 和用户标识 (userId) 透传给下游服务
 *
 * @author crossoverJie
 * Date: 2018/10/14
 * @since JDK 1.8
 */
public class HeaderPassthroughFilter extends ZuulFilter {

    private Logger logger = LoggerFactory.getLogger(HeaderPassthroughFilter.class);

    public static final String HEADER_REQ_NO = "X-Request-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String PARAM_REQ_NO = "reqNo";
    public static final String PARAM_USER_ID = "userId";

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        // 在 RequestFilter (order=0) 之后执行
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        // 透传请求号: 优先从请求头获取，其次从参数获取
        String reqNo = request.getHeader(HEADER_REQ_NO);
        if (StringUtil.isEmpty(reqNo)) {
            reqNo = request.getParameter(PARAM_REQ_NO);
        }
        if (StringUtil.isNotEmpty(reqNo)) {
            ctx.addZuulRequestHeader(HEADER_REQ_NO, reqNo);
            logger.info("透传请求号: {}={}", HEADER_REQ_NO, reqNo);
        }

        // 透传用户标识: 优先从请求头获取，其次从参数获取
        String userId = request.getHeader(HEADER_USER_ID);
        if (StringUtil.isEmpty(userId)) {
            userId = request.getParameter(PARAM_USER_ID);
        }
        if (StringUtil.isNotEmpty(userId)) {
            ctx.addZuulRequestHeader(HEADER_USER_ID, userId);
            logger.info("透传用户标识: {}={}", HEADER_USER_ID, userId);
        }

        return null;
    }
}
