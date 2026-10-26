package com.hariklabs.usermanager03.rest.mapper;

import com.hariklabs.usermanager03.rest.generated.model.*;
import com.hariklabs.usermanager03.service.*;
import org.springframework.stereotype.Component;
import java.time.ZoneOffset;

@Component
public class UserRestMapper {
    public UserValues.Input input(UserCreateRequest r) {return new UserValues.Input(r.getFirstName(),r.getLastName(),r.getDni(),r.getEmail());}
    public UserValues.Input input(UserUpdateRequest r) {return new UserValues.Input(r.getFirstName(),r.getLastName(),r.getDni(),r.getEmail());}
    public User output(UserService.Result r) {return new User(r.id(),r.firstName(),r.lastName(),r.dni(),r.email(),r.active(),r.createdAt().atOffset(ZoneOffset.UTC),r.updatedAt().atOffset(ZoneOffset.UTC));}
    public UserPage page(UserService.Results p) {
        long pages=p.total()/p.size()+(p.total()%p.size()==0?0:1);
        return new UserPage(p.items().stream().map(this::output).toList(),(long)p.page(),(long)p.size(),(long)p.items().size(),p.total(),pages,p.page()>0,(long)p.page()+1<pages);
    }
}
