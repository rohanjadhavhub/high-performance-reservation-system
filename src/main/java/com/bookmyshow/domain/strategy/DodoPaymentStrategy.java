package com.bookmyshow.domain.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Simulated DODO payment gateway implementation.
 */
@Slf4j
@Component
public class DodoPaymentStrategy implements PaymentGatewayStrategy {

    @Override
    public String getGatewayName() {
        return "DODO";
    }

    @Override
    public String initiateCharge(Double amount, String currency, String metadata) {
        log.info("[DODO] Initiating charge: amount={}, currency={}, metadata={}",
            amount, currency, metadata);

        String transactionId = "dodo_" + UUID.randomUUID().toString().substring(0, 12);
        log.info("[DODO] Charge initiated. Transaction ID: {}", transactionId);
        return transactionId;
    }

    @Override
    public boolean verifyPayment(String gatewayTransactionId) {
        log.info("[DODO] Verifying payment: {}", gatewayTransactionId);
        return true;
    }

    @Override
    public String refund(String gatewayTransactionId, Double amount) {
        log.info("[DODO] Initiating refund: txn={}, amount={}", gatewayTransactionId, amount);
        String refundId = "dodo_ref_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[DODO] Refund initiated: {}", refundId);
        return refundId;
    }
}
