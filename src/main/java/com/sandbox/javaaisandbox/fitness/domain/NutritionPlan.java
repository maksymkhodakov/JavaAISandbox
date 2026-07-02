package com.sandbox.javaaisandbox.fitness.domain;

import java.util.List;

public record NutritionPlan(
        int dailyCalories,
        String macroBreakdown,
        List<String> mealSuggestions
) {
}
