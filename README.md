# Smart Parking System Backend

This project is the backend implementation of a Smart Parking System built using Spring Boot.  
It provides REST APIs for managing parking slots, vehicle entry and exit, and parking records.

## Technologies Used

- Java
- Spring Boot
- Spring Data JPA
- MySQL/PostgresSQL
- Maven
- Git & GitHub

## Project Structure

src/main/java/com/smartparking

controller  
Handles REST API requests.

service  
Contains business logic.

repository  
Handles database operations.

entity  
Defines database tables.

dto  
Transfers data between layers.

## Features

- View available parking slots
- Book a parking slot
- Vehicle entry and exit management
- Parking history tracking
- Parking fee calculation

## API Endpoints

### Parking Slots

GET /api/slots  
Get all parking slots

GET /api/slots/available  
Get available slots

POST /api/slots  
Create parking slot

### Parking Records

POST /api/entry  
Vehicle entry

POST /api/exit  
Vehicle exit

GET /api/history  
Parking history

## Setup Instructions

1. Clone the repository

git clone https://github.com/Ajita-920/smart-parking-system-backend.git

2. Open project in IntelliJ IDEA

3. Configure database in application.properties

4. Run the application

## Author

Ajita Shrestha

Backend Developer (Java & Spring Boot)