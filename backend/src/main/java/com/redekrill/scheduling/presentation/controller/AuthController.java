package com.redekrill.scheduling.presentation.controller;

import com.redekrill.scheduling.application.dto.auth.LoginCommand;
import com.redekrill.scheduling.application.dto.auth.LoginResult;
import com.redekrill.scheduling.application.usecase.auth.GetCurrentUserUseCase;
import com.redekrill.scheduling.application.usecase.auth.LoginUseCase;
import com.redekrill.scheduling.application.usecase.auth.LogoutUseCase;
import com.redekrill.scheduling.presentation.mapper.WebMapper;
import com.redekrill.scheduling.presentation.request.LoginRequest;
import com.redekrill.scheduling.presentation.response.LoginResponse;
import com.redekrill.scheduling.presentation.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public AuthController(LoginUseCase loginUseCase,
                          LogoutUseCase logoutUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase) {
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica o usuário e retorna um token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginUseCase.execute(
                new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok(WebMapper.toResponse(result));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Registra o logout do usuário na trilha de auditoria")
    public void logout(Authentication authentication) {
        logoutUseCase.execute(authentication.getName());
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Retorna o usuário autenticado")
    public UserResponse me(Authentication authentication) {
        return WebMapper.toResponse(getCurrentUserUseCase.execute(authentication.getName()));
    }
}
