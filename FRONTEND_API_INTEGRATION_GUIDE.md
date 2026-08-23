# OFOOD Frontend API Integration Guide

This guide is designed for frontend engineers integrating with the OFOOD backend. It explains the actual business logic, API behaviour, state flows, and error handling mechanisms derived directly from the backend implementation.

---

## 1. API Overview

* **Project:** OFOOD Backend
* **Purpose:** Provides APIs for a subscription-based meal delivery service, including catalog browsing, authentication, checkout, payment, and subscription management.
* **Base URL:** `http://localhost:8080` (Local) / *[Render URL]* (Staging)
* **API Versioning:** All APIs are prefixed with `/api/v1`
* **Content Type:** All APIs accept and return JSON.

```http
Content-Type: application/json
Accept: application/json
```

---

## 2. Authentication and Authorization

OFOOD uses **stateless JWT Bearer Token** authentication. 

* **Public APIs:** Login, Registration, Plan Catalog, Cities, Pincodes, and Serviceability.
* **Protected APIs:** Checkout, Subscriptions, Addresses, Payments, User Profile.
* **Roles:** `ROLE_CUSTOMER` (Frontend App) and `ROLE_ADMIN` (Admin Dashboard).

### Sending the Token
The frontend must attach the JWT token to the `Authorization` header for all protected routes:

```http
Authorization: Bearer <your_jwt_token>
```

### Authentication Errors
* `401 Unauthorized`: Sent when the token is missing, expired, or invalid. The frontend should clear the local session and redirect to the Login screen.
* `403 Forbidden`: Sent when a valid token does not have the required Role (e.g. a customer trying to access an admin route).

---

## 3. API Endpoint Documentation

### Authentication Domain

#### 1. Register a New Customer
* **Purpose:** Create a new customer account.
* **Endpoint:** `POST /api/v1/auth/register`
* **Auth:** Not required
* **Request Payload:**
  ```json
  {
    "email": "user@example.com",
    "password": "SecurePassword123",
    "fullName": "John Doe",
    "firstName": "John",
    "lastName": "Doe",
    "mobile": "9876543210"
  }
  ```
  **Business Logic Behind Fields:**
  * `email`: Unique identifier. Backend validates uniqueness. If duplicate, returns 400 Bad Request.
  * `password`: Must meet complexity rules. Backend hashes it using BCrypt.
  * `mobile`: Stored for delivery contact purposes.
* **Response Payload (200 OK):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5c..."
  }
  ```

#### 2. Login
* **Purpose:** Authenticate an existing user and retrieve a JWT.
* **Endpoint:** `POST /api/v1/auth/login`
* **Auth:** Not required
* **Request Payload:**
  ```json
  {
    "email": "user@example.com",
    "password": "SecurePassword123"
  }
  ```
  **Business Logic Behind Fields:**
  * Backend verifies the `email` exists and `password` matches the BCrypt hash. Upon success, generates a JWT containing the user's UUID as the subject and `roles: ["ROLE_CUSTOMER"]`.
* **Response Payload (200 OK):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5c..."
  }
  ```

#### 3. Get Current User Profile
* **Purpose:** Retrieve the authenticated user's details.
* **Endpoint:** `GET /api/v1/auth/me`
* **Auth:** Required (Any Role)
* **Response Payload (200 OK):**
  ```json
  {
    "id": "76aadace-17a5-4fa2-bfa8-618ea84da684",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe",
    "mobile": "9876543210",
    "avatarUrl": "https://example.com/avatar.jpg",
    "isActive": true,
    "roles": ["ROLE_CUSTOMER"]
  }
  ```
  **Business Logic Behind Fields:**
  * Parses the User ID securely from the JWT context; ignores any manual ID sent by the client.

---

### Catalog & Serviceability Domain

#### 4. Get All Serviceable Cities
* **Purpose:** Fetch the list of cities where OFOOD operates.
* **Endpoint:** `GET /api/v1/cities`
* **Auth:** Not required
* **Response Payload (200 OK):**
  ```json
  [
    {
      "id": "cd689b2f-910f-423a-9c1c-2431cd38c31a",
      "name": "Mumbai",
      "slug": "mumbai",
      "state": "Maharashtra",
      "status": "ACTIVE"
    }
  ]
  ```
  **Business Logic Behind Fields:**
  * Only returns cities where `status == "ACTIVE"`. `INACTIVE` cities are hidden to prevent users from ordering there.

#### 5. Get Active Plans
* **Purpose:** Fetch all active meal plans to display on the storefront.
* **Endpoint:** `GET /api/v1/plans`
* **Auth:** Not required
* **Response Payload (200 OK):**
  ```json
  [
    {
      "id": "4c7e3e33-9fcb-4cc2-bdea-0796adc3e258",
      "name": "Standard Weekly Plan",
      "slug": "standard-weekly",
      "shortDescription": "7 days of healthy meals",
      "description": "...",
      "image": "https://example.com/plan.jpg",
      "price": 5000.00,
      "compareAtPrice": 6000.00,
      "currency": "INR",
      "durationUnit": "WEEK",
      "status": "ACTIVE",
      "meals": [
        {
          "id": "uuid-meal-1",
          "mealType": "LUNCH",
          "name": "Grilled Chicken Salad",
          "description": "Healthy greens",
          "servingSize": "1 bowl",
          "imageUrl": "https://example.com/meal.jpg"
        }
      ]
    }
  ]
  ```

