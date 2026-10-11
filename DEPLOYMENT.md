# Deploying Convertify

Convertify is a single Spring Boot application: the REST API and the static frontend are served by the same executable jar, on one port, under one domain. It has no database and needs no other services. The only outbound call is to a public exchange-rate API.

## Requirements

| | |
|---|---|
| Java | **25** (LTS), matching `<java.version>` in `backend/pom.xml` |
| Maven | Not required. The wrapper (`backend/mvnw`) downloads Maven 3.9.9 |
| Docker | Required for Render (it has no native Java runtime). Optional locally |
| Accounts | A GitHub repository plus a Render or Railway account |

> **Important:** build from the **repository root**. The Maven build packages the sibling `frontend/` folder into the jar, so building `backend/` on its own (or with a Docker context of `backend/`) produces a jar without the UI.

## Environment variables

**None are required.** The app starts with sensible defaults. All of these are optional:

| Variable | Required | Purpose |
|---|---|---|
| `PORT` | Set by the platform | HTTP port. Render and Railway inject it; locally it defaults to 8090 (8080 in the Docker image) |
| `SPRING_PROFILES_ACTIVE` | No | Set to `prod,container` by the Docker image. Locally it defaults to `dev` |
| `CURRENCY_API_KEY` | No | Only for a keyed exchange-rate provider. Treat it as a secret |
| `CURRENCY_PROVIDER_URL` | No | Rate API URL template, default `https://open.er-api.com/v6/latest/{base}` |
| `CURRENCY_API_KEY_HEADER` | No | Send the key in this header instead of in the URL |
| `CURRENCY_CACHE_TTL` | No | How long rates are cached (prod default `12h`) |
| `DATE_DEFAULT_TIME_ZONE` | No | Time zone that defines "today" (default `Asia/Kolkata`) |
| `LAND_DEFAULT_REGION` | No | Region preselected in the Land card |
| `LOG_LEVEL_APP` | No | `INFO` (prod default) or `DEBUG` |
| `CORS_ALLOWED_ORIGINS` | No | Only if a frontend is hosted on a *different* origin. Leave unset when deployed as one app |

See [.env.example](.env.example) for the full list.

## What the production container does

- **Profiles:** runs with `SPRING_PROFILES_ACTIVE=prod,container`:
  - `prod`: plain INFO logs, 12 h rate cache, `X-Forwarded-*` headers honoured behind the platform's proxy, graceful shutdown.
  - `container`: logs go **only to stdout** (the platform collects them). No log files are written, because container disks are ephemeral.
- **User and memory:** runs as a non-root user. The heap is sized from the container memory limit (`-XX:MaxRAMPercentage=75`), and the JVM exits on out-of-memory so the platform restarts it.
- **Health check:** `GET /actuator/health` returns `{"status":"UP"}` (liveness at `/actuator/health/liveness`, readiness at `/actuator/health/readiness`). No other actuator endpoints are exposed.
- **Storage:** nothing is stored on disk. Exchange rates are cached in memory and simply refetched after a restart.

## Local deployment

```bash
cd backend
./mvnw clean package
java -jar target/convertify.jar
```

Open http://localhost:8090. To run with production settings:

```bash
SPRING_PROFILES_ACTIVE=prod PORT=8080 java -jar target/convertify.jar
```

On Windows use `mvnw.cmd`. If you cloned on Windows and `./mvnw` is "permission denied" on Linux or macOS, run `chmod +x backend/mvnw` (or commit it with `git update-index --chmod=+x backend/mvnw`).

## Docker deployment

Run from the repository root:

```bash
docker build -t convertify .
docker run --rm -p 8080:8080 convertify
```

Open http://localhost:8080 and check http://localhost:8080/actuator/health. Pass settings with `-e`, for example `-e LOG_LEVEL_APP=DEBUG`, or `--env-file .env`.

## Render deployment (Docker)

Render has no native Java runtime, so Convertify deploys from its `Dockerfile`. The repository includes a [`render.yaml`](render.yaml) Blueprint.

1. Push the repository to GitHub.
2. In Render: **New → Blueprint**, connect GitHub, and select the repository. Render reads `render.yaml`.
   - Or: **New → Web Service → Build and deploy from a Git repository**, with **Language: Docker**, **Dockerfile path** `./Dockerfile`, and **Docker context** `.` (the repository root).
3. **Environment variables:** none are needed. Only set `CURRENCY_API_KEY` if you switch to a keyed provider. Render injects `PORT` itself.
4. **Health check path:** `/actuator/health`. The Blueprint already sets this.
5. **Deploy.** The first build downloads the Java 25 images and Maven dependencies, which takes a few minutes.
6. **Verify:** open `https://<service>.onrender.com/`, run a conversion, and check `/actuator/health`.

Render's free instances sleep after inactivity, so the first request after idling takes a little longer while the JVM starts.

## Railway deployment (Docker)

Railway detects the root `Dockerfile` automatically. [`railway.json`](railway.json) pins the Dockerfile builder and the health check.

1. Push the repository to GitHub.
2. In Railway: **New Project → Deploy from GitHub repo**, and select the repository.
3. **Variables:** none are needed. Railway injects `PORT`. Add `CURRENCY_API_KEY` only if required.
4. **Health check:** `/actuator/health` comes from `railway.json`.
5. **Networking → Generate Domain** to get a public URL.
6. **Verify** the same way as on Render.

## Verifying a deployment

| Check | Expected |
|---|---|
| `GET /` | The Convertify page, with styles, scripts and images loading |
| `GET /actuator/health` | `{"groups":["liveness","readiness"],"status":"UP"}` |
| `GET /api/v1/info` | App name and version |
| Convert 180 cm → m in the UI | `1.8 m` |
| Platform logs | Banner, then `Convertify started successfully in … seconds` and one line per API request, each with a request ID |

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Page loads without styling, or 404 on `/` | The jar was built without `frontend/`. Build from the repository root |
| Health check fails, app killed on startup | Allow a longer health-check grace period. JVM startup on small instances can take 10–20 s |
| Currency card shows "rates temporarily unavailable" | The exchange-rate API is unreachable from the platform. The other calculators keep working |
| `Unable to establish loopback connection` (Windows only) | JDK temp-socket issue with short 8.3 `TEMP` paths. See the README troubleshooting note. It doesn't affect Linux containers |
