/*
 * Aqui vive o agendamento de coleta — a entidade central do sistema.
 *
 * É um record imutável: alterar data, placa ou quantidade devolve uma cópia
 * nova em vez de mexer no objeto. Isso existe de propósito: um agendamento
 * já salvo não pode mudar de escondido enquanto ainda está sendo usado por
 * outra parte do código, o que tornaria a auditoria e o cache duvidosos.
 *
 * Todas as regras de integridade ficam no construtor compacto. Na prática,
 * se existe um Appointment, ele já é válido — não há meio-preenchido.
 *
 * Camada: domain. Não conhece Spring, JPA nem HTTP; só Java. Quem traduz
 * este record para o banco é o mapper da infraestrutura, e quem o expõe na
 * API é o mapper da apresentação.
 */
package com.redekrill.scheduling.domain.model;

import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/**
 * Representa uma coleta agendada: quem fornece, quando, com qual caminhão,
 * quantos pallets e em que situação está (ativa ou cancelada).
 *
 * O que ela guarda: dados do fornecedor, a janela de coleta (data + faixa
 * de horário), os tipos cadastrados (caminhão e paletização) e o histórico
 * de cancelamento/criação.
 *
 * O que ela NÃO faz: decidir se há capacidade disponível, consultar banco,
 * saber se o usuário tem permissão. Essas decisões vêm de fora — capacidade
 * fica em {@link com.redekrill.scheduling.domain.policy.CapacityPolicy} e
 * orquestração fica nos casos de uso.
 */
