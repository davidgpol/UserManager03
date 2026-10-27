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
    private record Login(org.springframework.mock.web.MockHttpSession session,String token) {}
    private Login login() throws Exception {
        var c=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/csrf")).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)c.getRequest().getSession();
        String token=json.readTree(c.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login").session(session).header("X-CSRF-TOKEN",token).contentType("application/x-www-form-urlencoded").param("username","synthetic-operator").param("password",PASSWORD))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
        c=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/csrf").session(session)).andReturn();
        return new Login(session,json.readTree(c.getResponse().getContentAsString()).get("token").asText());
    }
    private org.springframework.mock.web.MockHttpServletResponse write(Login login,String method,String path,UserValues.Input input,String correlation) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method),path)
            .session(login.session()).header("X-CSRF-TOKEN",login.token()).header("X-Correlation-ID",correlation)
            .contentType("application/json").content(json.writeValueAsString(input))).andReturn().getResponse();
    }
    private void conflict(org.springframework.mock.web.MockHttpServletResponse response,String correlation) throws Exception {
        assertEquals(409,response.getStatus());assertEquals("DNI_CONFLICT",json.readTree(response.getContentAsString()).get("code").asText());
        assertEquals(correlation,response.getHeader("X-Correlation-ID"));
    }
    @Test void httpConflictsRemainAtomicAndCorrelated() throws Exception {
        var a=login();var b=login();
        var original=service.create(input("00000005M","Har110Original"));
        conflict(write(a,"POST","/api/users",input(" 00000005m ","Har110Duplicate"),"har110-ordinary"),"har110-ordinary");
        service.deactivate(original.id().toString());
        conflict(write(a,"POST","/api/users",input("00000005M","Har110Inactive"),"har110-inactive"),"har110-inactive");
        var unchanged=service.create(input("00000006Y","Har110Unchanged"));
        conflict(write(a,"PUT","/api/users/"+unchanged.id(),input("00000005M","Har110Rollback"),"har110-update"),"har110-update");
        assertEquals(unchanged,service.get(unchanged.id().toString()));
        var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var futures=new ArrayList<Future<org.springframework.mock.web.MockHttpServletResponse>>();
            for(var login:List.of(a,b)) futures.add(pool.submit(()->{ready.countDown();go.await();return write(login,"POST","/api/users",input(" 00000007f ","Har110Race"),"har110-race-"+(login==a?"a":"b"));}));
            assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();var statuses=new ArrayList<Integer>();
            for(var f:futures){var response=f.get(15,TimeUnit.SECONDS);statuses.add(response.getStatus());if(response.getStatus()==409)conflict(response,response.getHeader("X-Correlation-ID"));}
            Collections.sort(statuses);assertEquals(List.of(201,409),statuses);
        }
        assertEquals(1,service.list(new UserQuery(0,20,"all","00000007F")).total());
        assertEquals(1,service.list(new UserQuery(0,20,"all","00000005M")).total());
        assertFalse(service.get(original.id().toString()).active());
    }
    @Test void exactCreatedAtTiesUsePostgresUuidOrder() throws Exception {
        var ids=List.of("ffffffff-ffff-4fff-bfff-ffffffffffff","00000000-0000-4000-8000-000000000001","80000000-0000-4000-8000-000000000000");
        var dnis=List.of("00000008P","00000009D","00000010X");
        try(var c=dataSource.getConnection();var s=c.prepareStatement("update user_record set id=?,created_at='2020-01-01T00:00:00Z' where id=?")) {
            for(int i=0;i<3;i++){var item=service.create(input(dnis.get(i),"Har110ExactTie"));s.setObject(1,UUID.fromString(ids.get(i)));s.setObject(2,item.id());assertEquals(1,s.executeUpdate());}
        }
        var expected=ids.stream().sorted().toList();
        for(int repeat=0;repeat<3;repeat++) {
            var actual=new ArrayList<String>();
            for(int page=0;page<3;page++) {var result=service.list(new UserQuery(page,1,"all","Har110ExactTie"));assertEquals(3,result.total());assertEquals(java.time.Instant.parse("2020-01-01T00:00:00Z"),result.items().getFirst().createdAt());actual.add(result.items().getFirst().id().toString());}
            assertEquals(expected,actual);
        }
    }
    @Test void mutationsBetweenPagesHaveFreshTotalsWithoutCrossRequestSnapshot() {
        var a=service.create(input("00000011B","Har110Moving"));
        var b=service.create(input("00000012N","Har110Moving"));
        var c=service.create(input("00000013J","Har110Moving"));
        var first=service.list(new UserQuery(0,1,"active","Har110Moving"));assertEquals(a.id(),first.items().getFirst().id());assertEquals(3,first.total());
        var d=service.create(input("00000014Z","Har110Moving"));
        var second=service.list(new UserQuery(1,1,"active","Har110Moving"));assertEquals(b.id(),second.items().getFirst().id());assertEquals(4,second.total());
        service.deactivate(a.id().toString());
        var shifted=service.list(new UserQuery(1,1,"active","Har110Moving"));assertEquals(c.id(),shifted.items().getFirst().id());assertEquals(3,shifted.total());
        var refreshed=service.list(new UserQuery(0,20,"active","Har110Moving"));assertEquals(List.of(b.id(),c.id(),d.id()),refreshed.items().stream().map(UserService.Result::id).toList());
    }
    @Test void databaseLoggingConfigurationIsEffective() throws Exception {
        var expected=Map.of("log_error_verbosity","terse","log_min_error_statement","panic","log_statement","none","log_parameter_max_length","0","log_parameter_max_length_on_error","0","log_min_messages","warning");
        try(var c=dataSource.getConnection();var s=c.prepareStatement("select current_setting(?)")){
            for(var entry:expected.entrySet()){s.setString(1,entry.getKey());try(var rs=s.executeQuery()){assertTrue(rs.next());assertEquals(entry.getValue(),rs.getString(1),entry.getKey());}}
        }
    }
}
