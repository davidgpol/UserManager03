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
        if(exception instanceof DomainException d) failure=d;
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
            failure=new DomainException(400,code,"Revise los datos indicados.",errors);
        } else if(exception instanceof org.springframework.web.method.annotation.HandlerMethodValidationException validation) {
            var result=validation.getParameterValidationResults().getFirst();
            String parameter=result.getMethodParameter().getParameterName();
            boolean path="id".equals(parameter);
            failure=new DomainException(400,path?"INVALID_ID":"INVALID_QUERY",path?"Identificador no válido.":"Consulta no válida.",
                List.of(new DomainException.FieldError(path?"path":"query",path?"id":Set.of("page","size","status","q").contains(parameter)?parameter:"$","INVALID_FORMAT","Valor no válido.")));
        } else if(exception instanceof MethodArgumentNotValidException validation) {
            var errors=validation.getBindingResult().getFieldErrors().stream().map(e->new DomainException.FieldError("body",safe(e.getField()),e.getRejectedValue()==null?"REQUIRED":"INVALID_FORMAT","Valor no válido.")).toList();
            failure=new DomainException(400,"VALIDATION_ERROR","Revise los campos indicados.",errors);
        } else if(exception instanceof HttpRequestMethodNotSupportedException method) {
            if(method.getSupportedHttpMethods()!=null) headers.setAllow(method.getSupportedHttpMethods());
            failure=new DomainException(405,"METHOD_NOT_ALLOWED","Método no permitido.");
        } else if(exception instanceof HttpMediaTypeNotSupportedException) failure=new DomainException(415,"UNSUPPORTED_MEDIA_TYPE","Tipo de contenido no admitido.");
        else if(exception instanceof HttpMediaTypeNotAcceptableException) failure=new DomainException(406,"NOT_ACCEPTABLE","La representación solicitada no está disponible.");
        else if(exception instanceof TransientDataAccessException || exception instanceof DataAccessResourceFailureException) failure=new DomainException(503,"SERVICE_UNAVAILABLE","Inténtelo más tarde.");
        else {
            // Deliberately exclude exception message/stack: JDBC exceptions can carry personal values.
            LoggerFactory.getLogger(ExceptionHandler.class).atError().addKeyValue("event_type","operation_failed").addKeyValue("exception_type",exception.getClass().getSimpleName()).log("Operación fallida");
            failure=new DomainException(500,"INTERNAL_ERROR","No se pudo completar la operación.");
        }
        if(failure.status()==406) {
            String accept=request.getHeader("Accept");
            try {
                if(accept!=null && MediaType.parseMediaTypes(accept).stream().noneMatch(m->m.getQualityValue()>0&&m.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)))
                    return ResponseEntity.status(406).cacheControl(CacheControl.noStore()).build();
            } catch(InvalidMediaTypeException ignored) {return ResponseEntity.status(406).cacheControl(CacheControl.noStore()).build();}
        }
        return ResponseEntity.status(failure.status()).headers(headers).body(problems.body(request,failure));
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
        return new DomainException(400,"MALFORMED_JSON","El cuerpo JSON no es válido.");
    }
}
