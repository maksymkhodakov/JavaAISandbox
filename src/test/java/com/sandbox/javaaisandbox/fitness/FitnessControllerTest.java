package com.sandbox.javaaisandbox.fitness;

import com.embabel.agent.core.AgentPlatform;
import com.sandbox.javaaisandbox.fitness.controller.FitnessController;
import com.sandbox.javaaisandbox.fitness.domain.FitnessRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FitnessControllerTest {

    private final AgentPlatform agentPlatform = Mockito.mock(AgentPlatform.class);
    private final FitnessController controller = new FitnessController(agentPlatform);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsBlankOrMissingDescription(String description) {
        assertThatThrownBy(() -> controller.createPlan(new FitnessRequest(description)))
                .isInstanceOf(IllegalArgumentException.class);

        Mockito.verifyNoInteractions(agentPlatform);
    }
}
