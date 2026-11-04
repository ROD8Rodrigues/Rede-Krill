/*
 * Tipos de caminhão: a entidade de referência mais simples do sistema.
 *
 * Nome e descrição, sem métrica nenhuma — o papel do tipo é servir de rótulo e de alvo
 * de vínculo, então o controller tem quatro métodos e nenhum deles pensa.
 *
 * create devolve 201 construído à mão com ResponseEntity porque o corpo precisa sair
 * junto do status; update fica no 200 padrão retornando o objeto, e delete usa
 * @ResponseStatus(NO_CONTENT) por não haver o que devolver.
 *
 * delete é a única rota com consequência invisível: se houver agendamento usando o tipo,
 * o caso de uso recusa. Não se tenta proteger aqui consultando antes — a checagem e a
 * exclusão precisam estar na mesma transação para não prometer o que não se cumpre.
 */
package com.redekrill.scheduling.presentation.controller;

import com.redekrill.scheduling.application.dto.trucktype.CreateTruckTypeCommand;
import com.redekrill.scheduling.application.dto.trucktype.TruckTypeResult;
import com.redekrill.scheduling.application.dto.trucktype.UpdateTruckTypeCommand;
import com.redekrill.scheduling.application.usecase.trucktype.CreateTruckTypeUseCase;
import com.redekrill.scheduling.application.usecase.trucktype.DeleteTruckTypeUseCase;
import com.redekrill.scheduling.application.usecase.trucktype.ListTruckTypesUseCase;
import com.redekrill.scheduling.application.usecase.trucktype.UpdateTruckTypeUseCase;
import com.redekrill.scheduling.presentation.mapper.WebMapper;
import com.redekrill.scheduling.presentation.request.CreateTruckTypeRequest;
import com.redekrill.scheduling.presentation.request.UpdateTruckTypeRequest;
import com.redekrill.scheduling.presentation.response.TruckTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rotas de tipo de caminhão. Não traz listagem paginada: a lista é curta e vai inteira
 * no select do formulário.
 */
@RestController
@RequestMapping("/api/truck-types")
@Tag(name = "Tipos de caminhão")
@SecurityRequirement(name = "bearerAuth")
public class TruckTypeController {

    private final CreateTruckTypeUseCase createTruckTypeUseCase;
    private final UpdateTruckTypeUseCase updateTruckTypeUseCase;
    private final DeleteTruckTypeUseCase deleteTruckTypeUseCase;
    private final ListTruckTypesUseCase listTruckTypesUseCase;

    public TruckTypeController(CreateTruckTypeUseCase createTruckTypeUseCase,
                               UpdateTruckTypeUseCase updateTruckTypeUseCase,
                               DeleteTruckTypeUseCase deleteTruckTypeUseCase,
                               ListTruckTypesUseCase listTruckTypesUseCase) {
        this.createTruckTypeUseCase = createTruckTypeUseCase;
        this.updateTruckTypeUseCase = updateTruckTypeUseCase;
        this.deleteTruckTypeUseCase = deleteTruckTypeUseCase;
        this.listTruckTypesUseCase = listTruckTypesUseCase;
    }

    @GetMapping
    @Operation(summary = "Lista os tipos de caminhão")
    public List<TruckTypeResponse> list() {
        return listTruckTypesUseCase.execute().stream().map(WebMapper::toResponse).toList();
    }

    @PostMapping
    @Operation(summary = "Cria um tipo de caminhão")
    public ResponseEntity<TruckTypeResponse> create(
            @Valid @RequestBody CreateTruckTypeRequest request,
            Authentication authentication) {

        TruckTypeResult result = createTruckTypeUseCase.execute(
                new CreateTruckTypeCommand(
                        request.name(), request.description(), authentication.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(WebMapper.toResponse(result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um tipo de caminhão")
    public TruckTypeResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTruckTypeRequest request,
            Authentication authentication) {

        TruckTypeResult result = updateTruckTypeUseCase.execute(
                new UpdateTruckTypeCommand(
                        id, request.name(), request.description(), authentication.getName()));
        return WebMapper.toResponse(result);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um tipo de caminhão sem agendamentos vinculados")
    public void delete(@PathVariable Long id, Authentication authentication) {
        deleteTruckTypeUseCase.execute(id, authentication.getName());
    }
}
