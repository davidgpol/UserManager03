package com.hariklabs.usermanager03.service;

import java.math.BigInteger;
import java.util.*;

public record UserQuery(int page,int size,String status,String q) {
    public static UserQuery parse(Map<String,String[]> params) {
        for(var e:params.entrySet()) {
            if(!Set.of("page","size","status","q").contains(e.getKey())) throw invalid("$","UNKNOWN_FIELD");
            if(e.getValue().length!=1) throw invalid(e.getKey(),"DUPLICATE");
        }
        int page=number(params,"page",0,0,1000000),size=number(params,"size",20,1,100);
        String status=value(params,"status","active");
        if(!Set.of("active","inactive","all").contains(status)) throw invalid("status","INVALID_FORMAT");
        return new UserQuery(page,size,status,UserValues.trimSpaces(value(params,"q","")));
    }
    private static String value(Map<String,String[]> p,String key,String fallback) { return p.containsKey(key)?p.get(key)[0]:fallback; }
    private static int number(Map<String,String[]> p,String key,int fallback,int min,int max) {
        String text=value(p,key,Integer.toString(fallback));
        if(!text.matches("[0-9]+")) throw invalid(key,"INVALID_FORMAT");
        var n=new BigInteger(text);
        if(n.compareTo(BigInteger.valueOf(min))<0||n.compareTo(BigInteger.valueOf(max))>0) throw invalid(key,"OUT_OF_RANGE");
        return n.intValueExact();
    }
    private static DomainException invalid(String field,String code) {
        return new DomainException(400,"INVALID_QUERY","Revise los parámetros de búsqueda.",
            List.of(new DomainException.FieldError("query",field,code,"Parámetro no válido.")));
    }
}
