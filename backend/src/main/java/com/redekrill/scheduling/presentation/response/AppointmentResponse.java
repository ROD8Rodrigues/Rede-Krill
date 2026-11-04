/*
 * O agendamento inteiro como o cliente o vê.
 *
 * Vinte campos em três blocos: o que foi digitado (pedido, fornecedor, placa, data,
 * quantidade, observação), as referências expandidas (faixa, tipo de caminhão, tipo de
 * paletização) e a situação de capacidade já com este agendamento somado — effectivePallets,
 * dailyPallets e slotPallets vêm junto para a leitura não exigir outra consulta.
 *
 * O resto conta o ciclo de vida: status, quem cancelou e quando, quem criou e as duas
 * datas de escrita. canceledBy e createdBy são resumos de três campos (id, nome, e-mail),
 * não a conta inteira — papel, flag de troca de senha e datas de cadastro de uma conta não
 * pertencem à leitura de um agendamento.
 *
 * truckType vem só com id e nome, enquanto palletizationType traz também o multiplicador
 * — a edição precisa do multiplicador para explicar a ocupação, e do nome do caminhão
 * para preencher o select.
 */
package com.redekrill.scheduling.presentation.response;

import com.redekrill.scheduling.domain.model.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Projeção de agendamento para a API. Não carrega hash, token nem estado interno de
 * transação: só o que já foi decidido pela aplicação.
 */
public record AppointmentResponse(
        Long id,
        Long orderId,
        String supplierName,
        String supplierEmail,
        String truckPlate,
        LocalDate date,
        TimeSlotResponse timeSlot,
        TruckTypeSummaryResponse truckType,
        PalletizationTypeResponse palletizationType,
        int palletQuantity,
        int effectivePallets,
        int dailyPallets,
        int slotPallets,
        String notes,
        AppointmentStatus status,
        UserSummaryResponse canceledBy,
        Instant canceledAt,
        UserSummaryResponse createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public record TruckTypeSummaryResponse(Long id, String name) {
    }

    public record UserSummaryResponse(Long id, String name, String email) {
    }
}
