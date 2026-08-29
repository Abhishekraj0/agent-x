package com.abhishekraj0.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for configuring AgentX components.
 */
@ConfigurationProperties(prefix = "agentx")
public class AgentProperties {

    private String modelId = "mock-model";
    private String modelProvider = "mock-provider";
    private int maxIterations = 10;
    private double temperature = 0.7;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getModelProvider() {
        return modelProvider;
    }

    public void setModelProvider(String modelProvider) {
        this.modelProvider = modelProvider;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
