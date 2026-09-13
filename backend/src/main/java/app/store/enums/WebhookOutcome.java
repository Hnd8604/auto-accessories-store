package app.store.enums;

import lombok.Getter;

@Getter
public enum WebhookOutcome {

    PAID("Order has been marked as paid"),
    ORDER_ALREADY_PAID("Order was already paid; transaction recorded for reconciliation"),
    UNDERPAID("Transfer amount is less than the order total; transaction recorded as unpaid"),
    DUPLICATE_TRANSACTION("Transaction has already been processed"),
    NOT_SUCCESS("Transaction is not reported as successful; ignored"),
    ORDER_NOT_FOUND("No payment link matches the payOS order code"),
    INVALID_PAYLOAD("Webhook payload is missing required fields"),
    ;

    private final String message;

    private WebhookOutcome(String message) {
        this.message = message;
    }
}
