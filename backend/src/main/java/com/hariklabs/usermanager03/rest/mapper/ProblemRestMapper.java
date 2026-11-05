package com.hariklabs.usermanager03.rest.mapper;

import com.hariklabs.usermanager03.rest.exception.RestException;
import com.hariklabs.usermanager03.rest.generated.api.dto.FieldErrorDTO;
import com.hariklabs.usermanager03.rest.generated.api.dto.ProblemDTO;
import com.hariklabs.usermanager03.service.api.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ProblemRestMapper {
    public ProblemDTO problem(HttpServletRequest request, DomainException failure) {
        return problem(request, failure.code(), failure.getMessage(), failure.errors().stream()
            .map(e -> new RestException.Violation("body", e.field(), e.code(), e.message())).toList());
    }
    public ProblemDTO problem(HttpServletRequest request, RestException failure) {
        return problem(request, failure.code(), failure.getMessage(), failure.errors());
    }
    public ProblemDTO problem(HttpServletRequest request, String code, String detail) {
        return problem(request, code, detail, List.of());
    }
    private ProblemDTO problem(HttpServletRequest request, String code, String detail, List<RestException.Violation> violations) {
        var errors=violations.stream().sorted(Comparator.comparing(RestException.Violation::location)
            .thenComparing(RestException.Violation::field).thenComparing(RestException.Violation::code))
            .map(e -> new FieldErrorDTO(FieldErrorDTO.LocationEnum.fromValue(e.location()), e.field(),
                FieldErrorDTO.CodeEnum.fromValue(e.code()), e.message())).toList();
        String requestId=Objects.toString(request.getAttribute("requestId"),UUID.randomUUID().toString());
        return new ProblemDTO("/problems/"+code.toLowerCase(Locale.ROOT).replace('_','-'), title(code),
            status(code), detail, ProblemDTO.CodeEnum.fromValue(code), requestId, errors);
    }
    public int status(String code) {
        return switch(code) {
            case "INVALID_ID", "INVALID_QUERY", "VALIDATION_ERROR", "MALFORMED_JSON" -> 400;
            case "AUTHENTICATION_REQUIRED", "INVALID_CREDENTIALS" -> 401;
            case "CSRF_INVALID", "ACCESS_DENIED" -> 403;
            case "USER_NOT_FOUND" -> 404;
            case "METHOD_NOT_ALLOWED" -> 405;
            case "NOT_ACCEPTABLE" -> 406;
            case "DNI_CONFLICT" -> 409;
            case "UNSUPPORTED_MEDIA_TYPE" -> 415;
            case "SERVICE_UNAVAILABLE" -> 503;
            default -> 500;
        };
    }
    private String title(String code) {
        return switch(code) {
            case "VALIDATION_ERROR" -> "Datos no válidos";
            case "AUTHENTICATION_REQUIRED" -> "Autenticación requerida";
            case "INVALID_CREDENTIALS" -> "Credenciales incorrectas";
            case "CSRF_INVALID","ACCESS_DENIED" -> "Acceso denegado";
            case "USER_NOT_FOUND" -> "Ficha no encontrada";
            case "DNI_CONFLICT" -> "DNI duplicado";
            case "INVALID_QUERY" -> "Consulta no válida";
            case "INVALID_ID" -> "Identificador no válido";
            case "MALFORMED_JSON" -> "JSON no válido";
            case "METHOD_NOT_ALLOWED" -> "Método no permitido";
            case "NOT_ACCEPTABLE" -> "Representación no disponible";
            case "UNSUPPORTED_MEDIA_TYPE" -> "Tipo de contenido no admitido";
            case "SERVICE_UNAVAILABLE" -> "Servicio no disponible";
            default -> "Error interno";
        };
    }
}
