import React from 'react';
import { motion } from 'framer-motion';
import { Star, ShieldCheck, Quote } from 'lucide-react';

const LOGOS = [
  'DATADOG', 'STRIPE', 'VERCEL', 'GITLAB', 'SNYK', 'HASHICORP', 'CLOUDFLARE', 'GITHUB', 'POSTMAN', 'KUBERNETES'
];

const TESTIMONIALS = [
  {
    name: 'Marcus Vance',
    role: 'Principal DevSecOps Architect',
    company: 'FinScale Cloud',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=80',
    content:
      'We spent 15 hours every sprint arguing over StackHawk false positives. This scanner maps the exact Java controller line and creates the pull request with @PreAuthorize already written. Our vulnerability resolution time dropped by 78% in the first week.',
    rating: 5,
    tag: 'Verified Enterprise Customer',
  },
  {
    name: 'Sarah Chen',
    role: 'VP of Platform Engineering',
    company: 'AeroPay Global',
    avatar: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=120&auto=format&fit=crop&q=80',
    content:
      'The --air-gapped mode was the game changer for our SOC2 audit. No code or OpenAPI specs leave our internal VPC. Plus, the Multi-Step BOLA engine caught a critical tenant-isolation flaw that passed our manual penetration test.',
    rating: 5,
    tag: 'FinTech Banking Core',
  },
  {
    name: 'David Lindqvist',
    role: 'Lead Security Engineer',
    company: 'Nordic AI Systems',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&auto=format&fit=crop&q=80',
    content:
      'We are deploying GenAI agent endpoints rapidly. This is the first DAST scanner that natively tests LLM system prompt leakage and prompt injection alongside traditional OWASP API Top 10 vulnerabilities.',
    rating: 5,
    tag: 'GenAI & Autonomous Agents',
  },
];

export default function SocialProof() {
  return (
    <section className="py-20 bg-[#07090e] border-y border-white/5 relative overflow-hidden">
      {/* Logos Ticker Header */}
      <div className="max-w-7xl mx-auto px-6 mb-8 text-center">
        <p className="text-xs font-mono uppercase tracking-widest text-slate-500">
          Trusted by DevSecOps Teams Shipping 10,000+ API Endpoints Daily
        </p>
      </div>

      {/* Infinite Logo Marquee */}
      <div className="relative w-full overflow-hidden mb-20 flex">
        {/* Left & Right Fade Gradients */}
        <div className="absolute left-0 top-0 bottom-0 w-24 bg-gradient-to-r from-[#07090e] to-transparent z-10 pointer-events-none" />
        <div className="absolute right-0 top-0 bottom-0 w-24 bg-gradient-to-l from-[#07090e] to-transparent z-10 pointer-events-none" />

        <div className="flex shrink-0 animate-marquee gap-14 items-center">
          {[...LOGOS, ...LOGOS].map((logo, idx) => (
            <div
              key={idx}
              className="text-slate-600 hover:text-slate-400 font-extrabold tracking-wider text-lg font-mono transition-colors cursor-default whitespace-nowrap"
            >
              {logo}
            </div>
          ))}
        </div>
      </div>

      {/* Wall of Love Testimonial Grid */}
      <div className="max-w-7xl mx-auto px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center max-w-2xl mx-auto mb-14"
        >
          <span className="text-emerald-400 font-mono text-xs uppercase tracking-wider font-semibold">
            Wall of Love &bull; Customer Reviews
          </span>
          <h2 className="text-3xl font-extrabold text-white mt-2">
            Why Engineers Love Zero-Click Auto-Fix
          </h2>
        </motion.div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {TESTIMONIALS.map((t, idx) => (
            <motion.div
              key={idx}
              initial={{ opacity: 0, y: 40 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: idx * 0.15 }}
              className="rounded-2xl p-6 bg-[#0f1422]/70 border border-white/10 hover:border-emerald-500/40 backdrop-blur-md transition-all duration-300 flex flex-col justify-between hover:-translate-y-1 shadow-lg"
            >
              <div>
                <div className="flex items-center justify-between mb-4">
                  <div className="flex gap-1 text-amber-400">
                    {[...Array(t.rating)].map((_, i) => (
                      <Star key={i} className="w-4 h-4 fill-amber-400" />
                    ))}
                  </div>
                  <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-2.5 py-0.5 rounded-full border border-emerald-500/20">
                    {t.tag}
                  </span>
                </div>

                <Quote className="w-6 h-6 text-slate-600 mb-2" />
                <p className="text-slate-300 text-sm leading-relaxed mb-6">
                  "{t.content}"
                </p>
              </div>

              <div className="flex items-center gap-3 pt-4 border-t border-white/5">
                <img
                  src={t.avatar}
                  alt={t.name}
                  className="w-10 h-10 rounded-full object-cover border border-emerald-500/40"
                />
                <div>
                  <h4 className="text-white font-semibold text-sm">{t.name}</h4>
                  <p className="text-slate-400 text-xs">{t.role} &bull; {t.company}</p>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
