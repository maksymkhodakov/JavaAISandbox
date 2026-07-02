package com.sandbox.javaaisandbox.fitness.controller;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.domain.io.UserInput;
import com.sandbox.javaaisandbox.fitness.domain.FitnessPlan;
import com.sandbox.javaaisandbox.fitness.domain.FitnessRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fitness")
@CrossOrigin(origins = "*")  // lock down in production
public class FitnessController {

    private final AgentPlatform agentPlatform;

    public FitnessController(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    @PostMapping("/plan")
    public FitnessPlan createPlan(@RequestBody FitnessRequest request) {
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }

        AgentInvocation<FitnessPlan> invocation = AgentInvocation.builder(agentPlatform).build(FitnessPlan.class);
        return invocation.invoke(new UserInput(request.description()));
    }
}
