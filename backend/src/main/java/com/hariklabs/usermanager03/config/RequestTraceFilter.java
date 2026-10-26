package com.hariklabs.usermanager03.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.*;
import java.io.IOException;
import java.util.UUID;

public class RequestTraceFilter extends OncePerRequestFilter {
    private static final Logger LOG=LoggerFactory.getLogger(RequestTraceFilter.class);
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String received=req.getHeader("X-Correlation-ID");
        String id=received!=null&&received.matches("[A-Za-z0-9_-]{1,64}")?received:UUID.randomUUID().toString();
        String path=req.getRequestURI().substring(req.getContextPath().length());
        String flow=path.startsWith("/api/auth/")?"operator_auth":path.startsWith("/api/users")?"user_records":"navigation";
        var previous=MDC.getCopyOfContextMap();
        try {
            MDC.put("correlation_id",id);MDC.put("flow",flow);MDC.put("event_type","request");
            req.setAttribute("requestId",UUID.randomUUID().toString());res.setHeader("X-Correlation-ID",id);
            if(path.startsWith("/api/"))res.setHeader("Cache-Control","no-store");
            chain.doFilter(req,res);
            LOG.atInfo().addKeyValue("http_status",res.getStatus()).log("Petición completada");
        } finally {if(previous==null)MDC.clear();else MDC.setContextMap(previous);}
    }
}
