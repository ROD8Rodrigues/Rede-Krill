package com.redekrill.scheduling.domain.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CapacityPolicyTest {

    private final CapacityPolicy policy = new CapacityPolicy();

    @Test
    void keepsAppointmentWithoutWarningsWhenLimitsAreNotReached() {
        CapacityAssessment assessment = policy.assess(100, 50, 50);

        assertThat(assessment.dailyPallets()).isEqualTo(150);
        assertThat(assessment.slotPallets()).isEqualTo(100);
        assertThat(assessment.dailyLimit()).isEqualTo(CapacityPolicy.DAILY_LIMIT);
        assertThat(assessment.slotLimit()).isEqualTo(CapacityPolicy.TIME_SLOT_LIMIT);
        assertThat(assessment.hasWarnings()).isFalse();
        assertThat(assessment.warnings()).isEmpty();
    }

    @Test
    void doesNotWarnWhenTotalsAreExactlyAtTheLimits() {
        CapacityAssessment assessment = policy.assess(460, 100, 20);

        assertThat(assessment.dailyPallets()).isEqualTo(480);
        assertThat(assessment.slotPallets()).isEqualTo(120);
        assertThat(assessment.hasWarnings()).isFalse();
    }

    @Test
    void warnsWhenOnlyDailyLimitIsExceeded() {
        CapacityAssessment assessment = policy.assess(475, 10, 10);

        assertThat(assessment.dailyPallets()).isEqualTo(485);
        assertThat(assessment.slotPallets()).isEqualTo(20);
        assertThat(assessment.warnings()).hasSize(1);
        assertThat(assessment.warnings().getFirst())
                .contains("Limite diário de 480 pallets excedido", "485", "5");
    }

    @Test
    void warnsWhenOnlySlotLimitIsExceeded() {
        CapacityAssessment assessment = policy.assess(100, 115, 10);

        assertThat(assessment.dailyPallets()).isEqualTo(110);
        assertThat(assessment.slotPallets()).isEqualTo(125);
        assertThat(assessment.warnings()).hasSize(1);
        assertThat(assessment.warnings().getFirst())
                .contains("Limite da faixa horária de 120 pallets excedido", "125", "5");
    }

    @Test
    void warnsForBothLimitsAtOnce() {
        CapacityAssessment assessment = policy.assess(470, 115, 20);

        assertThat(assessment.warnings()).hasSize(2);
        assertThat(assessment.warnings().get(0)).contains("Limite diário");
        assertThat(assessment.warnings().get(1)).contains("Limite da faixa horária");
    }

    @Test
    void rejectsNegativeTotals() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> policy.assess(-1, 0, 10))
                .withMessage("Totais de pallets não podem ser negativos.");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> policy.assess(0, -1, 10))
                .withMessage("Totais de pallets não podem ser negativos.");
    }

    @Test
    void rejectsNonPositiveEffectivePallets() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> policy.assess(0, 0, 0))
                .withMessage("Pallets efetivos do agendamento devem ser positivos.");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> policy.assess(0, 0, -5))
                .withMessage("Pallets efetivos do agendamento devem ser positivos.");
    }
}
