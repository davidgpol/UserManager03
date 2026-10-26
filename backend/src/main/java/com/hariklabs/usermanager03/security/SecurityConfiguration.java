package com.hariklabs.usermanager03.security;

import com.hariklabs.usermanager03.rest.Problems;
import com.hariklabs.usermanager03.config.RequestTraceFilter;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.*;
import java.util.Map;

@Configuration
@org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("argon2",Map.of("argon2",new Argon2PasswordEncoder(16,32,1,19456,2)));
    }
    @Bean UserDetailsService operators(@Value("${um03.operator.username}")String username,@Value("${um03.operator.password-hash}")String hash) {
        if(username.isBlank()||!hash.matches("\\{argon2}\\$argon2id\\$v=19\\$m=19456,t=2,p=1\\$[A-Za-z0-9+/]{22}\\$[A-Za-z0-9+/]{43}"))
            throw new IllegalArgumentException("Missing or invalid operator configuration");
        return new InMemoryUserDetailsManager(User.withUsername(username).password(hash).authorities("OPERATOR").build());
    }
    @Bean SecurityFilterChain security(HttpSecurity http,Problems problems,SessionPolicy sessions) throws Exception {
        var csrf=new HttpSessionCsrfTokenRepository();csrf.setHeaderName("X-CSRF-TOKEN");
        http.csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler()))
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).sessionFixation(f->f.changeSessionId()))
            .requestCache(c->c.disable())
            .authorizeHttpRequests(a->a.requestMatchers("/login","/index.html","/assets/**","/favicon.ico","/api/auth/csrf","/api/auth/login").permitAll()
                .requestMatchers("/actuator/health/**").access((authentication,context)->new org.springframework.security.authorization.AuthorizationDecision(context.getRequest().getLocalPort()==8081))
                .anyRequest().authenticated())
            .formLogin(f->f.loginPage("/login").loginProcessingUrl("/api/auth/login")
                .successHandler((req,res,auth)->{sessions.start(req.getSession());res.setStatus(204);res.setHeader("Cache-Control","no-store");})
                .failureHandler((req,res,ex)->problems.write(req,res,401,"INVALID_CREDENTIALS","Usuario o contraseña incorrectos.")))
            .logout(l->l.logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req,res,auth)->{res.setStatus(204);res.setHeader("Cache-Control","no-store");res.addHeader("Set-Cookie",org.springframework.http.ResponseCookie.from("UM03SESSION", "").path("/").secure(true).httpOnly(true).sameSite("Lax").maxAge(0).build().toString());}))
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{
                    if(req.getRequestURI().substring(req.getContextPath().length()).startsWith("/api/"))problems.write(req,res,401,"AUTHENTICATION_REQUIRED","Inicie sesión para continuar.");else res.sendRedirect("/login");
                }).accessDeniedHandler((req,res,ex)->problems.write(req,res,403,ex instanceof CsrfException?"CSRF_INVALID":"ACCESS_DENIED","No se pudo autorizar la operación.")))
            .addFilterBefore(new RequestTraceFilter(),SecurityContextHolderFilter.class)
            .addFilterAfter(new SessionGuard(sessions,problems),CsrfFilter.class);
        return http.build();
    }
}
