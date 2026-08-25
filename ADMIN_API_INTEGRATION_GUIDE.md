# OFOOD Admin API Integration Guide

This guide is designed for frontend engineers building the Admin Dashboard for OFOOD. It covers all the protected administrative APIs required to manage cities, pincodes, plans, vouchers, and delivery personnel.

All endpoints documented here require the user to be authenticated with a JWT token containing the `ROLE_ADMIN` authority.

---

## 1. Authentication

Just like the customer APIs, all Admin APIs expect a valid JWT passed in the Authorization header.

```http
Authorization: Bearer <your_jwt_token>
```

If a regular customer (without `ROLE_ADMIN`) attempts to access these APIs, the backend will return a `403 Forbidden` error.

---

## 2. Core Operational Domains

### A. Location & Serviceability (Cities & Pincodes)

#### 1. Create a City
* **Purpose:** Add a new city to the OFOOD service catalog.
* **Endpoint:** `POST /api/v1/cities`
* **Request Payload:**
  ```json
  {
    "name": "Mumbai",
    "slug": "mumbai",
    "state": "Maharashtra",
    "status": "ACTIVE"
  }
  ```
  **Business Logic Behind Fields:**
  * `slug`: Must be unique. Used for SEO-friendly routing.
  * `status`: Can be `ACTIVE` or `INACTIVE`. If `INACTIVE`, it won't appear to customers during checkout.
* **Response Payload (201 Created):** Returns the generated `CityResponse`.

#### 2. Update a City
* **Purpose:** Modify an existing city (e.g., disable it by marking it `INACTIVE`).
* **Endpoint:** `PUT /api/v1/cities/{id}`
* **Request Payload:** Same as Create City.

#### 3. Delete a City
* **Purpose:** Permanently remove a city.
* **Endpoint:** `DELETE /api/v1/cities/{id}`
* **Business Logic:** Hard deletes the city. If there are dependent pincodes or customer addresses, this may fail due to foreign key constraints.
* **Response:** `204 No Content`

#### 4. Create a Service Pincode
* **Purpose:** Define a specific delivery zone inside a city.
* **Endpoint:** `POST /api/v1/pincodes`
* **Request Payload:**
  ```json
  {
    "pincode": "400001",
    "cityId": "cd689b2f-910f-423a-9c1c-2431cd38c31a",
    "status": "ACTIVE"
  }
  ```
  **Business Logic Behind Fields:**
  * `cityId`: Must reference a valid UUID from the Cities table.
  * `pincode`: Must be unique across the platform.

---

### B. Catalog Management (Meal Plans)

#### 1. Create a Plan
* **Purpose:** Draft or publish a new subscription plan with its associated meals.
* **Endpoint:** `POST /api/v1/plans`
* **Request Payload:**
  ```json
  {
    "name": "Standard Weekly Plan",
    "slug": "standard-weekly",
    "shortDescription": "7 days of healthy meals",
    "description": "Full HTML or markdown description here",
    "image": "https://example.com/plan.jpg",
    "price": 5000.00,
    "compareAtPrice": 6000.00,
    "currency": "INR",
    "durationUnit": "WEEK",
    "status": "DRAFT",
    "meals": [
      {
        "mealType": "LUNCH",
        "name": "Grilled Chicken Salad",
        "description": "Healthy greens",
        "servingSize": "1 bowl",
        "imageUrl": "https://example.com/meal.jpg"
      }
    ]
  }
  ```
  **Business Logic Behind Fields:**
  * `status`: Plans created as `DRAFT` are completely invisible to customers. They must be updated to `ACTIVE` to be sellable.
  * `price` & `compareAtPrice`: The difference is automatically calculated and shown as `planDiscount` in the customer checkout.

#### 2. Update a Plan
* **Purpose:** Edit an existing plan, such as changing pricing or meals.
* **Endpoint:** `PUT /api/v1/plans/{id}` or `PATCH /api/v1/plans/{id}`
* **Request Payload:** Same as Create Plan. Overwrites the existing plan data and meal mappings.

