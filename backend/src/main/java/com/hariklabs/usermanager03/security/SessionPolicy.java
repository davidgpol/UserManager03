package com.hariklabs.usermanager03.security;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.time.*;

@Component
public class SessionPolicy {
    private static final String START="um03.authenticatedAt", LAST="um03.lastAuthenticatedAccess";
    private final Clock clock; private final long idle,absolute;
    public SessionPolicy(Clock clock,@Value("${um03.session.idle-seconds:1800}") long idle,@Value("${um03.session.absolute-seconds:28800}") long absolute) {
        if(idle<=0||absolute<idle) throw new IllegalArgumentException("Invalid session durations");
        this.clock=clock;this.idle=idle;this.absolute=absolute;
    }
    public void start(HttpSession session) {Instant now=clock.instant();session.setAttribute(START,now);session.setAttribute(LAST,now);}
    public boolean valid(HttpSession session) {
        if(session==null) return false;
        try {
            if(!(session.getAttribute(START) instanceof Instant start)||!(session.getAttribute(LAST) instanceof Instant last))return false;
            Instant now=clock.instant();return now.isBefore(start.plusSeconds(absolute))&&now.isBefore(last.plusSeconds(idle));
        } catch(IllegalStateException ignored) {return false;}
    }
    public void accepted(HttpSession session) {
        synchronized(session) {
            try {if(session.getAttribute(START)!=null)session.setAttribute(LAST,clock.instant());} catch(IllegalStateException ignored) { /* Concurrent logout is already effective. */ }
        }
    }
    public Instant expires(HttpSession session) {return ((Instant)session.getAttribute(START)).plusSeconds(absolute);}
    public long idleSeconds() {return idle;}
}
