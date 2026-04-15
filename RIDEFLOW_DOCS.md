# RideFlow Backend — Complete Technical Documentation

> **Spring Boot 4.0.5 | Java 21 | PostgreSQL (Supabase) | JWT Auth | Flyway Migrations**

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture & Package Structure](#2-architecture--package-structure)
3. [Database Schema](#3-database-schema)
4. [Security System — Deep Dive](#4-security-system--deep-dive)
5. [Authentication Endpoints (`/auth`)](#5-authentication-endpoints-auth)
6. [Ride Endpoints (`/rides`)](#6-ride-endpoints-rides)
7. [Driver Endpoints (`/drivers`)](#7-driver-endpoints-drivers)
8. [Dispatcher Endpoints (`/dispatcher`)](#8-dispatcher-endpoints-dispatcher)
9. [Admin Endpoints (`/admin`)](#9-admin-endpoints-admin)
10. [Error Handling — Every Possible Error](#10-error-handling--every-possible-error)
11. [Data Transfer Objects (DTOs)](#11-data-transfer-objects-dtos)
12. [All Entities / Models](#12-all-entities--models)
13. [Service Layer Logic](#13-service-layer-logic)
14. [Frontend Integration Guide](#14-frontend-integration-guide)
15. [Complete Endpoint Reference Table](#15-complete-endpoint-reference-table)
16. [Running the Application](#16-running-the-application)

---

## 1. Project Overview

RideFlow is a ride management backend system that supports:

- JWT-based stateless authentication
- Role-based access: `ROLE_CUSTOMER`, `ROLE_DRIVER`, `ROLE_DISPATCHER`, `ROLE_ADMIN`
- Ride lifecycle: PENDING → ASSIGNED → IN_PROGRESS → COMPLETED / CANCELLED
- Driver availability management (auto-released on ride completion)
- Vehicle and maintenance record tracking
- Dispatcher operations (manual + auto-assign)

**Base URL when running locally:** `http://localhost:8080`

---

## 2. Architecture & Package Structure

```
com.cwtw.rideflow
│
├── config/
│   ├── PasswordConfig.java         → BCryptPasswordEncoder bean
│   └── SecurityConfig.java         → Spring Security filter chain, CSRF off, JWT filter
│
├── controller/
│   ├── AuthController.java         → /auth/** (public)
│   ├── RideController.java         → /rides/**
│   ├── DriverController.java       → /drivers/**
│   ├── DispatcherController.java   → /dispatcher/**
│   └── AdminController.java        → /admin/**
│
├── dto/
│   ├── AuthRequestDTO.java         → {email, password}
│   ├── AuthResponseDTO.java        → {token, role}
│   ├── RideRequestDTO.java         → {pickupLocation, dropLocation, scheduledTime}
│   ├── RideResponseDTO.java        → {id, pickupLocation, dropLocation, status, driverId}
│   └── DriverDTO.java              → {id, licenseNumber, isAvailable}
│
├── exception/
│   ├── CustomException.java        → RuntimeException + HttpStatus
│   └── GlobalExceptionHandler.java → @RestControllerAdvice — handles all exceptions
│
├── model/
│   ├── User.java                   → users table
│   ├── Driver.java                 → drivers table (1:1 with User)
│   ├── Vehicle.java                → vehicles table
│   ├── Ride.java                   → rides table (ManyToOne: User, Driver)
│   ├── License.java                → licenses table (ManyToOne: Driver)
│   └── MaintenanceRecord.java      → maintenance_records table (ManyToOne: Vehicle)
│
├── repository/
│   ├── UserRepository.java         → findByEmail(String)
│   ├── DriverRepository.java       → findByIsAvailable(boolean)
│   ├── VehicleRepository.java
│   ├── RideRepository.java         → findByUserId, findByDriverId
│   ├── LicenseRepository.java
│   └── MaintenanceRecordRepository.java
│
├── security/
│   ├── JwtService.java                  → generate / extract / validate JWT
│   ├── JwtAuthenticationFilter.java     → OncePerRequestFilter for every HTTP request
│   └── CustomUserDetailsService.java    → loads UserDetails from DB by email
│
└── RideFlowApplication.java        → Entry point
```

---

## 3. Database Schema

Managed by **Flyway** (`V1__init.sql`). Tables are created automatically on first boot.

```sql
-- Users (authentication principals)
CREATE TABLE users (
    id       BIGSERIAL PRIMARY KEY,
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,        -- BCrypt hash, NEVER plaintext
    role     VARCHAR(50)  NOT NULL         -- e.g. ROLE_CUSTOMER
);

-- Drivers (linked 1:1 to a User)
CREATE TABLE drivers (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL UNIQUE REFERENCES users(id),
    license_number VARCHAR(50) NOT NULL UNIQUE,
    is_available   BOOLEAN NOT NULL DEFAULT TRUE
);

-- Vehicles
CREATE TABLE vehicles (
    id           BIGSERIAL PRIMARY KEY,
    plate_number VARCHAR(50)  NOT NULL UNIQUE,
    model        VARCHAR(100) NOT NULL,
    status       VARCHAR(20)  NOT NULL       -- ACTIVE | INACTIVE | MAINTENANCE
);

-- Rides
CREATE TABLE rides (
    id              BIGSERIAL PRIMARY KEY,
    pickup_location VARCHAR(255) NOT NULL,
    drop_location   VARCHAR(255) NOT NULL,
    scheduled_time  TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    user_id         BIGINT NOT NULL REFERENCES users(id),
    driver_id       BIGINT REFERENCES drivers(id)     -- nullable until assigned
);

-- Driver Licenses
CREATE TABLE licenses (
    id          BIGSERIAL PRIMARY KEY,
    driver_id   BIGINT NOT NULL REFERENCES drivers(id),
    expiry_date DATE NOT NULL
);

-- Vehicle Maintenance Records
CREATE TABLE maintenance_records (
    id          BIGSERIAL PRIMARY KEY,
    vehicle_id  BIGINT NOT NULL REFERENCES vehicles(id),
    description TEXT NOT NULL,
    date        DATE NOT NULL
);
```

### Column Notes

| Table | Column | Notes |
|-------|--------|-------|
| `users` | `password` | Always stored as BCrypt hash. Never returned in any API response. |
| `users` | `role` | One of: `ROLE_CUSTOMER`, `ROLE_DRIVER`, `ROLE_DISPATCHER`, `ROLE_ADMIN` |
| `drivers` | `is_available` | `true` = can be assigned to a ride. Auto-flipped on ride completion. |
| `rides` | `driver_id` | NULL until a driver is assigned. |
| `rides` | `status` | Finite set: `PENDING`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |

---

## 4. Security System — Deep Dive

### 4.1 How JWT Works in This Application

```
Client                          Server
  │                               │
  ├──── POST /auth/login ────────►│
  │     {email, password}         │  1. Load user from DB by email
  │                               │  2. Compare BCrypt hash
  │                               │  3. Generate JWT (email + role as claims)
  │◄─── {token, role} ───────────┤
  │                               │
  ├──── GET /rides ──────────────►│
  │  Authorization: Bearer <jwt>  │  4. JwtAuthenticationFilter runs
  │                               │  5. Extract + validate token
  │                               │  6. Set SecurityContext
  │                               │  7. Controller handles request
  │◄─── 200 OK [{...}] ──────────┤
```

### 4.2 JwtService.java — What it Does

```java
// Located: com.cwtw.rideflow.security.JwtService

generateToken(String email, String role)
// → Creates a signed HS256 JWT with:
//     subject  = email
//     claim    = role
//     issuedAt = now
//     expiry   = now + 86400000ms (24 hours)
//     signature= HMAC-SHA256 using secret from application.properties

extractEmail(String token)
// → Parses token, returns the 'subject' claim (email)

extractRole(String token)
// → Parses token, returns the 'role' claim

validateToken(String token)
// → Returns true if:
//     1. Token parses without exception (not tampered)
//     2. Token is not expired
// → Returns false otherwise (never throws)
```

**JWT Secret (configured in `application.properties`):**
```properties
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
jwt.expiration=86400000
```
The secret is Base64-decoded into an HMAC-SHA256 key. **Change this value in production.**

### 4.3 JwtAuthenticationFilter.java — Request Lifecycle

This filter runs **on every single HTTP request** before Spring routes it to a controller.

```
Incoming Request
      │
      ▼
Does request have "Authorization: Bearer xxx" header?
      │
      ├── NO  → skip filter, pass to Spring Security
      │            (will result in 401 for protected endpoints)
      │
      └── YES → extract token string (after "Bearer ")
                      │
                      ▼
                Is token valid? (validateToken)
                      │
                      ├── NO  → skip, pass to Spring Security (401)
                      │
                      └── YES → extract email from token
                                      │
                                      ▼
                                Load UserDetails from DB (by email)
                                      │
                                      ▼
                                Create UsernamePasswordAuthenticationToken
                                Set it in SecurityContextHolder
                                      │
                                      ▼
                                Continue to Controller
```

### 4.4 SecurityConfig.java — Rules

```java
// CSRF: DISABLED (stateless REST API, no sessions)
// Session: STATELESS (no HttpSession created or used)

// Public routes (no token needed):
//   /auth/**   → all registration and login endpoints

// Protected routes (valid JWT required):
//   everything else → authenticated()
```

> **Important:** There is NO role-based restriction at the endpoint level in the current implementation. Any authenticated user (regardless of role) can call any protected endpoint. Role-based restrictions would need `@PreAuthorize("hasRole('ADMIN')")` annotations — not yet added. **The JWT still encodes the role**, so you can enforce it in the frontend or add it server-side later.

### 4.5 CustomUserDetailsService.java

```java
// Located: com.cwtw.rideflow.security.CustomUserDetailsService
// Implements: UserDetailsService

loadUserByUsername(String email)
// → Queries UserRepository.findByEmail(email)
// → If not found: throws UsernameNotFoundException
// → If found: returns Spring's UserDetails with:
//     username   = user.email
//     password   = user.password (BCrypt hash)
//     authorities = [SimpleGrantedAuthority(user.role)]
```

### 4.6 PasswordConfig.java

```java
// Provides a BCryptPasswordEncoder bean
// Used by:
//   - AuthService.register() → to hash new passwords before saving
//   - AuthService.login()   → to compare submitted password against hash
//   - SecurityConfig        → DaoAuthenticationProvider needs it
```

### 4.7 Token Expiry & Refresh

- Tokens expire after **24 hours** (86400000 ms)
- There is **no refresh token endpoint** — when a token expires, call `/auth/login` again
- An expired token will cause the filter to skip authentication → Spring Security returns `401`

---

## 5. Authentication Endpoints (`/auth`)

> ✅ No token required for any endpoint in this section.
> ✅ CSRF is disabled — no CSRF token needed.

---

### `POST /auth/register`

**Purpose:** Register a new end-user customer.

**Request**
```
POST http://localhost:8080/auth/register
Content-Type: application/json
```
```json
{
  "email": "alice@example.com",
  "password": "mypassword123"
}
```

**Field Validation Rules:**

| Field | Type | Required | Rules | Error if violated |
|-------|------|----------|-------|-------------------|
| `email` | String | YES | Must be valid email format (RFC 5322) | `"Invalid email format"` |
| `email` | String | YES | Must not be blank/null | `"Email is required"` |
| `password` | String | YES | Must not be blank/null | `"Password is required"` |

**Success Response — `200 OK`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZUBleGFtcGxlLmNvbSIsInJvbGUiOiJST0xFX0NVU1RPTUVSIiwiaWF0IjoxNzEzMTI0MDAwLCJleHAiOjE3MTMyMTA0MDB9.XXXX",
  "role": "ROLE_CUSTOMER"
}
```

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `400` | `email` field missing or empty string | `{"timestamp":"...","status":400,"errors":{"email":"Email is required"}}` |
| `400` | `email` value not a valid email | `{"timestamp":"...","status":400,"errors":{"email":"Invalid email format"}}` |
| `400` | `password` field missing or empty string | `{"timestamp":"...","status":400,"errors":{"password":"Password is required"}}` |
| `400` | Body is not valid JSON | Spring HttpMessageNotReadableException (400) |
| `400` | Content-Type header missing | Spring 415 or 400 |
| `409` | Email already exists in DB | `{"timestamp":"...","status":409,"error":"Conflict","message":"Email already registered"}` |
| `500` | DB connection failure | `{"timestamp":"...","status":500,"message":"..."}` |

---

### `POST /auth/register/driver`

**Purpose:** Register a new user with the `ROLE_DRIVER` role.

Identical request and error handling to `/auth/register`. The only difference is the role in the response.

**Success Response — `200 OK`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "ROLE_DRIVER"
}
```

> **After registering a driver-user**, you must also call `POST /drivers` with the user's ID to create their **driver profile** (license number, availability). Without this step, they cannot be assigned rides.

---

### `POST /auth/register/dispatcher`

**Purpose:** Register with `ROLE_DISPATCHER`.

**Success Response — `200 OK`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "ROLE_DISPATCHER"
}
```

---

### `POST /auth/register/admin`

**Purpose:** Register with `ROLE_ADMIN`.

**Success Response — `200 OK`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "ROLE_ADMIN"
}
```

---

### `POST /auth/login`

**Purpose:** Authenticate any existing user and get a fresh JWT. Works for all roles.

**Request**
```
POST http://localhost:8080/auth/login
Content-Type: application/json
```
```json
{
  "email": "alice@example.com",
  "password": "mypassword123"
}
```

**Success Response — `200 OK`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "ROLE_CUSTOMER"
}
```

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `400` | Missing or blank fields | `{"errors":{"email":"Email is required",...}}` |
| `400` | Invalid email format | `{"errors":{"email":"Invalid email format"}}` |
| `401` | Email not found in database | `{"status":401,"message":"Invalid email or password"}` |
| `401` | Email found but password doesn't match BCrypt hash | `{"status":401,"message":"Invalid email or password"}` |
| `500` | DB connection failure | `{"status":500,"message":"..."}` |

> **Security note:** Both "wrong email" and "wrong password" return the exact same `401` message intentionally, to prevent attackers from enumerating valid emails.

---

## 6. Ride Endpoints (`/rides`)

> 🔐 All endpoints require: `Authorization: Bearer <valid_jwt_token>`

---

### `POST /rides`

**Purpose:** Create a new ride request. The ride is created with status `PENDING` and no driver assigned. The ride is linked to the **currently authenticated user** (extracted from the JWT).

**Request**
```
POST http://localhost:8080/rides
Authorization: Bearer <token>
Content-Type: application/json
```
```json
{
  "pickupLocation": "Times Square, New York",
  "dropLocation": "JFK Airport, New York",
  "scheduledTime": "2026-04-15T10:30:00"
}
```

**Field Validation Rules:**

| Field | Type | Required | Rules | Error if violated |
|-------|------|----------|-------|-------------------|
| `pickupLocation` | String | YES | Not blank | `"Pickup location is required"` |
| `dropLocation` | String | YES | Not blank | `"Drop location is required"` |
| `scheduledTime` | String (ISO 8601) | YES | Not null, format: `YYYY-MM-DDTHH:mm:ss` | `"Scheduled time is required"` |

**Success Response — `200 OK`**
```json
{
  "id": 1,
  "pickupLocation": "Times Square, New York",
  "dropLocation": "JFK Airport, New York",
  "status": "PENDING",
  "driverId": null
}
```

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `400` | Any required field missing or blank | `{"status":400,"errors":{"pickupLocation":"Pickup location is required"}}` |
| `400` | `scheduledTime` has wrong format (e.g. `"15/04/2026"`) | `{"status":400,"message":"JSON parse error: ..."}` |
| `401` | No `Authorization` header | Spring Security 401 (no body or default body) |
| `401` | Token is expired | Spring Security 401 |
| `401` | Token is malformed/tampered | Spring Security 401 |
| `404` | User extracted from JWT doesn't exist in DB (rare edge case) | `{"status":404,"message":"User not found"}` |
| `500` | Database error | `{"status":500,"message":"..."}` |

---

### `GET /rides`

**Purpose:** Retrieve a list of all rides in the system.

**Request**
```
GET http://localhost:8080/rides
Authorization: Bearer <token>
```
No request body.

**Success Response — `200 OK`**
```json
[
  {
    "id": 1,
    "pickupLocation": "Times Square, NY",
    "dropLocation": "JFK Airport, NY",
    "status": "PENDING",
    "driverId": null
  },
  {
    "id": 2,
    "pickupLocation": "Brooklyn Bridge",
    "dropLocation": "LaGuardia Airport",
    "status": "ASSIGNED",
    "driverId": 3
  },
  {
    "id": 3,
    "pickupLocation": "Central Park",
    "dropLocation": "Newark Airport",
    "status": "COMPLETED",
    "driverId": 1
  }
]
```

Returns an empty array `[]` if no rides exist.

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `401` | Missing/invalid/expired JWT | Spring Security 401 |
| `500` | Database error | `{"status":500,"message":"..."}` |

---

### `POST /rides/{rideId}/assign/{driverId}`

**Purpose:** Assign a specific driver to a specific ride. Sets ride status to `ASSIGNED` and marks the driver as `isAvailable = false`.

**Request**
```
POST http://localhost:8080/rides/1/assign/2
Authorization: Bearer <token>
```
No request body. Both parameters are in the URL path.

**URL Path Variables:**

| Variable | Type | Description |
|----------|------|-------------|
| `rideId` | Long | The ID of the ride to assign |
| `driverId` | Long | The ID of the driver profile (NOT the user ID) |

**Success Response — `200 OK`**
```json
{
  "id": 1,
  "pickupLocation": "Times Square, NY",
  "dropLocation": "JFK Airport, NY",
  "status": "ASSIGNED",
  "driverId": 2
}
```

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `400` | `rideId` or `driverId` is not a valid number (e.g. `/rides/abc/assign/2`) | Spring 400 MethodArgumentTypeMismatchException |
| `400` | Driver exists but `isAvailable = false` (already on a ride) | `{"status":400,"message":"Driver is not available"}` |
| `401` | Missing/invalid/expired JWT | Spring Security 401 |
| `404` | No ride found with that `rideId` | `{"status":404,"message":"Ride not found"}` |
| `404` | No driver found with that `driverId` | `{"status":404,"message":"Driver not found"}` |
| `500` | Database error | `{"status":500,"message":"..."}` |

---

### `PATCH /rides/{rideId}/status?status=X`

**Purpose:** Update the status of a ride. When set to `COMPLETED` or `CANCELLED`, the assigned driver (if any) is automatically set back to `isAvailable = true`.

**Request**
```
PATCH http://localhost:8080/rides/1/status?status=IN_PROGRESS
Authorization: Bearer <token>
```
No request body.

**URL Path Variables:**

| Variable | Type | Description |
|----------|------|-------------|
| `rideId` | Long | The ID of the ride |

**Query Parameters:**

| Parameter | Type | Required | Allowed Values |
|-----------|------|----------|----------------|
| `status` | String | YES | `PENDING`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |

**Ride Status State Transitions:**

```
PENDING ──────────────────────────────────────────▶ CANCELLED
  │
  └──▶ ASSIGNED ──────────────────────────────────▶ CANCELLED
          │
          └──▶ IN_PROGRESS ──────────────────────▶ CANCELLED
                    │
                    └──▶ COMPLETED
```

**Side Effects of Status Changes:**

| Transition | Side Effect |
|------------|-------------|
| `PENDING → ASSIGNED` | Driver `isAvailable = false` (done via `assignDriver` endpoint) |
| `ANY → COMPLETED` | Driver (if assigned) set to `isAvailable = true` |
| `ANY → CANCELLED` | Driver (if assigned) set to `isAvailable = true` |
| All others | No side effect |

**Success Response — `200 OK`**
```json
{
  "id": 1,
  "pickupLocation": "Times Square, NY",
  "dropLocation": "JFK Airport, NY",
  "status": "COMPLETED",
  "driverId": 2
}
```

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `400` | `status` query param missing entirely | Spring 400 MissingServletRequestParameterException |
| `400` | `status` value is not one of the 5 allowed values (case-insensitive) | `{"status":400,"message":"Invalid ride status: FLYING"}` |
| `400` | `rideId` not a valid number | Spring 400 MethodArgumentTypeMismatchException |
| `401` | Missing/invalid/expired JWT | Spring Security 401 |
| `404` | No ride found with that `rideId` | `{"status":404,"message":"Ride not found"}` |
| `500` | Database error | `{"status":500,"message":"..."}` |

---

## 7. Driver Endpoints (`/drivers`)

> 🔐 All endpoints require: `Authorization: Bearer <valid_jwt_token>`

---

### `POST /drivers`

**Purpose:** Create a driver profile by linking it to an existing user. You must first register a user via `/auth/register/driver` to get their user ID, then call this endpoint to complete the driver setup.

**Request**
```
POST http://localhost:8080/drivers
Authorization: Bearer <token>
Content-Type: application/json
```
```json
{
  "userId": 2,
  "licenseNumber": "DL-2024-NY-00123"
}
```

**Field Rules:**

| Field | Type | Required | Rules |
|-------|------|----------|-------|
| `userId` | Number (Long) | YES | Must match an existing user in the `users` table |
| `licenseNumber` | String | YES | Must be unique across all drivers |

**Where does `userId` come from?**  
It is the **auto-generated database primary key** of the user. Currently there is no `GET /users` endpoint to fetch it — you would need to either:
- Store it in your frontend after registration (the registration endpoint does not currently return the user ID — only the token and role)
- Query your Supabase database directly

> **Note for improvement:** If you need the user ID, either check your Supabase dashboard → `users` table, or add a `GET /users/me` endpoint that decodes the JWT to get the email and returns the user's ID.

**Success Response — `200 OK`**

```json
{
  "id": 1,
  "licenseNumber": "DL-2024-NY-00123",
  "isAvailable": true
}
```
> New drivers always start with `isAvailable = true`.

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `400` | `userId` or `licenseNumber` missing from request body | Spring 400 / NullPointerException mapped to 500 |
| `401` | Missing/invalid/expired JWT | Spring Security 401 |
| `404` | No user found with that `userId` | `{"status":404,"message":"User not found"}` |
| `500` | `licenseNumber` already exists (DB unique constraint violation) | `{"status":500,"message":"could not execute statement [ERROR: duplicate key..."}` |
| `500` | Database error | `{"status":500,"message":"..."}` |

---

### `GET /drivers`

**Purpose:** Retrieve a list of all driver profiles.

**Request**
```
GET http://localhost:8080/drivers
Authorization: Bearer <token>
```

**Success Response — `200 OK`**
```json
[
  {
    "id": 1,
    "licenseNumber": "DL-2024-NY-00123",
    "isAvailable": true
  },
  {
    "id": 2,
    "licenseNumber": "DL-2022-CA-55501",
    "isAvailable": false
  }
]
```

Returns `[]` if no driver profiles exist.

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `401` | Missing/invalid/expired JWT | Spring Security 401 |
| `500` | Database error | `{"status":500,"message":"..."}` |

---

### `PATCH /drivers/{driverId}/availability?available=X`

**Purpose:** Manually set a driver's availability status. Useful if a driver goes offline, takes a break, or comes back online.

**Request**
```
PATCH http://localhost:8080/drivers/1/availability?available=false
Authorization: Bearer <token>
```

**URL Path Variables:**

| Variable | Type | Description |
|----------|------|-------------|
| `driverId` | Long | The driver profile ID (from the `drivers` table, not user ID) |

**Query Parameters:**

| Parameter | Type | Required | Allowed Values |
|-----------|------|----------|----------------|
| `available` | Boolean | YES | `true` or `false` |

**Success Response — `200 OK`**
```json
{
  "id": 1,
  "licenseNumber": "DL-2024-NY-00123",
  "isAvailable": false
}
```

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `400` | `available` query param missing | Spring 400 MissingServletRequestParameterException |
| `400` | `available` is not `true` or `false` (e.g. `?available=yes`) | Spring 400 MethodArgumentTypeMismatchException |
| `400` | `driverId` not a number | Spring 400 MethodArgumentTypeMismatchException |
| `401` | Missing/invalid/expired JWT | Spring Security 401 |
| `404` | No driver found with that `driverId` | `{"status":404,"message":"Driver not found"}` |
| `500` | Database error | `{"status":500,"message":"..."}` |

---

## 8. Dispatcher Endpoints (`/dispatcher`)

> 🔐 All endpoints require: `Authorization: Bearer <valid_jwt_token>`

---

### `POST /dispatcher/rides`

**Purpose:** A dispatcher creates a ride on behalf of the system. The ride is linked to the authenticated dispatcher's own user account. Functionally identical to `POST /rides`.

**Request**
```
POST http://localhost:8080/dispatcher/rides
Authorization: Bearer <dispatcher_token>
Content-Type: application/json
```
```json
{
  "pickupLocation": "Central Park North",
  "dropLocation": "Newark Liberty International Airport",
  "scheduledTime": "2026-04-15T14:30:00"
}
```

Same field validation rules as `POST /rides`.

**Success Response — `200 OK`**
```json
{
  "id": 4,
  "pickupLocation": "Central Park North",
  "dropLocation": "Newark Liberty International Airport",
  "status": "PENDING",
  "driverId": null
}
```

**All Possible Errors:** Identical to `POST /rides` above.

---

### `POST /dispatcher/rides/{rideId}/assign/{driverId}`

**Purpose:** Dispatcher manually picks a specific driver and assigns them to a specific ride. Sets `ride.status = ASSIGNED` and `driver.isAvailable = false`.

**Request**
```
POST http://localhost:8080/dispatcher/rides/4/assign/1
Authorization: Bearer <dispatcher_token>
```
No request body.

**URL Path Variables:**

| Variable | Type | Description |
|----------|------|-------------|
| `rideId` | Long | ID of the ride to assign |
| `driverId` | Long | ID of the driver (from `drivers.id`, not `users.id`) |

**Success Response — `200 OK`**
```json
{
  "id": 4,
  "pickupLocation": "Central Park North",
  "dropLocation": "Newark Liberty International Airport",
  "status": "ASSIGNED",
  "driverId": 1
}
```

**All Possible Errors:** Identical to `POST /rides/{rideId}/assign/{driverId}` above.

---

### `POST /dispatcher/rides/{rideId}/auto-assign`

**Purpose:** The system automatically finds the **first available driver** in the database (ordered by their database insert order) and assigns them to the ride. No need to specify a `driverId`.

**How auto-assign works:**
1. Queries `DriverRepository.findByIsAvailable(true)` — returns list of available drivers
2. If list is **empty** → throws `404` with "No available drivers at the moment"
3. Takes the **first driver** in the list (`index 0`)
4. Calls the same assign logic as the manual assign: sets `ride.status = ASSIGNED`, sets `driver.isAvailable = false`

**Request**
```
POST http://localhost:8080/dispatcher/rides/4/auto-assign
Authorization: Bearer <dispatcher_token>
```
No request body.

**URL Path Variables:**

| Variable | Type | Description |
|----------|------|-------------|
| `rideId` | Long | ID of the ride to auto-assign |

**Success Response — `200 OK`**
```json
{
  "id": 4,
  "pickupLocation": "Central Park North",
  "dropLocation": "Newark Liberty International Airport",
  "status": "ASSIGNED",
  "driverId": 1
}
```

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `400` | `rideId` not a number | Spring 400 |
| `401` | Missing/invalid/expired JWT | Spring Security 401 |
| `404` | No ride found with that `rideId` (checked inside `assignDriver`) | `{"status":404,"message":"Ride not found"}` |
| `404` | Zero available drivers in the system | `{"status":404,"message":"No available drivers at the moment"}` |
| `500` | Database error | `{"status":500,"message":"..."}` |

---

## 9. Admin Endpoints (`/admin`)

> 🔐 All endpoints require: `Authorization: Bearer <valid_jwt_token>`

---

### `POST /admin/vehicles`

**Purpose:** Add a new vehicle to the system.

**Request**
```
POST http://localhost:8080/admin/vehicles
Authorization: Bearer <admin_token>
Content-Type: application/json
```
```json
{
  "plateNumber": "NYC-AB-1234",
  "model": "Toyota Camry 2024",
  "status": "ACTIVE"
}
```

**Field Rules:**

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `plateNumber` | String | YES | Must be unique across all vehicles |
| `model` | String | YES | Free-form text (e.g. "Toyota Camry 2024") |
| `status` | String | NO | Defaults to `"ACTIVE"` if omitted. Suggested: `ACTIVE`, `INACTIVE`, `MAINTENANCE` |

**Success Response — `200 OK`**
```json
{
  "id": 1,
  "plateNumber": "NYC-AB-1234",
  "model": "Toyota Camry 2024",
  "status": "ACTIVE"
}
```

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `400` | Body is not valid JSON | Spring 400 HttpMessageNotReadableException |
| `401` | Missing/invalid/expired JWT | Spring Security 401 |
| `500` | `plateNumber` already exists (DB unique constraint) | `{"status":500,"message":"could not execute statement [ERROR: duplicate key value..."}` |
| `500` | Database error | `{"status":500,"message":"..."}` |

---

### `GET /admin/vehicles`

**Purpose:** Retrieve a list of all vehicles in the system.

**Request**
```
GET http://localhost:8080/admin/vehicles
Authorization: Bearer <token>
```

**Success Response — `200 OK`**
```json
[
  {
    "id": 1,
    "plateNumber": "NYC-AB-1234",
    "model": "Toyota Camry 2024",
    "status": "ACTIVE"
  },
  {
    "id": 2,
    "plateNumber": "LAX-XY-9900",
    "model": "Honda Accord 2023",
    "status": "MAINTENANCE"
  }
]
```

Returns `[]` if no vehicles exist.

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `401` | Missing/invalid/expired JWT | Spring Security 401 |
| `500` | Database error | `{"status":500,"message":"..."}` |

---

### `POST /admin/vehicles/{vehicleId}/maintenance`

**Purpose:** Add a maintenance record for a specific vehicle. The `date` is set automatically to today's date on the server.

**Request**
```
POST http://localhost:8080/admin/vehicles/1/maintenance
Authorization: Bearer <admin_token>
Content-Type: application/json
```
```json
{
  "description": "Full engine service, oil change, and tire rotation"
}
```

**URL Path Variables:**

| Variable | Type | Description |
|----------|------|-------------|
| `vehicleId` | Long | ID of the vehicle from the `vehicles` table |

**Field Rules:**

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `description` | String | YES | Free-form text describing the maintenance done |

**Success Response — `200 OK`**
```json
{
  "id": 1,
  "vehicle": {
    "id": 1,
    "plateNumber": "NYC-AB-1234",
    "model": "Toyota Camry 2024",
    "status": "ACTIVE"
  },
  "description": "Full engine service, oil change, and tire rotation",
  "date": "2026-04-15"
}
```

> Note: The full `vehicle` object is returned embedded because `MaintenanceRecord` entity has a direct `@ManyToOne` reference to `Vehicle` and it serializes the whole object. This is one place where an entity is exposed directly.

**All Possible Errors:**

| HTTP | When it happens | Response body |
|------|----------------|---------------|
| `400` | `vehicleId` not a number | Spring 400 |
| `400` | Body is not valid JSON or `description` key missing | Spring 400 / NullPointerException |
| `401` | Missing/invalid/expired JWT | Spring Security 401 |
| `404` | No vehicle found with that `vehicleId` | `{"status":404,"message":"Vehicle not found"}` |
| `500` | Database error | `{"status":500,"message":"..."}` |

---

## 10. Error Handling — Every Possible Error

### Error Response Format

All errors from `GlobalExceptionHandler` follow this format:
```json
{
  "timestamp": "2026-04-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Human-readable error description"
}
```

Validation errors have an extra `errors` map instead of `message`:
```json
{
  "timestamp": "2026-04-15T10:30:00",
  "status": 400,
  "errors": {
    "email": "Invalid email format",
    "password": "Password is required"
  }
}
```

### HTTP Status Code Reference

| Code | Meaning | Common Cause |
|------|---------|-------------|
| `200` | OK | Successful operation |
| `400` | Bad Request | Validation failure, wrong format, missing field, invalid enum value |
| `401` | Unauthorized | Missing/expired/invalid JWT, wrong password |
| `403` | Forbidden | Valid JWT but accessing a resource that requires different role *(not currently enforced)* |
| `404` | Not Found | Entity with given ID doesn't exist, no available drivers |
| `405` | Method Not Allowed | Calling `GET` on a `POST`-only endpoint |
| `409` | Conflict | Email already registered |
| `415` | Unsupported Media Type | Missing `Content-Type: application/json` on POST/PATCH |
| `500` | Internal Server Error | DB constraint violations, unexpected null, DB connection loss |

### Spring Security 401 vs Application 401

| Source | When | Has JSON body? |
|--------|------|----------------|
| Spring Security | No `Authorization` header, invalid token format | No, or minimal response |
| `GlobalExceptionHandler` | Email not found, wrong password (in AuthService) | Yes, full JSON |

### What the frontend must always do

1. Check `Content-Type: application/json` is set on all POST/PATCH requests
2. Store the JWT token after login/register
3. Add `Authorization: Bearer <token>` to every non-auth request
4. On `401` — redirect to login screen and clear the stored token
5. On `400` — display the `errors` map or `message` field to the user
6. On `404` — resource doesn't exist, show appropriate UI
7. On `409` — email already taken, prompt user to login instead

---

## 11. Data Transfer Objects (DTOs)

DTOs are used **only in controllers** — entities are never directly returned or accepted.

### `AuthRequestDTO`
```java
// Used as request body for: POST /auth/register, POST /auth/login
String email;       // @Email @NotBlank
String password;    // @NotBlank
```

### `AuthResponseDTO`
```java
// Returned by: all /auth/** endpoints
String token;   // JWT string
String role;    // e.g. "ROLE_CUSTOMER"
```

### `RideRequestDTO`
```java
// Used as request body for: POST /rides, POST /dispatcher/rides
String pickupLocation;      // @NotBlank
String dropLocation;        // @NotBlank
LocalDateTime scheduledTime; // @NotNull — format: "2026-04-15T10:00:00"
```

### `RideResponseDTO`
```java
// Returned by: all /rides and /dispatcher endpoints
Long id;
String pickupLocation;
String dropLocation;
String status;      // "PENDING" | "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"
Long driverId;      // null if not assigned
```

### `DriverDTO`
```java
// Returned by: all /drivers endpoints
Long id;
String licenseNumber;
boolean isAvailable;
```

---

## 12. All Entities / Models

### `User`
```java
Long id;             // auto-generated PK
String email;        // unique, validated @Email @NotBlank
String password;     // BCrypt hash, never returned in responses
String role;         // ROLE_CUSTOMER | ROLE_DRIVER | ROLE_DISPATCHER | ROLE_ADMIN
```
**Table:** `users`

---

### `Driver`
```java
Long id;               // auto-generated PK
User user;             // @OneToOne → users.id (unique FK)
String licenseNumber;  // unique
boolean isAvailable;   // true = ready for rides
```
**Table:** `drivers`

---

### `Vehicle`
```java
Long id;            // auto-generated PK
String plateNumber; // unique
String model;       // e.g. "Toyota Camry"
String status;      // free text: ACTIVE | INACTIVE | MAINTENANCE
```
**Table:** `vehicles`

---

### `Ride`
```java
Long id;                    // auto-generated PK
String pickupLocation;
String dropLocation;
LocalDateTime scheduledTime;
RideStatus status;          // enum: PENDING | ASSIGNED | IN_PROGRESS | COMPLETED | CANCELLED
User user;                  // @ManyToOne → users.id (NOT NULL)
Driver driver;              // @ManyToOne → drivers.id (nullable)
```
**Table:** `rides`
**Enum:** `Ride.RideStatus` — defined as an inner enum, stored as String in DB.

---

### `License`
```java
Long id;           // auto-generated PK
Driver driver;     // @ManyToOne → drivers.id
LocalDate expiryDate;
```
**Table:** `licenses`

> Note: There is no endpoint currently for creating or querying licenses. The entity and repository exist but the service/controller layer is not yet implemented for licenses.

---

### `MaintenanceRecord`
```java
Long id;            // auto-generated PK
Vehicle vehicle;    // @ManyToOne → vehicles.id
String description;
LocalDate date;     // set to today automatically by AdminService
```
**Table:** `maintenance_records`

---

## 13. Service Layer Logic

### `AuthService`
| Method | Logic |
|--------|-------|
| `register(request, role)` | 1) Check email uniqueness → `409` if exists. 2) Hash password with BCrypt. 3) Save User. 4) Generate JWT. 5) Return `AuthResponseDTO`. |
| `login(request)` | 1) Find user by email → `401` if not found. 2) Compare raw password vs BCrypt hash → `401` if mismatch. 3) Generate JWT. 4) Return `AuthResponseDTO`. |

### `RideService`
| Method | Logic |
|--------|-------|
| `createRide(request, email)` | Find user by email → `404`. Build Ride with PENDING status. Save and return DTO. |
| `assignDriver(rideId, driverId)` | Find ride → `404`. Find driver → `404`. Check `isAvailable` → `400`. Set `ride.driver`, `ride.status = ASSIGNED`, `driver.isAvailable = false`. Save both. |
| `updateStatus(rideId, status)` | Find ride → `404`. Parse status enum → `400` if invalid. If COMPLETED or CANCELLED and driver exists → set `driver.isAvailable = true`. Save. |
| `getAllRides()` | `rideRepository.findAll()` → map to DTOs. |

### `DriverService`
| Method | Logic |
|--------|-------|
| `createDriver(userId, licenseNumber)` | Find user by ID → `404`. Build Driver. Save. Return DTO. |
| `updateAvailability(driverId, available)` | Find driver → `404`. Set `isAvailable`. Save. Return DTO. |
| `getAllDrivers()` | `driverRepository.findAll()` → map to DTOs. |

### `DispatcherService`
| Method | Logic |
|--------|-------|
| `createRideForCustomer(request, email)` | Delegates to `RideService.createRide()`. |
| `assignDriver(rideId, driverId)` | Delegates to `RideService.assignDriver()`. |
| `autoAssignDriver(rideId)` | Query `driverRepository.findByIsAvailable(true)`. If empty → `404`. Take index 0. Call `RideService.assignDriver()`. |

### `AdminService`
| Method | Logic |
|--------|-------|
| `addVehicle(plateNumber, model, status)` | Build Vehicle (default status `ACTIVE`). Save. Return entity directly. |
| `getAllVehicles()` | `vehicleRepository.findAll()`. |
| `addMaintenanceRecord(vehicleId, description)` | Find vehicle → `404`. Build record with `date = LocalDate.now()`. Save. Return entity. |

---

## 14. Frontend Integration Guide

### Step 1 — Register and store token
```javascript
const res = await fetch("http://localhost:8080/auth/register", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ email: "user@test.com", password: "pass123" })
});
const data = await res.json();
localStorage.setItem("token", data.token);
localStorage.setItem("role", data.role);
```

### Step 2 — Authenticated request helper
```javascript
function authHeaders() {
  return {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${localStorage.getItem("token")}`
  };
}
```

### Step 3 — Create a ride
```javascript
const res = await fetch("http://localhost:8080/rides", {
  method: "POST",
  headers: authHeaders(),
  body: JSON.stringify({
    pickupLocation: "Times Square",
    dropLocation: "JFK Airport",
    scheduledTime: "2026-04-15T10:00:00"
  })
});
if (res.status === 401) { /* redirect to login */ }
const ride = await res.json();
// ride.id, ride.status === "PENDING"
```

### Step 4 — Handle errors
```javascript
if (!res.ok) {
  const err = await res.json();
  if (err.errors) {
    // Validation: show err.errors.email, err.errors.password, etc.
  } else {
    // General: show err.message
  }
}
```

### scheduledTime format
Must be ISO 8601 — `"2026-04-15T10:30:00"` (no timezone suffix, treated as server local time).

---

## 15. Complete Endpoint Reference Table

| # | Method | Endpoint | Auth | What it Does |
|---|--------|----------|------|-------------|
| 1 | POST | `/auth/register` | ❌ | Register new CUSTOMER, returns JWT |
| 2 | POST | `/auth/register/driver` | ❌ | Register new DRIVER user, returns JWT |
| 3 | POST | `/auth/register/dispatcher` | ❌ | Register new DISPATCHER, returns JWT |
| 4 | POST | `/auth/register/admin` | ❌ | Register new ADMIN, returns JWT |
| 5 | POST | `/auth/login` | ❌ | Login any user, returns JWT |
| 6 | POST | `/rides` | ✅ | Create a ride (PENDING, no driver) |
| 7 | GET | `/rides` | ✅ | Get all rides |
| 8 | POST | `/rides/{rideId}/assign/{driverId}` | ✅ | Assign driver to ride → ASSIGNED |
| 9 | PATCH | `/rides/{rideId}/status?status=X` | ✅ | Update ride status |
| 10 | POST | `/drivers` | ✅ | Create driver profile (needs userId) |
| 11 | GET | `/drivers` | ✅ | Get all driver profiles |
| 12 | PATCH | `/drivers/{driverId}/availability?available=X` | ✅ | Toggle driver availability |
| 13 | POST | `/dispatcher/rides` | ✅ | Dispatcher creates a ride |
| 14 | POST | `/dispatcher/rides/{rideId}/assign/{driverId}` | ✅ | Dispatcher assigns driver manually |
| 15 | POST | `/dispatcher/rides/{rideId}/auto-assign` | ✅ | Auto-assign first available driver |
| 16 | POST | `/admin/vehicles` | ✅ | Add a vehicle |
| 17 | GET | `/admin/vehicles` | ✅ | Get all vehicles |
| 18 | POST | `/admin/vehicles/{vehicleId}/maintenance` | ✅ | Log maintenance for a vehicle |

> **No DELETE or PUT endpoints exist yet.** To add them, create new methods in the relevant Service and Controller.

---

## 16. Running the Application

### Prerequisites
- Java 21 JRE installed (`java -version`)
- Internet access (connects to Supabase PostgreSQL on startup)

### Start
```bash
cd /home/aliakbar/IdeaProjects/hanz/RideFlow
mvn spring-boot:run
```

### What happens on first boot
1. Flyway runs `V1__init.sql` — creates all 6 tables in Supabase
2. Hibernate validates schema against entities (`ddl-auto=update`)
3. Application listens on port 8080

### Compile only (no run)
```bash
mvn compile
```

### Build JAR
```bash
mvn package -DskipTests
java -jar target/RideFlow-0.0.1-SNAPSHOT.jar
```

### Configuration file
`src/main/resources/application.properties`
```properties
spring.datasource.url=jdbc:postgresql://db.gwtmkxnwbyjpdtzluvgn.supabase.co:5432/postgres?sslmode=require
spring.datasource.username=postgres
spring.datasource.password=Aliakbar786$
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
jwt.expiration=86400000
```

> ⚠️ Change `jwt.secret` and DB credentials before deploying to production.