#### 2a. Complete a draft (Activate)
* **Purpose:** Activate a DRAFT plan so it becomes visible to customers.
* **Endpoint:** `PATCH /api/v1/plans/{id}`
* **Request Payload:** All activation-required fields must be supplied.
  ```json
  {
    "status": "ACTIVE",
    "price": 5000.00,
    "currency": "INR",
    "duration": 7,
    "durationUnit": "DAYS",
    "mealCount": 14,
    "mealsPerDay": 2,
    "servingsPerMeal": 1,
    "mealTypes": ["LUNCH", "DINNER"]
  }
  ```
  **Business Logic:** Backend will validate that the final state of the plan has all mandatory fields for activation (name, price, currency, duration, durationUnit, mealCount, mealsPerDay, servingsPerMeal, mealTypes). If any are missing, it throws `400 VALIDATION_FAILED` with a list of missing fields.

#### 3. Duplicate a Plan
* **Purpose:** Clone a complex plan instead of building it from scratch.
* **Endpoint:** `POST /api/v1/plans/{id}/duplicate`
* **Request Payload:** None required in body.
* **Business Logic:** Reads the source plan, creates a copy with `(Copy)` appended to the name, appends a random string to the slug, sets the status to `DRAFT`, and returns the newly cloned `PlanResponse`.

#### 4. Reorder Plans
* **Purpose:** Control the display order of plans on the customer storefront.
* **Endpoint:** `POST /api/v1/plans/reorder`
* **Request Payload:**
  ```json
  {
    "planIds": [
      "uuid-plan-B",
      "uuid-plan-C",
      "uuid-plan-A"
    ]
  }
  ```
  **Business Logic:** Receives an ordered array of UUIDs and updates their `sortOrder` database columns sequentially.

#### 5. Get All Plans (Including Drafts)
* **Purpose:** Fetch all plans for the Admin table, ignoring the `ACTIVE` restriction.
* **Endpoint:** `GET /api/v1/plans/all`

---

### C. Promotional Domain (Vouchers)

#### 1. Create a Voucher
* **Purpose:** Create a discount code.
* **Endpoint:** `POST /api/v1/vouchers`
* **Request Payload:**
  ```json
  {
    "code": "WELCOME10",
    "name": "Welcome Bonus",
    "description": "10% off for new users",
    "discountValue": 500.00,
    "maxDiscount": 500.00,
    "minimumOrderValue": 2000.00
  }
  ```
  **Business Logic Behind Fields:**
  * `code`: Must be unique and is strictly uppercase-validated in checkout.
  * `minimumOrderValue`: Evaluated during checkout. If the base plan price is lower than this, the voucher will throw a validation error.

#### 2. Update/Delete Voucher
* **Endpoints:** `PUT /api/v1/vouchers/{id}` and `DELETE /api/v1/vouchers/{id}`
* **Business Logic:** Deleting a voucher makes it immediately unusable in the customer checkout.

---

### D. Logistics (Delivery Personnel)

#### 1. Create a Delivery Person
* **Purpose:** Onboard a new driver/rider and assign them to specific pincodes.
* **Endpoint:** `POST /api/v1/delivery-persons`
* **Request Payload:**
  ```json
  {
    "firstName": "Alex",
    "lastName": "Rider",
    "mobile": "9876500000",
    "vehicleType": "BIKE",
    "vehicleNumber": "MH-01-AB-1234",
    "status": "ACTIVE",
    "pincodeIds": [
      "uuid-pincode-1",
      "uuid-pincode-2"
    ]
  }
  ```
  **Business Logic Behind Fields:**
  * `pincodeIds`: Maps the driver to a Many-To-Many relationship with `ServicePincode`. The driver is strictly authorized to deliver only to these active zones.

#### 2. Update Delivery Person
* **Endpoint:** `PUT /api/v1/delivery-persons/{id}`
* **Business Logic:** Updating `pincodeIds` overwrites their assigned zones. Changing `status` to `INACTIVE` suspends them from duty.

---

## 3. General Admin Error Handling

The Admin APIs share the same robust error handling pipeline as the Customer APIs.

| Status | Error Case | Meaning | Frontend Action |
|--------|------------|---------|-----------------|
| `400` | `MALFORMED_JSON` | Syntax error in the request payload. | Check field data types (e.g., passing string to a decimal field). |
| `400` | `VALIDATION_FAILED` | Entity constraints failed (e.g. duplicate slug). | Show the exact error `message` in a toast/notification. |
| `401` | `UNAUTHORIZED` | Token expired or missing. | Redirect Admin to login screen. |
| `403` | `FORBIDDEN` | Missing `ROLE_ADMIN`. | Block access and log the security violation. |
| `404` | `NOT_FOUND` | Entity UUID does not exist. | Return user to the data table list view. |
