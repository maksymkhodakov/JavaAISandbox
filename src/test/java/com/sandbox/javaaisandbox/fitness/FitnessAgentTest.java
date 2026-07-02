package com.sandbox.javaaisandbox.fitness;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.sandbox.javaaisandbox.fitness.domain.FitnessPlan;
import com.sandbox.javaaisandbox.fitness.domain.FitnessProfile;
import com.sandbox.javaaisandbox.fitness.domain.NutritionPlan;
import com.sandbox.javaaisandbox.fitness.domain.WorkoutPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FitnessAgentTest extends EmbabelMockitoIntegrationTest {

    @Test
    void producesCompletePlanFromDescription() {
        FitnessProfile profile = new FitnessProfile("lose fat", "beginner", 3, "none", "no equipment");
        WorkoutPlan workout = new WorkoutPlan(List.of(new WorkoutPlan.WorkoutDay("Monday", "full body", List.of("squats"))));
        NutritionPlan nutrition = new NutritionPlan(2000, "40/30/30", List.of("oatmeal"));

        whenCreateObject(s -> true, FitnessProfile.class).thenReturn(profile);
        whenCreateObject(s -> true, WorkoutPlan.class).thenReturn(workout);
        whenCreateObject(s -> true, NutritionPlan.class).thenReturn(nutrition);

        AgentInvocation<FitnessPlan> invocation = AgentInvocation.builder(agentPlatform).build(FitnessPlan.class);
        FitnessPlan plan = invocation.invoke(new UserInput("beginner wants to lose fat, 3 days/week, no equipment"));

        assertThat(plan.profile()).isEqualTo(profile);
        assertThat(plan.workoutPlan()).isEqualTo(workout);
        assertThat(plan.nutritionPlan()).isEqualTo(nutrition);
    }
}
