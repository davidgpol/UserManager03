package com.hariklabs.usermanager03.service.impl;

import com.hariklabs.usermanager03.persistence.*;
import com.hariklabs.usermanager03.service.DomainException;
import com.hariklabs.usermanager03.service.UserValues;
import com.hariklabs.usermanager03.service.api.UserService;
import com.hariklabs.usermanager03.service.model.*;
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
public class UserServiceImpl implements UserService {
    private static final Logger LOG=LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository repository; private final UserValues values; private final Clock clock;
    public UserServiceImpl(UserRepository repository,UserValues values,Clock clock) {this.repository=repository;this.values=values;this.clock=clock;}
    @Transactional(readOnly=true)
    public UserResult get(UUID id) {return result(repository.findById(id).orElseThrow(UserServiceImpl::missing));}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public UserResults list(UserQuery query) {
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
        return new UserResults(page.getContent().stream().map(UserServiceImpl::result).toList(),query.page(),query.size(),page.getTotalElements());
    }
    private static String like(String text) {return "%"+text.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";}
    @Transactional
    public UserResult create(UserInput input) {
        var valid=values.validate(input); var entity=new UserEntity(UUID.randomUUID(),now());
        edit(entity,valid,entity.getCreatedAt()); save(entity);
        LOG.atInfo().addKeyValue("event_type","user_created").log("Ficha creada"); return result(entity);
    }
    @Transactional
    public UserResult update(UUID id,UserInput input) {
        var valid=values.validate(input);var entity=repository.lockById(id).orElseThrow(UserServiceImpl::missing);
        if(!new UserInput(entity.getFirstName(),entity.getLastName(),entity.getDni(),entity.getEmail()).equals(valid)) {
            edit(entity,valid,next(entity.getUpdatedAt()));save(entity);
            LOG.atInfo().addKeyValue("event_type","user_updated").log("Ficha modificada");
        }
        return result(entity);
    }
    @Transactional
    public void deactivate(UUID id) {
        var entity=repository.lockById(id).orElseThrow(UserServiceImpl::missing);
        if(entity.isActive()) {entity.deactivate(next(entity.getUpdatedAt()));repository.flush();
            LOG.atInfo().addKeyValue("event_type","user_deactivated").log("Ficha dada de baja");}
    }
    private Instant now() {return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private Instant next(Instant previous) {Instant now=now();return now.isAfter(previous)?now:previous.plus(1,ChronoUnit.MICROS);}
    private static void edit(UserEntity e,UserInput v,Instant now) {e.edit(v.firstName(),v.lastName(),v.dni(),v.email(),UserValues.nameSearch(v.firstName()),UserValues.nameSearch(v.lastName()),UserValues.emailSearch(v.email()),now);}
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
    private static UserResult result(UserEntity e) {return new UserResult(e.getId(),e.getFirstName(),e.getLastName(),e.getDni(),e.getEmail(),e.isActive(),e.getCreatedAt(),e.getUpdatedAt());}
    private static DomainException missing() {return new DomainException(404,"USER_NOT_FOUND","La ficha no existe.");}
}
