package com.hariklabs.usermanager03.config;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SafeJsonEncoderTest {
    @Test void driverDetailsCannotEnterJsonButSqlstateAndCorrelationRemain() {
        var encoder=new SafeJsonEncoder();
        for(String category:new String[]{"org.hibernate.orm.jdbc.error","org.hibernate.engine.jdbc.spi.SqlExceptionHelper"}) {
            var event=new LoggingEvent();event.setLoggerName(category);event.setLevel(Level.WARN);event.setTimeStamp(0);
            event.setMDCPropertyMap(Map.of("correlation_id","synthetic-correlation","flow","user_records"));
            event.setMessage("ERROR: duplicate key; Detail: Key (dni)=(00000005M); Har110Private synthetic@example.test");
            String encoded=new String(encoder.encode(event),StandardCharsets.UTF_8);
            assertFalse(encoded.contains("00000005M"));assertFalse(encoded.contains("Har110Private"));assertFalse(encoded.contains("synthetic@example.test"));
            assertTrue(encoded.contains("synthetic-correlation"));assertTrue(encoded.contains("WARN"));
            var diagnostic=new LoggingEvent();diagnostic.setLoggerName(category);diagnostic.setLevel(Level.WARN);diagnostic.setTimeStamp(0);diagnostic.setMDCPropertyMap(Map.of());
            diagnostic.setMessage("HHH000247: ErrorCode: 0, SQLState: 23505");
            assertTrue(new String(encoder.encode(diagnostic),StandardCharsets.UTF_8).contains("\"sqlstate\":\"23505\""));
        }
    }
    @Test void unexpectedFailureHasUsefulSanitizedTraceWithoutMessagesOrForeignFrames() {
        var failure=new IllegalStateException("synthetic-secret 00000005M synthetic@example.test");
        failure.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("com.hariklabs.usermanager03.service.impl.UserServiceImpl","save","UserServiceImpl.java",81),
            new StackTraceElement("org.postgresql.jdbc.PgStatement","execute","synthetic-secret.java",44),
            new StackTraceElement("com.hariklabs.usermanager03.leak!","synthetic-secret","synthetic-secret.java",4)
        });
        var event=new LoggingEvent();event.setLoggerName("com.hariklabs.usermanager03.rest.ExceptionHandler");
        event.setLevel(Level.ERROR);event.setTimeStamp(0);event.setMessage("Operación fallida");
        event.setMDCPropertyMap(Map.of("correlation_id","synthetic-correlation","flow","user_records","event_type","operation_failed"));
        event.setThrowableProxy(new ThrowableProxy(failure));
        String encoded=new String(new SafeJsonEncoder().encode(event),StandardCharsets.UTF_8);
        assertTrue(encoded.contains("\"stack_trace\""));
        assertTrue(encoded.contains("UserServiceImpl.save:81"));
        assertTrue(encoded.contains("synthetic-correlation"));
        assertFalse(encoded.contains("synthetic-secret"));
        assertFalse(encoded.contains("00000005M"));
        assertFalse(encoded.contains("synthetic@example.test"));
        assertFalse(encoded.contains("PgStatement"));
    }

    @Test void wrappedFailureRetainsOriginFromCauseWithoutSensitiveMessages() {
        var origin=new IllegalArgumentException("secret-token synthetic@example.test");
        origin.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("com.hariklabs.usermanager03.persistence.impl.JpaUserStore","save","JpaUserStore.java",64),
            new StackTraceElement("org.postgresql.jdbc.PgStatement","execute","PgStatement.java",10)
        });
        var wrapper=new IllegalStateException("private-body 00000005M",origin);
        wrapper.setStackTrace(new StackTraceElement[]{new StackTraceElement("java.lang.Thread","run","Thread.java",1)});
        var event=new LoggingEvent();event.setLoggerName("com.hariklabs.usermanager03.rest.ExceptionHandler");
        event.setLevel(Level.ERROR);event.setTimeStamp(0);event.setMessage("Operación fallida");
        event.setMDCPropertyMap(Map.of("correlation_id","synthetic-correlation"));
        event.setThrowableProxy(new ThrowableProxy(wrapper));
        String encoded=new String(new SafeJsonEncoder().encode(event),StandardCharsets.UTF_8);
        assertTrue(encoded.contains("\"cause_types\":[\"java.lang.IllegalArgumentException\"]"));
        assertTrue(encoded.contains("1:com.hariklabs.usermanager03.persistence.impl.JpaUserStore.save:64"));
        for(String secret:new String[]{"secret-token","synthetic@example.test","private-body","00000005M","PgStatement"})
            assertFalse(encoded.contains(secret));
    }
}
