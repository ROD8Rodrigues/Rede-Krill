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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                        .content(appointmentPayload("Fornecedor USER", "user@example.com", 30)))
                .andExpect(status().isForbidden());

        MvcResult created = mockMvc.perform(post("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentPayload("Fornecedor Krill", "fornecedor@example.com", 30)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointment.supplierName").value("Fornecedor Krill"))
                .andExpect(jsonPath("$.appointment.truckPlate").value("ABC1D23"))
                .andExpect(jsonPath("$.appointment.date").value("2026-03-10"))
                .andExpect(jsonPath("$.appointment.timeSlot.startTime").value("08:00"))
                .andExpect(jsonPath("$.appointment.timeSlot.endTime").value("10:00"))
                .andExpect(jsonPath("$.appointment.palletQuantity").value(30))
                .andExpect(jsonPath("$.appointment.effectivePallets").value(60))
                .andExpect(jsonPath("$.warnings").isEmpty())
                .andExpect(jsonPath("$.capacity.dailyPallets").value(60))
                .andExpect(jsonPath("$.capacity.slotPallets").value(60))
                .andReturn();

        Instant persistedCreatedAt = Instant.parse(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .at("/appointment/createdAt").asText());
        assertThat(persistedCreatedAt)
                .isBetween(Instant.now().minusSeconds(300), Instant.now().plusSeconds(300));

        verify(emailNotifier).sendAppointmentConfirmation(any(Appointment.class), anyList());

        mockMvc.perform(get("/api/appointments")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].supplierName").value("Fornecedor Krill"));

        mockMvc.perform(get("/api/reports/appointments")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAppointments").value(1))
                .andExpect(jsonPath("$.totalEffectivePallets").value(60));

        mockMvc.perform(get("/api/audit-logs").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.entityName == 'Appointment')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'CREATE')]").isNotEmpty());

        mockMvc.perform(get("/api/audit-logs").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());
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
                                  "supplierName": "",
                                  "supplierEmail": "invalido",
                                  "truckPlate": "",
                                  "palletQuantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos."))
                .andExpect(jsonPath("$.fieldErrors.supplierName").exists())
                .andExpect(jsonPath("$.fieldErrors.supplierEmail").exists())
                .andExpect(jsonPath("$.fieldErrors.truckPlate").exists())
                .andExpect(jsonPath("$.fieldErrors.palletQuantity").exists());
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

    private String appointmentPayload(String supplierName, String supplierEmail, int quantity) {
        return """
                {
                  "supplierName": "%s",
                  "supplierEmail": "%s",
                  "truckPlate": "ABC1D23",
                  "date": "2026-03-10",
                  "timeSlotId": 1,
                  "truckTypeId": 1,
                  "palletizationTypeId": 2,
                  "palletQuantity": %d
                }
                """.formatted(supplierName, supplierEmail, quantity);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
