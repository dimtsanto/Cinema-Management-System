
## ⚙️ IntelliJ IDEA Setup (Σημαντικό!)

### 1. Ενεργοποίηση Annotation Processing
Αυτό είναι **υποχρεωτικό** για να δουλέψει το Lombok:

`File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors`
→ ✅ **Enable annotation processing** (τσεκάρε το checkbox)

### 2. Java SDK
Χρησιμοποίησε Java **17** ή **21**:

`File → Project Structure → Project → SDK`

### 3. Reload Maven
`Maven panel (δεξιά) → 🔄 Reload All Maven Projects`

### 4. Αν εμφανιστεί `TypeTag::UNKNOWN` error
Αυτό φτιάχνεται με τα παραπάνω βήματα. Αν συνεχίσει:
- `File → Invalidate Caches → Invalidate and Restart`
- Μετά redo τα βήματα 1-3

# Cinema Management System — Backend API

Developed for the course **321-4002 Software Engineering**  
Framework: **Java 17 + Spring Boot 3.2**

---

## Project Structure

```
cinema-management/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/gr/aegean/cinema/
    │   │   ├── CinemaManagementApplication.java   ← Entry point
    │   │   ├── controller/
    │   │   │   ├── AuthController.java
    │   │   │   ├── ProgramController.java
    │   │   │   └── ScreeningController.java
    │   │   ├── dto/
    │   │   │   ├── AuthDTO.java
    │   │   │   ├── ProgramDTO.java
    │   │   │   └── ScreeningDTO.java
    │   │   ├── exception/
    │   │   │   ├── BadRequestException.java
    │   │   │   ├── ConflictException.java
    │   │   │   ├── ForbiddenException.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   └── ResourceNotFoundException.java
    │   │   ├── model/
    │   │   │   ├── entity/
    │   │   │   │   ├── Program.java
    │   │   │   │   ├── Screening.java
    │   │   │   │   ├── User.java
    │   │   │   │   └── UserProgramRole.java
    │   │   │   └── enums/
    │   │   │       ├── ProgramRole.java
    │   │   │       ├── ProgramState.java
    │   │   │       └── ScreeningState.java
    │   │   ├── repository/
    │   │   │   ├── ProgramRepository.java
    │   │   │   ├── ScreeningRepository.java
    │   │   │   ├── UserProgramRoleRepository.java
    │   │   │   └── UserRepository.java
    │   │   ├── security/
    │   │   │   ├── JwtAuthFilter.java
    │   │   │   ├── JwtUtils.java
    │   │   │   ├── SecurityConfig.java
    │   │   │   └── UserDetailsServiceImpl.java
    │   │   └── service/
    │   │       ├── AuthService.java
    │   │       ├── ProgramService.java
    │   │       └── ScreeningService.java
    │   └── resources/
    │       ├── application.properties
    │       └── schema.sql
    └── test/
        ├── java/gr/aegean/cinema/service/
        │   ├── ProgramServiceTest.java
        │   └── ScreeningServiceTest.java
        └── resources/
            └── application.properties
```

---

## Prerequisites

| Software | Version |
|----------|---------|
| Java JDK | 17+     |
| Maven    | 3.8+    |
| MySQL    | 8.0+    |

---

## Setup & Run

### 1. Create the database
```sql
-- Run schema.sql in MySQL:
mysql -u root -p < src/main/resources/schema.sql
```

### 2. Configure application.properties
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cinema_db?...
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD
app.jwt.secret=YourSecretKeyAtLeast256BitsLong
```

### 3. Build & Run
```bash
# Compile and package
mvn clean package -DskipTests

# Run
java -jar target/cinema-management-1.0.0.jar

# Or run directly with Maven
mvn spring-boot:run
```

### 4. Run Tests
```bash
mvn test
```

---

## API Endpoints Summary

### Authentication
| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login → get JWT token |

> **For all authenticated requests:** add header `Authorization: Bearer <token>`

### Programs
| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/programs` | USER | Create program |
| PUT | `/api/programs/{id}` | PROGRAMMER | Update program |
| GET | `/api/programs/search` | Optional | Search programs |
| GET | `/api/programs/{id}` | Optional | View program |
| DELETE | `/api/programs/{id}` | PROGRAMMER | Delete program |
| PATCH | `/api/programs/{id}/state` | PROGRAMMER | Change state |
| POST | `/api/programs/{id}/programmers` | PROGRAMMER | Add programmer |
| POST | `/api/programs/{id}/staff` | PROGRAMMER | Add staff |

### Screenings
| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/screenings` | USER | Create screening |
| PUT | `/api/screenings/{id}` | SUBMITTER | Update screening |
| PATCH | `/api/screenings/{id}/submit` | SUBMITTER | Submit screening |
| DELETE | `/api/screenings/{id}` | SUBMITTER | Withdraw screening |
| PATCH | `/api/screenings/{id}/handler` | PROGRAMMER | Assign handler |
| PATCH | `/api/screenings/{id}/review` | STAFF | Review screening |
| PATCH | `/api/screenings/{id}/approve` | PROGRAMMER | Approve screening |
| PATCH | `/api/screenings/{id}/reject` | PROGRAMMER | Reject screening |
| PATCH | `/api/screenings/{id}/final-submit` | SUBMITTER | Final submission |
| PATCH | `/api/screenings/{id}/accept` | PROGRAMMER | Accept to schedule |
| GET | `/api/screenings/search?programId=X` | Optional | Search screenings |
| GET | `/api/screenings/{id}` | Optional | View screening |

---

## Program State Machine

```
CREATED → SUBMISSION → ASSIGNMENT → REVIEW → SCHEDULING → FINAL_PUBLICATION → DECISION → ANNOUNCED
```

## Screening State Machine

```
CREATED → SUBMITTED → REVIEWED → APPROVED → SCHEDULED (final)
                                          ↘ REJECTED  (final)
                      ↓ (at SCHEDULING)
                   REJECTED (final)
```
