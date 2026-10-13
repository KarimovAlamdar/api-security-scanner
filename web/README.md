# API Security Scanner - Web Portal & Interactive Showcase 🌐

A cyber-grade, modern SaaS landing page, interactive playground, and live demo portal for **API Security Scanner (DAST + SAST Hybrid Engine)**.

---

## 🚀 Key Modules Included

1. **Cyber-Grade Hero & Live Terminal Simulator:**
   - Real-time terminal log simulator demonstrating Spec Drift, Stateful BOLA exploit, GenAI testing, and Zero-Click PR generation.
2. **5 Killer Features Showcase:**
   - 🏆 Feature 1: Zero-Click AI PR Auto-Fix (Automated GitHub Bot)
   - 🕵️ Feature 2: Shadow & Zombie API Discovery (Spec Drift Engine)
   - 🔒 Feature 3: 100% Air-Gapped Mode (SOC2 Type II & GDPR Attestation)
   - 🔄 Feature 4: Stateful BOLA / IDOR Verification
   - 🤖 Feature 5: LLM & AI Agent API Security
3. **Head-to-Head Benchmark Matrix:**
   - Detailed comparison against StackHawk, Snyk API, and Salt Security across 7 critical enterprise dimensions.
4. **Live Interactive Playground:**
   - Preset selector for *E-Commerce Microservices*, *Banking Core*, and *GenAI LLM Assistant*.
   - Live simulated scan progress, findings with exact Java source code lines, and 1-click Auto-PR generator.
5. **1-Minute CI/CD Configurator:**
   - Interactive checkbox builder generating production-ready GitHub Actions YAML workflows.
6. **Enterprise ROI & Savings Calculator:**
   - Real-time slider calculating engineering hours and dollar savings saved per sprint.
7. **Dark / Light Theme Engine:**
   - Cyber space dark mode and clean daylight mode with local storage persistence.

---

## 🛠️ How to Run Locally

### Option 1: Direct File Opening
Simply double-click or open `index.html` in any modern web browser (Chrome, Edge, Firefox, Safari).

### Option 2: Local HTTP Server (Recommended)
Using Python:
```bash
cd web
python -m http.server 3000
```
Then navigate to: `http://localhost:3000`

Using Node.js / npx:
```bash
npx serve web -p 3000
```

---

## 🚢 Deployment (Vercel / Netlify / GitHub Pages)

- **Vercel / Netlify**: Set `Root Directory` to `web` (Zero build command required, purely static HTML5/CSS3/Vanilla JS).
- **GitHub Pages**: Configure GitHub Pages to serve from the repository or `/web` branch.
