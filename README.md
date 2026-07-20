# Currency Converter API

A full-stack currency conversion application built with **Spring Boot**, **MongoDB**, and a **Node.js/Express** web client, fully containerized with **Docker Compose**.

Built as part of the Service Oriented Computing coursework (IT41073), demonstrating a layered REST API architecture, database-backed API key security, and a decoupled proxy-based web client.

---

## Features

- **Currency conversion** between USD, LKR, EUR, GBP, and INR, with results persisted to MongoDB
- **Conversion history** — view all past conversions, or filter by currency
- **Large-transaction warning check** — flags unusually large or small transaction amounts
- **API key authentication** — protected endpoints require a valid `X-API-KEY` header, validated against a MongoDB-backed key store
- **Web client** — a simple HTML/JS front end served via a Node.js/Express proxy, so the browser never needs direct access to the API key or CORS configuration
- **Fully Dockerized** — one command spins up MongoDB, the API, and the web client together

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend API | Java 21, Spring Boot 3.x, Spring Web, Spring Data MongoDB |
| Database | MongoDB 7 |
| Web Client | Node.js, Express |
| Auth | Custom MongoDB-backed API key validation |
| Containerization | Docker, Docker Compose |
| Testing | Postman |

How it fits together: the browser talks only to the Node.js/Express client (port 3000), which proxies requests to the Spring Boot API (port 8084) and attaches the `X-API-KEY` header on the browser's behalf. The Spring Boot API talks to MongoDB (port 27017) through a standard Controller → Service → Repository layered structure.

---

## Project Structure

```
currencyconverter/
├── docker-compose.yml
├── Dockerfile                          # Spring Boot backend image
├── pom.xml
├── mvnw / mvnw.cmd
├── HELP.md
├── src/main/java/com/example/demo/
│   ├── DemoApplication.java
│   ├── controller/
│   │   └── CurrencyController.java
│   ├── service/
│   │   └── CurrencyService.java
│   ├── repository/
│   │   ├── CurrencyRepository.java
│   │   └── ApiKeyRepository.java
│   ├── model/
│   │   ├── CurrencyLog.java
│   │   └── ApiKey.java
│   └── exception/
│       └── UnauthorizedException.java
├── src/main/resources/
│   └── application.properties
└── UI/                                 # Node.js/Express web client
    ├── Dockerfile
    ├── package.json
    ├── server.js
    └── index.html
```

The Spring Boot backend sits at the **project root**. The Node.js client lives in the **`UI/`** subfolder.

---

## API Endpoints

| Method | Endpoint | Auth required | Description |
|---|---|---|---|
| POST | `/api/currencies/convert?amount=&from=&to=` | ✅ `X-API-KEY` | Converts an amount between currencies and saves the record |
| GET | `/api/currencies/history` | ✅ `X-API-KEY` | Returns all saved conversion records |
| GET | `/api/currencies/warning-check?amount=&currency=` | ❌ | Returns a warning message for unusually large/small amounts |
| GET | `/api/currencies/history/filter?currency=` | ❌ | Returns conversion records filtered by input currency |

**Supported currency codes:** `USD`, `LKR`, `EUR`, `GBP`, `INR`

---

## Running with Docker (recommended)

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop) installed and running

### Start everything

From the project root (where `docker-compose.yml` lives):

```bash
docker compose up --build
```

This builds and starts three containers:
- `mongodb` — MongoDB 7, exposed on port `27017`
- `springboot-api` — the backend (built from the project root `Dockerfile`), exposed on port `8084`
- `node-client` — the web client (built from `UI/Dockerfile`), exposed on port `3000`

### `docker-compose.yml` reference

```yaml
services:
  mongodb:
    image: mongo:7
    container_name: mongodb
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db

  springboot-api:
    build: .
    container_name: springboot-api
    ports:
      - "8084:8084"
    depends_on:
      - mongodb

  node-client:
    build: ./UI
    container_name: node-client
    ports:
      - "3000:3000"
    depends_on:
      - springboot-api

volumes:
  mongo-data:
```

### Access the app

- Web client: [http://localhost:3000](http://localhost:3000)
- API base URL: `http://localhost:8084/api/currencies`
- MongoDB (via Compass): `mongodb://localhost:27017`

### Stop everything

```bash
docker compose down
```

Add `-v` to also wipe the MongoDB data volume and start fresh next time:

```bash
docker compose down -v
```

---

## Running Locally (without Docker)

### Prerequisites
- Java 21
- Maven
- Node.js (LTS)
- MongoDB running locally on port `27017`

### 1. Configure the backend

In `src/main/resources/application.properties`:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/currency_db
spring.data.mongodb.database=currency_db
server.port=8084
```

### 2. Seed API keys in MongoDB

Using MongoDB Compass, insert into a `currency_db.api_keys` collection:

```json
[
  { "keyValue": "SUPER-SECRET-DEV-KEY-123", "clientName": "Frontend-Web-App", "active": true },
  { "keyValue": "EXPIRED-HACKER-KEY-999", "clientName": "Suspicious-Client", "active": false }
]
```

### 3. Run the backend

From the project root:

```bash
./mvnw spring-boot:run
```

### 4. Run the web client

```bash
cd UI
npm install
node server.js
```

### 5. Open the app

Visit [http://localhost:3000](http://localhost:3000)

---

## Testing with Postman

**Convert (requires API key):**
```
POST http://localhost:8084/api/currencies/convert?amount=100&from=USD&to=LKR
Header: X-API-KEY: SUPER-SECRET-DEV-KEY-123
```

**View history (requires API key):**
```
GET http://localhost:8084/api/currencies/history
Header: X-API-KEY: SUPER-SECRET-DEV-KEY-123
```

**Warning check (no key needed):**
```
GET http://localhost:8084/api/currencies/warning-check?amount=15000&currency=USD
```

**Filter history by currency (no key needed):**
```
GET http://localhost:8084/api/currencies/history/filter?currency=usd
```

**Security test cases:**

| Scenario | Header sent | Expected result |
|---|---|---|
| No API key | *(none)* | `401 Unauthorized` |
| Invalid/inactive key | `X-API-KEY: EXPIRED-HACKER-KEY-999` | `401 Unauthorized` |
| Valid key | `X-API-KEY: SUPER-SECRET-DEV-KEY-123` | `200 OK` |

---

## Data Model

**CurrencyLog** (collection: `currency_logs`)

| Field | Type | Description |
|---|---|---|
| id | String | Auto-generated MongoDB ID |
| inputAmount | double | Original amount entered |
| inputCurrency | String | Source currency code |
| outputAmount | double | Converted amount |
| outputCurrency | String | Target currency code |
| timestamp | String | When the conversion occurred |

**ApiKey** (collection: `api_keys`)

| Field | Type | Description |
|---|---|---|
| id | String | Auto-generated MongoDB ID |
| keyValue | String | The actual API key string |
| clientName | String | Descriptive client/application name |
| active | boolean | Whether the key is currently valid |

---

## Known Limitations / Future Improvements

- Exchange rates are currently **fixed values** hardcoded in `CurrencyService`, not pulled from a live rates API
- No pagination on the `/history` endpoint
- API key is currently hardcoded in the Node.js client rather than sourced from an environment variable
- No automated test suite (Postman manual testing only)

---

## Author

Coursework project for IT41073: Service Oriented Computing
