package com.abhishekraj0.core.loop;

import com.abhishekraj0.api.model.Cost;
import com.abhishekraj0.api.model.CostCalculator;
import com.abhishekraj0.api.model.ModelMetadata;
import com.abhishekraj0.api.model.TokenUsage;

/**
 * Default implementation of CostCalculator using standard rates per million tokens.
 */
public class DefaultCostCalculator implements CostCalculator {

    @Override
    public Cost calculate(ModelMetadata model, TokenUsage usage) {
        if (usage == null) {
            return new Cost(0.0, 0.0, 0.0);
        }

        double inputRate = 5.0 / 1_000_000.0; // Default $5 per million tokens
        double outputRate = 15.0 / 1_000_000.0; // Default $15 per million tokens

        String provider = model != null && model.provider() != null ? model.provider().toLowerCase() : "";
        if (provider.contains("openai") || provider.contains("azure")) {
            inputRate = 2.50 / 1_000_000.0;
            outputRate = 10.00 / 1_000_000.0;
        } else if (provider.contains("anthropic")) {
            inputRate = 3.00 / 1_000_000.0;
            outputRate = 15.00 / 1_000_000.0;
        } else if (provider.contains("gemini")) {
            inputRate = 0.075 / 1_000_000.0;
            outputRate = 0.300 / 1_000_000.0;
        }

        double inputCost = usage.promptTokens() * inputRate;
        double outputCost = usage.completionTokens() * outputRate;
        double totalCost = inputCost + outputCost;

        return new Cost(inputCost, outputCost, totalCost);
    }
}
