package com.sandbox.javaaisandbox.fitness.domain;

public record FitnessProfile(
        String goal,
        String experienceLevel,
        int daysPerWeek,
        String availableEquipment,
        String constraints
) {
}
