# Smart Parking System Backend

Backend API for the Smart Parking System project. It is built with Spring Boot and provides role-based REST APIs for drivers, vendors, and admins to manage parking discovery, bookings, slot availability, payments, user profiles, and dashboards.

## Current Stack

- Java 21
- Spring Boot 4.0.3
- Spring Web
- Spring Security with JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven wrapper
- Lombok
- Dotenv Java
- Khalti e-payment integration
- Docker / Docker Compose support

## Main Features

- Driver and vendor registration
- JWT access tokens and refresh tokens
- Refresh token rotation
- Logout and logout from all devices
- Access token blacklisting on logout
- BCrypt password hashing
- Role-based authorization for `DRIVER`, `VENDOR`, and `ADMIN`
- User profile viewing and updating
- Password change from profile update
- Vendor parking location CRUD
- Automatic parking slot creation when a vendor creates a location
- Separate two-wheeler and four-wheeler slot counts
- Available slot updates per vehicle type
- Parking search by area and availability
- Nearby and nearest parking lookup
- Thamel-focused road-distance search using Dijkstra
- GPS-distance based parking search
- Driver booking creation
- Vehicle-type aware slot reservation
- Booking amount calculation using hourly parking rates
- Driver booking history
- Booking lookup with permission checks
- Booking cancellation
- Vendor booking check-in and completion through one status route
- Refund eligibility handling for cancellations more than 1 hour before start time
- Khalti payment initiation
- Khalti payment verification callback
- Vendor dashboard with location and slot summaries
- Admin dashboard with user and booking stats
- Admin booking listing
- Admin user listing with optional role filter
- Soft-delete support through `deleted_at`
- Unified API response wrapper
- Health endpoint and Spring Actuator health exposure
- Seed users for local development

## Project Structure

```text
src/main/java/com/projectwork/Smart/Parking/System/
├── config/         API constants, seed data, app configuration
├── controller/     REST controllers
├── dto/            Request and response DTOs
├── entity/         JPA entities and enums
├── exception/      Global exception handling
├── repository/     Spring Data repositories
├── security/       JWT, security filter, user details service
└── service/        Business logic
```

## Requirements

- Java 21
- Maven is optional because the project includes `mvnw`
- PostgreSQL 16 or compatible
- Docker Desktop, optional

## Environment Variables

Create or update `smart-parking-system-backend/.env`:

```env
DB_URL=jdbc:postgresql://localhost:5432/smart_parking
DB_USERNAME=postgres
DB_PASSWORD=your_database_password

JWT_SECRET=replace_with_a_long_random_secret_at_least_32_characters

APP_WEBSITE_URL=http://localhost:8080
KHALTI_RETURN_URL=http://localhost:8080/api/payments/khalti/verify
```

Notes:

- `application.yml` reads database values from `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- Keep `JWT_SECRET` long and private. HS256 signing needs a sufficiently long secret.
- Khalti URLs are configured for Khalti development endpoints.
- The current Khalti secret key is configured in `application.yml`; move it to an environment variable before production use.

## Local Setup

1. Go to the backend folder:

   ```bash
   cd smart-parking-system-backend
   ```

2. Create the PostgreSQL database:

   ```sql
   CREATE DATABASE smart_parking;
   ```

3. Configure `.env` using the template above.

4. Start the backend:

   ```bash
   ./mvnw spring-boot:run
   ```

5. Check the API:

   ```bash
   curl http://localhost:8080/api/health
   ```

The API runs on `http://localhost:8080` by default.

## Docker Setup

For backend development with PostgreSQL:

```bash
cd smart-parking-system-backend
docker compose -f docker-compose-dev.yml up --build
```

The development compose file starts:

- Backend on `http://localhost:8080`
- PostgreSQL on `localhost:5432`
- A Maven cache volume for faster rebuilds

If the external Docker network does not exist yet, create it first:

```bash
docker network create smart-parking-dev-network
```

## Build and Test

```bash
cd smart-parking-system-backend
./mvnw clean package
./mvnw test
```

Build without running tests:

```bash
./mvnw clean package -DskipTests
```

## Seed Users

On startup, the app creates these users if they do not already exist:

