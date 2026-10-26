package com.hariklabs.usermanager03.rest;

import com.hariklabs.usermanager03.service.DomainException;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.*;

@Component
public class Problems {
    public record Body(String type,String title,int status,String detail,String code,String requestId,List<DomainException.FieldError> errors) {}
    private final JsonMapper json;
    public Problems(JsonMapper json) {this.json=json;}
    public Body body(HttpServletRequest req,DomainException e) {
        String requestId=Objects.toString(req.getAttribute("requestId"),UUID.randomUUID().toString());
        return new Body("/problems/"+e.code().toLowerCase(Locale.ROOT).replace('_','-'),title(e.code()),e.status(),e.getMessage(),e.code(),requestId,
            e.errors().stream().sorted(Comparator.comparing(DomainException.FieldError::location).thenComparing(DomainException.FieldError::field).thenComparing(DomainException.FieldError::code)).toList());
    }
    public void write(HttpServletRequest req,HttpServletResponse res,int status,String code,String detail) throws IOException {
        res.setStatus(status);res.setContentType("application/problem+json");res.setHeader("Cache-Control","no-store");
        json.writeValue(res.getOutputStream(),body(req,new DomainException(status,code,detail)));
    }
    private static String title(String code) {
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
