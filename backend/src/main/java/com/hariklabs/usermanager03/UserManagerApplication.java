package com.hariklabs.usermanager03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.Clock;

@SpringBootApplication
public class UserManagerApplication {
    public static void main(String[] args) { SpringApplication.run(UserManagerApplication.class, args); }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
