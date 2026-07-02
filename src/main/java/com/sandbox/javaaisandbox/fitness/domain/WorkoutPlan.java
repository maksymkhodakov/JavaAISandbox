package com.sandbox.javaaisandbox.fitness.domain;

import java.util.List;

public record WorkoutPlan(List<WorkoutDay> days) {

    public record WorkoutDay(String day, String focus, List<String> exercises) {
    }
}
