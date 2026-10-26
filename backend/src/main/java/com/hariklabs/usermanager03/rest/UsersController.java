package com.hariklabs.usermanager03.rest;

import com.hariklabs.usermanager03.rest.generated.api.UsersApi;
import com.hariklabs.usermanager03.rest.generated.model.*;
import com.hariklabs.usermanager03.rest.mapper.UserRestMapper;
import com.hariklabs.usermanager03.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;

@RestController
public class UsersController implements UsersApi {
    private final UserService service;private final UserRestMapper mapper;private final HttpServletRequest request;
    public UsersController(UserService service,UserRestMapper mapper,HttpServletRequest request) {this.service=service;this.mapper=mapper;this.request=request;}
    @Override public ResponseEntity<User> createUser(UserCreateRequest input) {
        var result=mapper.output(service.create(mapper.input(input)));
        return ResponseEntity.created(URI.create("/api/users/"+result.getId())).body(result);
    }
    @Override public ResponseEntity<User> getUser(String id) {return ResponseEntity.ok(mapper.output(service.get(id)));}
    @Override public ResponseEntity<User> updateUser(String id,UserUpdateRequest input) {return ResponseEntity.ok(mapper.output(service.update(id,mapper.input(input))));}
    @Override public ResponseEntity<Void> deactivateUser(String id) {service.deactivate(id);return ResponseEntity.noContent().build();}
    @Override public ResponseEntity<UserPage> listUsers(String page,String size,String status,String q) {
        return ResponseEntity.ok(mapper.page(service.list(UserQuery.parse(request.getParameterMap()))));
    }
}
