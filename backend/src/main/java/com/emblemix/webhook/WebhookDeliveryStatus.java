package com.emblemix.webhook;

public enum WebhookDeliveryStatus {
    SUCCESS,
    INVALID_SIGNATURE,
    INVALID_PAYLOAD,
    PROCESSING_ERROR,
    SYSTEM_DISABLED
}
