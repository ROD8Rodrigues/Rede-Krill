package com.redekrill.scheduling.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAppointmentRequest(
        @NotNull(message = "ID Pedido é obrigatório.")
        @Positive(message = "ID Pedido deve ser maior que zero.")
        Long orderId,

        @NotBlank(message = "Nome do fornecedor é obrigatório.")
        @Size(max = 80, message = "Nome do fornecedor deve ter no máximo 80 caracteres.")
        String supplierName,

        @NotBlank(message = "E-mail do fornecedor é obrigatório.")
        @Email(message = "E-mail do fornecedor inválido.")
        @Size(max = 80, message = "E-mail do fornecedor deve ter no máximo 80 caracteres.")
        String supplierEmail,

        @NotBlank(message = "Placa do caminhão é obrigatória.")
        @Size(max = 20, message = "Placa do caminhão deve ter no máximo 20 caracteres.")
        String truckPlate,

        @NotNull(message = "Data do agendamento é obrigatória.")
        LocalDate date,

        @NotNull(message = "Faixa horária é obrigatória.")
        Long timeSlotId,

        @NotNull(message = "Tipo de caminhão é obrigatório.")
        Long truckTypeId,

        @NotNull(message = "Tipo de paletização é obrigatório.")
        Long palletizationTypeId,

        @Min(value = 1, message = "Quantidade de pallets deve ser maior que zero.")
        @Max(value = 1_000_000, message = "Quantidade de pallets excede o limite permitido.")
        int palletQuantity,

        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres.")
        String notes
) {
}
