package com.redekrill.scheduling.presentation.exception;

import com.redekrill.scheduling.application.dto.error.ApiError;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.exception.TooManyRequestsException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import ch.qos.logback.classic.Level;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.core.MethodParameter;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request =
            new MockHttpServletRequest("POST", "/api/appointments");

    @Test
    void mapsBeanValidationFailuresToFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Payload(), "createAppointmentRequest");
        bindingResult.rejectValue("palletQuantity", "Min",
                "A quantidade de paletes deve ser maior que zero.");
        bindingResult.rejectValue("orderId", "NotNull", "Pedido é obrigatório.");

        ResponseEntity<ApiError> response = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(new MethodParameter(method(), 0), bindingResult),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Dados inválidos.");
        assertThat(response.getBody().fieldErrors())
                .containsEntry("palletQuantity", "A quantidade de paletes deve ser maior que zero.")
                .containsEntry("orderId", "Pedido é obrigatório.");
    }

    @Test
    void mapsConstraintViolationsToFieldErrors() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Payload> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(new FixedPath("orderId"));
        when(violation.getMessage()).thenReturn("Pedido é obrigatório.");

        ResponseEntity<ApiError> response = handler.handleConstraintViolation(
                new jakarta.validation.ConstraintViolationException(Set.of(violation)), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fieldErrors()).containsEntry("orderId", "Pedido é obrigatório.");
    }

    @Test
    void mapsMissingParameterToBadRequest() {
        ResponseEntity<ApiError> response = handler.handleMissingParameter(
                new MissingServletRequestParameterException("date", "String"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Parâmetro obrigatório ausente: date.");
    }

    @Test
    void mapsUnreadableBodyToBadRequest() {
        ResponseEntity<ApiError> response = handler.handleMessageNotReadable(
                new HttpMessageNotReadableException("JSON parse error",
                        new MockHttpInputMessage(new byte[0])), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Corpo da requisição inválido.");
    }

    @Test
    void mapsUnknownResourceToNotFound() {
        ResponseEntity<ApiError> response = handler.handleNoResource(
                new NoResourceFoundException(HttpMethod.GET, "actuator/health", "/actuator/health"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Recurso não encontrado.");
    }

    @Test
    void mapsUnsupportedMethodToMethodNotAllowed() {
        ResponseEntity<ApiError> response = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void mapsUnsupportedRequestContentToUnsupportedMediaType() {
        ResponseEntity<ApiError> response = handler.handleMediaTypeNotSupported(
                new HttpMediaTypeNotSupportedException("text/csv"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void mapsUnacceptableResponseToNotAcceptableInsteadOfServerError() {
        ResponseEntity<ApiError> response = handler.handleMediaTypeNotAcceptable(
                new HttpMediaTypeNotAcceptableException("application/json"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Tipo de resposta não aceitável.");
    }

    @Test
    void mapsAuthenticationAndAuthorizationFailures() {
        assertThat(handler.handleAuthenticationFailed(
                new AuthenticationFailedException("Credenciais inválidas."), request).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleNotFound(
                new ResourceNotFoundException("Agendamento não encontrado."), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleConflict(
                new ConflictException("Pedido já agendado."), request).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleAccessDenied(
                new org.springframework.security.access.AccessDeniedException("negado"), request)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleDomain(new DomainException("Regra violada."), request).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsHandlerMethodValidationToBadRequest() throws Exception {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                        .getLogger(GlobalExceptionHandler.class);
        Level previous = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        try {
            Method method = method();
            ParameterValidationResult rejected = new ParameterValidationResult(
                    new MethodParameter(method, 0), "senha-do-usuario",
                    List.of(new DefaultMessageSourceResolvable("Size.password")),
                    null, null, null, (resolvable, type) -> null);
            HandlerMethodValidationException exception = new HandlerMethodValidationException(
                    MethodValidationResult.create(new Target(), method, List.of(rejected)));

            ResponseEntity<ApiError> response =
                    handler.handleHandlerMethodValidation(exception, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Parâmetros inválidos.");
            assertThat(response.getBody().fieldErrors()).isNull();
        } finally {
            logger.setLevel(previous);
        }
    }

    @Test
    void mapsTooManyRequestsToRetryAfter() {
        ResponseEntity<ApiError> response = handler.handleTooManyRequests(
                new TooManyRequestsException("Muitas tentativas.", 42), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("42");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RATE_LIMITED");
    }

    @Test
    void mapsPersistenceConflictsToConflict() {
        assertThat(handler.handleDataIntegrity(
                new DataIntegrityViolationException("violates unique constraint",
                        new IllegalStateException("duplicate key")), request).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleOptimisticLock(
                new OptimisticLockingFailureException("row was changed"), request).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void hidesUnexpectedFailureDetails() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new IllegalStateException("detalhe interno"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Erro interno do servidor.");
        assertThat(response.getBody().path()).isEqualTo("/api/appointments");
        assertThat(response.getBody().fieldErrors()).isNull();
    }

    private static Method method() throws NoSuchMethodException {
        return Target.class.getDeclaredMethod("create", Payload.class);
    }

    private record FixedPath(String value) implements Path {

        @Override
        public Iterator<Node> iterator() {
            return List.<Node>of().iterator();
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final class Target {
        @SuppressWarnings("unused")
        void create(Payload payload) {
        }
    }

    static final class Payload {

        private Long orderId;
        private int palletQuantity;

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public int getPalletQuantity() {
            return palletQuantity;
        }

        public void setPalletQuantity(int palletQuantity) {
            this.palletQuantity = palletQuantity;
        }
    }
}
