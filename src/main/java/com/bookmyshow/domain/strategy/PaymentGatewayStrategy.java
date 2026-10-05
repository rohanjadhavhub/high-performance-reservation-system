package com.bookmyshow.domain.strategy;

/**
 * Strategy Pattern interface for payment gateways.
 * Allows swapping payment providers without changing core booking logic.
 */
public interface PaymentGatewayStrategy {

    /**
     * @return the name of this gateway (e.g., "RAZORPAY", "DODO")
     */
    String getGatewayName();

    /**
     * Initiate a charge with the external payment gateway.
     *
     * @param amount   the amount to charge
     * @param currency the currency code (e.g., "INR")
     * @param metadata any metadata to attach (e.g., reservation ID)
     * @return the gateway's transaction ID
     */
    String initiateCharge(Double amount, String currency, String metadata);

    /**
     * Verify a payment status with the gateway.
     *
     * @param gatewayTransactionId the transaction ID to verify
     * @return true if the payment is confirmed successful
     */
    boolean verifyPayment(String gatewayTransactionId);

    /**
     * Initiate a refund for a completed payment.
     *
     * @param gatewayTransactionId the original transaction ID
     * @param amount               the amount to refund
     * @return the refund transaction ID
     */
    String refund(String gatewayTransactionId, Double amount);
}
