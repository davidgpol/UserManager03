package com.hariklabs.usermanager03.config;

import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import tools.jackson.databind.json.JsonMapper;
import java.util.LinkedHashMap;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** One JSON event per line; exceptions are classified by type without raw JDBC details. */
public class SafeJsonEncoder extends EncoderBase<ILoggingEvent> {
    private final JsonMapper mapper=JsonMapper.builder().build();
    @Override public byte[] headerBytes(){return new byte[0];}
    @Override public byte[] footerBytes(){return new byte[0];}
    @Override public byte[] encode(ILoggingEvent event) {
        var value=new LinkedHashMap<String,Object>();
        value.put("timestamp",Instant.ofEpochMilli(event.getTimeStamp()).toString());
        value.put("level",event.getLevel().toString());value.put("application","usermanager03");
        value.put("component",event.getLoggerName());value.put("message",event.getFormattedMessage());
        value.putAll(event.getMDCPropertyMap());
        if(event.getKeyValuePairs()!=null)for(var pair:event.getKeyValuePairs()) value.put(pair.key,pair.value);
        if(event.getThrowableProxy()!=null)value.put("exception_type",event.getThrowableProxy().getClassName());
        return (mapper.writeValueAsString(value)+"\n").getBytes(StandardCharsets.UTF_8);
    }
}
