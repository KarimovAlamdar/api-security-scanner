# 🛡️ API Security Scanner — Next-Gen DAST + SAST Hybrid Engine

[![Live Portal](https://img.shields.io/badge/Live_Portal-Active_Demo-10B981?style=for-the-badge&logo=googlechrome&logoColor=white)](https://karimovalamdar.github.io/api-security-scanner/)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![GitHub Action](https://img.shields.io/badge/GitHub_Action-v1.2.0-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/features/actions)
[![SARIF 2.1.0](https://img.shields.io/badge/SARIF-2.1.0_Compatible-0A84FF?style=for-the-badge)](https://sarifweb.azurewebsites.net/)
[![Lemon Squeezy](https://img.shields.io/badge/Billing-Lemon_Squeezy_%26_Stripe-FFC800?style=for-the-badge)](https://www.lemonsqueezy.com/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue?style=for-the-badge)](LICENSE)

> **Stop reading vulnerability reports. Automatically discover Shadow APIs, exploit stateful cross-tenant BOLA/IDOR, secure LLM agents, and generate ready-to-merge Pull Requests directly in CI/CD.**

---

## 🌟 Why API Security Scanner? (The 5 "Killer" Advantages Over StackHawk & ZAP)

| Feature | Legacy Scanners (StackHawk, ZAP, Snyk) | 🛡️ Our API Security Scanner |
|---|---|---|
| **Remediation Speed** | ❌ Manual: gives documentation links for devs to read | 🏆 **Zero-Click AI PR Auto-Fix**: Automatically creates a branch, applies the exact patch to the code line, and opens a GitHub PR |
| **API Drift & Shadow APIs** | ❌ Scans only routes listed in OpenAPI specs | 🕵️ **Shadow & Zombie API Discovery**: Discovers unlisted endpoints in controllers and flags spec drift |
| **BOLA / IDOR Testing** | ⚠️ Stateless: single user fuzzer | ⚡ **Stateful Dual-Role BOLA Matrix**: Tests cross-tenant object access with User A vs User B |
| **AI Agent Security** | ❌ None | 🤖 **Agentic AI & MCP Defense**: Tests OWASP LLM Top 10 (Tool Poisoning, SSRF in Agent Tool Calls) |
| **Telemetry & Air-Gap** | ⚠️ Forces telemetry / cloud dashboard upload | 🔒 **100% Air-Gapped Mode**: Zero network telemetry, local SARIF, offline HMAC licenses |
| **Runtime Dependencies** | ⚠️ Heavy Node/Python/Go environments | ⚡ **Zero Runtime Bloat**: Native Java 17+, instant startup, ~15MB Docker image |

---

## 📁 Repository Structure

```
.
├── api-security-scanner/       # Core Engine & GitHub Action
│   ├── action.yml              # GitHub Action Manifest
│   ├── Dockerfile              # Ultra-slim container image (Eclipse Temurin JRE)
│   ├── pom.xml                 # Maven build configuration (Java 17+)
│   ├── src/main/java/          # Scanner engine, DAST+SAST line mapper, LicenseValidator
│   └── src/test/java/          # Comprehensive unit test suite (23 passing tests)
│
├── server/                     # Billing & License Management API
│   ├── pom.xml                 # Embedded Java HTTP server (Port 4242)
│   ├── nodejs/                 # Node.js / Express alternative implementation
│   ├── src/main/resources/     # application.properties (Lemon Squeezy & Stripe config)
│   ├── data/licenses.json      # Persistent customer licenses database
│   └── src/main/java/          # LemonSqueezyService, StripeService, LicenseManager
│
└── web/                        # B2B SaaS Customer Portal & Landing Page
    ├── index.html              # High-converting landing page with 3D WebGL Hero
    ├── styles.css              # Custom WCAG AAA responsive design system (Dark & Light)
    ├── app.js                  # Interactive playground, ROI calculator & checkout integration
    └── src/                    # Modular React / Tailwind / Three Fiber components
```

---

## ⚡ Quick Start

### 1. Run Core Scanner CLI
```powershell
cd api-security-scanner
mvn clean package -DskipTests
java -jar target/api-security-scanner.jar --spec openapi.json --base-url http://localhost:8080
```

### 2. GitHub Actions Integration (`.github/workflows/security.yml`)
Add this step to your pipeline:
```yaml
- name: Run API Security Scanner
  uses: api-security-scanner/action@v1
  with:
    spec: openapi.json
    base-url: https://staging.api.company.com
    auth-token: ${{ secrets.API_TOKEN }}
    license-key: ${{ secrets.SCANNER_LICENSE_KEY }} # Unlocks Auto-PR & Stateful BOLA
    auto-pr: true                                   # Opens automated PRs with remediations
    fail-on: high
```

### 3. Launch Local Web Portal & Billing Server
* **Frontend Portal (Port 3000):**
  ```powershell
  cd web
  python -m http.server 3000
  ```
  Open: [http://localhost:3000](http://localhost:3000)

* **Billing & License Server (Port 4242):**
  ```powershell
  cd server
  java -jar target/api-security-billing-server-1.0.0.jar
  ```
  API Docs & Health: [http://localhost:4242/api/health](http://localhost:4242/api/health)

---

## 💳 Billing & Payout Architecture (Lemon Squeezy MoR)

Our billing system is configured with **Variant A (Lemon Squeezy - Merchant of Record)**:
* **No Foreign LLC / LTD Required:** Sell globally to US & EU enterprise clients immediately.
* **Direct Payouts:** Funds route automatically to your **Payoneer**, **Wise**, or **Local Bank Card** (Kapital Bank, ABB, Leobank, etc.).
* **Instant Licensing:** Every successful order emits a cryptographic `sec_live_...` key stored in `server/data/licenses.json`.
* **Air-Gapped Validation:** Enterprise clients running offline can validate their license signature via local HMAC without making external network calls.

---

## 🛡️ License Tiers & Feature Matrix

| Feature | Community (Free) | Starter ($15/mo) | Pro ($24.50/mo)* | Enterprise ($119/mo)* |
|---|:---:|:---:|:---:|:---:|
| **DAST Core Scanning** | ✅ | ✅ | ✅ | ✅ |
| **SARIF 2.1.0 Export** | ✅ | ✅ | ✅ | ✅ |
| **HTML Interactive Triaj Dashboard** | ❌ | ✅ | ✅ | ✅ |
| **DAST + SAST Line Mapping** | ❌ | ✅ | ✅ | ✅ |
| **Zero-Click AI PR Auto-Fix (`--auto-pr`)** | ❌ | ❌ | ✅ | ✅ |
| **Shadow & Zombie API Discovery** | ❌ | ❌ | ✅ | ✅ |
| **Stateful Dual-Role BOLA Matrix** | ❌ | ❌ | ✅ | ✅ |
| **Active Repositories** | 1 | 3 | 15 | Unlimited |
| **SLA & Priority Rules** | Discord | Email | Priority | 24/7 Dedicated |

*\* Reflects Early Bird 50% discount with coupon `EARLYBIRD10`.*

---

## 🧪 Automated Test Suite Status

* **Scanner Engine Tests:** `23 / 23` passed (`com.scanner.ApiSecurityScannerTest`, `com.scanner.LicenseValidatorTest`).
* **Billing & Licensing Server Tests:** `7 / 7` passed (`com.scanner.server.LemonSqueezyServiceTest`, `com.scanner.server.StripeServiceTest`, `com.scanner.server.LicenseManagerTest`).
* **Total Automated Tests:** **`30 / 30 Passed (100% Success Rate)`**.

---

## 📄 License
Licensed under the Apache License, Version 2.0.
