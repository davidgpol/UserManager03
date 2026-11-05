package com.hariklabs.usermanager03.rest;

import com.hariklabs.usermanager03.rest.exception.RestException;
import com.hariklabs.usermanager03.rest.generated.api.dto.ProblemDTO;
import com.hariklabs.usermanager03.rest.mapper.ProblemRestMapper;
import com.hariklabs.usermanager03.service.api.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.*;
import org.springframework.transaction.CannotCreateTransactionException;
import java.sql.SQLException;
import tools.jackson.databind.exc.*;
import java.util.*;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ExceptionHandler {
    private final ProblemRestMapper mapper;
    public ExceptionHandler(ProblemRestMapper mapper) {this.mapper=mapper;}
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDTO> handle(Exception exception,HttpServletRequest request) {
        var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);headers.setCacheControl("no-store");
        DomainException domain=null;
        RestException rest=null;
        if(exception instanceof DomainException d) {
            if(mapper.status(d.code())>=500) {
                logFailure(d);
                domain=new DomainException("INTERNAL_ERROR","No se pudo completar la operación.");
            } else domain=d;
        } else if(exception instanceof RestException r) rest=r;
        else if(exception instanceof HttpMessageNotReadableException unreadable) rest=jsonError(unreadable);
        else if(exception instanceof jakarta.validation.ConstraintViolationException validation) {
            var errors=new ArrayList<RestException.Violation>();
            String code="VALIDATION_ERROR";
            for(var violation:validation.getConstraintViolations()) {
                String parameter="$";
                for(var node:violation.getPropertyPath()) if(node.getKind()==jakarta.validation.ElementKind.PARAMETER)parameter=node.getName();
                boolean id="id".equals(parameter),query=Set.of("page","size","status","q").contains(parameter);
                if(id)code="INVALID_ID";else if(query)code="INVALID_QUERY";
                errors.add(new RestException.Violation(id?"path":query?"query":"body",id||query?parameter:"$","INVALID_FORMAT","Valor no válido."));
            }
            rest=new RestException(code,"Revise los datos indicados.",errors);
        } else if(exception instanceof org.springframework.web.method.annotation.HandlerMethodValidationException validation) {
            var result=validation.getParameterValidationResults().getFirst();
            String parameter=result.getMethodParameter().getParameterName();
            boolean path="id".equals(parameter);
            rest=RestException.invalid(path?"INVALID_ID":"INVALID_QUERY",path?"Identificador no válido.":"Consulta no válida.",
                path?"path":"query",path?"id":Set.of("page","size","status","q").contains(parameter)?parameter:"$","INVALID_FORMAT","Valor no válido.");
        } else if(exception instanceof MethodArgumentNotValidException validation) {
            var errors=validation.getBindingResult().getFieldErrors().stream()
                .map(e->new RestException.Violation("body",safe(e.getField()),e.getRejectedValue()==null?"REQUIRED":"INVALID_FORMAT","Valor no válido.")).toList();
            rest=new RestException("VALIDATION_ERROR","Revise los campos indicados.",errors);
        } else if(exception instanceof HttpRequestMethodNotSupportedException method) {
            if(method.getSupportedHttpMethods()!=null) headers.setAllow(method.getSupportedHttpMethods());
            rest=new RestException("METHOD_NOT_ALLOWED","Método no permitido.");
        } else if(exception instanceof HttpMediaTypeNotSupportedException) rest=new RestException("UNSUPPORTED_MEDIA_TYPE","Tipo de contenido no admitido.");
        else if(exception instanceof HttpMediaTypeNotAcceptableException) rest=new RestException("NOT_ACCEPTABLE","La representación solicitada no está disponible.");
        else if(isConnectionFailure(exception)) {
            logFailure(exception);
            rest=new RestException("SERVICE_UNAVAILABLE","Inténtelo más tarde.");
        } else {
            logFailure(exception);
            rest=new RestException("INTERNAL_ERROR","No se pudo completar la operación.");
        }
        int status=mapper.status(domain!=null?domain.code():rest.code());
        if(status==406) {
            String accept=request.getHeader("Accept");
            try {
                if(accept!=null && MediaType.parseMediaTypes(accept).stream().noneMatch(m->m.getQualityValue()>0&&m.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)))
                    return ResponseEntity.status(406).cacheControl(CacheControl.noStore()).build();
            } catch(InvalidMediaTypeException ignored) {return ResponseEntity.status(406).cacheControl(CacheControl.noStore()).build();}
        }
        return ResponseEntity.status(status).headers(headers).body(domain!=null?mapper.problem(request,domain):mapper.problem(request,rest));
    }
    private static void logFailure(Exception exception) {
        LoggerFactory.getLogger(ExceptionHandler.class).atError().addKeyValue("event_type","operation_failed")
            .setCause(exception).log("Operación fallida");
    }
    private static boolean isConnectionFailure(Exception exception) {
        if (!(exception instanceof CannotCreateTransactionException) && !(exception instanceof DataAccessException))
            return false;
        for (Throwable cause=exception; cause!=null; cause=cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getSQLState()!=null && sql.getSQLState().startsWith("08"))
                return true;
        }
        return false;
    }
    private static String safe(String field) {return Set.of("firstName","lastName","dni","email","id","active","createdAt","updatedAt").contains(field)?field:"$";}
    private static RestException jsonError(Throwable error) {
        for(Throwable e=error;e!=null;e=e.getCause()) {
            if(e instanceof UnrecognizedPropertyException u) {
                String field=safe(u.getPropertyName());boolean readOnly=Set.of("id","active","createdAt","updatedAt").contains(field);
                return RestException.invalid("VALIDATION_ERROR","Revise los campos indicados.","body",readOnly?field:"$",
                    readOnly?"READ_ONLY_FIELD":"UNKNOWN_FIELD","Valor no válido.");
            }
            if(e instanceof MismatchedInputException mismatch) {
                String field=mismatch.getPath().isEmpty()?"$":safe(mismatch.getPath().getLast().getPropertyName());
                return RestException.invalid("VALIDATION_ERROR","Revise los campos indicados.","body",field,"INVALID_TYPE","Valor no válido.");
            }
        }
        return new RestException("MALFORMED_JSON","El cuerpo JSON no es válido.");
    }
}
