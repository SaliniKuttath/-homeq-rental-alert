# HomeQ Rental Alert — WhatsApp only

Java 21 / Spring Boot starter: reads a configured HomeQ search-results page, filters by location, suppresses duplicates, and sends new-listing notifications through Twilio WhatsApp.

**Important:** This is a repository-ready starter, not yet a verified live HomeQ scraper. HomeQ's page structure and access rules can change. Verify the current page selectors and confirm automated access is permitted before enabling monitoring. Do not bypass login, CAPTCHA, rate limits, or access restrictions.

## Included
- Java 21, Maven, Spring Boot
- Playwright-based browser reader with configurable CSS selectors
- Area filtering
- H2 persistence for duplicate detection
- Twilio WhatsApp notification sender
- JUnit 5 tests and GitHub Actions build

## Requirements
- JDK 21 and Maven 3.9+
- HomeQ saved-search/results URL
- Twilio account with WhatsApp enabled (Sandbox for testing or approved sender for production)

## Configure HomeQ
Set `HOMEQ_SEARCH_URL` to your HomeQ search-results URL. Inspect the listing page in your browser developer tools and set:
- `HOMEQ_CARD_SELECTOR`: selector matching one apartment card
- `HOMEQ_TITLE_SELECTOR`: title selector within a card
- `HOMEQ_AREA_SELECTOR`: area selector within a card
- `HOMEQ_PRICE_SELECTOR`: rent selector within a card
- `HOMEQ_LINK_SELECTOR`: listing anchor selector within a card

The selectors are intentionally blank/ungessed. The application reads no listings until `HOMEQ_CARD_SELECTOR` is configured. Use `HOMEQ_ALLOWED_AREAS` as a comma-separated filter, e.g. `Södra Änggården,Marklandsgatan,Högsbo,Frölunda`. This is an example; change it to your chosen locations.

## Configure Twilio WhatsApp
1. Create/sign in to Twilio and enable WhatsApp Sandbox for development, or configure an approved WhatsApp sender.
2. Follow Twilio's current Sandbox instructions to join it from your WhatsApp number.
3. Set `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_WHATSAPP_FROM`, and `WHATSAPP_TO`.
4. Use international phone format, e.g. `whatsapp:+467XXXXXXXX`.

WhatsApp business-initiated messages may require an approved template outside the customer-service window. Confirm current Twilio/Meta rules before production.

## Run locally
Copy `.env.example` to `.env` and fill it in. Spring Boot does not automatically load `.env`; export variables in your shell or set them in your IDE run configuration.

PowerShell example:
```powershell
Get-Content .env -Encoding UTF8 | ForEach-Object {
  if ($_ -match '^([^#=]+)=(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
  }
}
mvn spring-boot:run
```

Install the Playwright browser using the Playwright Java CLI. The CLI is available from the Playwright dependency; consult the official Playwright Java installation instructions for the command matching your environment.

Run tests:
```bash
mvn clean verify
```

## Create your private GitHub repository
1. Create an account: https://github.com/signup
2. Create a **Private** repository named `homeq-rental-alert`.
3. Extract this ZIP, open a terminal in the extracted folder, then run:
```bash
git init
git add .
git commit -m "Initial HomeQ WhatsApp alert project"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/homeq-rental-alert.git
git push -u origin main
```

Never commit `.env`, Twilio tokens, or private phone numbers. `.gitignore` excludes `.env`.

GitHub Actions runs the Maven build/tests on push and pull requests. It is CI only, not the continuous monitoring host. For 24/7 monitoring, deploy the app to a Java-capable host with persistent storage for the H2 database and store Twilio credentials as host secrets/environment variables.

## Environment variables
| Variable | Purpose |
|---|---|
| `HOMEQ_SEARCH_URL` | Saved-search results URL |
| `HOMEQ_CARD_SELECTOR` | CSS selector for each listing card |
| `HOMEQ_TITLE_SELECTOR` | Title within card |
| `HOMEQ_AREA_SELECTOR` | Area within card |
| `HOMEQ_PRICE_SELECTOR` | Rent within card |
| `HOMEQ_LINK_SELECTOR` | Listing link within card |
| `HOMEQ_ALLOWED_AREAS` | Comma-separated location filter |
| `HOMEQ_POLL_INTERVAL_MS` | Poll interval; default 600000 (10 min) |
| `HOMEQ_BASELINE_ON_FIRST_RUN` | Default `true`: on the first successful read with an empty DB, record current listings without alerting |
| `HOMEQ_CARD_WAIT_TIMEOUT_MS` | How long to wait for listing cards to render; default 30000 |
| `TWILIO_ACCOUNT_SID` | Twilio account SID |
| `TWILIO_AUTH_TOKEN` | Twilio auth token |
| `TWILIO_WHATSAPP_FROM` | WhatsApp-enabled Twilio sender |
| `WHATSAPP_TO` | Your WhatsApp number |

## Current limitations / next development steps
- Inspect and validate HomeQ selectors against the exact saved-search page.
- Confirm HomeQ permits the chosen automated access method.
- Some results may require sign-in or client-side actions; do not circumvent access restrictions.
- Listing URL is used as the initial duplicate key; a stable listing ID would be preferable if available.
- If notification fails, the listing is not marked as seen and will be retried at the next check.
- Add deployment health checks and persistent storage before relying on time-sensitive alerts.
