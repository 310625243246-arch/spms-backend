# SPMS Backend — Setup & Run Guide

Spring Boot backend for the Smart Procurement Management System (SIH26032).

> **Note on this codebase's origin:** it was written and statically reviewed in a
> sandboxed environment with no internet access and no Maven/MySQL installed, so it
> could not be compiled or run there. Every file was reviewed by hand for import
> correctness, method signatures, and Spring Data query derivation, but you should
> still expect to fix small issues on first real build — follow the "If something
> doesn't compile" section at the end if that happens.

## 1. Install Java 21

Check if you already have it:
```bash
java -version
```
If not, install a JDK 21 (e.g. from [Adoptium](https://adoptium.net/)) and make sure `JAVA_HOME` points to it.

## 2. Install Maven

Check:
```bash
mvn -version
```
If missing, install from [maven.apache.org](https://maven.apache.org/install.html) or via your package manager (`brew install maven`, `sudo apt install maven`, etc.).

## 3. Install MySQL 8

Install MySQL 8 (e.g. from [dev.mysql.com](https://dev.mysql.com/downloads/mysql/) or via `brew install mysql` / `sudo apt install mysql-server`), then start the server.

## 4. Create the database

```bash
mysql -u root -p
```
```sql
CREATE DATABASE IF NOT EXISTS spms_db;
```
(You can skip this — `application.properties` has `createDatabaseIfNotExist=true` — but creating it explicitly avoids permission surprises.)

## 5. Configure your password

Open `src/main/resources/application.properties` and set your MySQL password:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

## 6. Run the backend

```bash
cd spms-backend
mvn spring-boot:run
```
On first run, Hibernate creates all tables (`ddl-auto=update`) and `DataSeeder` populates demo data automatically. Watch the console for:
```
Demo data seeded successfully.
Login credentials -> Admin: admin@spms.gov.in / admin123 | Officer: officer@spms.gov.in / officer123 | Farmer: farmer@spms.gov.in / farmer123
```
The API is now live at `http://localhost:8080`.

## 7. Run the frontend

```bash
cd spms-frontend
npm install
npm run dev
```
Open `http://localhost:5173`.

## 8. Test login

Use the demo credentials below on the matching login page. On the Farmer login screen, either the email `farmer@spms.gov.in` or the Farmer ID `FRM-20481` works in the same field.

| Role | Identifier | Password |
|---|---|---|
| Admin | `admin@spms.gov.in` | `admin123` |
| Officer | `officer@spms.gov.in` (assigned to Center 2) | `officer123` |
| Officer 2 | `officer2@spms.gov.in` (assigned to Center 1) | `officer123` |
| Farmer | `farmer@spms.gov.in` or `FRM-20481` | `farmer123` |
| Farmer 2 | `farmer2@spms.gov.in` or `FRM-20482` | `farmer123` |
| Farmer 3 | `farmer3@spms.gov.in` or `FRM-20483` | `farmer123` |

## 9. Test the APIs

With the backend running, try:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"farmer@spms.gov.in","password":"farmer123"}'
```
Copy the returned `token`, then:
```bash
curl http://localhost:8080/api/farmer/dashboard \
  -H "Authorization: Bearer <token>"
```
See `API_DOCUMENTATION.md` for every endpoint, request/response shape, and status code.

---

## What demo data is seeded

- 1 Admin, 2 Officers (each assigned to a different center), 3 Farmers
- 3 Procurement Centers, 3 Crops
- 3 bookings for today at Center 2 (one being weighed, two waiting) + their tokens
- 1 completed historical booking with a payment record, for the first farmer
- A couple of notifications for the first farmer

## If something doesn't compile

This is the most likely first-run friction point since the code was never machine-compiled before you run it. Common fixes:

- **Lombok not applying** (`getX()`/`setX()`/builder "cannot find symbol"): make sure annotation processing is enabled in your IDE (IntelliJ: Settings → Build → Compiler → Annotation Processors → Enable). `mvn spring-boot:run` from the terminal handles this automatically.
- **MySQL connection refused**: confirm MySQL is running (`mysql.server start` / `sudo service mysql start`) and the port/credentials in `application.properties` match your local setup.
- **Port 8080 already in use**: change `server.port` in `application.properties`, and update `API_BASE_URL` in `spms-frontend/src/services/api.ts` to match.
- **CORS errors in the browser**: confirm the frontend is running on exactly `http://localhost:5173` (the origin allowed in `SecurityConfig`/`application.properties`), or update `spms.cors.allowed-origin`.

If you hit a genuine compilation error, paste it back and it can be fixed directly — the code has not been machine-verified end-to-end.
