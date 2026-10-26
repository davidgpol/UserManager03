package com.hariklabs.usermanager03.service;

import java.util.List;

public class DomainException extends RuntimeException {
    public record FieldError(String location, String field, String code, String message) {}
    private final int status;
    private final String code;
    private final List<FieldError> errors;
    public DomainException(int status, String code, String message) { this(status, code, message, List.of()); }
    public DomainException(int status, String code, String message, List<FieldError> errors) {
        super(message); this.status=status; this.code=code; this.errors=List.copyOf(errors);
    }
    public int status() { return status; }
    public String code() { return code; }
    public List<FieldError> errors() { return errors; }
    public static DomainException invalid(String field, String code) {
        return new DomainException(400,"VALIDATION_ERROR","Revise los campos indicados.",
            List.of(new FieldError("body",field,code,"Valor no válido.")));
    }
}
