package com.hariklabs.usermanager03.persistence.impl;

import com.hariklabs.usermanager03.persistence.*;
import com.hariklabs.usermanager03.persistence.api.*;
import com.hariklabs.usermanager03.persistence.mapper.UserPersistenceMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserStore implements UserStore {
    private static final Logger LOG = LoggerFactory.getLogger(JpaUserStore.class);
    private final UserRepository repository;
    private final UserPersistenceMapper mapper;

    public JpaUserStore(UserRepository repository, UserPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Optional<StoredUser> find(UUID id) { return repository.findById(id).map(mapper::fromEntity); }
    public Optional<StoredUser> lock(UUID id) { return repository.lockById(id).map(mapper::fromEntity); }

    public StoredUsers search(UserSearch search) {
        Specification<UserEntity> spec = (root, query, cb) -> {
            var status = search.status().equals("all") ? cb.conjunction() : cb.equal(root.get("active"), search.status().equals("active"));
            if (search.nameText().isEmpty()) return status;
            var text = cb.or(cb.like(root.get("firstNameSearch"), like(search.nameText()), '\\'),
                cb.like(root.get("lastNameSearch"), like(search.nameText()), '\\'),
                cb.like(root.get("emailSearch"), like(search.emailText()), '\\'),
                search.dni() == null ? cb.disjunction() : cb.equal(root.get("dni"), search.dni()));
            return cb.and(status, text);
        };
        var page = repository.findAll(spec, PageRequest.of(search.page(), search.size(), Sort.by("createdAt", "id")));
        return new StoredUsers(page.getContent().stream().map(mapper::fromEntity).toList(), page.getTotalElements());
    }

    private static String like(String text) {
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    public StoredUser save(StoredUser user) {
        try {
            return mapper.fromEntity(repository.saveAndFlush(mapper.toEntity(user)));
        } catch (DataIntegrityViolationException ex) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.postgresql.util.PSQLException pg && "23505".equals(pg.getSQLState())
                    && pg.getServerErrorMessage() != null && "user_record_dni_key".equals(pg.getServerErrorMessage().getConstraint())) {
                    LOG.atWarn().addKeyValue("event_type", "dni_conflict").addKeyValue("sqlstate", "23505")
                        .addKeyValue("constraint", "user_record_dni_key").log("Conflicto de unicidad de ficha");
                    throw new DuplicateDniException();
                }
            }
            throw ex;
        }
    }
}
