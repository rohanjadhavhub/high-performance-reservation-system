package com.bookmyshow.domain.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Simulated Razorpay payment gateway implementation.
 * In production, this would call the actual Razorpay SDK/API.
 */
@Slf4j
@Component
public class RazorpayPaymentStrategy implements PaymentGatewayStrategy {

    @Override
    public String getGatewayName() {
        return "RAZORPAY";
    }

    @Override
    public String initiateCharge(Double amount, String currency, String metadata) {
        log.info("[Razorpay] Initiating charge: amount={}, currency={}, metadata={}",
            amount, currency, metadata);

        // Simulate gateway call - in production this calls Razorpay's Orders API
        String transactionId = "rzp_" + UUID.randomUUID().toString().substring(0, 12);
        log.info("[Razorpay] Charge initiated. Transaction ID: {}", transactionId);
        return transactionId;
    }

    @Override
    public boolean verifyPayment(String gatewayTransactionId) {
        log.info("[Razorpay] Verifying payment: {}", gatewayTransactionId);
        // Simulate verification - always returns true in this stub
        return true;
    }

    @Override
    public String refund(String gatewayTransactionId, Double amount) {
        log.info("[Razorpay] Initiating refund: txn={}, amount={}", gatewayTransactionId, amount);
        String refundId = "rzp_ref_" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[Razorpay] Refund initiated: {}", refundId);
        return refundId;
    }
}
