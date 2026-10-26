package com.hariklabs.usermanager03.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.json.JsonMapper;
import jakarta.validation.Validation;
import java.nio.file.*;
import java.util.*;

class DomainTest {
    private final JsonMapper json=JsonMapper.builder().build();
    @Test void unicodeFixtures() throws Exception {
        var fixtures=json.readTree(Files.readString(Path.of("../api/fixtures/unicode.json")));
        for(var test:fixtures.get("cases")) {
            String field=test.get("field").asText(),value=test.get("input").asText(); int max=field.equals("firstName")?100:150;
            if(test.get("valid").asBoolean()) assertEquals(test.get("expectedStored").asText(),UnicodeCategories.name(value,field,max),test.get("id").asText());
            else assertThrows(DomainException.class,()->UnicodeCategories.name(value,field,max),test.get("id").asText());
        }
    }
    @Test void allUnicodeCategoriesMatchSource() throws Exception {
        int checked=0;
        for(String line:Files.readAllLines(Path.of("../api/unicode/DerivedGeneralCategory-16.0.0.txt"))) {
            String data=line.split("#",2)[0].trim();if(data.isEmpty())continue;
            String[] fields=data.split(";");String[] range=fields[0].trim().split("\\.\\.");
            int start=Integer.parseInt(range[0],16),end=Integer.parseInt(range[range.length-1],16);
            for(int cp=start;cp<=end;cp++) {assertEquals(fields[1].trim(),UnicodeCategories.category(cp));checked++;}
        }
        assertEquals(1114112,checked);
    }
    @Test void dniAndQueryFixtures() throws Exception {
        var fixtures=json.readTree(Files.readString(Path.of("../api/fixtures/dni-query-errors.json")));
        for(var test:fixtures.get("dni")) assertEquals(test.get("canonical").isNull()?null:test.get("canonical").asText(),UserValues.canonicalDni(test.get("input").asText()));
        for(var test:fixtures.get("queryRejections")) {
            var params=new HashMap<String,List<String>>();
            for(String entry:test.get("query").asText().split("&")) {String[] kv=entry.split("=",2);params.computeIfAbsent(kv[0],k->new ArrayList<>()).add(kv[1]);}
            var raw=new HashMap<String,String[]>();params.forEach((k,v)->raw.put(k,v.toArray(String[]::new)));
            assertEquals("INVALID_QUERY",assertThrows(DomainException.class,()->UserQuery.parse(raw)).code());
        }
        assertEquals(new UserQuery(0,20,"active",""),UserQuery.parse(Map.of()));
        assertEquals(1,UserQuery.parse(Map.of("page",new String[]{"000001"})).page());
        assertEquals("nunez",UserValues.nameSearch("Núñez"));
        assertNotEquals(UserValues.emailSearch("josé"),UserValues.emailSearch("jose"));
    }
    @Test void emailFixturesAndPreservation() throws Exception {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var values=new UserValues(factory.getValidator());
            var fixtures=json.readTree(Files.readString(Path.of("../api/fixtures/email.json")));
            for(var test:fixtures.get("cases")) {
                var input=new UserValues.Input(" Ana ","Prueba"," 00000000t ",test.get("value").asText());
                if(test.get("domainAccepts").asBoolean()) {var result=values.validate(input);assertEquals(input.email(),result.email());assertEquals("Ana",result.firstName());assertEquals("00000000T",result.dni());}
                else assertThrows(DomainException.class,()->values.validate(input));
            }
        }
    }
}
