package com.hariklabs.usermanager03.rest;

import com.hariklabs.usermanager03.service.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.*;
import tools.jackson.databind.exc.*;
import java.util.*;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ExceptionHandler {
    private final Problems problems;
    public ExceptionHandler(Problems problems) {this.problems=problems;}
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<?> handle(Exception exception,HttpServletRequest request) {
        var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);headers.setCacheControl("no-store");
        DomainException failure;
        if(exception instanceof DomainException d) {
            if(status(d.code())>=500) {
                logFailure(d);
                failure=new DomainException("INTERNAL_ERROR","No se pudo completar la operación.");
            } else failure=d;
        }
        else if(exception instanceof HttpMessageNotReadableException unreadable) failure=jsonError(unreadable);
        else if(exception instanceof jakarta.validation.ConstraintViolationException validation) {
            var errors=new ArrayList<DomainException.FieldError>();
            String code="VALIDATION_ERROR";
            for(var violation:validation.getConstraintViolations()) {
                String parameter="$";
                for(var node:violation.getPropertyPath()) if(node.getKind()==jakarta.validation.ElementKind.PARAMETER)parameter=node.getName();
                boolean id="id".equals(parameter),query=Set.of("page","size","status","q").contains(parameter);
                if(id)code="INVALID_ID";else if(query)code="INVALID_QUERY";
                errors.add(new DomainException.FieldError(id?"path":query?"query":"body",id||query?parameter:"$","INVALID_FORMAT","Valor no válido."));
            }
            failure=new DomainException(code,"Revise los datos indicados.",errors);
        } else if(exception instanceof org.springframework.web.method.annotation.HandlerMethodValidationException validation) {
            var result=validation.getParameterValidationResults().getFirst();
            String parameter=result.getMethodParameter().getParameterName();
            boolean path="id".equals(parameter);
            failure=new DomainException(path?"INVALID_ID":"INVALID_QUERY",path?"Identificador no válido.":"Consulta no válida.",
                List.of(new DomainException.FieldError(path?"path":"query",path?"id":Set.of("page","size","status","q").contains(parameter)?parameter:"$","INVALID_FORMAT","Valor no válido.")));
        } else if(exception instanceof MethodArgumentNotValidException validation) {
            var errors=validation.getBindingResult().getFieldErrors().stream().map(e->new DomainException.FieldError("body",safe(e.getField()),e.getRejectedValue()==null?"REQUIRED":"INVALID_FORMAT","Valor no válido.")).toList();
            failure=new DomainException("VALIDATION_ERROR","Revise los campos indicados.",errors);
        } else if(exception instanceof HttpRequestMethodNotSupportedException method) {
            if(method.getSupportedHttpMethods()!=null) headers.setAllow(method.getSupportedHttpMethods());
            failure=new DomainException("METHOD_NOT_ALLOWED","Método no permitido.");
        } else if(exception instanceof HttpMediaTypeNotSupportedException) failure=new DomainException("UNSUPPORTED_MEDIA_TYPE","Tipo de contenido no admitido.");
        else if(exception instanceof HttpMediaTypeNotAcceptableException) failure=new DomainException("NOT_ACCEPTABLE","La representación solicitada no está disponible.");
        else if(exception instanceof TransientDataAccessException || exception instanceof DataAccessResourceFailureException) {
            logFailure(exception);
            failure=new DomainException("SERVICE_UNAVAILABLE","Inténtelo más tarde.");
        }
        else {
            logFailure(exception);
            failure=new DomainException("INTERNAL_ERROR","No se pudo completar la operación.");
        }
        int status=status(failure.code());
        if(status==406) {
            String accept=request.getHeader("Accept");
            try {
                if(accept!=null && MediaType.parseMediaTypes(accept).stream().noneMatch(m->m.getQualityValue()>0&&m.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)))
                    return ResponseEntity.status(406).cacheControl(CacheControl.noStore()).build();
            } catch(InvalidMediaTypeException ignored) {return ResponseEntity.status(406).cacheControl(CacheControl.noStore()).build();}
        }
        return ResponseEntity.status(status).headers(headers).body(problems.body(request,status,failure));
    }
    private static int status(String code) {
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
    private static void logFailure(Exception exception) {
        // The encoder extracts safe frames from the cause; it never serializes its message.
        LoggerFactory.getLogger(ExceptionHandler.class).atError().addKeyValue("event_type","operation_failed")
            .setCause(exception).log("Operación fallida");
    }
    private static String safe(String field) {return Set.of("firstName","lastName","dni","email","id","active","createdAt","updatedAt").contains(field)?field:"$";}
    private static DomainException jsonError(Throwable error) {
        for(Throwable e=error;e!=null;e=e.getCause()) {
            if(e instanceof UnrecognizedPropertyException u) {
                String field=safe(u.getPropertyName());boolean readOnly=Set.of("id","active","createdAt","updatedAt").contains(field);
                return DomainException.invalid(readOnly?field:"$",readOnly?"READ_ONLY_FIELD":"UNKNOWN_FIELD");
            }
            if(e instanceof MismatchedInputException mismatch) {
                String field=mismatch.getPath().isEmpty()?"$":safe(mismatch.getPath().getLast().getPropertyName());
                return DomainException.invalid(field,"INVALID_TYPE");
            }
        }
        return new DomainException("MALFORMED_JSON","El cuerpo JSON no es válido.");
    }
}
