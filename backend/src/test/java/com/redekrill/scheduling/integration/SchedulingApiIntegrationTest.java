package com.redekrill.scheduling.integration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchedulingApiIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@redekrill.com";
    private static final String ADMIN_PASSWORD = "TestAdmin@2026!";
    private static final String ACCESS_COOKIE = "redekrill_access";
    private static final String REFRESH_COOKIE = "redekrill_refresh";

    private static final EmbeddedPostgres POSTGRES = startPostgres();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível iniciar o PostgreSQL embarcado.", exception);
        }
    }

    @Test
    void supportsTheCompleteSchedulingFlowWithRoleRestrictions() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/time-slots").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].startTime").value("08:00"))
                .andExpect(jsonPath("$[0].endTime").value("10:00"));

        mockMvc.perform(get("/api/palletization-types")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].capacityMultiplier").value(2));

        String userToken = createActiveUser(adminToken, "Operador", "operador@redekrill.com",
                "Operador@123", "Operador@4567", "USER");

        mockMvc.perform(get("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentPayload(9000, "Fornecedor USER", "user@example.com",
                                "2026-03-10", 30, null)))
                .andExpect(status().isForbidden());

        MvcResult created = mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentPayload(9001, "Fornecedor Krill", "fornecedor@example.com",
                                "2026-03-10", 30, "Entregar na doca 3")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointment.orderId").value(9001))
                .andExpect(jsonPath("$.appointment.supplierName").value("Fornecedor Krill"))
                .andExpect(jsonPath("$.appointment.truckPlate").value("ABC1D23"))
                .andExpect(jsonPath("$.appointment.date").value("2026-03-10"))
                .andExpect(jsonPath("$.appointment.timeSlot.startTime").value("08:00"))
                .andExpect(jsonPath("$.appointment.timeSlot.endTime").value("10:00"))
                .andExpect(jsonPath("$.appointment.notes").value("Entregar na doca 3"))
                .andExpect(jsonPath("$.appointment.status").value("ACTIVE"))
                .andExpect(jsonPath("$.appointment.palletQuantity").value(30))
                .andExpect(jsonPath("$.appointment.dailyPallets").value(30))
                .andExpect(jsonPath("$.appointment.effectivePallets").value(60))
                .andExpect(jsonPath("$.appointment.slotPallets").value(60))
                .andExpect(jsonPath("$.warnings").isEmpty())
                .andExpect(jsonPath("$.emailQueued").value(true))
                .andExpect(jsonPath("$.emailSent").value(false))
                .andExpect(jsonPath("$.capacity.dailyPallets").value(30))
                .andExpect(jsonPath("$.capacity.slotPallets").value(60))
                .andExpect(jsonPath("$.capacity.dailyOccupancyPercentage").value(6.3))
                .andExpect(jsonPath("$.capacity.slotOccupancyPercentage").value(50.0))
                .andReturn();

        JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsString());
        long appointmentId = createdBody.at("/appointment/id").asLong();
        Instant persistedCreatedAt = Instant.parse(
                createdBody.at("/appointment/createdAt").asText());
        assertThat(persistedCreatedAt)
                .isBetween(Instant.now().minusSeconds(300), Instant.now().plusSeconds(300));

        Integer queued = jdbcTemplate.queryForObject(
                "select count(*) from email_outbox where appointment_id = ? and status = 'PENDING'",
                Integer.class, appointmentId);
        assertThat(queued).isEqualTo(1);

        mockMvc.perform(get("/api/appointments/capacity")
                        .param("date", "2026-03-10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyPallets").value(30))
                .andExpect(jsonPath("$.dailyLimit").value(480))
                .andExpect(jsonPath("$.dailyOccupancyPercentage").value(6.3))
                .andExpect(jsonPath("$.slots.length()").value(4))
                .andExpect(jsonPath("$.slots[0].pallets").value(60))
                .andExpect(jsonPath("$.slots[0].occupancyPercentage").value(50.0));

        mockMvc.perform(get("/api/appointments")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].orderId").value(9001))
                .andExpect(jsonPath("$.content[0].supplierName").value("Fornecedor Krill"));

        mockMvc.perform(get("/api/reports/appointments")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAppointments").value(1))
                .andExpect(jsonPath("$.totalPallets").value(30))
                .andExpect(jsonPath("$.totalEffectivePallets").value(60))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages").value(1));

        mockMvc.perform(get("/api/audit-logs").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.entityName == 'Appointment')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'CREATE')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'LOGIN')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'PASSWORD_CHANGE')]").isNotEmpty());

        mockMvc.perform(get("/api/audit-logs").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelsLogicallyAndKeepsHistory() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        MvcResult created = mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentPayload(9101, "Fornecedor Cancelado",
                                "cancelado@example.com", "2026-04-15", 50, "Cancelar se necessário")))
                .andExpect(status().isCreated())
                .andReturn();

        long appointmentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .at("/appointment/id").asLong();

        mockMvc.perform(delete("/api/appointments/{id}", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/appointments/{id}", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.canceledBy.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.canceledAt").isNotEmpty())
                .andExpect(jsonPath("$.orderId").value(9101))
                .andExpect(jsonPath("$.supplierName").value("Fornecedor Cancelado"));

        mockMvc.perform(delete("/api/appointments/{id}", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Agendamento já está cancelado."));

        mockMvc.perform(put("/api/appointments/{id}", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentPayload(9101, "Fornecedor Cancelado",
                                "cancelado@example.com", "2026-04-15", 50, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Agendamento cancelado não pode ser alterado."));

        mockMvc.perform(get("/api/appointments/capacity")
                        .param("date", "2026-04-15")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyPallets").value(0))
                .andExpect(jsonPath("$.dailyOccupancyPercentage").value(0.0))
                .andExpect(jsonPath("$.slots[0].pallets").value(0));

        mockMvc.perform(get("/api/reports/appointments")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-30")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAppointments").value(1))
                .andExpect(jsonPath("$.totalPallets").value(50))
                .andExpect(jsonPath("$.appointments[0].status").value("CANCELED"));

        mockMvc.perform(get("/api/audit-logs").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.action == 'CANCEL')]").isNotEmpty());
    }

    @Test
    void queuesEmailWithoutBlockingAppointmentCreation() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        MvcResult created = mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentPayload(9201, "Fornecedor Sem Email",
                                "sem-email@example.com", "2026-05-20", 10, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailQueued").value(true))
                .andExpect(jsonPath("$.emailSent").value(false))
                .andExpect(jsonPath("$.warnings").isEmpty())
                .andReturn();

        long appointmentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .at("/appointment/id").asLong();
        Map<String, Object> outbox = jdbcTemplate.queryForMap(
                "select status, attempts, recipient from email_outbox where appointment_id = ?",
                appointmentId);
        assertThat(outbox.get("status")).isEqualTo("PENDING");
        assertThat(((Number) outbox.get("attempts")).intValue()).isZero();
        assertThat(outbox.get("recipient")).isEqualTo("sem-email@example.com");

        mockMvc.perform(get("/api/appointments")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].orderId").value(9201));
    }

    @Test
    void rejectsDuplicatedOrderId() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentPayload(9301, "Fornecedor A", "a@example.com",
                                "2026-06-10", 10, null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentPayload(9301, "Fornecedor B", "b@example.com",
                                "2026-06-11", 20, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe um agendamento com o ID Pedido 9301."));
    }

    @Test
    void recordsLogoutAndAccessDeniedInAudit() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String userToken = createActiveUser(adminToken, "Negado", "negado@redekrill.com",
                "Negado@12345", "Negado@67890", "USER");

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentPayload(9401, "Fornecedor Negado", "negado@example.com",
                                "2026-07-10", 10, null)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/audit-logs").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.action == 'LOGOUT')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'ACCESS_DENIED')]").isNotEmpty());
    }

    @Test
    void authenticatesWithCookiesRotatesRefreshAndRevokesSession() throws Exception {
        Csrf csrf = csrf();
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.mustChangePassword").value(false))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("HttpOnly")))
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie(ACCESS_COOKIE);
        Cookie refreshCookie = loginResult.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie.getPath()).isEqualTo("/");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getPath()).isEqualTo("/api/auth");
        String accessToken = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/auth/me").cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL));

        Csrf refreshCsrf = csrf();
        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie)
                        .cookie(refreshCsrf.cookie())
                        .header(refreshCsrf.headerName(), refreshCsrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(ADMIN_EMAIL))
                .andReturn();

        Cookie rotatedRefreshCookie = refreshed.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(rotatedRefreshCookie).isNotNull();
        assertThat(rotatedRefreshCookie.getValue()).isNotEqualTo(refreshCookie.getValue());
        String refreshedToken = objectMapper.readTree(
                refreshed.getResponse().getContentAsString()).get("token").asText();

        Csrf replayCsrf = csrf();
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie)
                        .cookie(replayCsrf.cookie())
                        .header(replayCsrf.headerName(), replayCsrf.token()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(refreshedToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(refreshedToken)))
                .andExpect(status().isUnauthorized());

        assertThat(accessToken).isNotBlank();
    }

    @Test
    void rejectsLoginWithoutCsrfAndInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@redekrill.com","password":"TestAdmin@2026!"}
                                """))
                .andExpect(status().isForbidden());

        Csrf csrf = csrf();
        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@redekrill.com","password":"senha-errada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));

        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void preventsAdminFromDeactivatingOrDemotingOwnAccount() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(put("/api/users/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Administrador de Teste",
                                  "email": "admin@redekrill.com",
                                  "role": "ADMIN",
                                  "active": false
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Administrador não pode desativar a própria conta."));

        mockMvc.perform(put("/api/users/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Administrador de Teste",
                                  "email": "admin@redekrill.com",
                                  "role": "USER",
                                  "active": true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Administrador não pode remover o próprio perfil ADMIN."));

        login(ADMIN_EMAIL, ADMIN_PASSWORD);
        mockMvc.perform(get("/api/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void validatesAppointmentPayload() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 0,
                                  "supplierName": "",
                                  "supplierEmail": "invalido",
                                  "truckPlate": "",
                                  "palletQuantity": 0,
                                  "notes": "%s"
                                }
                                """.formatted("N".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos."))
                .andExpect(jsonPath("$.fieldErrors.orderId").exists())
                .andExpect(jsonPath("$.fieldErrors.supplierName").exists())
                .andExpect(jsonPath("$.fieldErrors.supplierEmail").exists())
                .andExpect(jsonPath("$.fieldErrors.truckPlate").exists())
                .andExpect(jsonPath("$.fieldErrors.palletQuantity").exists())
                .andExpect(jsonPath("$.fieldErrors.notes").exists());
    }

    @Test
    void requiresActiveFlagOnUserUpdate() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(put("/api/users/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Administrador de Teste",
                                  "email": "admin@redekrill.com",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.active").exists());
    }

    private Csrf csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return new Csrf(body.get("headerName").asText(), body.get("token").asText(), cookie);
    }

    private String login(String email, String password) throws Exception {
        Csrf csrf = csrf();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private String createActiveUser(String adminToken, String name, String email,
                                    String initialPassword, String finalPassword,
                                    String role) throws Exception {
        createUser(adminToken, name, email, initialPassword, role);
        String initialToken = login(email, initialPassword);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(initialToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        mockMvc.perform(get("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(initialToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

        mockMvc.perform(post("/api/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(initialToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", initialPassword,
                                "newPassword", finalPassword))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/appointments").header(HttpHeaders.AUTHORIZATION, bearer(initialToken)))
                .andExpect(status().isUnauthorized());

        return login(email, finalPassword);
    }

    private void createUser(String adminToken, String name, String email, String password,
                            String role) throws Exception {
        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "email", email,
                                "password", password,
                                "role", role))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    private String appointmentPayload(long orderId, String supplierName, String supplierEmail,
                                      String date, int quantity, String notes) {
        return """
                {
                  "orderId": %d,
                  "supplierName": "%s",
                  "supplierEmail": "%s",
                  "truckPlate": "ABC1D23",
                  "date": "%s",
                  "timeSlotId": 1,
                  "truckTypeId": 1,
                  "palletizationTypeId": 2,
                  "palletQuantity": %d,
                  "notes": %s
                }
                """.formatted(orderId, supplierName, supplierEmail, date, quantity,
                notes == null ? "null" : "\"" + notes + "\"");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Csrf(String headerName, String token, Cookie cookie) {
    }
}
