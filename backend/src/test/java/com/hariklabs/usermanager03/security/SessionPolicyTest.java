package com.hariklabs.usermanager03.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class SessionPolicyTest {
    static class MutableClock extends Clock {
        Instant now=Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return now;}
    }
    @Test void exactIdleAndAbsoluteLimitsAndIndependentSessions() {
        var clock=new MutableClock();var policy=new SessionPolicy(clock,10,30);
        var a=new MockHttpSession();var b=new MockHttpSession();policy.start(a);policy.start(b);
        clock.now=clock.now.plusSeconds(9);assertTrue(policy.valid(a));policy.accepted(a);
        clock.now=clock.now.plusSeconds(1);assertFalse(policy.valid(b));assertTrue(policy.valid(a));
        clock.now=clock.now.plusSeconds(9);assertFalse(policy.valid(a));
        policy.start(a);Instant start=clock.now;
        for(int i=1;i<=3;i++){clock.now=start.plusSeconds(i*9);assertTrue(policy.valid(a));policy.accepted(a);}
        clock.now=start.plusSeconds(30);assertFalse(policy.valid(a));
    }
    @Test void invalidConfigurationFailsClosed() {
        assertThrows(IllegalArgumentException.class,()->new SessionPolicy(Clock.systemUTC(),0,30));
        assertThrows(IllegalArgumentException.class,()->new SessionPolicy(Clock.systemUTC(),31,30));
    }
}