| Role | Email | Password |
| --- | --- | --- |
| ADMIN | `admin@parking.com` | `Admin@123` |
| VENDOR | `vendor@parking.com` | `Vendor@123` |
| DRIVER | `driver@parking.com` | `Driver@123` |

Use these only for local development.

## Authentication

Public endpoints:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/health`
- `GET /api/payments/khalti/verify`

Protected endpoints require:

```http
Authorization: Bearer <accessToken>
```

Login and registration return:

```json
{
  "accessToken": "jwt-token",
  "refreshToken": "refresh-token",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "userId": "uuid",
  "name": "Driver One",
  "email": "driver@parking.com",
  "role": "DRIVER"
}
```

## Common Response Format

All controllers return an `ApiResponse<T>` wrapper:

```json
{
  "responseCode": 200,
  "responseMessage": "Success message",
  "timestamp": "2026-05-20T10:30:00",
  "data": {}
}
```

## API Endpoints

### Health

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| GET | `/api/health` | Public | Service health check |

### Auth

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | Register a `DRIVER` or `VENDOR` |
| POST | `/api/auth/login` | Public | Login and receive access and refresh tokens |
| POST | `/api/auth/refresh` | Public | Rotate refresh token and receive a new access token |
| POST | `/api/auth/logout` | Authenticated | Blacklist current access token and revoke refresh token |
| POST | `/api/auth/logout-all` | Authenticated | Revoke all refresh tokens for the current user |

Register body:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "Password@123",
  "phone": "9800000000",
  "role": "DRIVER"
}
```

Login body:

```json
{
  "email": "john@example.com",
  "password": "Password@123"
}
```

Refresh body:

```json
{
  "refreshToken": "refresh-token"
}
```

Logout body:

```json
{
  "refreshToken": "refresh-token"
}
```

### Users

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| GET | `/api/users/myDetail` | Authenticated | Get current user profile |
| PUT | `/api/users/update/profile` | Authenticated | Update name, phone, or password |

Profile update body:

```json
{
  "name": "Updated Name",
  "phone": "9811111111",
  "currentPassword": "OldPassword@123",
  "newPassword": "NewPassword@123"
}
```

`currentPassword` is required only when changing password.

### Parking

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| GET | `/api/parking?area=thamel&available=true` | Authenticated | List parking locations with optional area and availability filters |
| GET | `/api/parking/nearby?lat=27.7172&lng=85.3240&limit=5` | Authenticated | Find nearby parking using Thamel road-distance logic |
| GET | `/api/parking/nearest?lat=27.7172&lng=85.3240` | Authenticated | Find the nearest parking location |
| GET | `/api/parking/nearby-gps?latitude=27.7172&longitude=85.3240&maxSpots=20` | Authenticated | Find closest parking by GPS distance |
| GET | `/api/parking/thamel/closest?latitude=27.7172&longitude=85.3240&maxSpots=5` | Authenticated | Find closest available parking in Thamel |
| GET | `/api/parking/thamel/nearest?latitude=27.7172&longitude=85.3240` | Authenticated | Find nearest available parking in Thamel |
| GET | `/api/parking/mine` | VENDOR | List current vendor's parking locations |
| POST | `/api/parking` | VENDOR | Create a parking location |
| PUT | `/api/parking/{id}` | VENDOR | Update owned parking location details |
| PATCH | `/api/parking/{id}/slots` | VENDOR | Update available slot counts |
| DELETE | `/api/parking/{id}` | VENDOR | Soft-delete owned parking location |

Create parking body:

```json
{
  "name": "Thamel Parking A",
  "address": "Thamel, Kathmandu",
  "latitude": 27.7172,
  "longitude": 85.3240,
  "totalFourWheelerSlots": 10,
  "totalTwoWheelerSlots": 20,
  "twoWheelerRatePerHour": 50,
  "fourWheelerRatePerHour": 100
}
```

Update slots body:

```json
{
  "availableFourWheelerSlots": 8,
  "availableTwoWheelerSlots": 15
}
```

### Bookings

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| POST | `/api/bookings` | DRIVER | Create a booking |
| GET | `/api/bookings/me` | Authenticated | Get current user's bookings |
| GET | `/api/bookings/{id}` | Authenticated | Get a booking if the current user is allowed to view it |
| PUT | `/api/bookings/{bookingId}/cancel` | DRIVER | Cancel a driver's booking |
| GET | `/api/bookings/debug-user` | Public | Legacy debug endpoint |

