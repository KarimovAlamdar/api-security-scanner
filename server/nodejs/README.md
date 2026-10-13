# Stripe Checkout & Licensing Backend

This directory contains the Node.js / Express implementation of the API Security Scanner billing service.

## Prerequisites
- Node.js 18+ and npm

## Setup & Running

1. **Install Dependencies:**
   ```bash
   npm install
   ```

2. **Configure Environment Variables:**
   Copy `.env.example` to `.env` and enter your Stripe keys:
   ```bash
   cp .env.example .env
   ```
   *(If keys are left blank, the server operates in Sandbox Simulation mode, allowing full testing without a Stripe account!)*

3. **Start the Server:**
   ```bash
   npm start
   ```
   The server will start on port `4242`: `http://localhost:4242`

## API Endpoints

- `POST /api/checkout/create-session` - Creates a Stripe Checkout Session with support for coupon `EARLYBIRD10` (50% discount).
- `POST /api/webhook/stripe` - Handles Stripe webhooks (`checkout.session.completed`) and automatically issues `sec_live_...` license keys.
- `GET  /api/license/verify?key=sec_live_...` - Verifies license keys for Scanner CLI or CI/CD pipelines.
- `GET  /api/health` - Server health status.
