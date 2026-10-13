import React from 'react';
import { motion } from 'framer-motion';
import { Bot, Search, Lock, RefreshCw, Cpu } from 'lucide-react';

const KILLER_FEATURES = [
  {
    id: 'auto-pr',
    icon: Bot,
    tag: 'Feature #1 • Highest Engineering Value',
    title: 'Zero-Click AI PR Auto-Fix',
    desc: 'StackHawk and Snyk give developers long reports and tell them to figure out the fix. Our hybrid scanner connects runtime faults to the exact Java file and line number, branches out security/auto-fix-..., applies the patch, and opens a ready-to-merge GitHub Pull Request.',
    codeDiff: [
      { type: 'del', text: '- @GetMapping("/orders/{id}")' },
      { type: 'add', text: '+ // [AUTO-FIX: STATEFUL_BOLA_CONFIRMED]' },
      { type: 'add', text: '+ @PreAuthorize("@securityService.isResourceOwner(authentication, #id)")' },
      { type: 'add', text: '+ @GetMapping("/orders/{id}")' },
    ],
    featured: true,
  },
  {
    id: 'shadow-api',
    icon: Search,
    tag: 'Feature #2 • Spec Drift',
    title: 'Shadow & Zombie API Discovery',
    desc: 'Compares Java Spring & JAX-RS controller route mappings with your OpenAPI JSON. Instantly detects undocumented debug routes (SHADOW_API_EXPOSED) and abandoned gateway endpoints (ZOMBIE_API_ENDPOINT).',
    featured: false,
  },
  {
    id: 'air-gapped',
    icon: Lock,
    tag: 'Feature #3 • SOC2 & GDPR',
    title: '100% Air-Gapped & Zero Data Egress',
    desc: 'Runs completely offline on self-hosted runners. Guarantees zero telemetry or third-party cloud data transmission. Automatically generates cryptographic SHA-256 audit attestations for enterprise compliance.',
    featured: false,
  },
  {
    id: 'stateful-bola',
    icon: RefreshCw,
    tag: 'Feature #4 • Real Exploiter',
    title: 'Stateful BOLA / IDOR Verification',
    desc: 'Simulates authentic multi-step attacks: User A creates a resource via POST, extracts the returned ID, and User B immediately tests unauthorized modification/deletion. Confirms true positive privilege escalations.',
    featured: false,
  },
  {
    id: 'llm-security',
    icon: Cpu,
    tag: 'Feature #5 • OWASP GenAI Top 10',
    title: 'LLM & AI Agent API Gateways',
    desc: 'Custom security scanner for /chat, /completions, and AI agent action endpoints. Fuzzes for prompt injection escapes, system prompt extraction, and autonomous agent guardrail bypasses.',
    featured: false,
  },
];

export default function Features() {
  return (
    <section id="features" className="py-24 bg-[#07090e] relative overflow-hidden">
      <div className="max-w-7xl mx-auto px-6">
        <motion.div
          initial={{ opacity: 0, y: 50 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5 }}
          className="text-center max-w-3xl mx-auto mb-16"
        >
          <span className="text-emerald-400 font-mono text-xs uppercase tracking-wider font-semibold">
            Competitive Edge
          </span>
          <h2 className="text-4xl md:text-5xl font-extrabold text-white mt-3 tracking-tight">
            5 "Killer" Features StackHawk & Snyk Lack
          </h2>
          <p className="text-slate-400 text-lg mt-4">
            Built from the ground up to eliminate developer report fatigue, catch undocumented shadow APIs, 
            and enforce zero-leakage enterprise governance.
          </p>
        </motion.div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {KILLER_FEATURES.map((feat, idx) => {
            const Icon = feat.icon;
            return (
              <motion.div
                key={feat.id}
                initial={{ opacity: 0, y: 50 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: idx * 0.1 }}
                className={`rounded-2xl p-7 bg-[#0f1422]/80 border transition-all duration-300 flex flex-col justify-between ${
                  feat.featured
                    ? 'md:col-span-2 bg-gradient-to-br from-[#121b2d] to-[#0c121e] border-indigo-500/40 shadow-xl shadow-indigo-500/10'
                    : 'border-white/10 hover:border-emerald-500/40 hover:-translate-y-1'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-emerald-400">
                      <Icon className="w-6 h-6" />
                    </div>
                    <span className="text-[11px] font-mono font-semibold px-2.5 py-1 rounded-full bg-white/5 border border-white/10 text-slate-300">
                      {feat.tag}
                    </span>
                  </div>

                  <h3 className="text-xl font-bold text-white mb-3">{feat.title}</h3>
                  <p className="text-slate-400 text-sm leading-relaxed mb-6">{feat.desc}</p>
                </div>

                {feat.codeDiff && (
                  <div className="rounded-xl bg-[#06090f] p-4 border border-slate-800 font-mono text-xs overflow-x-auto space-y-1">
                    <div className="text-slate-500 text-[10px] uppercase font-bold mb-2">Automated Code Patch:</div>
                    {feat.codeDiff.map((d, dIdx) => (
                      <div
                        key={dIdx}
                        className={
                          d.type === 'add'
                            ? 'text-emerald-400 bg-emerald-950/30 px-2 py-0.5 rounded border-l-2 border-emerald-400'
                            : 'text-rose-400 bg-rose-950/20 px-2 py-0.5 rounded border-l-2 border-rose-500 line-through opacity-80'
                        }
                      >
                        {d.text}
                      </div>
                    ))}
                  </div>
                )}
              </motion.div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
