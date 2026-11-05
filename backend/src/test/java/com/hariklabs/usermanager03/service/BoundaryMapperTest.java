package com.hariklabs.usermanager03.service;

import com.hariklabs.usermanager03.persistence.api.dto.*;
import com.hariklabs.usermanager03.persistence.mapper.UserPersistenceMapper;
import com.hariklabs.usermanager03.rest.exception.RestException;
import com.hariklabs.usermanager03.rest.mapper.ProblemRestMapper;
import com.hariklabs.usermanager03.rest.mapper.UserRestMapper;
import com.hariklabs.usermanager03.rest.generated.api.dto.*;
import com.hariklabs.usermanager03.service.api.dto.*;
import com.hariklabs.usermanager03.service.api.exception.DomainException;
import com.hariklabs.usermanager03.service.mapper.UserServiceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BoundaryMapperTest {
    @Test void serviceAndPersistenceMappersPreserveChosenValues() {
        var service=new UserServiceMapper();
        var persistence=new UserPersistenceMapper();
        var id=UUID.randomUUID();
        var created=Instant.parse("2026-01-01T12:00:00Z");
        var updated=created.plusSeconds(2);
        var input=new UserInputDTO("Núñez", "Pérez", "00000000T", "Test@Example.test");
        var stored=service.stored(id,input,"nunez","perez","test@example.test",false,created,updated);
        assertEquals("nunez",stored.firstNameSearch());
        assertFalse(stored.active());
        assertEquals(created,stored.createdAt());
        assertEquals(updated,stored.updatedAt());
        assertEquals(input,service.input(stored));
        assertEquals(new UserResultDTO(id,input.firstName(),input.lastName(),input.dni(),input.email(),false,created,updated),service.result(stored));
        assertEquals(stored,persistence.fromEntity(persistence.toEntity(stored)));
        var query=new UserQueryDTO(3,17,"inactive","Núñez");
        assertEquals(new UserSearchDTO(3,17,"inactive","nunez","núñez",null),service.search(query,"nunez","núñez",null));
    }

    @Test void restMapperPreservesUtcPaginationAndSyntaxOwnership() {
        var rest=new UserRestMapper();
        var input=new UserInputDTO("Ana","Pérez","00000000T","a@example.test");
        assertEquals(input,rest.input(new UserCreateRequestDTO("Ana","Pérez","00000000T","a@example.test")));
        assertEquals(input,rest.input(new UserUpdateRequestDTO("Ana","Pérez","00000000T","a@example.test")));
        var now=Instant.parse("2026-01-01T00:00:00Z");
        var result=new UserResultDTO(UUID.randomUUID(),input.firstName(),input.lastName(),input.dni(),input.email(),true,now,now);
        var page=rest.page(new UserResultsDTO(List.of(result),1,20,21));
        assertEquals(2L,page.getTotalPages());
        assertTrue(page.getHasPrevious());
        assertFalse(page.getHasNext());
        assertEquals("Z",page.getItems().getFirst().getCreatedAt().getOffset().getId());
        var failure=assertThrows(RestException.class,()->rest.query(Map.of("page",new String[]{"-1"})));
        assertEquals("INVALID_QUERY",failure.code());
        assertEquals("query",failure.errors().getFirst().location());
        assertEquals("page",failure.errors().getFirst().field());
    }

    @Test void problemMapperAddsOnlyRestMetadataAndOrdersSafeErrors() {
        var mapper=new ProblemRestMapper();
        var request=new MockHttpServletRequest();
        request.setAttribute("requestId","synthetic-request-id");
        var failure=new DomainException("VALIDATION_ERROR","Revise los campos indicados.",List.of(
            new FieldViolationDTO("lastName","BLANK","Valor no válido."),
            new FieldViolationDTO("firstName","REQUIRED","Valor no válido.")));
        var problem=mapper.problem(request,failure);
        assertEquals(400,problem.getStatus());
        assertEquals("synthetic-request-id",problem.getRequestId());
        assertEquals("/problems/validation-error",problem.getType());
        assertEquals(List.of("firstName","lastName"),problem.getErrors().stream().map(FieldErrorDTO::getField).toList());
        assertTrue(problem.getErrors().stream().allMatch(e->e.getLocation()==FieldErrorDTO.LocationEnum.BODY));
        var http=mapper.problem(request,RestException.invalid("INVALID_ID","Identificador no válido.","path","id","INVALID_FORMAT","Valor no válido."));
        assertEquals(FieldErrorDTO.LocationEnum.PATH,http.getErrors().getFirst().getLocation());
    }
}
