/**
 * API Security Scanner - Stripe Checkout & License Backend (Node.js / Express)
 * 
 * Provides:
 *  - POST /api/checkout/create-session (Stripe Checkout Session creation)
 *  - POST /api/webhook/stripe (Stripe Webhook signature verification & License Issuance)
 *  - GET  /api/license/verify (License Key verification for Scanner CLI / GitHub Action)
 *  - GET  /api/licenses (Admin list of issued licenses)
 *  - GET  /api/health (Service health check)
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { v4: uuidv4 } = require('uuid');

const app = express();
const PORT = process.env.PORT || 4242;
const FRONTEND_URL = process.env.FRONTEND_URL || 'http://localhost:3000';
const STRIPE_SECRET_KEY = process.env.STRIPE_SECRET_KEY || '';
const STRIPE_WEBHOOK_SECRET = process.env.STRIPE_WEBHOOK_SECRET || '';
const LICENSE_SECRET_KEY = process.env.LICENSE_SECRET_KEY || 'api-security-scanner-secret-hmac-key-2026';

const isStripeConfigured = STRIPE_SECRET_KEY.startsWith('sk_') && !STRIPE_SECRET_KEY.includes('replace_with');
const stripe = isStripeConfigured ? require('stripe')(STRIPE_SECRET_KEY) : null;

// License file storage path
const DATA_DIR = path.join(__dirname, 'data');
const LICENSES_FILE = path.join(DATA_DIR, 'licenses.json');

if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

// Pricing definitions (USD Cents)
const PRICING = {
  monthly: {
    community: 0,
    starter: 1900,     // $19/mo
    pro: 4900,         // $49/mo
    enterprise: 29900  // $299/mo
  },
  annual: {
    community: 0,
    starter: 18000,    // $15 * 12 = $180/yr
    pro: 46800,        // $39 * 12 = $468/yr
    enterprise: 286800 // $239 * 12 = $2868/yr
  }
};

const TIER_FEATURES = {
  Community: ['DAST_CORE', 'SARIF_EXPORT'],
  Starter: ['DAST_CORE', 'SAST_MAPPING', 'SARIF_EXPORT', 'HTML_REPORT'],
  Pro: ['DAST_CORE', 'SAST_MAPPING', 'AUTO_PR_BOT', 'SHADOW_API_DISCOVERY', 'STATEFUL_BOLA', 'SARIF_EXPORT', 'HTML_REPORT'],
  Enterprise: ['DAST_CORE', 'SAST_MAPPING', 'AUTO_PR_BOT', 'SHADOW_API_DISCOVERY', 'STATEFUL_BOLA', 'SARIF_EXPORT', 'HTML_REPORT', 'CUSTOM_RULES', 'PRIORITY_SLA']
};

const TIER_MAX_REPOS = {
  Community: 1,
  Starter: 3,
  Pro: 15,
  Enterprise: 9999
};

// In-memory cache synced with JSON disk
let licensesCache = {};
function loadLicenses() {
  try {
    if (fs.existsSync(LICENSES_FILE)) {
      const data = JSON.parse(fs.readFileSync(LICENSES_FILE, 'utf8'));
      if (Array.isArray(data)) {
        data.forEach(lic => { if (lic.licenseKey) licensesCache[lic.licenseKey] = lic; });
      }
    }
  } catch (err) {
    console.error('Error loading licenses from disk:', err.message);
  }
}
function saveLicenses() {
  try {
    fs.writeFileSync(LICENSES_FILE, JSON.stringify(Object.values(licensesCache), null, 2), 'utf8');
  } catch (err) {
    console.error('Error saving licenses to disk:', err.message);
  }
}
loadLicenses();

function issueLicense(email, tier, billingCycle, stripeSessionId, stripeCustomerId) {
  const cleanTier = tier || 'Pro';
  const cleanCycle = billingCycle || 'monthly';
  const now = new Date();
  const expiresAt = new Date(now.getTime() + (cleanCycle === 'annual' ? 365 : 30) * 24 * 60 * 60 * 1000);

  const rawToken = uuidv4().replace(/-/g, '') + Date.now().toString(16);
  const checksum = crypto.createHmac('sha256', LICENSE_SECRET_KEY).update(rawToken).digest('hex').substring(0, 10);
  const licenseKey = `sec_live_${rawToken.substring(0, 24)}_${checksum}`;

  const license = {
    licenseKey,
    customerEmail: email || 'user@example.com',
    tier: cleanTier,
    billingCycle: cleanCycle,
    status: 'ACTIVE',
    maxRepos: TIER_MAX_REPOS[cleanTier] || 15,
    features: TIER_FEATURES[cleanTier] || TIER_FEATURES.Pro,
    createdAt: now.toISOString(),
    expiresAt: expiresAt.toISOString(),
    stripeSessionId: stripeSessionId || 'none',
    stripeCustomerId: stripeCustomerId || 'none'
  };

  licensesCache[licenseKey] = license;
  saveLicenses();
  console.log(`[LICENSE] Issued ${cleanTier} license to ${email}: ${licenseKey}`);
  return license;
}

// ------------------- Middleware -------------------
app.use(cors());

// Webhook endpoint MUST use raw body for cryptographic signature verification
app.post('/api/webhook/stripe', express.raw({ type: 'application/json' }), async (req, res) => {
  const sig = req.headers['stripe-signature'];
  let event;

  if (isStripeConfigured && STRIPE_WEBHOOK_SECRET && !STRIPE_WEBHOOK_SECRET.includes('replace_with')) {
    try {
      event = stripe.webhooks.constructEvent(req.body, sig, STRIPE_WEBHOOK_SECRET);
    } catch (err) {
      console.error(`⚠️ Webhook signature verification failed: ${err.message}`);
      return res.status(400).send(`Webhook Error: ${err.message}`);
    }
  } else {
    // Development fallback parsing
    try {
      event = JSON.parse(req.body.toString('utf8'));
    } catch (e) {
      return res.status(400).json({ error: 'Invalid JSON payload' });
    }
  }

  // Handle checkout.session.completed event
  if (event.type === 'checkout.session.completed') {
    const session = event.data.object;
    const customerEmail = session.customer_details?.email || session.customer_email || 'customer@example.com';
    const tier = session.metadata?.tier || 'Pro';
    const cycle = session.metadata?.billingCycle || 'monthly';
    const customerId = session.customer || 'cus_none';

    const license = issueLicense(customerEmail, tier, cycle, session.id, customerId);
    console.log(`[STRIPE WEBHOOK] License created for session ${session.id}: ${license.licenseKey}`);
    return res.json({ received: true, licenseKey: license.licenseKey });
  }

  res.json({ received: true, eventType: event.type });
});

// JSON middleware for regular API endpoints
app.use(express.json());

// ------------------- REST API Endpoints -------------------

/**
 * POST /api/checkout/create-session
 */
