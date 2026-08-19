# Librio — Academic Library Resource Discovery & Availability Platform

Librio is an academic library resource discovery platform for readers and librarians. It guides a reader through browse/search, resource detail, and server-derived physical/digital availability.

---

## 📋 Prerequisites

- **Java**: JDK 17+
- **Build Tool**: Apache Maven 3.8+
- **Node.js**: Node 18+ & npm 9+
- **Database**: PostgreSQL 14+ (or local Docker container / local instance)

---

## 🛠️ Quick Start Guide (Clean Clone Setup)

### 1. Database Setup (PostgreSQL)

Ensure PostgreSQL is running locally on default port `5432`:

```sql
-- Connect to PostgreSQL and create database
CREATE DATABASE librio;
```

*Default connection credentials (can be overridden via environment variables):*
- **Host**: `localhost:5432`
- **Database**: `librio`
- **Username**: `postgres`
- **Password**: `postgres`

### 2. Backend Startup (Spring Boot)

Navigate to the `backend/` directory and start the application:

```powershell
cd backend
$env:JAVA_HOME="C:\Users\Admin\.jdks\ms-17.0.18"
$env:MAVEN_OPTS="-Dfile.encoding=UTF-8"
mvn spring-boot:run
```

*Or pass explicit database credentials if different:*
```powershell
mvn spring-boot:run -Dspring-boot.run.arguments="--SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/librio --SPRING_DATASOURCE_USERNAME=postgres --SPRING_DATASOURCE_PASSWORD=yourpassword"
```

The Spring Boot backend will initialize schema and seed 4 test cases automatically on startup, running on **`http://localhost:8080`**.

### 3. Frontend Startup (React + Vite)

In a separate terminal, navigate to `frontend/` and start the development server:

```powershell
cd frontend
npm install
npm run dev
```

Open your browser at **`http://localhost:5173`**.

---

## 🧪 Verification & Test Commands

### Backend Tests
```powershell
cd backend
mvn clean test
```

### Frontend Lint & Build
```powershell
cd frontend
npm run lint
npm run build
```

---

## 🔌 API Demo Path (`curl` Commands)

Once backend is running on `http://localhost:8080`:

1. **Health Check**:
   ```bash
   curl http://localhost:8080/health
   ```
2. **Browse Catalog**:
   ```bash
   curl http://localhost:8080/resources
   ```
3. **Keyword Search (Found Result)**:
   ```bash
   curl "http://localhost:8080/resources?q=Refactoring"
   ```
4. **Keyword Search (Empty Result)**:
   ```bash
   curl "http://localhost:8080/resources?q=does-not-exist"
   ```
5. **Resource Detail (Physical Available & Digital Available)**:
   ```bash
   curl http://localhost:8080/resources/1
   ```
6. **Resource Detail (Physical Out of Stock)**:
   ```bash
   curl http://localhost:8080/resources/2
   ```
7. **Resource Detail (Digital Only)**:
   ```bash
   curl http://localhost:8080/resources/3
   ```
8. **Resource Detail (404 Not Found)**:
   ```bash
   curl http://localhost:8080/resources/999999
   ```

---

## 📊 Fixed Seed Data Reference

| Resource ID | Title | Authors | Access Types | Stock / Availability |
| :--- | :--- | :--- | :--- | :--- |
| **1** | Clean Code | Robert C. Martin | `PHYSICAL`, `DIGITAL` | 2 / 5 copies available, Digital Available |
| **2** | Refactoring | Martin Fowler | `PHYSICAL` | 0 / 3 copies available (Out of stock) |
| **3** | Designing Data-Intensive Applications | Martin Kleppmann | `DIGITAL` | Digital Available |
| **4** | Structure and Interpretation of Computer Programs | Harold Abelson, Gerald Jay Sussman | `PHYSICAL`, `DIGITAL` | 1 / 2 copies available, Digital Available |
