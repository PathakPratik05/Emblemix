package com.emblemix.monitoring.salesforce.service;

import com.emblemix.monitoring.salesforce.dto.SalesforceAuthUrlResponse;
import com.emblemix.system.ConnectedSystem;

import java.util.UUID;

/**
 * Service interface for Salesforce OAuth2 authorization flow, token exchange, and disconnection.
 */
public interface SalesforceOAuthService {

    /**
     * Generates a CSRF-protected OAuth2 authorization URL targeting Salesforce production or sandbox login.
     */
    SalesforceAuthUrlResponse generateAuthorizationUrl(UUID systemId, boolean isSandbox);

    /**
     * Exchanges an authorization code for access and refresh tokens, securely persisting encrypted credentials.
     */
    ConnectedSystem handleCallback(String code, String state);

    /**
     * Revokes active tokens and disconnects a Salesforce organization from Emblemix.
     */
    void disconnect(UUID systemId);
}
