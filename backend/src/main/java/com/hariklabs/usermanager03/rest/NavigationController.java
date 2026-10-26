package com.hariklabs.usermanager03.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavigationController {
    @GetMapping({"/","/login"}) public String index() {return "forward:/index.html";}
}
