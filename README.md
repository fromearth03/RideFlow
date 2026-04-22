# RideFlow 🚗💨

RideFlow is a robust, production-ready ride management backend built with **Spring Boot 3+** and **Java 21**. It provides a scalable architecture for managing rides, drivers, vehicles, and dispatchers with a focus on security and efficiency.

---

## 🚀 Key Features

*   **JWT-based Authentication**: Secure, stateless authentication for all users.
*   **Role-Based Access Control (RBAC)**: Dedicated roles for `CUSTOMER`, `DRIVER`, `DISPATCHER`, and `ADMIN`.
*   **Ride Lifecycle Management**: Complete handling from `PENDING` to `ASSIGNED`, `IN_PROGRESS`, and `COMPLETED`/`CANCELLED`.
*   **Driver & Vehicle Tracking**: Real-time availability management and maintenance logging.
*   **Automated Migrations**: Database schema versioning with Flyway.
*   **Global Error Handling**: Centralized exception management for consistent API responses.

## 🛠️ Tech Stack

*   **Framework**: [Spring Boot](https://spring.io/projects/spring-boot)
*   **Language**: Java 21
*   **Database**: PostgreSQL
*   **Security**: Spring Security + JWT
*   **Migrations**: Flyway
*   **Utilities**: Lombok, Maven

---

## 🚦 Getting Started

### Prerequisites

*   **Java 21** or higher
*   **Maven 3.8+**
*   **PostgreSQL** instance

### Installation & Setup

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/fromearth03/RideFlow.git
    cd RideFlow
    ```

2.  **Configure Database**:
    Update `src/main/resources/application.properties` with your PostgreSQL credentials:
    ```properties
    spring.datasource.url=jdbc:postgresql://your-host:5432/rideflow
    spring.datasource.username=your-username
    spring.datasource.password=your-password
    ```

3.  **JWT Secret**:
    Ensure `jwt.secret` is configured in `application.properties`. Use a strong, Base64-encoded key.

### Running the Application

Use the Maven Wrapper to start the server:
```bash
./mvnw spring-boot:run
```
---

## 📁 Project Structure

```text
com.cwtw.rideflow
├── config/         # Security & Password configurations
├── controller/     # REST Endpoints (Auth, Ride, Driver, Admin, etc.)
├── dto/            # Data Transfer Objects
├── exception/      # Custom exceptions and Global Handler
├── model/          # JPA Entities
├── repository/     # Spring Data JPA Repositories
├── security/       # JWT Logic & UserDetailsService
└── service/        # Business Logic Layer
```

---

## 📖 Documentation

For a deep dive into every API endpoint, database schema, and security flow, please refer to the:
👉 [**RideFlow Complete Technical Documentation**](RIDEFLOW_DOCS.md)

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
