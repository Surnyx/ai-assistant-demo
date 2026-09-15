package com.demo.aiassistant.service;

import com.demo.aiassistant.dto.LoginRequest;
import com.demo.aiassistant.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}

