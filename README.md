# Nexora — Backend API

**Java 21 · Spring Boot 4 · Spring Security · MySQL · REST APIs**

Nexora is a digital-banking **application prototype** with a Spring Boot backend and a separate React Native client. The backend models account operations, authentication, transaction history, and virtual cards, with API endpoints intended for integration with the mobile application.

**Project scope:** This is a software-engineering demonstration, **not** a licensed banking service, a payment processor, or evidence of a production banking deployment. Money movements operate within the application's own database model.

[Engineering case study](https://www.syedharis.pro/work/nexora) · [Mobile client repository](https://github.com/SyedHarisM/nexora-mobile-terminal) · [Portfolio](https://www.syedharis.pro)

---

## Implemented capabilities

| Area | Source-backed functionality |
| --- | --- |
| Authentication | User registration/login, JWT authentication filter, role-based endpoint rules, refresh-token flow |
| Account operations | Authenticated account summary, account-number lookup, deposit, withdrawal, and transfers within the application |
| Transaction history | Account history returned through paginated API endpoints |
| Virtual cards | Authenticated card issuance, masked card details, and an explicit card-detail reveal endpoint |
| Recovery | One-time-code password-reset flow with expiry validation and configured email delivery |
| Data access | Spring Data JPA repositories with MySQL and Hibernate |
| Error handling | Centralized exception handling and structured error responses |

The account service uses transactional methods, and the repository includes tests focused on transfer atomicity, card access control, one-time-code expiry, and JWT secret configuration. **The presence of tests is not a claim that they have been run or that every security scenario is covered.**

## Architecture

```text
React Native / Expo client
           |
           v
Spring Boot REST controllers
           |
      Spring Security
      JWT authentication
      Role-based access
           |
     Service layer
  /        |         \
Account  User/Auth   Virtual Card
  |        |         |
  +--------+---------+
           |
    Spring Data JPA
           |
         MySQL
```

Representative API areas (see controllers for request and response contracts):

- `/api/users/**` — registration, login, profile, and password recovery
- `/api/auth/refresh` — refresh-token exchange
- `/api/accounts/**` — account details, lookup, transfers, deposits, withdrawals, transaction history
- `/api/v1/cards/**` — virtual-card issuance and authenticated card access
- `/api/admin/**` — administrative endpoints protected by role rules

## Technology

| Layer | Implementation |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.6, Spring MVC |
| Security | Spring Security, BCrypt, JJWT |
| Persistence | Spring Data JPA, Hibernate, MySQL |
| Email | Spring Boot Mail |
| Build | Maven / Maven Wrapper |
| Test infrastructure | JUnit/Spring Boot tests, H2 in test scope |

**PostgreSQL and Gradle are not used by the current repository.** This documentation follows the actual build configuration instead of listing prospective technologies.

## Local development

### Requirements

- JDK 21
- A local MySQL server
- Maven Wrapper (included)
- SMTP configuration if testing recovery emails

Create a MySQL database and configure the following environment variables before starting:

| Environment variable | Purpose |
| --- | --- |
| `DB_HOST` | MySQL hostname |
| `DB_NAME` | Database name |
| `DB_USER` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Random JWT signing secret (minimum 32 bytes) |
| `MAIL_USERNAME` | SMTP account name |
| `MAIL_PASSWORD` | SMTP account password |
| `JWT_ACCESS_EXPIRATION_MS` | Optional access-token expiry override |
| `OTP_EXPIRATION_MS` | Optional recovery-code expiry override |

The current `application.properties` reads connection details and secrets from the environment. Do **not** commit real credentials or signing keys.

From the repository root:

```bash
# macOS / Linux
./mvnw test
./mvnw spring-boot:run
```

```powershell
# Windows PowerShell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

A working local run requires valid database and relevant mail/JWT configuration. Test configuration uses an H2 database instead of a live MySQL instance.

## Engineering limitations

- This repository is **not** a deployed banking platform. Treat account balances and transfers as application-state demonstrations.
- The default access-token lifetime in configuration is long for a sensitive application and should be shortened before a real deployment.
- User-uploaded files, account enumeration surfaces, email delivery, operational logs, and token handling require deployment-specific review.
- Do not infer compliance, banking-grade encryption, external financial integrations, uptime, or security certification from this demonstration.
- Deployment, load testing, and end-to-end integration with external banking systems have not been verified here.

## Related links

- [Nexora engineering case study](https://www.syedharis.pro/work/nexora)
- [Nexora mobile application](https://github.com/SyedHarisM/nexora-mobile-terminal)
- [GitHub profile](https://github.com/SyedHarisM)
- [Contact](https://www.syedharis.pro/contact)
