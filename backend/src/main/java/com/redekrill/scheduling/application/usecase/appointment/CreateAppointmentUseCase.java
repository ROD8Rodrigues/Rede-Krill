/*
 * O caso de uso que materializa um agendamento novo.
 *
 * É o fluxo mais completo do sistema e a ordem das etapas importa: primeiro o
 * validate(), que recusa a ausência dos campos obrigatórios sem tocar no banco;
 * depois a transação, onde tudo que altera estado acontece junto — a reserva de
 * unicidade do pedido, o insert, a linha de auditoria e a entrada na fila de
 * e-mail. Se qualquer uma dessas falhar, as outras quatro falham também, e não
 * sobra agendamento sem registro nem aviso sem agendamento.
 *
 * A capacidade é consultada dentro da mesma transação de propósito. Os totais
 * "antes" vêm de uma soma sobre os registros ativos; medir antes de abrir a
 * transação permitiria que duas requisições concorrentes lessem o mesmo número e
 * cada uma se achasse dentro do limite.
 *
 * E, ainda assim, estourar o limite não impede nada: CapacityPolicy produz avisos,
 * este caso de uso grava o agendamento e devolve os avisos na resposta. O e-mail
 * leva os mesmos avisos, porque quem entrega precisa saber que a faixa ficou
 * acima do limite monitorado. O envio não acontece aqui — o que se grava é a
 * intenção na fila.
 *
 * Camada: application. Não vê HttpServletRequest nem entidade JPA; recebe um
 * CreateAppointmentCommand já montado pelo controller e devolve um resultado.
 */
package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentCommandResult;
import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.dto.appointment.CapacityResult;
import com.redekrill.scheduling.application.dto.appointment.CreateAppointmentCommand;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.application.service.EmailOutboxService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.model.TruckType;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.policy.CapacityAssessment;
import com.redekrill.scheduling.domain.policy.CapacityPolicy;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;
import com.redekrill.scheduling.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Orquestra a criação de um agendamento: resolve os cadastros referenciados,
 * reserva a unicidade do pedido, mede a capacidade e grava tudo junto do aviso ao
 * fornecedor.
 *
 * O que esta classe faz: coordenar. As regras estão no domínio — Appointment cria
 * e valida o registro, CapacityPolicy compara com os limites — e a persistência
 * está nos repositórios injetados.
 *
 * O que ela NÃO faz: decidir sobre HTTP, status code nem formatação de resposta,
 * e também não bloqueia quando a capacidade estoura. O aviso é devolvido, não
 * lançado.
 */
