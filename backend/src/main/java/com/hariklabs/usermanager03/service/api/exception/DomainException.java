package com.hariklabs.usermanager03.service.api.exception;

import java.util.List;
import com.hariklabs.usermanager03.service.api.dto.FieldViolationDTO;

public class DomainException extends RuntimeException {
    private final String code;
    private final List<FieldViolationDTO> errors;
    public DomainException(String code, String message) { this(code, message, List.of()); }
    public DomainException(String code, String message, List<FieldViolationDTO> errors) {
        super(message); this.code=code; this.errors=List.copyOf(errors);
    }
    public String code() { return code; }
    public List<FieldViolationDTO> errors() { return errors; }
    public static DomainException invalid(String field, String code) {
        return new DomainException("VALIDATION_ERROR","Revise los campos indicados.",
            List.of(new FieldViolationDTO(field,code,"Valor no válido.")));
    }
}