public record Appointment(
        Long id,
        // Identificador do pedido no sistema do fornecedor. É único por agendamento
        // e serve de trava anti-duplicidade: repetir o mesmo número devolve conflito.
        Long orderId,
        String supplierName,
        String supplierEmail,
        // Placa sempre em maiúsculas: a normalização acontece aqui, no domínio,
        // para que a busca por placa não dependa de como o usuário digitou.
        String truckPlate,
        LocalDate date,
        TimeSlot timeSlot,
        TruckType truckType,
        PalletizationType palletizationType,
        // Quantidade declarada pelo fornecedor, em pallets físicos.
        int palletQuantity,
        // Quantidade já convertida conforme o tipo de paletização (carga solta
        // conta dobro). É o número que pesa na faixa de horário.
        int effectivePallets,
        String notes,
        AppointmentStatus status,
        // Só têm valor com status CANCELED; o construtor também garante o contrário:
        // agendamento ativo não carrega dados de cancelamento.
        User canceledBy,
        Instant canceledAt,
        User createdBy,
        Instant createdAt,
        Instant updatedAt,
        // Controle otimista do JPA. Precisa existir no domínio para que a cópia
        // gerada por update()/cancel() preserve a versão lida do banco.
        long version
) {

    private static final int SUPPLIER_NAME_MAX_LENGTH = 80;
    private static final int SUPPLIER_EMAIL_MAX_LENGTH = 80;
    private static final int NOTES_MAX_LENGTH = 500;

    public Appointment {
        // Validação de identidade e integridade dos dados. Ela corre aqui, e não no
        // controller, porque a regra precisa valer para qualquer caminho que construa
        // um agendamento: o caso de uso de criação, o de edição, o adaptador que lê o
        // banco e os testes.
        orderId = DomainValidation.requirePositive(
                orderId, "ID Pedido é obrigatório e deve ser maior que zero.");
        supplierName = DomainValidation.requireText(
                supplierName, "Nome do fornecedor é obrigatório.");
        supplierName = DomainValidation.requireMaxLength(
                supplierName, SUPPLIER_NAME_MAX_LENGTH,
                "Nome do fornecedor deve ter no máximo 80 caracteres.");
        supplierEmail = DomainValidation.requireEmail(
                supplierEmail, "E-mail do fornecedor é obrigatório.");
        supplierEmail = DomainValidation.requireMaxLength(
                supplierEmail, SUPPLIER_EMAIL_MAX_LENGTH,
                "E-mail do fornecedor deve ter no máximo 80 caracteres.");
        truckPlate = DomainValidation.requireText(
                truckPlate, "Placa do caminhão é obrigatória.").toUpperCase(Locale.ROOT);
        Objects.requireNonNull(date, "Data do agendamento é obrigatória.");
        Objects.requireNonNull(timeSlot, "Faixa horária é obrigatória.");
        Objects.requireNonNull(truckType, "Tipo de caminhão é obrigatório.");
        Objects.requireNonNull(palletizationType, "Tipo de paletização é obrigatório.");
        DomainValidation.requirePositive(
                palletQuantity, "Quantidade de pallets deve ser maior que zero.");
        DomainValidation.requirePositive(
                effectivePallets, "Quantidade efetiva de pallets deve ser maior que zero.");
        notes = DomainValidation.optionalText(
                notes, NOTES_MAX_LENGTH, "Observação deve ter no máximo 500 caracteres.");
        Objects.requireNonNull(status, "Status do agendamento é obrigatório.");
        // Cancelamento é estado, não linha removida: o histórico continua inteiro.
        // Por isso quem cancela precisa dizer quem e quando — e quem está ativo
        // não pode carregar esses dados por engano.
        if (status == AppointmentStatus.CANCELED) {
            Objects.requireNonNull(canceledBy, "Usuário do cancelamento é obrigatório.");
            Objects.requireNonNull(canceledAt, "Data do cancelamento é obrigatória.");
        } else if (canceledBy != null || canceledAt != null) {
            throw new DomainException("Agendamento ativo não pode ter dados de cancelamento.");
        }
        Objects.requireNonNull(createdBy, "Usuário criador é obrigatório.");
        Objects.requireNonNull(createdAt, "Data de criação é obrigatória.");
        Objects.requireNonNull(updatedAt, "Data de atualização é obrigatória.");
    }

    /**
     * Construtor de compatibilidade para fixtures e testes legados.
     */
    public Appointment(Long id, Long orderId, String supplierName, String supplierEmail,
                       String truckPlate, LocalDate date, TimeSlot timeSlot, TruckType truckType,
                       PalletizationType palletizationType, int palletQuantity,
                       int effectivePallets, String notes, AppointmentStatus status,
                       User canceledBy, Instant canceledAt, User createdBy, Instant createdAt,
                       Instant updatedAt) {
        this(id, orderId, supplierName, supplierEmail, truckPlate, date, timeSlot, truckType,
                palletizationType, palletQuantity, effectivePallets, notes, status, canceledBy,
                canceledAt, createdBy, createdAt, updatedAt, 0L);
    }

    /**
     * Monta um agendamento novo, ainda sem id.
     *
     * É o caminho usado pelo caso de uso de criação: recebe os dados digitados
     * pelo usuário, calcula a quantidade efetiva de pallets de acordo com o tipo
     * de paletização e devolve o agendamento já como ATIVO, com a mesma marcação
     * de tempo para criação e atualização.
     *
     * O id fica nulo de propósito — quem dá identidade ao registro é o banco.
     *
     * @param now o relógio injetado por quem chama, para o teste controlar o tempo
     * @return um agendamento ativo, pronto para ser persistido
     */
    public static Appointment create(
            Long orderId,
            String supplierName,
            String supplierEmail,
            String truckPlate,
            LocalDate date,
            TimeSlot timeSlot,
            TruckType truckType,
            PalletizationType palletizationType,
            int palletQuantity,
            String notes,
            User createdBy,
            Instant now
    ) {
        int effectivePallets = calculateEffectivePallets(palletQuantity, palletizationType);
        return new Appointment(
                null,
                orderId,
                supplierName,
                supplierEmail,
                truckPlate,
                date,
                timeSlot,
                truckType,
                palletizationType,
                palletQuantity,
                effectivePallets,
                notes,
                AppointmentStatus.ACTIVE,
                null,
                null,
                createdBy,
                now,
                now,
                0L
        );
    }

    /**
     * Devolve a mesma cópia com o id preenchido, depois do insert.
     *
     * Existe porque o record é imutável: o adapter da infraestrutura salva a linha,
     * recebe o id gerado pelo banco e precisa devolver um agendamento completo para
     * a aplicação sem recorrer a setter.
     */
    public Appointment withId(Long newId) {
        return new Appointment(
                newId, orderId, supplierName, supplierEmail, truckPlate, date, timeSlot,
                truckType, palletizationType, palletQuantity, effectivePallets, notes, status,
                canceledBy, canceledAt, createdBy, createdAt, updatedAt, version
        );
    }

    /**
     * Aplica uma edição nos dados do agendamento.
     *
     * Recalcula a quantidade efetiva de pallets, preserva o id, a criação original
     * e a versão, e só atualiza a marca de tempo de edição. Se o agendamento já
     * estiver cancelado, a operação é recusada: cancelamento é decisão final.
     *
     * @param now a hora da alteração, vinda do relógio injetado
     * @return uma cópia atualizada
     * @throws ConflictException se o agendamento estiver cancelado
     */
    public Appointment update(
            Long newOrderId,
            String newSupplierName,
            String newSupplierEmail,
            String newTruckPlate,
            LocalDate newDate,
            TimeSlot newTimeSlot,
            TruckType newTruckType,
            PalletizationType newPalletizationType,
            int newPalletQuantity,
            String newNotes,
            Instant now
    ) {
        if (status == AppointmentStatus.CANCELED) {
            throw new ConflictException("Agendamento cancelado não pode ser alterado.");
        }
        int effectivePallets = calculateEffectivePallets(newPalletQuantity, newPalletizationType);
        return new Appointment(
                id,
                newOrderId,
                newSupplierName,
                newSupplierEmail,
                newTruckPlate,
                newDate,
                newTimeSlot,
                newTruckType,
                newPalletizationType,
                newPalletQuantity,
                effectivePallets,
                newNotes,
                status,
                canceledBy,
                canceledAt,
                createdBy,
                createdAt,
                now,
                version
        );
    }

    /**
     * Cancela o agendamento sem apagar nada.
     *
     * Guarda quem cancelou e quando e muda o status. A partir daí o agendamento
     * sai das somas de capacidade, porque os totais são recalculados a partir dos
     * registros ativos — não existe contador acumulado que precise ser diminuído.
     * Cancelar duas vezes é erro, não sucesso silencioso.
     *
     * @param actor o usuário que pediu o cancelamento
     * @return uma cópia com status CANCELED
     */
    public Appointment cancel(User actor, Instant now) {
        if (status == AppointmentStatus.CANCELED) {
            throw new ConflictException("Agendamento já está cancelado.");
        }
        Objects.requireNonNull(actor, "Usuário do cancelamento é obrigatório.");
        Objects.requireNonNull(now, "Data do cancelamento é obrigatória.");
        return new Appointment(
                id, orderId, supplierName, supplierEmail, truckPlate, date, timeSlot,
                truckType, palletizationType, palletQuantity, effectivePallets, notes,
                AppointmentStatus.CANCELED, actor, now, createdBy, createdAt, now, version
        );
    }

    /**
     * Quantidade física declarada, usada no limite diário.
     */
    public int dailyPallets() {
        return palletQuantity;
    }

    /**
     * Quantidade considerada na faixa horária. Para carga não paletizada,
     * corresponde ao dobro da quantidade declarada.
     */
    public int slotPallets() {
        return effectivePallets;
    }

    private static int calculateEffectivePallets(int palletQuantity, PalletizationType palletizationType) {
        DomainValidation.requirePositive(palletQuantity, "Quantidade de pallets deve ser maior que zero.");
        return Math.multiplyExact(palletQuantity, palletizationType.capacityMultiplier());
    }
}
