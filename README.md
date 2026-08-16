# OFOOD Backend

This is the Spring Boot backend for the OFOOD application.

## Development Setup

### 1. Prerequisites
- Java 21
- PostgreSQL (e.g. running via Docker)
- Maven

### 2. Environment Configuration
The application requires certain environment variables to start, particularly the RSA private key for JWT signing.
**WARNING: Never commit private keys or secrets to version control!**

1. Copy `.env.example` to `.env`
2. Generate an RSA key pair if you haven't already and point `OFOOD_JWT_PRIVATE_KEY_PATH` to the absolute path of your `.pem` file.
3. Fill in the required `DB_PASSWORD` in your environment.

### 3. Running PostgreSQL
If you use Docker, you can start a local PostgreSQL 15 instance:
```bash
docker run --name ofood-postgres -e POSTGRES_USER=ofood -e POSTGRES_PASSWORD=ofood_local -e POSTGRES_DB=ofood -p 5432:5432 -d postgres:15-alpine
```

### 4. Running the Application
Activate the `dev` profile to use safe local defaults (e.g., connecting to `localhost:5432` for DB).
Provide the variables via your IDE or terminal.
```bash
export OFOOD_JWT_PRIVATE_KEY_PATH=/path/to/dev-private.pem
export DB_PASSWORD=ofood_local
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5. Running Tests
The tests use Testcontainers to automatically spin up a temporary PostgreSQL instance.
```bash
mvn clean test
```

## API Documentation
When running locally, Swagger UI and OpenAPI documentation are available at:
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **JWKS Endpoint**: http://localhost:8080/.well-known/jwks.json
- **OpenID Config**: http://localhost:8080/.well-known/openid-configuration

## Frontend Integration
The API uses CORS and HttpOnly cookies for refresh token rotation.
- **Expected Frontend Origin**: `http://localhost:5173` (Customizable via `OFOOD_FRONTEND_URL`)
- **Important**: To correctly receive and send the `HttpOnly` refresh token cookie, the frontend application must include credentials in its requests.
  - Using `fetch`: `{ credentials: "include" }`
  - Using `axios`: `{ withCredentials: true }`
- **Cookies and SameSite**: For standard local development where frontend and backend are on `localhost` (even if ports differ), `SameSite=Lax` and `Secure=false` are safe and work correctly in modern browsers. If you deploy the frontend and backend on genuinely different cross-origin domains, you must override the environment variables:
  - `OFOOD_REFRESH_TOKEN_SAME_SITE=None`
  - `OFOOD_REFRESH_TOKEN_SECURE=true` (Requires HTTPS)
