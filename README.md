# Smart Parking System Backend

This project is the backend implementation of a Smart Parking System built using Spring Boot.  
It provides REST APIs for managing parking slots, vehicle entry and exit, and parking records.

## Technologies Used

- Java
- Spring Boot
- Spring Data JPA
- MySQL/PostgreSQL
- Maven
- Git & GitHub

## Project Structure

```
src/main/java/com/projectwork/Smart/Parking/System/
├── controller/     Handles REST API requests
├── service/        Contains business logic
├── repository/     Handles database operations
├── entity/         Defines database tables
└── dto/            Transfers data between layers
    ├── request/    Incoming request payloads
    └── response/   Outgoing response payloads
```

## Features

- User registration and authentication (JWT)
- Role-based access control (DRIVER, VENDOR, ADMIN)
- View and search nearby parking locations
- Book a parking slot
- Parking fee calculation
- Khalti payment gateway integration
- Vendor dashboard for managing parking locations
- Admin dashboard for system overview

---

## Common Response Wrapper

All endpoints return a unified `ApiResponse<T>` envelope:

```json
{
  "responseCode": 200,
  "responseMessage": "Success",
  "timestamp": "2024-01-15T10:30:00",
  "data": { }
}
```

---

## Authentication

Most endpoints require a JWT Bearer token obtained from `/api/auth/login`.

**Header format:**
```
Authorization: Bearer <jwt_token>
```

---

## API Endpoints

---

### Health — `/api/health`

#### `GET /api/health`
Check if the service is running. No authentication required.

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Service is running",
  "timestamp": "2024-01-15T10:00:00",
  "data": {
    "status": "UP",
    "service": "Smart Parking System",
    "timestamp": "2024-01-15T10:00:00"
  }
}
```

---

### Auth — `/api/auth`

#### `POST /api/auth/register`
Register a new user account.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "secret123",
  "phone": "9800000000",
  "role": "DRIVER"
}
```
> `role` must be one of: `DRIVER`, `VENDOR`, `ADMIN`

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "User registered successfully",
  "timestamp": "2024-01-15T10:00:00",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "userId": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "DRIVER"
  }
}
```

---

#### `POST /api/auth/login`
Authenticate and receive a JWT token.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "secret123"
}
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Login successful",
  "timestamp": "2024-01-15T10:05:00",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "userId": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "DRIVER"
  }
}
```

---

### Parking — `/api/parking`
> Requires: `Authorization: Bearer <token>`

#### `GET /api/parking/areas/thamel-nearby`
Find the closest parking spots near a given coordinate in Thamel.

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**

| Parameter  | Type   | Required | Default | Description               |
|------------|--------|----------|---------|---------------------------|
| latitude   | double | Yes      | —       | User's current latitude   |
| longitude  | double | Yes      | —       | User's current longitude  |
| maxSpots   | int    | No       | 5       | Maximum results to return |

**Example Request:**
```
GET /api/parking/areas/thamel-nearby?latitude=27.7172&longitude=85.3240&maxSpots=3
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Nearby parking spots fetched",
  "timestamp": "2024-01-15T10:10:00",
  "data": [
    {
      "id": 1,
      "name": "Thamel Parking A",
      "address": "Thamel, Kathmandu",
      "latitude": 27.7180,
      "longitude": 85.3245,
      "availableSlots": 10,
      "distance": 0.12,
      "vendorName": "Vendor One"
    }
  ]
}
```

---

#### `GET /api/parking/areas/thamel/nearest`
Find the single nearest parking spot.

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**

| Parameter | Type   | Required | Description              |
|-----------|--------|----------|--------------------------|
| latitude  | double | Yes      | User's current latitude  |
| longitude | double | Yes      | User's current longitude |

**Example Request:**
```
GET /api/parking/areas/thamel/nearest?latitude=27.7172&longitude=85.3240
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Nearest parking spot found",
  "timestamp": "2024-01-15T10:12:00",
  "data": {
    "id": 2,
    "name": "Thamel Parking B",
    "address": "Thamel Marg, Kathmandu",
    "latitude": 27.7175,
    "longitude": 85.3242,
    "availableSlots": 5,
    "distance": 0.04,
    "vendorName": "Vendor Two"
  }
}
```

---

#### `GET /api/parking/areas/thamel/available-slots`
Get all parking locations with available slots in Thamel.