public class CreateAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TruckTypeRepository truckTypeRepository;
    private final PalletizationTypeRepository palletizationTypeRepository;
    private final UserRepository userRepository;
    private final CapacityPolicy capacityPolicy;
    private final AuditService auditService;
    private final EmailOutboxService emailOutboxService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public CreateAppointmentUseCase(AppointmentRepository appointmentRepository,
                                    TimeSlotRepository timeSlotRepository,
                                    TruckTypeRepository truckTypeRepository,
                                    PalletizationTypeRepository palletizationTypeRepository,
                                    UserRepository userRepository,
                                    CapacityPolicy capacityPolicy,
                                    AuditService auditService,
                                    EmailOutboxService emailOutboxService,
                                    TransactionRunner transactionRunner,
                                    Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.truckTypeRepository = truckTypeRepository;
        this.palletizationTypeRepository = palletizationTypeRepository;
        this.userRepository = userRepository;
        this.capacityPolicy = capacityPolicy;
        this.auditService = auditService;
        this.emailOutboxService = emailOutboxService;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    /**
     * Grava o agendamento e devolve o resultado com os avisos de capacidade.
     *
     * As três buscas por id existem porque o comando traz identificadores, não
     * objetos: quem chama pela API não tem como saber se a faixa informada ainda
     * está cadastrada. A resolução acontece dentro da transação, junto do insert,
     * para que um cadastro excluído no meio do caminho não produza agendamento
     * órfão.
     *
     * @param command dados do agendamento, com o e-mail de quem está autenticado.
     * @return o agendamento criado, os avisos do lançamento, a ocupação resultante
     *         e a fila de e-mail apenas acionada — emailQueued verdadeiro, emailSent
     *         falso, porque o envio é do processador.
     */
    public AppointmentCommandResult execute(CreateAppointmentCommand command) {
        validate(command);

        PreparedAppointment prepared = transactionRunner.execute(() -> {
            TimeSlot timeSlot = timeSlotRepository.findById(command.timeSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Faixa horária não encontrada."));
            TruckType truckType = truckTypeRepository.findById(command.truckTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de caminhão não encontrado."));
            PalletizationType palletizationType = palletizationTypeRepository
                    .findById(command.palletizationTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de paletização não encontrado."));

            User actor = userRepository.findByEmail(command.actorEmail())
                    .filter(User::active)
                    .orElseThrow(() -> new AuthenticationFailedException(
                            "Usuário autenticado não encontrado ou inativo."));

            if (appointmentRepository.existsByOrderId(command.orderId())) {
                throw new ConflictException(
                        "Já existe um agendamento com o ID Pedido %d.".formatted(command.orderId()));
            }

            Appointment appointment = Appointment.create(
                    command.orderId(),
                    command.supplierName(),
                    command.supplierEmail(),
                    command.truckPlate(),
                    command.date(),
                    timeSlot,
                    truckType,
                    palletizationType,
                    command.palletQuantity(),
                    command.notes(),
                    actor,
                    Instant.now(clock)
            );

            long dailyPallets = appointmentRepository.sumDailyPalletsByDate(appointment.date());
            long slotPallets = appointmentRepository.sumSlotPalletsByDateAndTimeSlot(
                    appointment.date(), timeSlot.id());
            CapacityAssessment assessment = capacityPolicy.assess(
                    dailyPallets, slotPallets,
                    appointment.dailyPallets(), appointment.slotPallets());

            Appointment saved = appointmentRepository.save(appointment);
            auditService.record(
                    actor.email(),
                    AuditAction.CREATE,
                    "Appointment",
                    saved.id(),
                    "Agendamento criado: ID Pedido=%d, fornecedor=%s, data=%s, faixa=%s, pallets=%d, faixa=%d"
                            .formatted(saved.orderId(), saved.supplierName(), saved.date(),
                                    saved.timeSlot().label(), saved.dailyPallets(), saved.slotPallets())
            );
            emailOutboxService.enqueue(saved, assessment.warnings());

            return new PreparedAppointment(saved, assessment);
        });

        return new AppointmentCommandResult(
                AppointmentResult.from(prepared.appointment()),
                prepared.assessment().warnings(),
                CapacityResult.from(prepared.assessment()),
                true,
                false
        );
    }

    /**
     * Checagens que não precisam de banco: presença de identidade, data, os três
     * ids de cadastro e o autor da requisição.
     *
     * Formato de texto, tamanho e quantidade positiva não aparecem aqui de
     * propósito — quem confere isso é o construtor de Appointment, uma única vez,
     * para todo caminho que monta um agendamento, inclusive a leitura do banco.
     *
     * @param command o comando recebido do controller.
     * @throws DomainException se um campo obrigatório faltar.
     * @throws AuthenticationFailedException se não houver autor identificado.
     */
    private void validate(CreateAppointmentCommand command) {
        if (command == null) {
            throw new DomainException("Dados do agendamento são obrigatórios.");
        }
        if (command.orderId() == null || command.orderId() <= 0) {
            throw new DomainException("ID Pedido é obrigatório e deve ser maior que zero.");
        }
        if (command.date() == null) {
            throw new DomainException("Data do agendamento é obrigatória.");
        }
        if (command.timeSlotId() == null || command.truckTypeId() == null
                || command.palletizationTypeId() == null) {
            throw new DomainException("Faixa horária, tipo de caminhão e tipo de paletização são obrigatórios.");
        }
        if (command.actorEmail() == null || command.actorEmail().isBlank()) {
            throw new AuthenticationFailedException("Usuário autenticado não identificado.");
        }
    }

    // Carrega os dois valores que saem da transação: o registro salvo e a medição
    // feita contra ele. Existe porque o corpo de transactionRunner devolve uma coisa só.
    private record PreparedAppointment(Appointment appointment, CapacityAssessment assessment) {
    }
}
