package com.hariklabs.usermanager03.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="user_record")
public class UserEntity {
    @Id private UUID id;
    @Column(nullable=false,columnDefinition="text") private String firstName;
    @Column(nullable=false,columnDefinition="text") private String lastName;
    @Column(nullable=false,columnDefinition="text",unique=true) private String dni;
    @Column(nullable=false,columnDefinition="text") private String email;
    @Column(nullable=false,columnDefinition="text") private String firstNameSearch;
    @Column(nullable=false,columnDefinition="text") private String lastNameSearch;
    @Column(nullable=false,columnDefinition="text") private String emailSearch;
    @Column(nullable=false) private boolean active;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected UserEntity() {}
    public UserEntity(UUID id,Instant now) { this.id=id; createdAt=now; updatedAt=now; active=true; }
    public void edit(String first,String last,String dni,String email,String firstSearch,String lastSearch,String emailSearch,Instant now) {
        firstName=first; lastName=last; this.dni=dni; this.email=email;
        firstNameSearch=firstSearch; lastNameSearch=lastSearch; this.emailSearch=emailSearch; updatedAt=now;
    }
    public void deactivate(Instant now) { active=false; updatedAt=now; }
    public UUID getId(){return id;} public String getFirstName(){return firstName;}
    public String getLastName(){return lastName;} public String getDni(){return dni;}
    public String getEmail(){return email;} public boolean isActive(){return active;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
