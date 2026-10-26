package com.hariklabs.usermanager03.config;

import com.hariklabs.usermanager03.service.DomainException;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.http.MediaType;
import jakarta.servlet.http.*;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler) {
                String path=req.getRequestURI().substring(req.getContextPath().length());
                boolean json=(path.startsWith("/api/users")&&!req.getMethod().equals("DELETE")) || path.equals("/api/auth/csrf") || path.equals("/api/auth/session");
                String accept=req.getHeader("Accept");
                // Generator lists error media types too; successful representations remain application/json only.
                if(json&&accept!=null&&MediaType.parseMediaTypes(accept).stream().noneMatch(m->m.getQualityValue()>0&&m.isCompatibleWith(MediaType.APPLICATION_JSON)))
                    throw new DomainException(406,"NOT_ACCEPTABLE","La representación solicitada no está disponible.");
                return true;
            }
        }).addPathPatterns("/api/**");
    }
}
