import React from 'react';
import { motion } from 'framer-motion';
import { Check, X, AlertTriangle } from 'lucide-react';

const MATRIX = [
  {
    feature: 'Zero-Click AI PR Auto-Fix',
    sub: 'Creates branch & ready-to-merge PR with code patch',
    us: true,
    stackhawk: false,
    snyk: 'partial',
    salt: false,
  },
  {
    feature: 'Hybrid DAST + SAST Line Mapping',
    sub: 'Pins runtime HTTP flaws to exact controller source line',
    us: true,
    stackhawk: false,
    snyk: false,
    salt: false,
  },
  {
    feature: 'Shadow & Zombie API Discovery',
    sub: 'Detects OpenAPI spec drift against Java source routes',
    us: true,
    stackhawk: false,
    snyk: false,
    salt: 'partial',
  },
  {
    feature: '100% Air-Gapped / SOC2 Mode',
    sub: 'Zero external network calls with SHA-256 integrity hash',
    us: true,
    stackhawk: false,
    snyk: false,
    salt: 'partial',
  },
  {
    feature: 'Stateful BOLA Multi-Role Matrix',
    sub: 'User A creates resource -> ID extracted -> User B exploited',
    us: true,
    stackhawk: 'partial',
    snyk: false,
    salt: true,
  },
  {
    feature: 'LLM & GenAI Gateway Fuzzing',
    sub: 'System prompt leakage & autonomous agent guardrail bypass',
    us: true,
    stackhawk: false,
    snyk: false,
    salt: false,
  },
  {
    feature: 'Standard SARIF 2.1.0 & Zero Runtime Bloat',
    sub: 'Direct GitHub Security code scanning integration',
    us: true,
    stackhawk: true,
    snyk: true,
    salt: false,
  },
];

export default function BenchmarkTable() {
  const renderIcon = (val) => {
    if (val === true) return <span className="inline-flex items-center gap-1 text-emerald-400 font-bold text-xs"><Check className="w-4 h-4 text-emerald-400" /> Yes</span>;
    if (val === false) return <span className="inline-flex items-center gap-1 text-slate-500 font-medium text-xs"><X className="w-4 h-4 text-slate-500" /> No</span>;
    return <span className="inline-flex items-center gap-1 text-amber-400 font-semibold text-xs"><AlertTriangle className="w-3.5 h-3.5 text-amber-400" /> Partial</span>;
  };

  return (
    <section id="comparison" className="py-24 bg-[#07090e] border-t border-white/5 relative">
      <div className="max-w-7xl mx-auto px-6">
        <motion.div
          initial={{ opacity: 0, y: 50 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center max-w-3xl mx-auto mb-16"
        >
          <span className="text-emerald-400 font-mono text-xs uppercase tracking-wider font-semibold">
            Competitive Benchmark
          </span>
          <h2 className="text-4xl md:text-5xl font-extrabold text-white mt-3 tracking-tight">
            Head-to-Head: Why We Win Every RFP
          </h2>
          <p className="text-slate-400 text-lg mt-4">
            Compare our capabilities against traditional DAST (StackHawk), SAST (Snyk), and Runtime WAF (Salt).
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 40 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="rounded-2xl border border-white/10 bg-[#0f1422]/70 backdrop-blur-xl overflow-x-auto shadow-2xl"
        >
          <table className="w-full text-left border-collapse min-w-[720px]">
            <thead>
              <tr className="border-b border-white/10 bg-white/[0.02]">
                <th className="p-5 text-sm font-semibold text-slate-300">Capability / Dimension</th>
                <th className="p-5 text-sm font-bold text-emerald-400 bg-emerald-500/10 border-t-2 border-emerald-400">
                  Our Scanner 🛡️
                </th>
                <th className="p-5 text-sm font-semibold text-slate-300">StackHawk</th>
                <th className="p-5 text-sm font-semibold text-slate-300">Snyk API</th>
                <th className="p-5 text-sm font-semibold text-slate-300">Salt Security</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 text-sm">
              {MATRIX.map((row, idx) => (
                <tr key={idx} className="hover:bg-white/[0.02] transition-colors">
                  <td className="p-5">
                    <div className="font-semibold text-white">{row.feature}</div>
                    <div className="text-xs text-slate-400 mt-0.5">{row.sub}</div>
                  </td>
                  <td className="p-5 bg-emerald-500/[0.04] font-semibold">
                    {renderIcon(row.us)}
                  </td>
                  <td className="p-5">{renderIcon(row.stackhawk)}</td>
                  <td className="p-5">{renderIcon(row.snyk)}</td>
                  <td className="p-5">{renderIcon(row.salt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </motion.div>
      </div>
    </section>
  );
}
