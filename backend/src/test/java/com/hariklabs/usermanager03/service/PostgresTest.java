package com.hariklabs.usermanager03.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="UM03_DB_TEST",matches="true")
class PostgresTest {
    @Autowired UserService service;@Autowired DataSource dataSource;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired tools.jackson.databind.json.JsonMapper json;
    private static final String PASSWORD=UUID.randomUUID().toString();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        String password=PASSWORD;
        r.add("um03.operator.username",()->"synthetic-operator");
        String hash="{argon2}"+new Argon2PasswordEncoder(16,32,1,19456,2).encode(password);
        r.add("um03.operator.password-hash",()->hash);
    }

    @Test void httpToPostgresToGeneratedJsonPreservesUnicodeAndUtc() throws Exception {
        var csrf=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/csrf")).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)csrf.getRequest().getSession();
        String token=json.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login").session(session).header("X-CSRF-TOKEN",token).contentType("application/x-www-form-urlencoded").param("username","synthetic-operator").param("password",PASSWORD))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
        csrf=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/csrf").session(session)).andReturn();
        token=json.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        String name="👩".repeat(100);
        var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/users").session(session).header("X-CSRF-TOKEN",token).contentType("application/json")
            .content(json.writeValueAsString(new UserValues.Input(" "+name+" ","Jose\u0301","00000004G","a@localhost"))))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated()).andReturn();
        var created=json.readTree(response.getResponse().getContentAsString());
        assertEquals(name,created.get("firstName").asText());assertTrue(created.get("createdAt").asText().endsWith("Z"));
        var read=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users/"+created.get("id").asText()).session(session))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        assertEquals(created,json.readTree(read.getResponse().getContentAsString()));
    }
    UserValues.Input input(String dni,String name) {return new UserValues.Input(name,"Prueba Núñez",dni,"synthetic@example.test");}
    @Test void postgresVersionTlsRestrictedRoleAndRoundTrip()throws Exception {
        try(var c=dataSource.getConnection();var statement=c.createStatement()) {
            try(var rs=statement.executeQuery("select current_user,current_setting('server_version'),current_setting('server_encoding'),ssl from pg_stat_ssl where pid=pg_backend_pid()")) {
                assertTrue(rs.next());assertEquals("um03_app",rs.getString(1));assertTrue(rs.getString(2).startsWith("18.6"));assertEquals("UTF8",rs.getString(3));assertTrue(rs.getBoolean(4));
            }
            assertThrows(java.sql.SQLException.class,()->statement.execute("delete from user_record"));
        }
        var value=service.create(input("00000000T"," \uFEFF👩‍💻Jose\u0301 "));
        assertEquals("\uFEFF👩‍💻Jose\u0301",service.get(value.id().toString()).firstName());
        var unchanged=service.update(value.id().toString(),input(" 00000000t "," \uFEFF👩‍💻Jose\u0301 "));
        assertEquals(value,unchanged);
        service.deactivate(value.id().toString());var inactive=service.get(value.id().toString());
        assertFalse(inactive.active());assertTrue(inactive.updatedAt().isAfter(inactive.createdAt()));
        service.deactivate(value.id().toString());assertEquals(inactive,service.get(value.id().toString()));
        assertFalse(service.update(value.id().toString(),input("00000000T","Nuevo")).active());
        assertEquals("DNI_CONFLICT",assertThrows(DomainException.class,()->service.create(input("00000000t","Otro"))).code());
    }
    @Test void concurrentCanonicalDniHasOneWinnerAndOneConflict()throws Exception {
        var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try(var workers=Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Integer> create=()-> {ready.countDown();go.await();try{service.create(input(" 00000001r ","Carrera"));return 201;}catch(DomainException e){return e.status();}};
            var a=workers.submit(create);var b=workers.submit(create);assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();
            var results=new ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS)));Collections.sort(results);assertEquals(List.of(201,409),results);
        }
    }
    @Test void searchLiteralUnicodeAndStablePagination() {
        var a=service.create(input("00000002W","Álvaro % Caso"));var b=service.create(input("00000003A","Núñez"));
        assertTrue(service.list(new UserQuery(0,20,"active","alva")).items().stream().anyMatch(v->v.id().equals(a.id())));
        assertTrue(service.list(new UserQuery(0,20,"active","nunez")).items().stream().anyMatch(v->v.id().equals(b.id())));
        assertEquals(List.of(a.id()),service.list(new UserQuery(0,20,"active","%")).items().stream().map(UserService.Result::id).toList());
        var full=service.list(new UserQuery(0,100,"all",""));var ids=new ArrayList<UUID>();
        for(int page=0;page<full.total();page++) ids.add(service.list(new UserQuery(page,1,"all","")).items().getFirst().id());
        assertEquals(full.items().stream().map(UserService.Result::id).toList(),ids);
        var empty=service.list(new UserQuery(1000000,100,"all",""));assertTrue(empty.items().isEmpty());assertEquals(full.total(),empty.total());
    }
}
