package com.bookmyshow.domain.strategy;

import com.bookmyshow.exception.PaymentException;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry for payment gateway strategies.
 * Allows dynamic selection of payment gateway at runtime.
 */
@Component
public class PaymentGatewayRegistry {

    private final Map<String, PaymentGatewayStrategy> strategies;

    public PaymentGatewayRegistry(List<PaymentGatewayStrategy> gatewayStrategies) {
        this.strategies = gatewayStrategies.stream()
            .collect(Collectors.toMap(
                s -> s.getGatewayName().toUpperCase(),
                Function.identity()
            ));
    }

    /**
     * Get a payment gateway by name.
     *
     * @param gatewayName the gateway name (case-insensitive)
     * @return the strategy implementation
     * @throws PaymentException if gateway is not registered
     */
    public PaymentGatewayStrategy getGateway(String gatewayName) {
        PaymentGatewayStrategy strategy = strategies.get(gatewayName.toUpperCase());
        if (strategy == null) {
            throw new PaymentException("Unknown payment gateway: " + gatewayName
                + ". Available: " + strategies.keySet());
        }
        return strategy;
    }

    /**
     * @return the default payment gateway (Razorpay)
     */
    public PaymentGatewayStrategy getDefaultGateway() {
        return getGateway("RAZORPAY");
    }
}
