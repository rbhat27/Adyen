package com.adyen.workshop.controllers;

import com.adyen.model.RequestOptions;
import com.adyen.model.checkout.*;
import com.adyen.workshop.configurations.ApplicationConfiguration;
import com.adyen.workshop.services.RecurringTokenStore;
import com.adyen.service.checkout.PaymentsApi;
import com.adyen.service.checkout.RecurringApi;
import com.adyen.service.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for using the Adyen payments API.
 */
@RestController
public class ApiController {
    private final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final ApplicationConfiguration applicationConfiguration;
    private final PaymentsApi paymentsApi;
    private final RecurringApi recurringApi;
    private final RecurringTokenStore recurringTokenStore;

    public ApiController(ApplicationConfiguration applicationConfiguration, PaymentsApi paymentsApi, 
                         RecurringApi recurringApi, RecurringTokenStore recurringTokenStore) {
        this.applicationConfiguration = applicationConfiguration;
        this.paymentsApi = paymentsApi;
        this.recurringApi = recurringApi;
        this.recurringTokenStore = recurringTokenStore;
    }

    // Step 0
    @GetMapping("/hello-world")
    public ResponseEntity<String> helloWorld() throws Exception {
        return ResponseEntity.ok().body("This is the 'Hello World' from the workshop - You've successfully finished step 0!");
    }

    // Step 7
    @PostMapping("/api/paymentMethods")
    public ResponseEntity<PaymentMethodsResponse> paymentMethods() throws IOException, ApiException {

        return ResponseEntity.ok().body(null);
    }

    // Step 9 - Implement the /payments call to Adyen.
    @PostMapping("/api/payments")
    public ResponseEntity<PaymentResponse> payments(@RequestBody PaymentRequest body) throws IOException, ApiException {

        return ResponseEntity.ok().body(null);
    }

    // Step 13 - Handle details call (triggered after Native 3DS2 flow)
    @PostMapping("/api/payments/details")
    public ResponseEntity<PaymentDetailsResponse> paymentsDetails(@RequestBody PaymentDetailsRequest detailsRequest) throws IOException, ApiException
    {

        return ResponseEntity.ok().body(null);
    }

    // Step 14 - Handle Redirect 3DS2 during payment.
    @GetMapping("/handleShopperRedirect")
    public RedirectView redirect(@RequestParam(required = false) String payload, @RequestParam(required = false) String redirectResult) throws IOException, ApiException {

        return null;
    }

