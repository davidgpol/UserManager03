package com.hariklabs.usermanager03.rest;

import com.hariklabs.usermanager03.rest.generated.api.AuthApi;
import com.hariklabs.usermanager03.rest.generated.model.*;
import com.hariklabs.usermanager03.security.SessionPolicy;
import com.hariklabs.usermanager03.service.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.time.ZoneOffset;

@RestController
public class AuthController implements AuthApi {
    private final HttpServletRequest request;private final SessionPolicy sessions;
    public AuthController(HttpServletRequest request,SessionPolicy sessions) {this.request=request;this.sessions=sessions;}
    @Override public ResponseEntity<CsrfToken> getCsrfToken() {
        var token=(org.springframework.security.web.csrf.CsrfToken)request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        return ResponseEntity.ok(new CsrfToken(CsrfToken.HeaderNameEnum.X_CSRF_TOKEN,token.getToken()));
    }
    @Override public ResponseEntity<Session> getSession() {
        if(!sessions.valid(request.getSession(false)))throw new DomainException(401,"AUTHENTICATION_REQUIRED","Inicie sesión para continuar.");
        return ResponseEntity.ok(new Session(true,sessions.expires(request.getSession(false)).atOffset(ZoneOffset.UTC),sessions.idleSeconds()));
    }
    // The standard security filters consume these contract operations before MVC.
    @Override public ResponseEntity<Void> login(String username,String password) {throw new IllegalStateException("Authentication filter did not consume login");}
    @Override public ResponseEntity<Void> logout() {throw new IllegalStateException("Logout filter did not consume logout");}
}
