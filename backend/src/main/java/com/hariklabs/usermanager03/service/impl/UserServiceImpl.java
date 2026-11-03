package com.hariklabs.usermanager03.service.impl;

import com.hariklabs.usermanager03.persistence.api.*;
import com.hariklabs.usermanager03.service.DomainException;
import com.hariklabs.usermanager03.service.UserValues;
import com.hariklabs.usermanager03.service.api.UserService;
import com.hariklabs.usermanager03.service.mapper.UserServiceMapper;
import com.hariklabs.usermanager03.service.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserStore store;
    private final UserServiceMapper mapper;
    private final UserValues values;
    private final Clock clock;

    public UserServiceImpl(UserStore store, UserServiceMapper mapper, UserValues values, Clock clock) {
        this.store = store; this.mapper = mapper; this.values = values; this.clock = clock;
    }

    @Transactional(readOnly=true)
    public UserResult get(UUID id) { return mapper.result(store.find(id).orElseThrow(UserServiceImpl::missing)); }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public UserResults list(UserQuery query) {
        var page = store.search(mapper.search(query));
        if (page.total() > 9007199254740991L) throw new DomainException("INTERNAL_ERROR", "No se puede representar el total.");
        return new UserResults(page.items().stream().map(mapper::result).toList(), query.page(), query.size(), page.total());
    }

    @Transactional
    public UserResult create(UserInput input) {
        var valid = values.validate(input);
        var now = now();
        var saved = save(mapper.stored(UUID.randomUUID(), valid, true, now, now));
        LOG.atInfo().addKeyValue("event_type", "user_created").log("Ficha creada");
        return mapper.result(saved);
    }

    @Transactional
    public UserResult update(UUID id, UserInput input) {
        var valid = values.validate(input);
        var old = store.lock(id).orElseThrow(UserServiceImpl::missing);
        if (!mapper.input(old).equals(valid)) {
            old = save(mapper.changed(old, valid, next(old.updatedAt())));
            LOG.atInfo().addKeyValue("event_type", "user_updated").log("Ficha modificada");
        }
        return mapper.result(old);
    }

    @Transactional
    public void deactivate(UUID id) {
        var old = store.lock(id).orElseThrow(UserServiceImpl::missing);
        if (old.active()) {
            save(mapper.inactive(old, next(old.updatedAt())));
            LOG.atInfo().addKeyValue("event_type", "user_deactivated").log("Ficha dada de baja");
        }
    }

    private StoredUser save(StoredUser user) {
        try { return store.save(user); }
        catch (DuplicateDniException ex) {
            throw new DomainException("DNI_CONFLICT", "Ya existe una ficha con ese DNI.",
                List.of(new DomainException.FieldError("body", "dni", "DUPLICATE", "El DNI ya está registrado.")));
        }
    }

    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private Instant next(Instant previous) { Instant now = now(); return now.isAfter(previous) ? now : previous.plus(1, ChronoUnit.MICROS); }
    private static DomainException missing() { return new DomainException("USER_NOT_FOUND", "La ficha no existe."); }
}
