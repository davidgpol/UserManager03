package com.hariklabs.usermanager03.rest.exception;

import java.util.List;

/** A rejected HTTP representation or request syntax, without business semantics. */
public class RestException extends RuntimeException {
    public record Violation(String location, String field, String code, String message) {}
    private final String code;
    private final List<Violation> errors;
    public RestException(String code, String message) { this(code, message, List.of()); }
    public RestException(String code, String message, List<Violation> errors) {
        super(message); this.code=code; this.errors=List.copyOf(errors);
    }
    public String code() { return code; }
    public List<Violation> errors() { return errors; }
    public static RestException invalid(String code, String detail, String location, String field, String fieldCode, String message) {
        return new RestException(code, detail, List.of(new Violation(location,field,fieldCode,message)));
    }
}
