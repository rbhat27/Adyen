#!/bin/bash

# Subscription Flow Test Script
# This script demonstrates the subscription and recurring payment flow

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "Subscription Flow Test Script"
echo "=========================================="
echo ""

# Test 1: Create subscription
echo "Test 1: Creating subscription with zero-auth payment"
echo "POST ${BASE_URL}/api/subscription-create"
SHOPPER_REF="test_shopper_$(date +%s)"
echo "Using shopper reference: ${SHOPPER_REF}"
echo ""

# Note: In a real scenario, you would get encrypted card data from Adyen Web SDK
# This is just a placeholder to show the request structure
CREATE_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/subscription-create" \
  -H "Content-Type: application/json" \
  -d "{
    \"shopperReference\": \"${SHOPPER_REF}\",
    \"paymentMethod\": {
      \"type\": \"scheme\",
      \"encryptedCardNumber\": \"test_encrypted_card_number\",
      \"encryptedExpiryMonth\": \"test_encrypted_month\",
      \"encryptedExpiryYear\": \"test_encrypted_year\",
      \"encryptedSecurityCode\": \"test_encrypted_cvv\"
    }
  }")

echo "Response:"
echo "${CREATE_RESPONSE}" | python3 -m json.tool 2>/dev/null || echo "${CREATE_RESPONSE}"
echo ""
echo "=========================================="
echo ""

# Extract shopper reference from response
if [ $? -eq 0 ]; then
  echo "Subscription created. Waiting for RECURRING_CONTRACT webhook..."
  echo "Note: In a real scenario, the webhook would be received and processed automatically."
  echo ""
  
  # Simulate webhook reception delay
  sleep 2
  
  # Test 2: Charge subscription
  echo "Test 2: Charging subscription"
  echo "POST ${BASE_URL}/api/subscription-payment"
  echo ""
  
  PAYMENT_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/subscription-payment" \
    -H "Content-Type: application/json" \
    -d "{
      \"shopperReference\": \"${SHOPPER_REF}\"
    }")
  
  echo "Response:"
  echo "${PAYMENT_RESPONSE}" | python3 -m json.tool 2>/dev/null || echo "${PAYMENT_RESPONSE}"
  echo ""
  echo "=========================================="
  echo ""
  
  # Test 3: Cancel subscription
  echo "Test 3: Canceling subscription"
  echo "POST ${BASE_URL}/api/subscription-cancel"
  echo ""
  
  CANCEL_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/subscription-cancel" \
    -H "Content-Type: application/json" \
    -d "{
      \"shopperReference\": \"${SHOPPER_REF}\"
    }")
  
  echo "Response:"
  echo "${CANCEL_RESPONSE}" | python3 -m json.tool 2>/dev/null || echo "${CANCEL_RESPONSE}"
  echo ""
  echo "=========================================="
  echo ""
  
  # Test 4: Try to charge after cancellation (should fail)
  echo "Test 4: Attempting to charge after cancellation (should fail)"
  echo "POST ${BASE_URL}/api/subscription-payment"
  echo ""
  
  PAYMENT_RESPONSE_2=$(curl -s -X POST "${BASE_URL}/api/subscription-payment" \
    -H "Content-Type: application/json" \
    -d "{
      \"shopperReference\": \"${SHOPPER_REF}\"
    }")
  
  echo "Response:"
  echo "${PAYMENT_RESPONSE_2}" | python3 -m json.tool 2>/dev/null || echo "${PAYMENT_RESPONSE_2}"
  echo ""
  echo "=========================================="
  echo ""
fi

echo "Test script completed."
echo ""
echo "Note: This script uses placeholder encrypted card data."
echo "In a real integration, you would:"
echo "1. Use Adyen Web SDK to encrypt card data"
echo "2. Receive actual responses from Adyen API"
echo "3. Process real webhook notifications"
echo ""
echo "For testing with real Adyen APIs, you need to:"
echo "1. Set up ADYEN_API_KEY, ADYEN_MERCHANT_ACCOUNT, ADYEN_CLIENT_KEY"
echo "2. Use Adyen Web Drop-in or Components for card encryption"
echo "3. Configure webhook endpoints in Adyen Customer Area"
