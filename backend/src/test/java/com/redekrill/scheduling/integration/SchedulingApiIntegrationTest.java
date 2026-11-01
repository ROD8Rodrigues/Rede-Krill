package com.redekrill.scheduling.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redekrill.scheduling.application.port.EmailNotifier;
import com.redekrill.scheduling.domain.model.Appointment;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class SchedulingApiIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@redekrill.com";
    private static final String ADMIN_PASSWORD = "Admin@12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailNotifier emailNotifier;

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

        createUser(adminToken, "Operador", "operador@redekrill.com", "Operador@123", "USER");
        String userToken = login("operador@redekrill.com", "Operador@123");

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
                .andExpect(jsonPath("$.emailSent").value(true))
                .andExpect(jsonPath("$.capacity.dailyPallets").value(30))
                .andExpect(jsonPath("$.capacity.slotPallets").value(60))
                .andExpect(jsonPath("$.capacity.dailyOccupancyPercentage").value(6.3))
                .andExpect(jsonPath("$.capacity.slotOccupancyPercentage").value(50.0))
                .andReturn();

        Instant persistedCreatedAt = Instant.parse(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .at("/appointment/createdAt").asText());
        assertThat(persistedCreatedAt)
                .isBetween(Instant.now().minusSeconds(300), Instant.now().plusSeconds(300));

        verify(emailNotifier).sendAppointmentConfirmation(any(Appointment.class), anyList());

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
                .andExpect(jsonPath("$.totalEffectivePallets").value(60));

        mockMvc.perform(get("/api/audit-logs").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.entityName == 'Appointment')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'CREATE')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'LOGIN')]").isNotEmpty());

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
    void keepsAppointmentAndWarnsWhenEmailFails() throws Exception {
        doThrow(new IllegalStateException("SMTP indisponível"))
                .when(emailNotifier).sendAppointmentConfirmation(any(Appointment.class), anyList());

        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentPayload(9201, "Fornecedor Sem Email",
                                "sem-email@example.com", "2026-05-20", 10, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailSent").value(false))
                .andExpect(jsonPath("$.warnings[0]").value(
                        "O agendamento foi salvo, mas não foi possível enviar o e-mail de "
                                + "confirmação. O fornecedor não foi notificado."));

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
        createUser(adminToken, "Negado", "negado@redekrill.com", "Negado@123", "USER");
        String userToken = login("negado@redekrill.com", "Negado@123");

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentPayload(9401, "Fornecedor Negado", "negado@example.com",
                                "2026-07-10", 10, null)))
                .andExpect(status().isForbidden());

        String refreshedAdminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/audit-logs").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(refreshedAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.action == 'LOGOUT')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'ACCESS_DENIED')]").isNotEmpty());
    }

    @Test
    void rejectsInvalidCredentialsAndUnauthenticatedRequests() throws Exception {
        mockMvc.perform(post("/api/auth/login")
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

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private void createUser(String adminToken, String name, String email, String password,
                            String role) throws Exception {
        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "name", name,
                                "email", email,
                                "password", password,
                                "role", role))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value(role));
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
}
