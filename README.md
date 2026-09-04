# OFOOD Backend

This is the Spring Boot backend for the OFOOD application.

## Development Setup

### 1. Prerequisites
- Java 21
- PostgreSQL (e.g. running via Docker)
- Maven

### 2. Environment Configuration

The application is fully configurable via standard Spring Boot environment variables.

### JWT Keys Configuration

For local development, generate and configure RSA keys as files:
```bash
OFOOD_JWT_PRIVATE_KEY_PATH=file:/path/to/dev-private.pem
OFOOD_JWT_PUBLIC_KEY_PATH=file:/path/to/dev-public.pem
```

**AWS ECS / Production Configuration:**
For secure production deployments, you can inject the private key directly as a PEM string via AWS Secrets Manager:
```bash
OFOOD_JWT_PRIVATE_KEY_PEM="-----BEGIN PRIVATE KEY-----\nMIIEvQIBAD...\n-----END PRIVATE KEY-----"
```
When `OFOOD_JWT_PRIVATE_KEY_PEM` is provided, the backend will dynamically parse it and derive the public key automatically.
- Production private keys must **never** be committed to Git.
- Production private keys must **never** be baked into the Docker image.
- AWS ECS should inject the value securely from AWS Secrets Manager.
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

## Render Deployment (Web Service)
The application is pre-configured to be deployed natively on Render as a Web Service, or using Docker.
Render provides several environment variables automatically, such as `PORT`.

1. Ensure your Render Web Service starts with standard Maven commands or by deploying the Dockerfile.
2. In your Render Dashboard Environment variables, provide:
   - `SPRING_PROFILES_ACTIVE=prod` (or any appropriate profile)
   - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` (often from a Render managed database)
   - `OFOOD_FRONTEND_URL` (e.g., your deployed frontend URL)
   - `OFOOD_JWT_PRIVATE_KEY_PATH` (must point to the absolute path of the uploaded secret file, e.g., `/etc/secrets/jwt-private.pem`)
3. **Secret Files**: Upload your `dev-private.pem` (or production key) as a "Secret File" in Render rather than pasting its contents into an environment variable directly. The path Render provides is what you'll use for `OFOOD_JWT_PRIVATE_KEY_PATH`.

### Docker Deployment
You can build and run this application using Docker:
```bash
docker build -t ofood-backend .
docker run -p 8080:8080 -e PORT=8080 -e SPRING_PROFILES_ACTIVE=dev -e DB_PASSWORD=your_db_password -e OFOOD_JWT_PRIVATE_KEY_PATH=/app/keys/dev-private.pem -v /absolute/path/to/keys:/app/keys ofood-backend
```
