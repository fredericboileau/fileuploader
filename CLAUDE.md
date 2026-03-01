# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run full stack (app + PostgreSQL + Keycloak)
docker compose up --build

# Run app locally (no auth, filesystem storage)
mvn spring-boot:run

# Build fat jar (produces target/videouploader-0.0.1-SNAPSHOT.jar)
mvn package -DskipTests

# Run the fat jar
java -jar target/videouploader-0.0.1-SNAPSHOT.jar

# Provision Keycloak realm (run after Keycloak is up on :8180)
bash keycloak/setup-realm.sh
```

There are no tests in this project.

## Architecture

Spring Boot 4.0.3 file upload application (`com.clevertricks` package). All source lives in `src/main/java/com/clevertricks/`.

**Storage abstraction:** `StorageService` is the central interface with three implementations:
- `FileSystemStorageService` — stores files on disk under `upload-dir/`; default active bean for local dev
- `PostgresqlStorageService` — stores file metadata in PostgreSQL, files on disk
- `PostgresqlFileSystemStorageService` — hybrid: persists metadata (path, filename, size) in a `files` table, stores bytes on disk

Active backend is selected via `STORAGE_TYPE` env var (`filesystem` or `postgresql`). Docker Compose sets `STORAGE_TYPE=postgresql`.

**Startup behavior:** `MyApplication` runs `storageService.deleteAll()` then `storageService.init()` on every startup, wiping all previously uploaded files.

**Security:** `SecurityConfig` enforces OAuth2/OIDC login via Keycloak. All routes require authentication. Logout redirects to Keycloak's end-session endpoint. The OIDC endpoints are all configurable via env vars (`KEYCLOAK_AUTHORIZATION_URI`, `KEYCLOAK_TOKEN_URI`, `KEYCLOAK_USERINFO_URI`, `KEYCLOAK_JWK_URI`, `KEYCLOAK_END_SESSION_URI`).

**HTTP endpoints** (`FileUploadController`):
- `GET /` — lists uploaded files, renders `uploadForm.html`
- `GET /files/{filename}` — serves a file as a download attachment
- `POST /` — accepts `multipart/form-data` upload, redirects to `/`

**Infrastructure (docker-compose):**
- App on `:8080`, Keycloak on `:8180`, PostgreSQL on `:5432`
- Keycloak realm: `filevault`; client secret via `KEYCLOAK_CLIENT_SECRET` env var
- App depends on both DB health check and a `wait-for-keycloak` curl probe before starting

**Configuration (`StorageProperties` / `StorageConfig`):**
- `storage.location` / `STORAGE_LOCATION` — upload directory (default: `upload-dir`)
- `STORAGE_HOST`, `STORAGE_PORT`, `STORAGE_DB`, `STORAGE_USERNAME`, `STORAGE_PASSWORD` — PostgreSQL connection
