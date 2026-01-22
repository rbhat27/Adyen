# Subscription and Recurring Payment Flow

This implementation provides subscription and recurring payment functionality using Adyen's tokenization API.

## Overview

The implementation includes:
1. Zero-auth payment for tokenization (`/api/subscription-create`)
2. Recurring payment charges (`/api/subscription-payment`)
3. Subscription cancellation (`/api/subscription-cancel`)
4. Webhook handlers for `RECURRING_CONTRACT` and `AUTHORISATION` events

## Endpoints

### 1. Create Subscription (Zero-Auth Payment)

**Endpoint:** `POST /api/subscription-create`

**Description:** Creates a subscription by performing a zero-auth payment to tokenize the payment method.

**Request Body:**
```json
{
  "shopperReference": "unique_shopper_id",  // Optional, will be auto-generated if not provided
  "paymentMethod": {
    "type": "scheme",
    "encryptedCardNumber": "...",
    "encryptedExpiryMonth": "...",
    "encryptedExpiryYear": "...",
    "encryptedSecurityCode": "..."
  }
}
```

**Response:**
```json
{
  "resultCode": "Authorised",
  "pspReference": "...",
  "merchantReference": "subscription_...",
  "additionalData": {
    "shopperReference": "shopper_..."
  }
}
```

**Note:** Save the `shopperReference` from the response to use for subsequent payment and cancellation requests.

### 2. Charge Subscription

**Endpoint:** `POST /api/subscription-payment`

**Description:** Charges the stored payment method for a recurring subscription payment.

**Request Body:**
```json
{
  "shopperReference": "shopper_..."
}
```

**Response:**
```json
{
  "resultCode": "Authorised",
  "pspReference": "...",
  "merchantReference": "recurring_..."
}
```

**Note:** The subscription amount is currently hardcoded to 5.00 EUR (500 in minor units).

### 3. Cancel Subscription

**Endpoint:** `POST /api/subscription-cancel`

**Description:** Cancels the subscription by deleting the stored payment token.

**Request Body:**
```json
{
  "shopperReference": "shopper_..."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Subscription cancelled successfully"
}
```

## Webhooks

### RECURRING_CONTRACT Webhook

Triggered when a payment method is successfully tokenized. This webhook contains the `recurringDetailReference` which is automatically stored by the system.

**Webhook Data Extracted:**
- `recurring.recurringDetailReference` - The token for stored payment method
- `recurring.shopperReference` - The shopper reference

### AUTHORISATION Webhook

Triggered for each payment authorization (both zero-auth and recurring payments).

**Logged Information:**
- Authorization success/failure status
- PSP reference
- Payment amount and currency
- Merchant reference

## Frontend Integration

The frontend file `adyenWebImplementation.js` includes three helper functions:

### 1. createSubscription(paymentData)
Creates a subscription with the provided payment data.

### 2. chargeSubscription(shopperReference)
Charges a subscription for the given shopper.

### 3. cancelSubscription(shopperReference)
Cancels a subscription for the given shopper.

## Testing Flows

### Flow 1: Create → Payment
```javascript
// 1. Create subscription
const createResult = await createSubscription({
  paymentMethod: {
    type: "scheme",
    encryptedCardNumber: "...",
    encryptedExpiryMonth: "...",
    encryptedExpiryYear: "...",
    encryptedSecurityCode: "..."
  }
});

// 2. Wait for RECURRING_CONTRACT webhook to store the token
// (This happens automatically in the backend)

// 3. Charge the subscription
const paymentResult = await chargeSubscription(createResult.additionalData.shopperReference);
```

### Flow 2: Create → Cancel
```javascript
// 1. Create subscription
const createResult = await createSubscription({...});

// 2. Cancel subscription
const cancelResult = await cancelSubscription(createResult.additionalData.shopperReference);
```

### Flow 3: Create → Payment → Cancel
```javascript
// 1. Create subscription
const createResult = await createSubscription({...});

// 2. Charge subscription
const paymentResult = await chargeSubscription(createResult.additionalData.shopperReference);

// 3. Cancel subscription
const cancelResult = await cancelSubscription(createResult.additionalData.shopperReference);
```

### Flow 4: Create → Payment → Cancel → Payment (Should Fail)
```javascript
// 1. Create subscription
const createResult = await createSubscription({...});

// 2. Charge subscription
const paymentResult1 = await chargeSubscription(createResult.additionalData.shopperReference);

// 3. Cancel subscription
const cancelResult = await cancelSubscription(createResult.additionalData.shopperReference);

// 4. Try to charge again (should fail with "No recurring token found")
const paymentResult2 = await chargeSubscription(createResult.additionalData.shopperReference);
// Expected: {"error": "No recurring token found for this shopper"}
```

## Storage

The implementation uses an in-memory `RecurringTokenStore` to store the mapping between shopper references and recurring detail references.

**Important:** In a production environment, this should be replaced with a secure database storage solution.

## Security Considerations

1. The webhook handler validates HMAC signatures if `ADYEN_HMAC_KEY` is configured
2. Recurring tokens are stored securely and can only be used for MIT (Merchant Initiated Transactions)
3. The zero-auth payment uses `ECOMMERCE` shopper interaction (requires shopper authentication)
4. Recurring payments use `CONTAUTH` shopper interaction (continuous authority, no authentication required)

## Configuration

Ensure the following environment variables are set in `application.properties`:
```properties
ADYEN_API_KEY=your_api_key
ADYEN_MERCHANT_ACCOUNT=your_merchant_account
ADYEN_CLIENT_KEY=your_client_key
ADYEN_HMAC_KEY=your_hmac_key  # For webhook validation
```

## Recurring Processing Models

The implementation uses the `SUBSCRIPTION` recurring processing model which is suitable for:
- Fixed-amount recurring payments
- Regular subscription billing (monthly, yearly, etc.)
- Services with continuous authorization

## References

- [Adyen Tokenization Documentation](https://docs.adyen.com/online-payments/tokenization/advanced-flow)
- [Adyen Recurring Payments](https://docs.adyen.com/online-payments/tokenization)
- [Adyen Java API Library](https://github.com/Adyen/adyen-java-api-library)
