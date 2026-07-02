package com.sandbox.javaaisandbox.fitness.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.sandbox.javaaisandbox.fitness.domain.FitnessPlan;
import com.sandbox.javaaisandbox.fitness.domain.FitnessProfile;
import com.sandbox.javaaisandbox.fitness.domain.NutritionPlan;
import com.sandbox.javaaisandbox.fitness.domain.WorkoutPlan;

@Agent(description = "Builds a personalized fitness plan (workout + nutrition) from a free-text goal description")
public class FitnessAgent {

    @Action(description = "Extract a structured fitness profile from the user's free-text request")
    public FitnessProfile buildProfile(UserInput input, Ai ai) {
        return ai.withDefaultLlm().createObject(
                """
                        Extract a fitness profile from this request. Infer sensible defaults
                        for anything not explicitly stated:
                        %s""".formatted(input.getContent()),
                FitnessProfile.class
        );
    }

    @Action(description = "Generate a weekly workout plan for the given fitness profile")
    public WorkoutPlan generateWorkoutPlan(FitnessProfile profile, Ai ai) {
        return ai.withDefaultLlm().createObject(
                """
                        Create a weekly workout plan for someone with this profile:
                        goal: %s, experience level: %s, days per week: %d,
                        available equipment: %s, constraints: %s""".formatted(
                        profile.goal(), profile.experienceLevel(), profile.daysPerWeek(),
                        profile.availableEquipment(), profile.constraints()),
                WorkoutPlan.class
        );
    }

    @Action(description = "Generate nutrition guidance for the given fitness profile")
    public NutritionPlan generateNutritionPlan(FitnessProfile profile, Ai ai) {
        return ai.withDefaultLlm().createObject(
                """
                        Create daily nutrition guidance (calories, macro split, meal ideas)
                        for someone with this profile:
                        goal: %s, experience level: %s, constraints: %s""".formatted(
                        profile.goal(), profile.experienceLevel(), profile.constraints()),
                NutritionPlan.class
        );
    }

    @AchievesGoal(description = "Produce a complete personalized fitness plan combining workout and nutrition guidance")
    @Action
    public FitnessPlan assemblePlan(FitnessProfile profile, WorkoutPlan workoutPlan, NutritionPlan nutritionPlan) {
        return new FitnessPlan(profile, workoutPlan, nutritionPlan);
    }
}