    // Subscription endpoints for tokenization and recurring payments
    @PostMapping("/api/subscription-create")
    public ResponseEntity<PaymentResponse> subscriptionCreate(@RequestBody Map<String, Object> body) throws IOException, ApiException {
        log.info("Creating subscription with zero-auth payment");
        
        try {
            // Generate a unique shopper reference
            String shopperReference = body.getOrDefault("shopperReference", "shopper_" + UUID.randomUUID().toString()).toString();
            
            // Create a PaymentRequest for zero-auth
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());
            
            // Set amount to zero for tokenization
            Amount amount = new Amount();
            amount.setCurrency("EUR");
            amount.setValue(0L);
            paymentRequest.setAmount(amount);
            
            // Set payment method data from request
            if (body.containsKey("paymentMethod")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paymentMethodData = (Map<String, Object>) body.get("paymentMethod");
                
                // Create CardDetails for card payment
                CardDetails cardDetails = new CardDetails();
                cardDetails.type(CardDetails.TypeEnum.SCHEME);
                
                if (paymentMethodData.containsKey("encryptedCardNumber")) {
                    cardDetails.encryptedCardNumber(paymentMethodData.get("encryptedCardNumber").toString());
                }
                if (paymentMethodData.containsKey("encryptedExpiryMonth")) {
                    cardDetails.encryptedExpiryMonth(paymentMethodData.get("encryptedExpiryMonth").toString());
                }
                if (paymentMethodData.containsKey("encryptedExpiryYear")) {
                    cardDetails.encryptedExpiryYear(paymentMethodData.get("encryptedExpiryYear").toString());
                }
                if (paymentMethodData.containsKey("encryptedSecurityCode")) {
                    cardDetails.encryptedSecurityCode(paymentMethodData.get("encryptedSecurityCode").toString());
                }
                
                paymentRequest.setPaymentMethod(new CheckoutPaymentMethod(cardDetails));
            }
            
            // Set recurring processing model for tokenization
            paymentRequest.setRecurringProcessingModel(PaymentRequest.RecurringProcessingModelEnum.SUBSCRIPTION);
            paymentRequest.setShopperReference(shopperReference);
            paymentRequest.setStorePaymentMethod(true);
            
            // Set shopper interaction
            paymentRequest.setShopperInteraction(PaymentRequest.ShopperInteractionEnum.ECOMMERCE);
            
            // Set reference
            paymentRequest.setReference("subscription_" + UUID.randomUUID().toString());
            
            // Set return URL for 3DS
            String returnUrl = "http://localhost:" + applicationConfiguration.getServerPort() + "/handleShopperRedirect";
            paymentRequest.setReturnUrl(returnUrl);
            
            log.info("Sending zero-auth payment request for shopper: {}", shopperReference);
            
            // Make the payment request
            PaymentResponse response = paymentsApi.payments(paymentRequest);
            
            // Store the shopper reference in the response for frontend tracking
            if (response.getAdditionalData() == null) {
                response.setAdditionalData(new HashMap<>());
            }
            response.getAdditionalData().put("shopperReference", shopperReference);
            
            log.info("Zero-auth payment response: resultCode={}", response.getResultCode());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating subscription: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/api/subscription-payment")
    public ResponseEntity<Map<String, Object>> subscriptionPayment(@RequestBody Map<String, Object> body) throws IOException, ApiException {
        log.info("Processing subscription payment");
        
        try {
            // Validate request body
            if (body == null || !body.containsKey("shopperReference") || body.get("shopperReference") == null) {
                log.error("Missing shopperReference in request body");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "shopperReference is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String shopperReference = body.get("shopperReference").toString();
            
            // Get the stored token
            String recurringDetailReference = recurringTokenStore.getToken(shopperReference);
            if (recurringDetailReference == null) {
                log.error("No recurring token found for shopper: {}", shopperReference);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No recurring token found for this shopper");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            log.info("Found recurring token for shopper: {}", shopperReference);
            
            // Create payment request with stored token
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setMerchantAccount(applicationConfiguration.getAdyenMerchantAccount());
            
            // Set subscription amount (e.g., 5 EUR per month)
            Amount amount = new Amount();
            amount.setCurrency("EUR");
            amount.setValue(500L); // 5.00 EUR in minor units
            paymentRequest.setAmount(amount);
            
            // Set payment method with stored token
            CardDetails cardDetails = new CardDetails();
            cardDetails.type(CardDetails.TypeEnum.SCHEME);
            cardDetails.storedPaymentMethodId(recurringDetailReference);
            paymentRequest.setPaymentMethod(new CheckoutPaymentMethod(cardDetails));
            
            // Set recurring parameters
            paymentRequest.setShopperReference(shopperReference);
            paymentRequest.setRecurringProcessingModel(PaymentRequest.RecurringProcessingModelEnum.SUBSCRIPTION);
            paymentRequest.setShopperInteraction(PaymentRequest.ShopperInteractionEnum.CONTAUTH);
            
            // Set reference
            paymentRequest.setReference("recurring_" + UUID.randomUUID().toString());
            
            log.info("Sending recurring payment request for shopper: {}", shopperReference);
            
            // Make the payment request
            PaymentResponse response = paymentsApi.payments(paymentRequest);
            
            log.info("Recurring payment response: resultCode={}", response.getResultCode());
            
            Map<String, Object> result = new HashMap<>();
            result.put("resultCode", response.getResultCode().toString());
            result.put("pspReference", response.getPspReference());
            result.put("merchantReference", paymentRequest.getReference());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing subscription payment: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/api/subscription-cancel")
    public ResponseEntity<Map<String, Object>> subscriptionCancel(@RequestBody Map<String, Object> body) throws IOException, ApiException {
        log.info("Canceling subscription");
        
        try {
            // Validate request body
            if (body == null || !body.containsKey("shopperReference") || body.get("shopperReference") == null) {
                log.error("Missing shopperReference in request body");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "shopperReference is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String shopperReference = body.get("shopperReference").toString();
            
            // Get the stored token
            String recurringDetailReference = recurringTokenStore.getToken(shopperReference);
            
            if (recurringDetailReference == null) {
                log.warn("No recurring token found for shopper: {}", shopperReference);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No subscription found for this shopper");
                return ResponseEntity.ok(response);
            }
            
            log.info("Canceling subscription for shopper: {} with token: {}", shopperReference, recurringDetailReference);
            
            // Delete the token using Recurring API
            try {
                recurringApi.deleteTokenForStoredPaymentDetails(
                    applicationConfiguration.getAdyenMerchantAccount(),
                    recurringDetailReference,
                    shopperReference
                );
                log.info("Successfully deleted stored payment details from Adyen");
            } catch (Exception e) {
                log.warn("Could not delete token from Adyen (might not exist or already deleted): {}", e.getMessage());
                // Continue to delete from local storage even if Adyen deletion fails
            }
            
            // Remove from local storage
            boolean deleted = recurringTokenStore.deleteToken(shopperReference);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);
            response.put("message", deleted ? "Subscription cancelled successfully" : "Subscription was already cancelled");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error canceling subscription: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
