package com.adyen.workshop.controllers;

import com.adyen.model.notification.NotificationRequest;
import com.adyen.model.notification.NotificationRequestItem;
import com.adyen.util.HMACValidator;
import com.adyen.workshop.configurations.ApplicationConfiguration;
import com.adyen.workshop.services.RecurringTokenStore;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.SignatureException;

/**
 * REST controller for receiving Adyen webhook notifications
 */
@RestController
public class WebhookController {
    private final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final ApplicationConfiguration applicationConfiguration;

    private final HMACValidator hmacValidator;

    private final RecurringTokenStore recurringTokenStore;

    @Autowired
    public WebhookController(ApplicationConfiguration applicationConfiguration, HMACValidator hmacValidator, RecurringTokenStore recurringTokenStore) {
        this.applicationConfiguration = applicationConfiguration;
        this.hmacValidator = hmacValidator;
        this.recurringTokenStore = recurringTokenStore;
    }

    // Step 16 - Validate the HMAC signature using the ADYEN_HMAC_KEY
    @PostMapping("/webhooks")
    public ResponseEntity<String> webhooks(@RequestBody String json) throws Exception {
        log.info("Received webhook notification");
        
        try {
            // Parse the notification request
            NotificationRequest notificationRequest = NotificationRequest.fromJson(json);
            
            // Process each notification item
            for (NotificationRequestItem item : notificationRequest.getNotificationItems()) {
                
                // Validate HMAC signature if HMAC key is configured
                if (applicationConfiguration.getAdyenHmacKey() != null && !applicationConfiguration.getAdyenHmacKey().isEmpty()) {
                    try {
                        if (!hmacValidator.validateHMAC(item, applicationConfiguration.getAdyenHmacKey())) {
                            log.error("Invalid HMAC signature for notification: {}", item.getPspReference());
                            return ResponseEntity.badRequest().body("[invalid hmac signature]");
                        }
                        log.info("HMAC signature validated successfully");
                    } catch (Exception e) {
                        log.error("Error validating HMAC: {}", e.getMessage(), e);
                        return ResponseEntity.badRequest().body("[hmac validation error]");
                    }
                }
                
                String eventCode = item.getEventCode();
                log.info("Processing webhook - EventCode: {}, PSPReference: {}, Success: {}", 
                         eventCode, item.getPspReference(), item.isSuccess());
                
                // Handle RECURRING_CONTRACT webhook
                if ("RECURRING_CONTRACT".equals(eventCode)) {
                    handleRecurringContractWebhook(item);
                }
                
                // Handle AUTHORISATION webhook
                else if ("AUTHORISATION".equals(eventCode)) {
                    handleAuthorisationWebhook(item);
                }
                
                else {
                    log.info("Unhandled webhook event code: {}", eventCode);
                }
            }
            
            return ResponseEntity.accepted().body("[accepted]");
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("[error processing webhook]");
        }
    }
    
    private void handleRecurringContractWebhook(NotificationRequestItem item) {
        log.info("Handling RECURRING_CONTRACT webhook");
        
        if (item.isSuccess()) {
            // Extract the recurring detail reference
            String recurringDetailReference = item.getAdditionalData() != null ? 
                item.getAdditionalData().get("recurring.recurringDetailReference") : null;
            
            String shopperReference = item.getAdditionalData() != null ? 
                item.getAdditionalData().get("recurring.shopperReference") : null;
            
            if (recurringDetailReference != null && shopperReference != null) {
                log.info("Storing recurring token - Shopper: {}, Token: {}", shopperReference, recurringDetailReference);
                recurringTokenStore.storeToken(shopperReference, recurringDetailReference);
            } else {
                log.warn("RECURRING_CONTRACT webhook missing required data - shopperReference: {}, recurringDetailReference: {}", 
                         shopperReference, recurringDetailReference);
            }
        } else {
            log.warn("RECURRING_CONTRACT webhook failed for PSPReference: {}, Reason: {}", 
                     item.getPspReference(), item.getReason());
        }
    }
    
    private void handleAuthorisationWebhook(NotificationRequestItem item) {
        log.info("Handling AUTHORISATION webhook");
        
        if (item.isSuccess()) {
            log.info("Authorization successful - PSPReference: {}, Amount: {} {}, MerchantReference: {}", 
                     item.getPspReference(),
                     item.getAmount().getValue(),
                     item.getAmount().getCurrency(),
                     item.getMerchantReference());
        } else {
            log.warn("Authorization failed - PSPReference: {}, Reason: {}, MerchantReference: {}", 
                     item.getPspReference(),
                     item.getReason(),
                     item.getMerchantReference());
        }
    }
}