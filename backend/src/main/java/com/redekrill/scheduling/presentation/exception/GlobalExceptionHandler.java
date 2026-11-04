/*
 * Todo erro que sai da aplicação passa por aqui e vira o mesmo ApiError.
 *
 * A ordem dos handlers importa: DomainException é tratado em quatro linhas, mas cada
 * filho seu tem handler próprio, e o Spring escolhe o mais específico. É isso que faz
 * ResourceNotFound virar 404 e Conflict virar 409 em vez de ambos caírem no 400 genérico
 * da classe-mãe — o handler de DomainException serve de rede para um novo tipo de erro de
 * domínio que ainda não ganhou status próprio.
 *
 * O que vira o quê: validação de corpo e parâmetro é 400, às vezes com o mapa de erros por
 * campo (LinkedHashMap, para a ordem dos campos na resposta ser a ordem em que as violações
 * chegaram, e a primeira mensagem ganha quando um campo tem duas regras); autenticação falha
 * 401; acesso negado 403; não encontrado 404; conflito de unicidade, vínculo ou lock otimista
 * 409; muitos 429 com Retry-After em segundos; e o resto é 500 com frase neutra.
 *
 * O que não vira nada: valor de campo. Nos handlers de validação o log debug lista só os
 * nomes dos parâmetros rejeitados, porque esta advice atende também as rotas de login,
 * onde o valor recusado seria justamente a senha. A exceção inesperada é logada com
 * stack trace no servidor e mascarada na resposta.
 */
package com.redekrill.scheduling.presentation.exception;

import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.exception.TooManyRequestsException;
import com.redekrill.scheduling.application.dto.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tradução de exceções em respostas HTTP padronizadas. Não decide regra de negócio nem
 * gera mensagem de domínio: apenas escolhe o status que o erro já carregado merece.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null
                                ? "Valor inválido." : error.getDefaultMessage(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        return build(HttpStatus.BAD_REQUEST, "Dados inválidos.", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = exception.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage() == null
                                ? "Valor inválido." : violation.getMessage(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        return build(HttpStatus.BAD_REQUEST, "Dados inválidos.", request, fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            // Só os nomes dos parâmetros rejeitados: os valores podem carregar
            // credenciais ou PII, e este handler atende também as rotas de autenticação.
            String rejected = exception.getParameterValidationResults().stream()
                    .map(result -> result.getMethodParameter().getParameterName())
                    .collect(Collectors.joining(", "));
            log.debug("Validação de parâmetros rejeitada em {}: {}.", request.getRequestURI(), rejected);
        }
        return build(HttpStatus.BAD_REQUEST, "Parâmetros inválidos.", request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Parâmetro obrigatório ausente: " + exception.getParameterName() + ".", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro " + exception.getName() + ".", request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(
            NoResourceFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado.", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado.", request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado.", request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_ACCEPTABLE, "Tipo de resposta não aceitável.", request, null);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiError> handleAuthenticationFailed(
            AuthenticationFailedException exception, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ConflictException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> handleTooManyRequests(
            TooManyRequestsException exception, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                null,
                "RATE_LIMITED"
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Violação de integridade em {}: {}", request.getRequestURI(),
                exception.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "O registro conflita com dados existentes. Verifique os campos únicos.",
                request, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(
            OptimisticLockingFailureException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                "O registro foi alterado por outro usuário. Recarregue e tente novamente.",
                request, null);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(
            DomainException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Você não tem permissão para executar esta ação.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {}", request.getRequestURI(), exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor.", request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest request,
                                           Map<String, String> fieldErrors) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(error);
    }
}
