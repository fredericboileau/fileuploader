# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
mvn spring-boot:run

# Build (produces target/videouploader-0.0.1-SNAPSHOT.jar)
mvn package

# Run the fat jar directly
java -jar target/videouploader-0.0.1-SNAPSHOT.jar
```

There are no tests in this project.

## Architecture

This is a Spring Boot 4.0.3 file upload application (`com.clevertricks` package). All source lives in `src/main/java/com/clevertricks/`.

**Storage abstraction:** `StorageService` is the central interface with two implementations:
- `FileSystemStorageService` — the active `@Service` bean; stores files on disk under the `upload-dir/` directory.
- `PostgresqlStorageService` — incomplete stub that reads DB connection properties but does **not** implement `StorageService` yet.

**Startup behavior:** `MyApplication` runs `storageService.deleteAll()` then `storageService.init()` on every startup, wiping all previously uploaded files.

**Configuration:** `StorageProperties` (`@ConfigurationProperties("storage")`) controls:
- `storage.location` — upload directory (default: `upload-dir`)
- `storage.host`, `storage.port`, `storage.db` — PostgreSQL connection (unused by active service; defaults: `localhost:5432/fileUploader`)

**HTTP endpoints** (`FileUploadController`):
- `GET /files` — lists uploaded files, renders `uploadForm.html`
- `GET /files/{filename}` — serves a file as a download attachment
- `POST /` — accepts a `multipart/form-data` file upload, redirects to `/files`

**Views:** Two nearly identical Thymeleaf templates in `src/main/resources/templates/` — `uploadForm.html` (shows file list + upload form) and `homePage.html` (upload form only, no list).
