package com.hariklabs.usermanager03.config;

import ch.qos.logback.classic.spi.LoggingEvent;
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
}
