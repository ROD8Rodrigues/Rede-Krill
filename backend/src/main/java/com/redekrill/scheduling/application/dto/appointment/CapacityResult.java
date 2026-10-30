package com.redekrill.scheduling.application.dto.appointment;

import com.redekrill.scheduling.domain.policy.CapacityAssessment;

public record CapacityResult(
        long dailyPallets,
        long slotPallets,
        int dailyLimit,
        int slotLimit
) {

    public static CapacityResult from(CapacityAssessment assessment) {
        return new CapacityResult(
                assessment.dailyPallets(),
                assessment.slotPallets(),
                assessment.dailyLimit(),
                assessment.slotLimit()
        );
    }
}
