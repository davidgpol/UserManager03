package com.hariklabs.usermanager03.rest;

import com.hariklabs.usermanager03.rest.mapper.ProblemRestMapper;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;

@Component
public class Problems {
    private final JsonMapper json;
    private final ProblemRestMapper mapper;
    public Problems(JsonMapper json, ProblemRestMapper mapper) {this.json=json;this.mapper=mapper;}
    public void write(HttpServletRequest req,HttpServletResponse res,int status,String code,String detail) throws IOException {
        res.setStatus(status);res.setContentType("application/problem+json");res.setHeader("Cache-Control","no-store");
        json.writeValue(res.getOutputStream(),mapper.problem(req,code,detail));
    }
}