**Headers:**
```
Authorization: Bearer <token>
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Available parking slots fetched",
  "timestamp": "2024-01-15T10:15:00",
  "data": [
    {
      "id": 1,
      "name": "Thamel Parking A",
      "address": "Thamel, Kathmandu",
      "latitude": 27.7180,
      "longitude": 85.3245,
      "availableSlots": 10,
      "distance": 0.0,
      "vendorName": "Vendor One"
    },
    {
      "id": 3,
      "name": "Thamel Parking C",
      "address": "Chhetrapati, Kathmandu",
      "latitude": 27.7165,
      "longitude": 85.3233,
      "availableSlots": 3,
      "distance": 0.0,
      "vendorName": "Vendor Three"
    }
  ]
}
```

---

### Bookings — `/api/bookings`
> Requires: `Authorization: Bearer <token>` (DRIVER role)

#### `POST /api/bookings/create`
Create a new parking booking.

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "parkingLocationId": 1,
  "startTime": "2024-01-16T09:00:00",
  "endTime": "2024-01-16T11:00:00"
}
```
> Both `startTime` and `endTime` must be in the future.

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Booking created successfully",
  "timestamp": "2024-01-15T10:20:00",
  "data": {
    "bookingId": 42,
    "parkingName": "Thamel Parking A",
    "status": "CONFIRMED",
    "startTime": "2024-01-16T09:00:00",
    "endTime": "2024-01-16T11:00:00",
    "totalAmount": 150.0,
    "message": "Slot booked successfully"
  }
}
```

---

#### `GET /api/bookings/mybookings`
Retrieve all bookings for the currently authenticated user.

**Headers:**
```
Authorization: Bearer <token>
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Bookings fetched successfully",
  "timestamp": "2024-01-15T10:25:00",
  "data": [
    {
      "bookingId": 42,
      "parkingName": "Thamel Parking A",
      "status": "CONFIRMED",
      "startTime": "2024-01-16T09:00:00",
      "endTime": "2024-01-16T11:00:00",
      "totalAmount": 150.0,
      "message": null
    }
  ]
}
```

---

### Payment — `/api/payment`

#### `POST /api/payment/khalti/initiate`
Initiate a payment for a booking.

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "bookingId": 42,
  "paymentMethod": "ESEWA"
}
```
> `paymentMethod` must be one of: `ESEWA`, `CASH`

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Payment initiated",
  "timestamp": "2024-01-15T10:30:00",
  "data": {
    "paymentId": 7,
    "bookingId": 42,
    "amount": 150.0,
    "status": "PENDING",
    "transactionId": "TXN20240115103000",
    "PaymentUrl": "https://khalti.com/payment/...",
    "paidAt": null,
    "message": "Redirect user to PaymentUrl to complete payment",
    "pidx": "abc123xyz"
  }
}
```

---

#### `GET /api/payment/khalti/verify`
Verify a Khalti payment after the user completes payment on the gateway.

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**

| Parameter | Type   | Required | Description                |
|-----------|--------|----------|----------------------------|
| pidx      | String | Yes      | Payment index from Khalti  |

**Example Request:**
```
GET /api/payment/khalti/verify?pidx=abc123xyz
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Payment verified successfully",
  "timestamp": "2024-01-15T10:35:00",
  "data": {
    "paymentId": 7,
    "bookingId": 42,
    "amount": 150.0,
    "status": "SUCCESS",
    "transactionId": "TXN20240115103000",
    "PaymentUrl": null,
    "paidAt": "2024-01-15T10:34:00",
    "message": "Payment completed",
    "pidx": "abc123xyz"
  }
}
```

---

### Vendor — `/api/vendors`
> Requires: `Authorization: Bearer <token>` (VENDOR role)

