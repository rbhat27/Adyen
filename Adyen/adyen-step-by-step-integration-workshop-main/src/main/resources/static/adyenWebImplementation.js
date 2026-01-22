const clientKey = document.getElementById("clientKey").innerHTML;
const { AdyenCheckout, Dropin } = window.AdyenWeb;

// Starts the (Adyen.Web) AdyenCheckout with your specified configuration by calling the `/paymentMethods` endpoint.
async function startCheckout() {
    try {
        // Step 8 - Retrieve the available payment methods

    } catch (error) {
        console.error(error);
        alert("Error occurred. Look at console for details.");
    }
}

// Function to initiate subscription with zero-auth payment
async function createSubscription(paymentData) {
    try {
        console.log("Creating subscription with payment data:", paymentData);
        
        const response = await fetch("/api/subscription-create", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(paymentData)
        });
        
        const result = await response.json();
        console.log("Subscription creation result:", result);
        
        return result;
    } catch (error) {
        console.error("Error creating subscription:", error);
        throw error;
    }
}

// Function to charge a subscription
async function chargeSubscription(shopperReference) {
    try {
        console.log("Charging subscription for shopper:", shopperReference);
        
        const response = await fetch("/api/subscription-payment", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ shopperReference: shopperReference })
        });
        
        const result = await response.json();
        console.log("Subscription payment result:", result);
        
        return result;
    } catch (error) {
        console.error("Error charging subscription:", error);
        throw error;
    }
}

// Function to cancel a subscription
async function cancelSubscription(shopperReference) {
    try {
        console.log("Canceling subscription for shopper:", shopperReference);
        
        const response = await fetch("/api/subscription-cancel", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ shopperReference: shopperReference })
        });
        
        const result = await response.json();
        console.log("Subscription cancellation result:", result);
        
        return result;
    } catch (error) {
        console.error("Error canceling subscription:", error);
        throw error;
    }
}

// Step 10 - Function to handle payment completion redirects
function handleOnPaymentCompleted(response) {

}

// Step 10 - Function to handle payment failure redirects
function handleOnPaymentFailed(response) {

}

startCheckout();