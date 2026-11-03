package com.hariklabs.usermanager03.rest;

import com.hariklabs.usermanager03.config.JsonConfiguration;
import com.hariklabs.usermanager03.config.SafeJsonEncoder;
import com.hariklabs.usermanager03.rest.mapper.UserRestMapper;
import com.hariklabs.usermanager03.security.*;
import com.hariklabs.usermanager03.service.*;
import com.hariklabs.usermanager03.service.api.UserService;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebMvcTest({UsersController.class,AuthController.class})
@Import({SecurityConfiguration.class,SessionPolicy.class,Problems.class,UserRestMapper.class,JsonConfiguration.class,com.hariklabs.usermanager03.config.WebConfiguration.class})
class HttpContractTest {
    @MockitoBean UserService service;
    @Autowired MockMvc mvc; @Autowired JsonMapper json;
    private static final String PASSWORD=UUID.randomUUID().toString();
    private static final String HASH="{argon2}"+new Argon2PasswordEncoder(16,32,1,19456,2).encode(PASSWORD);
    @DynamicPropertySource static void config(DynamicPropertyRegistry r) {
        r.add("um03.operator.username",()->"synthetic-operator");r.add("um03.operator.password-hash",()->HASH);
    }
    record Access(MockHttpSession session,String token) {}
    Access csrf(MockHttpSession session)throws Exception {
        var request=get("/api/auth/csrf");if(session!=null)request.session(session);
        var response=mvc.perform(request).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn();
        return new Access((MockHttpSession)response.getRequest().getSession(),json.readTree(response.getResponse().getContentAsString()).get("token").asText());
    }
    Access login()throws Exception {
        var access=csrf(null);String old=access.session().getId();
        mvc.perform(post("/api/auth/login").session(access.session()).with(r->{r.setRequestedSessionId(old);r.setRequestedSessionIdValid(true);return r;}).header("X-CSRF-TOKEN",access.token()).contentType("application/x-www-form-urlencoded").param("username","synthetic-operator").param("password",PASSWORD)).andExpect(status().isNoContent());
        assertNotEquals(old,access.session().getId());return csrf(access.session());
    }
    @Test void csrfPrecedesAuthenticationAndLoginLogoutPersistContext()throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mvc.perform(post("/api/users").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        var anon=csrf(null);
        mvc.perform(post("/api/users").session(anon.session()).header("X-CSRF-TOKEN",anon.token()).contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").session(anon.session()).header("X-CSRF-TOKEN",anon.token()).contentType("application/x-www-form-urlencoded").param("username","unknown").param("password","incorrect"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        var access=login();
        mvc.perform(get("/api/auth/session").session(access.session())).andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(true));
        mvc.perform(post("/api/auth/logout").session(access.session())).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(access.session()).header("X-CSRF-TOKEN",access.token())).andExpect(status().isNoContent())
            .andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("SameSite=Lax")));
        assertTrue(access.session().isInvalid());verifyNoInteractions(service);
    }
    @Test void strictJsonRejectsCoercionReadonlyUnknownAndDuplicateKeys()throws Exception {
        var a=login();String base="\"firstName\":\"Ana\",\"lastName\":\"Prueba\",\"dni\":\"00000000T\",\"email\":\"a@example.test\"";
        for(String field:List.of("id","active","createdAt","updatedAt")) {
            mvc.perform(post("/api/users").session(a.session()).header("X-CSRF-TOKEN",a.token()).contentType("application/json").content("{"+base+",\""+field+"\":\"ignored\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].code").value("READ_ONLY_FIELD"));
        }
        mvc.perform(post("/api/users").session(a.session()).header("X-CSRF-TOKEN",a.token()).contentType("application/json").content("{"+base+",\"private-unknown\":true}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("$"));
        mvc.perform(post("/api/users").session(a.session()).header("X-CSRF-TOKEN",a.token()).contentType("application/json").content("{"+base+",\"firstName\":\"B\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
        mvc.perform(post("/api/users").session(a.session()).header("X-CSRF-TOKEN",a.token()).contentType("application/json").content("{"+base.replace("\"Ana\"","1")+"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].code").value("INVALID_TYPE"));
        verifyNoInteractions(service);
    }
    @Test void responseNegotiationPreservesJsonAndProblemTypes() throws Exception {
        var a=login();
        mvc.perform(get("/api/users").session(a.session()).accept("application/problem+json")).andExpect(status().isNotAcceptable()).andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
        mvc.perform(get("/api/users").session(a.session()).accept("text/html")).andExpect(status().isNotAcceptable()).andExpect(content().string(""));
        verifyNoInteractions(service);
    }
    @Test void invalidQueryAndIdHaveContractCodes()throws Exception {
        var a=login();
        mvc.perform(get("/api/users").session(a.session()).param("page","-1")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_QUERY"));
        mvc.perform(get("/api/users/short").session(a.session())).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ID"))
            .andExpect(jsonPath("$.errors[0].location").value("path"))
            .andExpect(jsonPath("$.errors[0].field").value("id"));
        mvc.perform(get("/api/users").session(a.session()).param("sort","id")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_QUERY"));
        verifyNoInteractions(service);
    }
    @Test void validRouteIdReachesServiceAsUuidAndMissingRecordRemains404()throws Exception {
        var a=login();UUID id=UUID.fromString("A0B1C2D3-1234-5678-9ABC-DEF012345678");
        when(service.get(id)).thenThrow(new DomainException("USER_NOT_FOUND","La ficha no existe."));
        mvc.perform(get("/api/users/"+id).session(a.session())).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        verify(service).get(id);
    }
    @Test void unexpectedFailureReturnsGenericProblemAndLogsOneSanitizedTrace()throws Exception {
        var a=login();UUID id=UUID.fromString("a0b1c2d3-1234-5678-9abc-def012345678");
        when(service.get(id)).thenThrow(new IllegalStateException("synthetic-secret 00000005M synthetic@example.test"));
        var logger=(Logger)org.slf4j.LoggerFactory.getLogger(ExceptionHandler.class);
        var captured=new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();captured.start();logger.addAppender(captured);
        try {
            mvc.perform(get("/api/users/"+id).session(a.session())).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("No se pudo completar la operación."));
            assertEquals(1,captured.list.size());
            String encoded=new String(new SafeJsonEncoder().encode(captured.list.getFirst()),StandardCharsets.UTF_8);
            assertTrue(encoded.contains("\"stack_trace\""));
            assertTrue(encoded.contains("HttpContractTest.unexpectedFailureReturnsGenericProblemAndLogsOneSanitizedTrace"));
            assertFalse(encoded.contains("synthetic-secret"));
            assertFalse(encoded.contains("00000005M"));
            assertFalse(encoded.contains("synthetic@example.test"));
        } finally {logger.detachAppender(captured);captured.stop();}
    }

    @Test void wrappedUnexpectedFailureReturnsGenericProblemAndLogsOneCauseChain()throws Exception {
        var a=login();UUID id=UUID.fromString("a0b1c2d3-1234-5678-9abc-def012345678");
        var cause=new IllegalArgumentException("private-body synthetic@example.test");
        cause.setStackTrace(new StackTraceElement[]{new StackTraceElement(
            "com.hariklabs.usermanager03.persistence.impl.JpaUserStore","search","JpaUserStore.java",42)});
        when(service.get(id)).thenThrow(new IllegalStateException("secret-token",cause));
        var logger=(Logger)org.slf4j.LoggerFactory.getLogger(ExceptionHandler.class);
        var captured=new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();captured.start();logger.addAppender(captured);
        try {
            mvc.perform(get("/api/users/"+id).session(a.session())).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("No se pudo completar la operación."));
            assertEquals(1,captured.list.size());
            String encoded=new String(new SafeJsonEncoder().encode(captured.list.getFirst()),StandardCharsets.UTF_8);
            assertTrue(encoded.contains("JpaUserStore.search:42"));
            assertTrue(encoded.contains("\"cause_types\""));
            assertFalse(encoded.contains("private-body"));assertFalse(encoded.contains("synthetic@example.test"));
            assertFalse(encoded.contains("secret-token"));
        } finally {logger.detachAppender(captured);captured.stop();}
    }
}
