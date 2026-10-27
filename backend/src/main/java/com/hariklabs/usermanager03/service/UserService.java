package com.hariklabs.usermanager03.service;

import com.hariklabs.usermanager03.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class UserService {
    public record Result(UUID id,String firstName,String lastName,String dni,String email,boolean active,Instant createdAt,Instant updatedAt) {}
    public record Results(List<Result> items,int page,int size,long total) {}
    private static final Logger LOG=LoggerFactory.getLogger(UserService.class);
    private final UserRepository repository; private final UserValues values; private final Clock clock;
    public UserService(UserRepository repository,UserValues values,Clock clock) {this.repository=repository;this.values=values;this.clock=clock;}
    public static UUID id(String text) {
        if(text==null || !text.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new DomainException(400,"INVALID_ID","Identificador no válido.",List.of(new DomainException.FieldError("path","id","INVALID_FORMAT","Use un identificador UUID completo.")));
        return UUID.fromString(text);
    }
    @Transactional(readOnly=true)
    public Result get(String text) {return result(repository.findById(id(text)).orElseThrow(UserService::missing));}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Results list(UserQuery query) {
        Specification<UserEntity> spec=(root,q,cb)-> {
            var status=query.status().equals("all")?cb.conjunction():cb.equal(root.get("active"),query.status().equals("active"));
            if(query.q().isEmpty()) return status;
            String dni=UserValues.canonicalDni(query.q());
            var text=cb.or(cb.like(root.get("firstNameSearch"),like(UserValues.nameSearch(query.q())),'\\'),
                cb.like(root.get("lastNameSearch"),like(UserValues.nameSearch(query.q())),'\\'),
                cb.like(root.get("emailSearch"),like(UserValues.emailSearch(query.q())),'\\'),
                dni==null?cb.disjunction():cb.equal(root.get("dni"),dni));
            return cb.and(status,text);
        };
        var page=repository.findAll(spec,PageRequest.of(query.page(),query.size(),Sort.by("createdAt","id")));
        if(page.getTotalElements()>9007199254740991L) throw new DomainException(500,"INTERNAL_ERROR","No se puede representar el total.");
        return new Results(page.getContent().stream().map(UserService::result).toList(),query.page(),query.size(),page.getTotalElements());
    }
    private static String like(String text) {return "%"+text.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";}
    @Transactional
    public Result create(UserValues.Input input) {
        var valid=values.validate(input); var entity=new UserEntity(UUID.randomUUID(),now());
        edit(entity,valid,entity.getCreatedAt()); save(entity);
        LOG.atInfo().addKeyValue("event_type","user_created").log("Ficha creada"); return result(entity);
    }
    @Transactional
    public Result update(String text,UserValues.Input input) {
        UUID id=id(text);var valid=values.validate(input);var entity=repository.lockById(id).orElseThrow(UserService::missing);
        if(!new UserValues.Input(entity.getFirstName(),entity.getLastName(),entity.getDni(),entity.getEmail()).equals(valid)) {
            edit(entity,valid,next(entity.getUpdatedAt()));save(entity);
            LOG.atInfo().addKeyValue("event_type","user_updated").log("Ficha modificada");
        }
        return result(entity);
    }
    @Transactional
    public void deactivate(String text) {
        var entity=repository.lockById(id(text)).orElseThrow(UserService::missing);
        if(entity.isActive()) {entity.deactivate(next(entity.getUpdatedAt()));repository.flush();
            LOG.atInfo().addKeyValue("event_type","user_deactivated").log("Ficha dada de baja");}
    }
    private Instant now() {return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private Instant next(Instant previous) {Instant now=now();return now.isAfter(previous)?now:previous.plus(1,ChronoUnit.MICROS);}
    private static void edit(UserEntity e,UserValues.Input v,Instant now) {e.edit(v.firstName(),v.lastName(),v.dni(),v.email(),UserValues.nameSearch(v.firstName()),UserValues.nameSearch(v.lastName()),UserValues.emailSearch(v.email()),now);}
    private void save(UserEntity e) {
        try {repository.saveAndFlush(e);} catch(DataIntegrityViolationException ex) {
            // Only the named UNIQUE invariant is translated; unrelated integrity failures remain internal errors.
            for(Throwable t=ex;t!=null;t=t.getCause()) if(t instanceof org.postgresql.util.PSQLException pg && "23505".equals(pg.getSQLState()) && pg.getServerErrorMessage()!=null && "user_record_dni_key".equals(pg.getServerErrorMessage().getConstraint())) {
                LOG.atWarn().addKeyValue("event_type","dni_conflict").addKeyValue("sqlstate","23505")
                    .addKeyValue("constraint","user_record_dni_key").log("Conflicto de unicidad de ficha");
                throw new DomainException(409,"DNI_CONFLICT","Ya existe una ficha con ese DNI.",List.of(new DomainException.FieldError("body","dni","DUPLICATE","El DNI ya está registrado.")));
            }
            throw ex;
        }
    }
    private static Result result(UserEntity e) {return new Result(e.getId(),e.getFirstName(),e.getLastName(),e.getDni(),e.getEmail(),e.isActive(),e.getCreatedAt(),e.getUpdatedAt());}
    private static DomainException missing() {return new DomainException(404,"USER_NOT_FOUND","La ficha no existe.");}
}