---

### Customer Domain

#### 6. Create Address
* **Purpose:** Save a new delivery address for the customer.
* **Endpoint:** `POST /api/v1/addresses`
* **Auth:** Required (`ROLE_CUSTOMER`)
* **Request Payload:**
  ```json
  {
    "fullName": "John Doe",
    "mobile": "9876543210",
    "addressLine1": "Apt 101, Building B",
    "addressLine2": "Near the park",
    "landmark": "City Mall",
    "area": "Downtown",
    "city": "cd689b2f-910f-423a-9c1c-2431cd38c31a",
    "state": "Maharashtra",
    "pincode": "400001",
    "latitude": 19.0760,
    "longitude": 72.8777,
    "addressType": "HOME"
  }
  ```
  **Business Logic Behind Fields:**
  * `city`: Must be a valid UUID of an `ACTIVE` City in the database. Otherwise returns 400 Bad Request.
  * Security: Backend forcibly maps this address to the `customerId` extracted from the JWT token. Users cannot create addresses for other users.
* **Response Payload (201 Created):** Same structure as request but with a generated `id` UUID.

---

### Checkout & Payment Domain (Core Flow)

#### 7. Preview Checkout
* **Purpose:** Calculate the exact pricing, taxes, and discounts before the user commits to buying.
* **Endpoint:** `POST /api/v1/checkout/preview`
* **Auth:** Required (`ROLE_CUSTOMER`)
* **Request Payload:**
  ```json
  {
    "planId": "4c7e3e33-9fcb-4cc2-bdea-0796adc3e258",
    "addressId": "b3d1eace-eaf0-4dc2-870a-358c15bb6142",
    "voucherCode": "WELCOME10"
  }
  ```
  **Business Logic Behind Fields:**
  * `planId`: Must belong to an `ACTIVE` Plan.
  * `addressId`: Must strictly belong to the authenticated user ID and the Address's City must be `ACTIVE`. If the address belongs to another user, throws `403 Forbidden` / `404 Not Found`.
  * `voucherCode`: If present, backend validates if it's active, if the `orderValue` > `minimumOrderValue`, and if the voucher permits the selected `planId` inside its `applicablePlans` list.
* **Response Payload (200 OK):**
  ```json
  {
    "planPrice": 5000.00,
    "planDiscount": 1000.00,
    "voucherDiscount": 500.00,
    "taxableAmount": 4500.00,
    "tax": 225.00,
    "deliveryFee": 50.00,
    "finalAmount": 4775.00
  }
  ```
  **Business Logic Behind Fields:**
  * `planDiscount`: Extracted from `compareAtPrice - price`.
  * `voucherDiscount`: Result of voucher application (up to `maxDiscount`).
  * `taxableAmount`: `planPrice` - discounts.
  * `tax`: Computed tax (e.g. 5% GST).
  * `deliveryFee`: Flat fee based on backend rules.
  * `finalAmount`: Total amount required from the payment gateway.

#### 8. Submit Checkout
* **Purpose:** Finalize the order and generate a payment intent.
* **Endpoint:** `POST /api/v1/checkout`
* **Auth:** Required (`ROLE_CUSTOMER`)
* **Request Payload:**
  ```json
  {
    "planId": "4c7e3e33-9fcb-4cc2-bdea-0796adc3e258",
    "addressId": "b3d1eace-eaf0-4dc2-870a-358c15bb6142",
    "voucherCode": "WELCOME10"
  }
  ```
  **Business Logic Behind Fields:**
  * Same validations as Preview. It physically creates a `Subscription` record mapped to `planId` and `addressId`, and a linked `Payment` record.
* **Response Payload (200 OK):**
  ```json
  {
    "subscriptionId": "d456b95b-58ed-421e-ac6d-c94fa9984cda",
    "subscriptionStatus": "PENDING",
    "paymentId": "7f92a83b-ca7c-4f85-892d-3ed27953778f",
    "paymentStatus": "PENDING",
    "provider": "mock",
    "providerPaymentId": "mock_pi_1525784b7a354ff8831ac3cda06d2bf7"
  }
  ```
  **Business Logic Behind Fields:**
  * `providerPaymentId`: Identifies the session on the external Payment Gateway (e.g., Stripe PaymentIntent ID). Frontend must use this ID to mount the gateway UI!

#### 9. Confirm Payment
* **Purpose:** Verify the payment success after the user completes the gateway flow.
* **Endpoint:** `POST /api/v1/payments/{paymentId}/confirm`
* **Auth:** Required (`ROLE_CUSTOMER`)
* **Path Parameters:**
  * `paymentId`: The UUID `paymentId` returned by `/api/v1/checkout`.