#### `POST /api/vendors/addparking`
Add a new parking location under the authenticated vendor.

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Thamel Parking D",
  "address": "Paknajol, Thamel, Kathmandu",
  "latitude": 27.7190,
  "longitude": 85.3250,
  "totalSlots": 20,
  "availableSlots": 20
}
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Parking location added successfully",
  "timestamp": "2024-01-15T10:40:00",
  "data": {
    "id": 5,
    "name": "Thamel Parking D",
    "address": "Paknajol, Thamel, Kathmandu",
    "latitude": 27.7190,
    "longitude": 85.3250,
    "availableSlots": 20,
    "distance": 0.0,
    "vendorName": "John Vendor"
  }
}
```

---

#### `GET /api/vendors/view/parking-locations`
Get all parking locations owned by the authenticated vendor.

**Headers:**
```
Authorization: Bearer <token>
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Parking locations fetched",
  "timestamp": "2024-01-15T10:45:00",
  "data": [
    {
      "id": 5,
      "name": "Thamel Parking D",
      "address": "Paknajol, Thamel, Kathmandu",
      "latitude": 27.7190,
      "longitude": 85.3250,
      "availableSlots": 18,
      "distance": 0.0,
      "vendorName": "John Vendor"
    }
  ]
}
```

---

#### `PUT /api/vendors/parking-locations/{id}/available-slots`
Update the available slot count for a specific parking location.

**Headers:**
```
Authorization: Bearer <token>
```

**Path Variables:**

| Variable | Type | Description           |
|----------|------|-----------------------|
| id       | Long | Parking location ID   |

**Query Parameters:**

| Parameter       | Type    | Required | Description                    |
|-----------------|---------|----------|--------------------------------|
| availableSlots  | Integer | No       | New available slot count       |
| newAvailableSlots | Integer | No     | Legacy alias for availableSlots |

**Example Request:**
```
PUT /api/vendors/parking-locations/5/available-slots?availableSlots=15
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Available slots updated",
  "timestamp": "2024-01-15T10:50:00",
  "data": {
    "id": 5,
    "availableSlots": 15,
    "message": "Slots updated successfully"
  }
}
```

---

#### `GET /api/vendors/dashboard`
Get dashboard statistics for the authenticated vendor.

**Headers:**
```
Authorization: Bearer <token>
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Dashboard data fetched",
  "timestamp": "2024-01-15T11:00:00",
  "data": {
    "totalParkingLocations": 2,
    "totalSlots": 40,
    "availableSlots": 25,
    "occupiedSlots": 15,
    "twoWheelerSlots": {
      "total": 20,
      "available": 13,
      "occupied": 7
    },
    "fourWheelerSlots": {
      "total": 20,
      "available": 12,
      "occupied": 8
    },
    "locations": [
      {
        "id": 5,
        "name": "Thamel Parking D",
        "totalSlots": 20,
        "availableSlots": 15,
        "occupiedSlots": 5,
        "twoWheelerSlots": {
          "total": 10,
          "available": 8,
          "occupied": 2
        },
        "fourWheelerSlots": {
          "total": 10,
          "available": 7,
          "occupied": 3
        }
      }
    ]
  }
}
```

---

### Admin — `/api/admin`
> Requires: `Authorization: Bearer <token>` (ADMIN role)

#### `GET /api/admin/bookings`
Get all bookings across the system.

**Headers:**
```
Authorization: Bearer <token>
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "All bookings fetched",
  "timestamp": "2024-01-15T11:10:00",
  "data": [
    {
      "id": 42,
      "startTime": "2024-01-16T09:00:00",
      "endTime": "2024-01-16T11:00:00",
      "status": "CONFIRMED",
      "totalAmount": 150.0
    }
  ]
}
```

---

#### `GET /api/admin/vendors`
Get all users with the VENDOR role.

**Headers:**
```
Authorization: Bearer <token>
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Vendors fetched",
  "timestamp": "2024-01-15T11:15:00",
  "data": [
    {
      "id": 3,
      "name": "John Vendor",
      "email": "vendor@example.com",
      "phone": "9811111111",
      "role": "VENDOR"
    }
  ]
}
```

---

#### `GET /api/admin/drivers`
Get all users with the DRIVER role.

**Headers:**
```
Authorization: Bearer <token>
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Drivers fetched",
  "timestamp": "2024-01-15T11:20:00",
  "data": [
    {
      "id": 1,
      "name": "John Doe",
      "email": "john@example.com",
      "phone": "9800000000",
      "role": "DRIVER"
    }
  ]
}
```

---

#### `GET /api/admin/dashboard`
Get system-wide statistics for the admin.

**Headers:**
```
Authorization: Bearer <token>
```

**Response `200`:**
```json
{
  "responseCode": 200,
  "responseMessage": "Dashboard data fetched",
  "timestamp": "2024-01-15T11:25:00",
  "data": {
    "totalBookings": 120,
    "totalVendors": 8,
    "totalDrivers": 95
  }
}
```

---

## Setup Instructions

1. Clone the repository

```bash
git clone https://github.com/Ajita-920/smart-parking-system-backend.git
```

2. Open project in IntelliJ IDEA

3. Configure database in `src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smart_parking
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
```

4. Run the application

```bash
./mvnw spring-boot:run
```

---

## Author

Ajita Shrestha  
Backend Developer (Java & Spring Boot)
