package com.rankwise.admin;

import com.rankwise.admin.dto.LoginRequest;
import com.rankwise.admin.dto.LoginResponse;
import com.rankwise.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AdminUserRepository adminUserRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       AdminUserRepository adminUserRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.adminUserRepository = adminUserRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AdminUser admin = adminUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        String token = jwtService.generateToken(admin.getUsername(), admin.getRole());
        return new LoginResponse(token, admin.getUsername(), admin.getRole());
    }
}
