# Convertify

**Convert. Calculate. Simplify.**

Convertify puts everyday conversion and calculation tools on one clean page: currency, height, distance, speed, travel time, land area (including regional Indian units) and dates. A Spring Boot backend does the math and a lightweight frontend with no build step presents it. Both ship as **one executable jar**: no database, no other services.

**Live Demo:** [convertify-x4bu.onrender.com](https://convertify-x4bu.onrender.com/) (free hosting: the first visit after a quiet spell can take up to a minute while the server wakes up)

---

## Features

| Tool | What it does |
|---|---|
| **Currency** | Live exchange rates, `BigDecimal` arithmetic, rounding to each currency's minor units, a TTL cache, stale-rate fallback and a "last updated" timestamp |
| **Height** | mm, cm, m, km, in, ft, yd, mi, plus a "show in all units" view |
| **Distance** | Everything in Height plus nautical miles, plus a "show in all units" view |
| **Speed** | m/s, km/h, mph, ft/s, knots |
| **Time · Distance · Speed** | Solves for any one of the three. Inputs are normalized to m, m/s and s first, so any combination of units works. Time results read like "6 hours 15 minutes" |
| **Land Area** | Standard metric and imperial area units, plus **region-specific** Cent, Ground, Guntha, Bigha, Katha, Decimal, Kanal, Marla, Biswa and more, each shown with its conversion basis |
| **Date Calculator** | Add or subtract days, weeks, months, years or **business days** (configurable weekends and holiday calendars). Also calculates the difference between two dates in days, weeks and a calendar period |
| **Unit Converter** | Any catalog category: weight, temperature (offset-aware), volume, time, and so on |

UX: light and dark themes (the choice is saved), responsive from desktop down to phone, live recalculation after the first result, copy-to-clipboard, clear loading and error states, keyboard-accessible tabs and controls, and a visible focus ring everywhere.

## Technology Stack

- **Backend:** Java 25 (LTS), Spring Boot 4.1.1, Spring Web MVC, Bean Validation, Maven (wrapper included), SLF4J/Logback, Spring Boot Actuator (health only)
- **Frontend:** semantic HTML, modern CSS (custom properties, `color-mix`), vanilla ES modules. No framework, no bundler, zero runtime dependencies
- **Persistence:** none needed. All reference data is configuration, so there is no repository layer
- **Deployment:** Docker image (JRE 25, non-root) that runs unchanged on Render and Railway. See [DEPLOYMENT.md](DEPLOYMENT.md)

## Architecture

```
Browser (ES modules)
   │  JSON over /api/v1
   ▼
Controllers ──► Services ──► UnitConversionEngine ──► UnitRegistry ◄── catalog/units.yml
                  │                                        ▲
                  │                        LandAreaService ─┘ (+ catalog/land-regions.yml)
                  ├──► ExchangeRateCache ──► ExchangeRateProvider (interface)
                  │                              └── ExternalExchangeRateProvider (HTTP)
                  └──► WorkingCalendarService ◄── catalog/date-calendars.yml
```

**One conversion algorithm.** Each unit is defined once as `base = value × factor + offset`. Every conversion follows **input → normalize to base unit → convert to target → display rounding**. Height, Distance and Speed are *presets* (curated unit lists) over the same generic engine. Land regions add their traditional units to the standard AREA category. Temperature works through `offset`. Adding a category or unit means editing YAML only.

**Replaceable currency provider.** Business logic depends only on `ExchangeRateProvider`. To use a different source, implement the interface as a bean and select it with `convertify.currency.provider.type`. `ExchangeRateCache` keeps rates for a configurable TTL. If a refresh fails, it keeps serving the previous rates with `stale: true` and backs off before retrying. If it has no rates at all, it returns `503 EXCHANGE_RATE_UNAVAILABLE`.

**Precision.** All arithmetic uses `BigDecimal` with 34 significant digits. Money is rounded to the target currency's minor units with `HALF_EVEN`. For display, magnitudes ≥ 1 get up to 6 decimals and magnitudes < 1 get 6 significant digits, so you see `1.8`, never `1.8000000000000003`. Numbers travel as decimal strings in JSON so the browser can't introduce float noise.

**Errors.** `GlobalExceptionHandler` (`@RestControllerAdvice`) maps every failure to one error shape with a stable code. Users get safe messages; technical detail goes to the logs, and those logs never contain API keys or provider URLs.

## Project Structure

```
convertify/
├── backend/
│   ├── pom.xml                       # also packages ../frontend into the jar as /static
│   ├── mvnw, mvnw.cmd, .mvn/         # Maven wrapper (Maven 3.9.9)
│   └── src/main/
│       ├── java/com/convertify/
│       │   ├── config/               # @ConfigurationProperties records, CORS, clock
│       │   ├── controller/           # REST endpoints (/api/v1/...)
│       │   ├── converter/            # UnitRegistry, UnitConversionEngine, UnitDefinitionFactory
│       │   ├── dto/                  # request/response records
│       │   ├── exception/            # ErrorCode, custom exceptions, GlobalExceptionHandler
│       │   ├── logging/              # request-ID filter, startup summary, level colour converter
│       │   ├── model/                # UnitDefinition, UnitCategory, WorkingCalendar, ...
│       │   ├── provider/currency/    # ExchangeRateProvider + ExternalExchangeRateProvider
│       │   ├── service/              # business logic per calculator
│       │   └── util/                 # DecimalExpressions, NumberDisplayFormatter, InputLimits, DurationFormatter
│       └── resources/
│           ├── application.yml       # shared settings (env-var driven)
│           ├── application-dev.yml   # debug logs, serves ../frontend live, local CORS
│           ├── application-test.yml
│           ├── application-prod.yml  # INFO logging, caching headers, graceful shutdown
│           ├── logback-spring.xml    # console + rolling file + error log appenders
│           ├── banner.txt            # startup banner
│           └── catalog/
│               ├── units.yml         # unit categories + presets
│               ├── land-regions.yml  # regional land units with conversion basis
│               └── date-calendars.yml# weekend presets + holiday calendars
├── frontend/
│   ├── index.html
│   ├── config.js                     # runtime config (API base URL, timeouts)
│   ├── css/styles.css                # design tokens, light/dark themes, components
│   └── js/
│       ├── main.js  api.js  dom.js  format.js  theme.js
│       ├── components/               # result panel, calculator binding, all-units disclosure
│       └── cards/                    # currency, unit (height/distance/speed/general), travel, land, date
├── Dockerfile                        # multi-stage build → JRE 25 runtime, non-root, stdout logging
├── .dockerignore
├── render.yaml                       # Render Blueprint (Docker runtime, health check)
├── railway.json                      # Railway config (Dockerfile builder, health check)
├── .env.example                      # every optional variable, no values
├── DEPLOYMENT.md
└── README.md
```

## Configuration

All tunables live in `application.yml` under `convertify.*`. Business data lives in `catalog/*.yml`. Nothing configurable is hard-coded in Java.

```yaml
convertify:
  conversion:
    calculation-precision: 34
    max-decimal-places: 6
    significant-digits: 6
    max-absolute-value: 1000000000000000
    max-input-decimal-places: 20
  currency:
    base-currency: ${CURRENCY_BASE:USD}
    provider:
      url: ${CURRENCY_PROVIDER_URL:https://open.er-api.com/v6/latest/{base}}
      api-key: ${CURRENCY_API_KEY:}
      cache-ttl: ${CURRENCY_CACHE_TTL:6h}
  date:
    default-time-zone: ${DATE_DEFAULT_TIME_ZONE:Asia/Kolkata}
  land:
    default-region: ${LAND_DEFAULT_REGION:WEST_BENGAL}
```

Profiles: `dev` (the default), `test` and `prod`, plus `container`, which switches logging to stdout only. The Docker image runs `prod,container`. Select profiles with `SPRING_PROFILES_ACTIVE`.

## Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` (image: `prod,container`) | `dev`, `test`, `prod`; add `container` for stdout-only logs |
| `PORT` | `8090` (image: `8080`) | HTTP port. Render/Railway set it automatically. `SERVER_PORT` also works |
| `CURRENCY_PROVIDER_URL` | `https://open.er-api.com/v6/latest/{base}` | Rates URL template (`{base}`, `{apiKey}` placeholders) |
| `CURRENCY_API_KEY` | *(empty)* | Provider key, if required. Never logged |
| `CURRENCY_API_KEY_HEADER` | *(empty)* | Send the key in this header instead of the URL |
| `CURRENCY_PROVIDER_NAME` | `ExchangeRate-API (open access)` | Attribution shown to users |
| `CURRENCY_PROVIDER_TYPE` | `external` | Which `ExchangeRateProvider` bean to use |
| `CURRENCY_BASE` | `USD` | Base currency fetched from the provider |
| `CURRENCY_CACHE_TTL` | `6h` (dev `1h`, prod `12h`) | How long rates are reused |
| `DATE_DEFAULT_TIME_ZONE` | `Asia/Kolkata` | Defines "today" |
| `LAND_DEFAULT_REGION` | `WEST_BENGAL` | Region preselected in the Land card |
| `CORS_ALLOWED_ORIGINS` | *(dev: localhost:5173)* | Comma-separated origins for a separately hosted frontend |
| `FRONTEND_DIR` | `../frontend` | dev profile only: where live frontend files are served from |
| `LOG_LEVEL_APP` | `DEBUG` (dev) / `INFO` | Log level for `com.convertify` |
| `LOG_LEVEL_ROOT` | `INFO` | Log level for everything else |
| `LOG_DIR` | `logs` | Log file directory (relative to the working directory). Unused under `container` |
| `LOG_MAX_FILE_SIZE` | `20MB` | Size at which the current day's log rolls into a new part |
| `LOG_MAX_HISTORY` / `LOG_ERROR_MAX_HISTORY` | `30` / `90` | Days of application / error logs kept |
| `LOG_TOTAL_SIZE_CAP` / `LOG_ERROR_TOTAL_SIZE_CAP` | `1GB` / `256MB` | Hard disk ceiling for archived logs |
| `LOG_SLOW_REQUEST_THRESHOLD` | `1s` | API requests slower than this are logged at WARN |
| `LOG_ANSI` | `always` (dev) | Console colours in dev: `always`, `detect` or `never` |

## How to Run

Prerequisites: **JDK 25**. Maven is optional because the wrapper downloads it. The frontend needs no toolchain.

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Open **http://localhost:8090**. In the `dev` profile the backend serves `../frontend` straight from disk, so frontend edits show up on refresh.

**Frontend on its own origin (optional):** set `apiBaseUrl: 'http://localhost:8090'` in `frontend/config.js`, then:

```bash
python -m http.server 5173 -d frontend
```

The `dev` profile already allows CORS from `localhost:5173`.

> **Windows troubleshooting:** if startup fails with `Unable to establish loopback connection`, the JDK can't create its internal socket in `%TEMP%`. This happens with short 8.3 paths such as `C:\Users\ABCDEF~1\...`. Point it at a plain directory:
> `mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=C:/temp"`

## Production Setup

```bash
cd backend
./mvnw clean package                    # produces target/convertify.jar with the frontend bundled
SPRING_PROFILES_ACTIVE=prod PORT=8080 java -jar target/convertify.jar
```

In `prod` the app logs plain INFO-level text to the console and to rolling files under `logs/` (see [Logging](#logging)). Add the `container` profile for stdout-only logging. It also sends 1-hour cache headers on static files, honours `X-Forwarded-*` headers behind a proxy, and shuts down gracefully. Put secrets such as `CURRENCY_API_KEY` in the platform's secret store.

### Docker

```bash
docker build -t convertify .            # from the repository root
docker run --rm -p 8080:8080 convertify
```

### Render / Railway

Both deploy the same `Dockerfile` (Render has no native Java runtime). Connect the GitHub repository and the included `render.yaml` / `railway.json` configure the rest, including the `/actuator/health` health check. No environment variables are required. Step-by-step instructions are in **[DEPLOYMENT.md](DEPLOYMENT.md)**.

## Logging

Logging uses SLF4J with Logback (`backend/src/main/resources/logback-spring.xml`). Levels are set in `application*.yml` and can be overridden without code changes, for example `LOG_LEVEL_APP=DEBUG` or `--logging.level.com.convertify=DEBUG`.

| Output | Contents |
|---|---|
| Console (dev) | Colour by level: ERROR red, WARN yellow, INFO green, DEBUG cyan, TRACE grey |
| Console (prod) | Plain text, INFO and above, no ANSI codes |
| `logs/convertify.log` | Everything. Rolls daily and at 20 MB into `logs/archive/convertify.YYYY-MM-DD.N.log.gz`. Keeps 30 days, capped at 1 GB |
| `logs/convertify-error.log` | ERROR events only. Keeps 90 days, capped at 256 MB |

Each line follows one format: timestamp, level, thread, request ID, logger, message. For example:

```
2026-09-29 12:41:43.136 INFO  [nio-8090-exec-2] [cur-1   ] c.convertify.service.CurrencyService   : Currency conversion requested: USD -> INR
2026-09-29 12:41:43.663 INFO  [nio-8090-exec-2] [cur-1   ] c.c.p.c.ExternalExchangeRateProvider   : Fetched 166 exchange rates from ExchangeRate-API (open access) (base USD) in 526 ms
2026-09-29 12:41:43.683 INFO  [nio-8090-exec-2] [cur-1   ] c.c.logging.RequestCorrelationFilter   : POST /api/v1/currency/convert completed with 200 in 553 ms
```

- **Request IDs:** send `X-Request-ID` (letters, digits and `._:-`, at most 64 characters) and it is used for every log line of that request. Otherwise one is generated. It is returned in the `X-Request-ID` response header and in the `requestId` field of error responses, so you can grep for the exact failing request.
- **Request log:** each API call logs its method, path, status, outcome (completed, rejected or failed) and duration. Calls slower than `LOG_SLOW_REQUEST_THRESHOLD` are logged at WARN.
- **Exceptions:** unexpected errors are logged at ERROR with the full stack trace. The client only receives a generic message plus the request ID.
- **Never logged:** request headers, query strings, request bodies, the currency API key, and the provider URL (only its host is logged). Line breaks in logged values are flattened, so user input can't forge log lines.
- **Startup:** a banner (from `banner.txt`) shows the version, Spring Boot version, Java version and profile. Once the app is ready, a summary lists the port, URLs, currency provider status, log directory and whether debug logging is on.

## API Documentation

Base path: `/api/v1`. All numeric results are decimal **strings**.

| Method | Path | Purpose |
|---|---|---|
| GET | `/info` | Name, tagline, version |
| GET | `/actuator/health` | Health probe: `{"status":"UP"}` (also `/liveness`, `/readiness`). The only actuator endpoint exposed; it sits outside `/api/v1` |
| GET | `/units` | Unit catalog: categories and presets |
| POST | `/units/convert` | Convert a value between two units |
| POST | `/units/convert-all` | Convert a value into many units |
| GET | `/currency/currencies` | Supported currencies, defaults, rate provenance |
| POST | `/currency/convert` | Convert money |
| GET | `/area/regions` | Land regions and their units |
| POST | `/area/convert` · `/area/convert-all` | Region-aware area conversion |
| GET | `/time-distance/options` | Units and defaults for the travel calculator |
| POST | `/time-distance/calculate` | Solve for distance, speed or time |
| GET | `/date/options` | Today, weekend presets, holiday calendars |
| POST | `/date/shift` | Add or subtract (`operation`: `ADD` / `SUBTRACT`) |
| POST | `/date/difference` | Difference between two dates |

Height, distance and speed use the generic `/units` endpoints with `category` set to `LENGTH` or `SPEED`. This avoids duplicate APIs.

### Examples

```http
POST /api/v1/units/convert
{"category":"LENGTH","value":180,"from":"cm","to":"m"}
```
```json
{"category":"LENGTH",
 "input":{"value":"180","unit":{"code":"cm","name":"Centimeter","symbol":"cm"}},
 "result":{"value":"1.8","unit":{"code":"m","name":"Meter","symbol":"m"}},
 "unitRate":"0.01"}
```

```http
POST /api/v1/currency/convert
{"amount":100,"from":"USD","to":"INR"}
```
```json
{"amount":"100","result":"9606.99","rate":"96.069945","inverseRate":"0.0104091",
 "from":{"code":"USD","name":"US Dollar","fractionDigits":2,"featured":true},
 "to":{"code":"INR","name":"Indian Rupee","fractionDigits":2,"featured":true},
 "rates":{"source":"ExchangeRate-API (open access)","lastUpdated":"2026-09-29T00:02:31Z",
          "nextUpdate":"2026-09-30T00:11:41Z","fetchedAt":"2026-09-29T05:52:14Z","stale":false}}
```

```http
POST /api/v1/area/convert
{"region":"MAHARASHTRA","value":1,"from":"ac","to":"guntha"}
```
```json
{"region":{"code":"MAHARASHTRA","name":"Maharashtra"},
 "input":{"value":"1","unit":{"code":"ac","name":"Acre","symbol":"ac"}},
 "result":{"value":"40","unit":{"code":"guntha","name":"Guntha","symbol":"guntha",
           "basis":"1 Guntha = 1/40 Acre = 1,089 sq ft"}},
 "unitRate":"40","notes":["1 Guntha = 1/40 Acre = 1,089 sq ft (Maharashtra)"],
 "disclaimer":"Traditional land units vary by state ..."}
```

```http
POST /api/v1/time-distance/calculate
{"solveFor":"TIME","distance":500,"distanceUnit":"km","speed":80,"speedUnit":"kmh"}
```
```json
{"solveFor":"TIME","formula":"Time = Distance ÷ Speed",
 "result":{"value":"6.25","unit":{"code":"h","name":"Hour","symbol":"h"}},
 "readable":"6 hours 15 minutes","duration":{"days":"0","hours":6,"minutes":15,"seconds":0},
 "inputs":[...]}
```

```http
POST /api/v1/date/shift
{"date":"2026-09-15","operation":"ADD","amount":20,"unit":"BUSINESS_DAYS","holidayCalendar":"IN_NATIONAL"}
```
```json
{"startDate":"2026-09-15","resultDate":"2026-10-14","resultDayOfWeek":"Wednesday",
 "calendarDays":29,"nonWorkingDaysSkipped":9,
 "calendar":{"weekend":"Saturday & Sunday","holidays":"India — National holidays"}, ...}
```
`unit` is one of `DAYS`, `WEEKS`, `MONTHS`, `YEARS` or `BUSINESS_DAYS`. Omit `date` to start from today.

```http
POST /api/v1/date/difference
{"startDate":"2026-08-15","endDate":"2026-11-03"}
```
```json
{"totalDays":80,"weeks":11,"remainingDays":3,"years":0,"months":2,"days":19,"businessDays":56, ...}
```

### Errors

```json
{"timestamp":"2026-09-29T05:52:04.569Z","status":400,"error":"INVALID_INPUT",
 "message":"Speed must be greater than zero","path":"/api/v1/time-distance/calculate"}
```

Codes: `INVALID_INPUT`, `VALIDATION_FAILED` (includes a `violations` list), `MALFORMED_REQUEST`, `UNSUPPORTED_CATEGORY`, `UNSUPPORTED_UNIT`, `INVALID_CURRENCY`, `INVALID_REGION`, `INVALID_DATE_RANGE`, `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`, `EXCHANGE_RATE_UNAVAILABLE` (503), `INTERNAL_ERROR` (500).

## Supported Units

- **Length:** mm, cm, m, km, in, ft, yd, mi, nmi
- **Area:** mm², cm², m², km², in², ft², yd², mi², acre, hectare
- **Speed:** m/s, km/h, mph, ft/s, knot
- **Time:** ms, s, min, h, day, week
- **Weight:** mg, g, kg, tonne, oz, lb, stone
- **Temperature:** °C, °F, K (cannot go below absolute zero)
- **Volume:** mL, L, m³, in³, ft³, US fl oz, US cup, US gal, imperial gal
- **Regional land units:** Andhra Pradesh & Telangana, Assam, Bihar, Gujarat, Haryana, Himachal Pradesh, Jammu & Kashmir, Karnataka, Kerala, Maharashtra, Odisha, Punjab, Rajasthan, Tamil Nadu, Uttar Pradesh, West Bengal. Each unit carries its conversion basis. Edit `catalog/land-regions.yml` to add a region or change a local definition.

## Currency Provider Configuration

The default provider is the open ExchangeRate-API endpoint. It needs no key and updates daily. To use the keyed v6 API:

```bash
CURRENCY_PROVIDER_URL='https://v6.exchangerate-api.com/v6/{apiKey}/latest/{base}'
CURRENCY_API_KEY=your-key
```

For providers that expect the key in a header, set `CURRENCY_API_KEY_HEADER` (for example `apikey`). If a provider uses a different response format, add a new `ExchangeRateProvider` implementation annotated with `@ConditionalOnProperty(prefix="convertify.currency.provider", name="type", havingValue="yourtype")` and set `CURRENCY_PROVIDER_TYPE=yourtype`. No business logic changes.

## Development Setup

1. Install JDK 25 and Maven.
2. Run `cd backend && ./mvnw spring-boot:run` (the `dev` profile is active by default).
3. Edit files in `frontend/` and refresh the browser. There is no build step.
4. For unit, region or holiday changes, edit `backend/src/main/resources/catalog/*.yml` and restart. Invalid definitions (unknown base unit, duplicate codes, bad factors) stop the app at startup with a clear message.

## Testing

```bash
cd backend
./mvnw test
```

Unit tests cover the decimal parsing, display rounding, input limits, duration formatting and the exchange-rate cache (TTL, stale fallback, back-off). `ApiIntegrationTest` boots the whole app with a stub exchange-rate provider, so it needs no network, and checks every calculator through the REST API, including error responses. GitHub Actions runs `./mvnw verify` on every push and pull request (`.github/workflows/ci.yml`).

## Future Enhancements

- More categories (data storage, energy, pressure, fuel economy) — YAML only
- District-level land definitions and user-selectable regional variants
- Historical exchange rates and rate charts
- Rules-based moveable holidays (e.g. "last Monday of May")
- Shareable deep links that encode calculator inputs in the URL
- PWA/offline mode for the unit calculators, which need no network once loaded
