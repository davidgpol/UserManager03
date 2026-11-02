package com.hariklabs.usermanager03.rest.mapper;

import com.hariklabs.usermanager03.rest.generated.model.*;
import com.hariklabs.usermanager03.service.DomainException;
import com.hariklabs.usermanager03.service.UserValues;
import com.hariklabs.usermanager03.service.model.*;
import org.springframework.stereotype.Component;
import java.time.ZoneOffset;
import java.math.BigInteger;
import java.util.*;

@Component
public class UserRestMapper {
    public UUID id(String text) {
        if(text==null || !text.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new DomainException(400,"INVALID_ID","Identificador no válido.",List.of(new DomainException.FieldError("path","id","INVALID_FORMAT","Use un identificador UUID completo.")));
        return UUID.fromString(text);
    }
    public UserInput input(UserCreateRequest r) {return new UserInput(r.getFirstName(),r.getLastName(),r.getDni(),r.getEmail());}
    public UserInput input(UserUpdateRequest r) {return new UserInput(r.getFirstName(),r.getLastName(),r.getDni(),r.getEmail());}
    public User output(UserResult r) {return new User(r.id(),r.firstName(),r.lastName(),r.dni(),r.email(),r.active(),r.createdAt().atOffset(ZoneOffset.UTC),r.updatedAt().atOffset(ZoneOffset.UTC));}
    public UserPage page(UserResults p) {
        long pages=p.total()/p.size()+(p.total()%p.size()==0?0:1);
        return new UserPage(p.items().stream().map(this::output).toList(),(long)p.page(),(long)p.size(),(long)p.items().size(),p.total(),pages,p.page()>0,(long)p.page()+1<pages);
    }
    public UserQuery query(Map<String,String[]> params) {
        for(var e:params.entrySet()) {
            if(!Set.of("page","size","status","q").contains(e.getKey())) throw invalid("$","UNKNOWN_FIELD");
            if(e.getValue().length!=1) throw invalid(e.getKey(),"DUPLICATE");
        }
        int page=number(params,"page",0,0,1000000),size=number(params,"size",20,1,100);
        String status=value(params,"status","active");
        if(!Set.of("active","inactive","all").contains(status)) throw invalid("status","INVALID_FORMAT");
        return new UserQuery(page,size,status,UserValues.trimSpaces(value(params,"q","")));
    }
    private static String value(Map<String,String[]> p,String key,String fallback) {return p.containsKey(key)?p.get(key)[0]:fallback;}
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
