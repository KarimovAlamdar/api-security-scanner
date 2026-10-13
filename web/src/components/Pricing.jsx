import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Check, Zap, Shield, Sparkles, Building2, Flame } from 'lucide-react';

export default function Pricing() {
  const [isAnnual, setIsAnnual] = useState(true);
  const [promoApplied, setPromoApplied] = useState(false);
  const [promoCode, setPromoCode] = useState('');
  const [loadingTier, setLoadingTier] = useState(null);

  // Early Bird 50% discount rate
  const discountMultiplier = promoApplied ? 0.5 : 1;

  const tiers = [
    {
      id: 'community',
      name: 'Community',
      tagline: 'For individual developers testing local APIs',
      priceMonthly: 0,
      priceAnnual: 0,
      badge: 'Free Forever',
      icon: Shield,
      features: [
        'Local CLI Runner (--spec, --base-url)',
        '30 DAST + SAST Hybrid Security Rules',
        'Interactive HTML Dashboard & Triage',
        'SARIF 2.1.0 Export format',
        'Unlimited local scans',
        'Community Discord Support',
      ],
      cta: 'Start Free Forever',
      highlighted: false,
    },
    {
      id: 'starter',
      name: 'Starter',
      tagline: 'Automated CI/CD security gate for solo devs & startups',
      priceMonthly: 19,
      priceAnnual: 15,
      badge: 'Best for Small Teams',
      icon: Zap,
      features: [
        'Everything in Community, plus:',
        'GitHub Actions CI/CD Integration',
        'Dual-Role BOLA / IDOR Testing',
        'Up to 3 Active Repositories',
        'GitHub Security Tab (CodeQL) Upload',
        'Slack & Microsoft Teams Alerts',
        'Standard Email Support',
      ],
      cta: 'Get Starter Pass',
      highlighted: false,
    },
    {
      id: 'pro',
      name: 'Pro (AI Auto-Fix)',
      tagline: 'Autonomous remediation bot for fast-shipping engineering teams',
      priceMonthly: 49,
      priceAnnual: 39,
      badge: 'Most Popular ⭐',
      icon: Sparkles,
      features: [
        'Everything in Starter, plus:',
        '🏆 Zero-Click AI PR Auto-Fix Bot (Auto PR + Code Patch)',
        '🕵️ Shadow & Zombie API Discovery (Spec Drift)',
        '🤖 LLM & AI Agent API Gateways (Prompt Injection Fuzzer)',
        'Up to 15 Active Repositories',
        'Multi-Threaded Concurrency (up to 16 workers)',
        'Priority Slack & Engineering Support',
      ],
      cta: 'Start 14-Day Free Trial',
      highlighted: true,
    },
    {
      id: 'enterprise',
      name: 'Enterprise Air-Gapped',
      tagline: 'Mission-critical security governance for FinTech, Banks & Defense',
      priceMonthly: 299,
      priceAnnual: 239,
      badge: 'SOC2 & Air-Gapped',
      icon: Building2,
      features: [
        'Everything in Pro, plus:',
        '🔒 100% Air-Gapped Mode (Zero External Data Egress)',
        '📜 SOC2 Type II & GDPR Cryptographic Audit Attestation',
        'Determinative SHA-256 Checksum Validation',
        'Self-Hosted On-Prem & Air-Gapped K8s Support',
        'Unlimited Repositories & Unlimited Scans',
        'Dedicated Security Architect & 99.99% SLA',
        'Custom SAML SSO & RBAC Governance',
      ],
      cta: 'Contact Enterprise Sales',
      highlighted: false,
    },
  ];

  const handleStripeCheckout = (tier) => {
    setLoadingTier(tier.id);
    // Simulate instant Stripe Checkout redirect
    setTimeout(() => {
      setLoadingTier(null);
      alert(`Stripe Checkout Session initialized for: ${tier.name} ($${calcPrice(tier)}/mo). Ready to redirect to Stripe Billing Portal!`);
    }, 900);
  };

  const handleApplyPromo = (e) => {
    e.preventDefault();
    if (promoCode.trim().toUpperCase() === 'EARLYBIRD10') {
      setPromoApplied(true);
    } else {
      alert('Invalid promo code. Try "EARLYBIRD10" for the first 10 customers deal!');
    }
  };

  const calcPrice = (tier) => {
    if (tier.priceMonthly === 0) return 0;
    const base = isAnnual ? tier.priceAnnual : tier.priceMonthly;
    return Math.round(base * discountMultiplier);
  };

  return (
    <section id="pricing" className="py-24 relative overflow-hidden bg-[#07090e]">
      {/* Background Glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[350px] bg-emerald-500/10 blur-[130px] rounded-full pointer-events-none" />

      <div className="max-w-7xl mx-auto px-6 relative z-10">
        {/* Section Header */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center max-w-3xl mx-auto mb-12"
        >
          <span className="text-emerald-400 font-mono text-sm uppercase tracking-wider font-semibold">
            Simple, Transparent Pricing
          </span>
          <h2 className="text-4xl md:text-5xl font-extrabold text-white mt-3 tracking-tight">
            Stop Paying for Inactionable PDF Reports.
          </h2>
          <p className="text-slate-400 text-lg mt-4">
            Invest in automated code remediation. Pick the plan that fits your security footprint.
          </p>
        </motion.div>

        {/* 🔥 Early Bird Special Banner (First 10 Customers) */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          className="max-w-3xl mx-auto mb-10 p-4 rounded-xl bg-gradient-to-r from-amber-500/15 via-rose-500/15 to-emerald-500/15 border border-amber-500/40 backdrop-blur-md flex flex-col md:flex-row items-center justify-between gap-4"
        >
          <div className="flex items-center gap-3 text-left">
            <div className="p-2.5 rounded-lg bg-amber-500/20 text-amber-400">
              <Flame className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-white font-bold text-sm md:text-base">
                  🔥 Early Bird Launch Special: 50% OFF Lifetime!
                </span>
                <span className="bg-rose-500/20 text-rose-300 text-xs px-2 py-0.5 rounded-full border border-rose-500/30 font-semibold font-mono">
                  Only 3 / 10 Spots Left
                </span>
              </div>
              <p className="text-slate-400 text-xs mt-0.5">
                First 10 customers lock in half-price forever with coupon code <code className="text-amber-300 font-bold">EARLYBIRD10</code>.
              </p>
            </div>
          </div>

          <form onSubmit={handleApplyPromo} className="flex items-center gap-2 w-full md:w-auto">
            <input
              type="text"
              placeholder="Code: EARLYBIRD10"
              value={promoCode}
              onChange={(e) => setPromoCode(e.target.value)}
              className="bg-black/50 border border-slate-700 text-white text-xs px-3 py-2 rounded-lg outline-none focus:border-amber-400 font-mono w-full md:w-36"
            />
            <button
              type="submit"
              className="bg-amber-500 hover:bg-amber-400 text-black font-bold text-xs px-3.5 py-2 rounded-lg transition whitespace-nowrap shadow-lg shadow-amber-500/20"
            >
              {promoApplied ? 'Applied ✓' : 'Apply'}
            </button>
          </form>
        </motion.div>

        {/* Monthly / Annual Billing Toggle */}
        <div className="flex items-center justify-center gap-4 mb-16">
          <span className={`text-sm font-medium ${!isAnnual ? 'text-white' : 'text-slate-400'}`}>
            Monthly Billing
          </span>
          <button
            onClick={() => setIsAnnual(!isAnnual)}
            className="w-14 h-8 bg-slate-800 rounded-full p-1 border border-slate-700 relative transition-colors focus:outline-none"
          >
            <motion.div
              animate={{ x: isAnnual ? 24 : 0 }}
              transition={{ type: 'spring', stiffness: 500, damping: 30 }}
              className="w-6 h-6 bg-emerald-400 rounded-full shadow-md"
            />
          </button>
          <span className={`text-sm font-medium flex items-center gap-1.5 ${isAnnual ? 'text-white' : 'text-slate-400'}`}>
            Annual Billing
            <span className="bg-emerald-500/20 text-emerald-400 text-xs px-2 py-0.5 rounded-full font-bold border border-emerald-500/30">
              Save 20%
            </span>
          </span>
        </div>

        {/* Pricing Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {tiers.map((tier, idx) => {
            const Icon = tier.icon;
            const price = calcPrice(tier);
            const originalPrice = isAnnual ? tier.priceAnnual : tier.priceMonthly;

            return (
              <motion.div
                key={tier.id}
                initial={{ opacity: 0, y: 40 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: idx * 0.1 }}
                className={`relative rounded-2xl p-6 flex flex-col justify-between transition-all duration-300 ${
                  tier.highlighted
                    ? 'bg-gradient-to-b from-[#162238] to-[#0f172a] border-2 border-emerald-500 shadow-2xl shadow-emerald-500/15 -translate-y-2'
                    : 'bg-[#0f1422]/90 border border-white/10 hover:border-white/20'
                }`}
              >
                {/* Popular Pill */}
                {tier.highlighted && (
                  <div className="absolute -top-3.5 left-1/2 -translate-x-1/2 bg-gradient-to-r from-emerald-500 to-cyan-500 text-black font-extrabold text-xs px-3.5 py-1 rounded-full shadow-lg tracking-wider uppercase">
                    {tier.badge}
                  </div>
                )}

                <div>
                  <div className="flex items-center justify-between mb-4">
                    <span className="text-slate-300 font-semibold text-lg">{tier.name}</span>
                    <div className={`p-2 rounded-lg ${tier.highlighted ? 'bg-emerald-500/20 text-emerald-400' : 'bg-white/5 text-slate-400'}`}>
                      <Icon className="w-5 h-5" />
                    </div>
                  </div>

                  <p className="text-slate-400 text-xs h-10 mb-6">{tier.tagline}</p>

                  {/* Price */}
                  <div className="mb-6 flex items-baseline gap-2">
                    <span className="text-4xl font-extrabold text-white font-mono">
                      ${price}
                    </span>
                    {tier.priceMonthly > 0 ? (
                      <span className="text-slate-400 text-xs">/month</span>
                    ) : (
                      <span className="text-emerald-400 text-xs font-semibold">Free Forever</span>
                    )}

                    {promoApplied && tier.priceMonthly > 0 && (
                      <span className="line-through text-slate-500 text-sm font-mono ml-1">
                        ${originalPrice}
                      </span>
                    )}
                  </div>

                  {/* Feature List */}
                  <ul className="space-y-3 mb-8 text-xs text-slate-300 border-t border-white/10 pt-6">
                    {tier.features.map((feat, fIdx) => (
                      <li key={fIdx} className="flex items-start gap-2.5">
                        <Check className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                        <span>{feat}</span>
                      </li>
                    ))}
                  </ul>
                </div>

                {/* Stripe Checkout CTA Button */}
                <button
                  onClick={() => handleStripeCheckout(tier)}
                  disabled={loadingTier === tier.id}
                  className={`w-full py-3 px-4 rounded-xl font-bold text-sm transition-all duration-200 flex items-center justify-center gap-2 shadow-lg ${
                    tier.highlighted
                      ? 'bg-emerald-500 hover:bg-emerald-400 text-black shadow-emerald-500/25 hover:scale-[1.02]'
                      : 'bg-white/10 hover:bg-white/15 text-white border border-white/10'
                  }`}
                >
                  {loadingTier === tier.id ? (
                    <span className="animate-spin text-lg">⏳</span>
                  ) : (
                    <>
                      <span>{tier.cta}</span>
                      <span className="text-xs opacity-75">→</span>
                    </>
                  )}
                </button>
              </motion.div>
            );
          })}
        </div>

        {/* Enterprise Security Guarantee Footer */}
        <div className="mt-14 text-center text-xs text-slate-500 flex flex-wrap items-center justify-center gap-6">
          <span>🔒 256-bit SSL Stripe Checkout</span>
          <span>&bull;</span>
          <span>⚡ Instant License Key Activation</span>
          <span>&bull;</span>
          <span>🛡️ 14-Day Money Back Guarantee</span>
          <span>&bull;</span>
          <span>🏢 SOC2 Type II & GDPR Compliant</span>
        </div>
      </div>
    </section>
  );
}