Booking body:

```json
{
  "parkingLocationId": "parking-location-uuid",
  "slotId": "selected-slot-uuid",
  "vehicleType": "FOUR_WHEELER",
  "startTime": "2026-05-21T10:00:00",
  "endTime": "2026-05-21T12:00:00"
}
```

Supported vehicle types:

- `TWO_WHEELER`
- `FOUR_WHEELER`

Booking behavior:

- Only drivers can create bookings.
- The request must include the selected `slotId`.
- The selected slot must belong to the parking location, match the requested vehicle type, and be available.
- The system reserves the selected slot.
- Available slot counts are decremented after booking.
- Minimum billable duration is 1 hour.
- Booking amount uses the location's vehicle-specific hourly rate.
- Cancelling releases the reserved slot.
- Refund is marked pending only when there is a successful payment and the booking starts more than 1 hour later.

### Payments

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| POST | `/api/payments/khalti/initiate` | Authenticated | Initiate Khalti payment for a booking |
| GET | `/api/payments/khalti/verify?pidx=...` | Public | Verify Khalti payment callback |

Payment initiation body:

```json
{
  "bookingId": "booking-uuid",
  "paymentMethod": "KHALTI"
}
```

The response includes `paymentUrl` and `pidx`. Redirect the user to `paymentUrl` to complete the payment.

### Vendor

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| GET | `/api/vendors/dashboard` | VENDOR | Vendor summary with parking locations and slot counts |
| PUT | `/api/vendors/bookings/{bookingId}/status` | VENDOR | Check in or complete a booking for the vendor's parking location |

Vendor booking status body:

```json
{
  "action": "CHECK_IN"
}
```

Supported actions:

- `CHECK_IN`: changes the assigned slot from `RESERVED` to `OCCUPIED`
- `COMPLETE`: changes the booking to `COMPLETED`, changes the slot from `OCCUPIED` to `AVAILABLE`, and restores the available slot count

### Admin

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| GET | `/api/admin/dashboard` | ADMIN | Platform totals for bookings, vendors, drivers, and admins |
| GET | `/api/admin/bookings` | ADMIN | List all bookings |
| GET | `/api/admin/users` | ADMIN | List all users |
| GET | `/api/admin/users?role=DRIVER` | ADMIN | List users by role |

Accepted role filters:

- `ADMIN`
- `VENDOR`
- `DRIVER`

## Role Access Summary

| Role | Main Permissions |
| --- | --- |
| DRIVER | Search parking, create bookings, view own bookings, cancel own bookings, initiate payments |
| VENDOR | Manage own parking locations, update available slots, view vendor dashboard, view related bookings by ID, check in and complete bookings |
| ADMIN | View dashboard stats, list users, list bookings, view bookings |

## Useful cURL Examples

Login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"driver@parking.com","password":"Driver@123"}'
```

List parking:

```bash
curl http://localhost:8080/api/parking \
  -H "Authorization: Bearer <accessToken>"
```

Find nearby parking:

```bash
curl "http://localhost:8080/api/parking/nearby?lat=27.7172&lng=85.3240&limit=5" \
  -H "Authorization: Bearer <accessToken>"
```

Create booking:

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "parkingLocationId": "parking-location-uuid",
    "slotId": "selected-slot-uuid",
    "vehicleType": "TWO_WHEELER",
    "startTime": "2026-05-21T10:00:00",
    "endTime": "2026-05-21T12:00:00"
  }'
```

## Development Notes

- Hibernate uses `ddl-auto: update`, so tables are created or updated automatically in development.
- PostgreSQL is the configured database dialect.
- CORS is enabled for:
  - `http://localhost:3000`
  - `http://localhost:5173`
  - `http://localhost:4200`
- Soft-deleted rows are excluded by repository/service queries that check `deleted_at`.
- The app loads `.env` through Dotenv before Spring Boot starts.

## Important Production Checklist

- Replace all development passwords and seeded credentials.
- Move Khalti secret key out of `application.yml`.
- Use a strong `JWT_SECRET`.
- Set explicit production database credentials.
- Disable or restrict SQL logging.
- Review CORS origins for production frontend domains.
- Consider Flyway or Liquibase for controlled database migrations.
