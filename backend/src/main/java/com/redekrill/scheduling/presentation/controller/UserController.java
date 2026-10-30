package com.redekrill.scheduling.presentation.controller;

import com.redekrill.scheduling.application.dto.user.CreateUserCommand;
import com.redekrill.scheduling.application.dto.user.UpdateUserCommand;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.usecase.user.CreateUserUseCase;
import com.redekrill.scheduling.application.usecase.user.ListUsersUseCase;
import com.redekrill.scheduling.application.usecase.user.UpdateUserUseCase;
import com.redekrill.scheduling.presentation.mapper.WebMapper;
import com.redekrill.scheduling.presentation.request.CreateUserRequest;
import com.redekrill.scheduling.presentation.request.UpdateUserRequest;
import com.redekrill.scheduling.presentation.response.PageResponse;
import com.redekrill.scheduling.presentation.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ListUsersUseCase listUsersUseCase;

    public UserController(CreateUserUseCase createUserUseCase,
                          UpdateUserUseCase updateUserUseCase,
                          ListUsersUseCase listUsersUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
    }

    @GetMapping
    @Operation(summary = "Lista os usuários")
    public PageResponse<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return PageResponse.from(listUsersUseCase.execute(page, size), WebMapper::toResponse);
    }

    @PostMapping
    @Operation(summary = "Cria um usuário")
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {

        UserResult result = createUserUseCase.execute(new CreateUserCommand(
                request.name(),
                request.email(),
                request.password(),
                request.role(),
                authentication.getName()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(WebMapper.toResponse(result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um usuário")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        UserResult result = updateUserUseCase.execute(new UpdateUserCommand(
                id,
                request.name(),
                request.email(),
                request.role(),
                request.active(),
                request.password(),
                authentication.getName()
        ));
        return WebMapper.toResponse(result);
    }
}
