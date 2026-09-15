package com.demo.aiassistant.controller;

import com.demo.aiassistant.config.SessionConstants;
import com.demo.aiassistant.dto.LoginRequest;
import com.demo.aiassistant.dto.LoginResponse;
import com.demo.aiassistant.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpSession session) {
        LoginResponse response = authService.login(request);
        session.setAttribute(SessionConstants.USER_ID, response.id());
        return response;
    }
}