* **Request Payload:**
  ```json
  {
    "providerPaymentId": "mock_pi_1525784b7a354ff8831ac3cda06d2bf7"
  }
  ```
  **Business Logic Behind Fields:**
  * The backend verifies the `providerPaymentId` with the gateway webhook/API. If successful, it updates the `Payment` to `SUCCESS` and the linked `Subscription` to `ACTIVE`.
* **Response Payload (200 OK):**
  ```json
  {
    "paymentId": "7f92a83b-ca7c-4f85-892d-3ed27953778f",
    "paymentStatus": "SUCCESS",
    "subscriptionId": "d456b95b-58ed-421e-ac6d-c94fa9984cda",
    "subscriptionStatus": "ACTIVE"
  }
  ```

---

## 4. Complete Business Flows

### Feature: Plan Purchase Flow

```text
Frontend fetches Serviceable Cities (GET /api/v1/cities)
        ↓
Frontend fetches Plans (GET /api/v1/plans)
        ↓
User clicks "Subscribe" -> Prompts Login if unauthenticated
        ↓
User selects Address (or creates new via POST /api/v1/addresses)
        ↓
Frontend calls POST /api/v1/checkout/preview to show final price
        ↓
User confirms -> Frontend calls POST /api/v1/checkout
        ↓
Backend returns `providerPaymentId`
        ↓
Frontend mounts Payment Gateway UI using `providerPaymentId`
        ↓
User pays -> Gateway returns success token
        ↓
Frontend calls POST /api/v1/payments/{paymentId}/confirm
        ↓
Frontend shows "Subscription Active" success screen
```

---

## 5. API Dependency Map

* `GET /api/v1/plans` -> Returns `planId` -> Used in Checkout
* `GET /api/v1/cities` -> Returns `cityId` -> Used in Create Address
* `GET /api/v1/addresses` -> Returns `addressId` -> Used in Checkout
* `POST /api/v1/checkout` -> Returns `paymentId` -> Used in Confirm Payment

---

## 6. State and Status Handling

### Subscription State Machine
* `PENDING`: Created during checkout, awaiting payment.
* `ACTIVE`: Payment successful, meals can be delivered.
* `CANCELLED`: Subscription terminated.
* `EXPIRED`: Duration completed.

### Plan/City/Pincode Status
* `DRAFT` (Plans only)
* `ACTIVE` (Visible to customers, allowed in checkout)
* `INACTIVE` (Hidden from customers, blocked in checkout validation)

---

## 7. Error Handling Guide for Frontend

| Status | Meaning | Frontend Action |
|--------|---------|-----------------|
| `400` | Bad Request / Validation Failed | Display inline form errors or toast message using the `message` field from the response. |
| `401` | Unauthorized | Redirect to Login. Clear invalid JWT from local storage. |
| `403` | Forbidden | Show "Access Denied". Usually means the user is trying to access another user's data (e.g. foreign address ID). |
| `404` | Not Found | Show generic 404 UI component. |
| `500` | Internal Server Error | Show generic fallback error boundary. Retry logic is generally unsafe unless it's a transient network issue. |

**Standard Error Response Format:**
```json
{
  "code": "VALIDATION_FAILED",
  "message": "Voucher is not applicable for this plan",
  "traceId": "uuid-for-debugging"
}
```

---

## 8. Important Edge Cases

* **Foreign Address Protection:** Passing an `addressId` that belongs to a different user into the checkout preview will result in a `403 Forbidden` or `404 Not Found`. Do not attempt to bypass this.
* **Lazy Initialization Constraints:** The backend ensures that vouchers and plans are properly loaded during checkout.
* **Malformed Plan IDs:** If `planId` contains curly/smart quotes (e.g. from copy-pasting), the backend will instantly reject it with `400 MALFORMED_JSON`.

---

## 9. Swagger vs Actual Implementation

* ⚠️ **JSON Errors:** Swagger indicates 500 for parsing errors, but the backend implementation specifically traps `HttpMessageNotReadableException` and returns a clean `400 Bad Request`.
* ⚠️ **Checkout Logging:** When calling protected APIs, the backend correctly attributes logs to the authenticated User UUID, which is securely parsed from the Bearer Token. The frontend does not need to manually send its User ID anywhere in the body for checkout; it is implicitly resolved.

---

## 10. Frontend Implementation Checklist

### Authentication
- [ ] Build Login / Register forms.
- [ ] Save JWT Token to Secure Storage / HttpOnly Cookie context.
- [ ] Create Axios/Fetch interceptor to attach `Authorization: Bearer` to all requests.
- [ ] Create Axios/Fetch interceptor to listen for `401` and trigger logout.

### Catalog
- [ ] Fetch and render `GET /api/v1/plans`.
- [ ] Build city selector using `GET /api/v1/cities`.

### Checkout
- [ ] Create checkout view holding local state for `selectedPlan`, `selectedAddress`, and `voucherCode`.
- [ ] Debounce voucher input and call `POST /api/v1/checkout/preview`.
- [ ] Render the returned `tax`, `discount`, and `finalAmount`.
- [ ] Submit `POST /api/v1/checkout` on confirmation.
- [ ] Handle payment gateway mount with `providerPaymentId`.
- [ ] Post-payment confirmation redirect to dashboard.
