package com.hariklabs.usermanager03.security;

import com.hariklabs.usermanager03.rest.Problems;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import java.io.IOException;
import java.util.Set;

/** Runs after CSRF and before logout/login: preserves the specified 403-before-401 precedence. */
public class SessionGuard extends OncePerRequestFilter {
    private final SessionPolicy policy;private final Problems problems;
    public SessionGuard(SessionPolicy policy,Problems problems) {this.policy=policy;this.problems=problems;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String path=req.getRequestURI().substring(req.getContextPath().length());
        var auth=SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated=auth!=null&&auth.isAuthenticated()&&!(auth instanceof AnonymousAuthenticationToken);
        if(authenticated&&!policy.valid(req.getSession(false))) {
            if(req.getSession(false)!=null)req.getSession(false).invalidate();SecurityContextHolder.clearContext();authenticated=false;
            res.addHeader("Set-Cookie","UM03SESSION=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=Lax");
        }
        if(path.equals("/api/auth/logout")&&!authenticated) {problems.write(req,res,401,"AUTHENTICATION_REQUIRED","Inicie sesión para continuar.");return;}
        if(path.equals("/api/auth/login")&&req.getMethod().equals("POST")) {
            String type=req.getContentType();
            if(type==null||!type.split(";",2)[0].equalsIgnoreCase("application/x-www-form-urlencoded")) {problems.write(req,res,415,"UNSUPPORTED_MEDIA_TYPE","Tipo de contenido no admitido.");return;}
            for(String key:req.getParameterMap().keySet()) if(!Set.of("username","password").contains(key)||req.getParameterValues(key).length!=1) {
                problems.write(req,res,400,"VALIDATION_ERROR","Revise los datos de acceso.");return;
            }
            if(req.getParameter("username")==null||req.getParameter("username").isEmpty()||req.getParameter("password")==null||req.getParameter("password").isEmpty()) {
                problems.write(req,res,400,"VALIDATION_ERROR","Complete usuario y contraseña.");return;
            }
        }
        chain.doFilter(req,res);
        if(authenticated&&path.startsWith("/api/")&&res.getStatus()>=200&&res.getStatus()<300) {
            var session=req.getSession(false);if(session!=null)policy.accepted(session);
        }
    }
}
