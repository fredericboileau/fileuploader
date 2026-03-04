# API
This is an app in java that provided file uploading and sharing functionalities
It uses thymeleaf for server side rendered views.
**HTTP endpoints** (`FileUploadController`):
- `GET /` — lists uploaded files, renders `uploadForm.html`
- `GET /files/{filename}` — serves a file as a download attachment
- `POST /` — accepts `multipart/form-data` upload, redirects to `/`
- POST /share - accepts a list of files with a user to share the files with
- POST /files/delete - accepts a list of files the user can delete, files are scoped by user

# Architecture
The app uses keycloak for authentification, PostgreSQL to store information about the users, the files
and the shared files. The app uses a controller and a storageService which handles all interactions with
the filesystem and the database.

# Infrastructure :
Uses docker-compose
- App on `:8080`, Keycloak on `:8180`, PostgreSQL on `:5432`
- Keycloak realm: `filevault`; client secret via `KEYCLOAK_CLIENT_SECRET` env var
- App depends on both DB health check and a `wait-for-keycloak` curl probe before starting