app.post('/api/checkout/create-session', async (req, res) => {
  try {
    const { tier = 'Pro', billingCycle = 'monthly', couponCode = '', customerEmail = '', successUrl, cancelUrl } = req.body;
    const cleanCycle = billingCycle.toLowerCase() === 'annual' ? 'annual' : 'monthly';
    const cleanTierKey = (tier || 'Pro').toLowerCase();

    const baseAmount = PRICING[cleanCycle][cleanTierKey] ?? PRICING[cleanCycle].pro;
    const discountApplied = (couponCode || '').trim().toUpperCase() === 'EARLYBIRD10';
    const finalAmount = discountApplied ? Math.round(baseAmount * 0.5) : baseAmount;

    // Free community plan
    if (cleanTierKey === 'community' || finalAmount === 0) {
      const sessionId = `cs_free_${uuidv4().substring(0, 8)}`;
      const license = issueLicense(customerEmail, 'Community', cleanCycle, sessionId, 'cus_free');
      const targetUrl = (successUrl || `${FRONTEND_URL}/index.html?session_id={CHECKOUT_SESSION_ID}&status=success`)
        .replace('{CHECKOUT_SESSION_ID}', sessionId) + `&license=${license.licenseKey}`;

      return res.json({
        success: true,
        sessionId,
        checkoutUrl: targetUrl,
        plan: 'Community',
        billingCycle: cleanCycle,
        amountCents: 0,
        currency: 'USD',
        mode: 'free_tier',
        simulatedLicenseKey: license.licenseKey,
        message: 'Community tier activated successfully.'
      });
    }

    // Live Stripe Checkout
    if (isStripeConfigured) {
      const session = await stripe.checkout.sessions.create({
        payment_method_types: ['card'],
        mode: 'payment',
        customer_email: customerEmail || undefined,
        line_items: [
          {
            price_data: {
              currency: 'usd',
              product_data: {
                name: `API Security Scanner (${tier} Plan - ${cleanCycle === 'annual' ? 'Annual' : 'Monthly'})`,
                description: 'Automated DAST + SAST Hybrid Vulnerability Scanner License'
              },
              unit_amount: finalAmount
            },
            quantity: 1
          }
        ],
        metadata: {
          tier,
          billingCycle: cleanCycle,
          couponCode: discountApplied ? 'EARLYBIRD10' : ''
        },
        success_url: successUrl || `${FRONTEND_URL}/index.html?session_id={CHECKOUT_SESSION_ID}&status=success`,
        cancel_url: cancelUrl || `${FRONTEND_URL}/index.html?status=cancelled`
      });

      return res.json({
        success: true,
        sessionId: session.id,
        checkoutUrl: session.url,
        plan: tier,
        billingCycle: cleanCycle,
        amountCents: finalAmount,
        currency: 'USD',
        discountApplied,
        discountDetails: discountApplied ? '50% Early Bird Discount applied (Coupon: EARLYBIRD10)' : null,
        mode: 'stripe_live',
        message: 'Live Stripe Checkout session created.'
      });
    }

    // Sandbox Simulation Mode (Zero Setup Required)
    const mockSessionId = `cs_test_mock_${uuidv4().replace(/-/g, '').substring(0, 20)}`;
    const mockLicense = issueLicense(customerEmail, tier, cleanCycle, mockSessionId, 'cus_mock');
    const mockRedirect = (successUrl || `${FRONTEND_URL}/index.html?session_id={CHECKOUT_SESSION_ID}&status=success`)
      .replace('{CHECKOUT_SESSION_ID}', mockSessionId) + `&license=${mockLicense.licenseKey}`;

    return res.json({
      success: true,
      sessionId: mockSessionId,
      checkoutUrl: mockRedirect,
      plan: tier,
      billingCycle: cleanCycle,
      amountCents: finalAmount,
      currency: 'USD',
      discountApplied,
      discountDetails: discountApplied ? '50% Early Bird Discount applied (Coupon: EARLYBIRD10)' : null,
      mode: 'sandbox_simulation',
      simulatedLicenseKey: mockLicense.licenseKey,
      message: 'Simulated Stripe Checkout session created. Provide STRIPE_SECRET_KEY in .env for live Stripe payments.'
    });
  } catch (error) {
    console.error('Checkout creation error:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

/**
 * GET /api/license/verify?key=...
 */
app.get('/api/license/verify', (req, res) => {
  const { key } = req.query;
  if (!key) {
    return res.status(400).json({ valid: false, message: 'License key is required' });
  }

  const cleanKey = key.trim();
  const license = licensesCache[cleanKey];

  if (!license) {
    return res.status(403).json({ valid: false, message: 'License key not found or unrecognized' });
  }

  if (license.status !== 'ACTIVE') {
    return res.status(403).json({ valid: false, message: `License status is ${license.status}` });
  }

  if (license.expiresAt && new Date() > new Date(license.expiresAt)) {
    license.status = 'EXPIRED';
    saveLicenses();
    return res.status(403).json({ valid: false, message: `License expired on ${license.expiresAt}` });
  }

  res.json({
    valid: true,
    message: `License is active and valid for plan: ${license.tier}`,
    license
  });
});

/**
 * GET /api/licenses
 */
app.get('/api/licenses', (req, res) => {
  res.json(Object.values(licensesCache));
});

/**
 * GET /api/health
 */
app.get('/api/health', (req, res) => {
  res.json({
    status: 'UP',
    service: 'API Security Scanner Billing & License API (Node.js/Express)',
    timestamp: new Date().toISOString(),
    stripeLiveConfigured: isStripeConfigured,
    totalLicensesIssued: Object.keys(licensesCache).length
  });
});

app.listen(PORT, () => {
  console.log(`================================================================================`);
  console.log(`🚀 Stripe Billing & License Server running on http://localhost:${PORT}`);
  console.log(`💳 Stripe Mode: ${isStripeConfigured ? 'LIVE / TESTNET (Keys Active)' : 'SANDBOX SIMULATION'}`);
  console.log(`Endpoints:`);
  console.log(`  - POST http://localhost:${PORT}/api/checkout/create-session`);
  console.log(`  - POST http://localhost:${PORT}/api/webhook/stripe`);
  console.log(`  - GET  http://localhost:${PORT}/api/license/verify?key=...`);
  console.log(`  - GET  http://localhost:${PORT}/api/licenses`);
  console.log(`  - GET  http://localhost:${PORT}/api/health`);
  console.log(`================================================================================`);
});
