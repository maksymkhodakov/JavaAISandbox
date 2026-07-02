package com.sandbox.javaaisandbox.fitness.domain;

public record FitnessPlan(
        FitnessProfile profile,
        WorkoutPlan workoutPlan,
        NutritionPlan nutritionPlan
) {
}
