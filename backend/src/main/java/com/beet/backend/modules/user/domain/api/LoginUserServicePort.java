package com.beet.backend.modules.user.domain.api;

import com.beet.backend.modules.user.application.dto.LoginResponse;

public interface LoginUserServicePort {

    LoginResponse login(String email, String password);
    
}
