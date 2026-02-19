package com.beet.backend.modules.user.domain.usecase;

import com.beet.backend.modules.user.application.dto.LoginResponse;
import com.beet.backend.modules.user.application.mapper.UserServiceMapper;
import com.beet.backend.modules.user.domain.api.LoginUserServicePort;
import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.domain.spi.UserPersistencePort;
import com.beet.backend.shared.infrastructure.security.CustomUserDetails;
import com.beet.backend.shared.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUserUseCaseImpl implements LoginUserServicePort {

    private final AuthenticationManager authenticationManager;
    private final UserPersistencePort userPersistencePort;
    private final JwtService jwtService;
    private final UserServiceMapper userServiceMapper;

    @Override
    public LoginResponse login(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        password));

        User user = userPersistencePort.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var userDetails = new CustomUserDetails(user);

        String jwtToken = jwtService.generateToken(userDetails);

        return new LoginResponse(jwtToken, userServiceMapper.toResponse(user));
    }
}
