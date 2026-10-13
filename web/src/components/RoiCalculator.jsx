import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { DollarSign, Clock, Users, ArrowUpRight } from 'lucide-react';

export default function RoiCalculator() {
  const [devs, setDevs] = useState(30);
  const [endpoints, setEndpoints] = useState(50);

  // Calculation parameters
  // Average findings per 10 endpoints: ~3 findings/month
  // Manual triage, reproduce curl, patch writing, review: 4.5 hours per finding
  // Developer blended rate: $80/hr
  // Zero-Click PR cuts triage & patch writing time by 75%
  const estimatedFindings = Math.round(endpoints * 0.35);
  const manualHours = estimatedFindings * 4.5;
  const hoursSavedPerMonth = Math.round(manualHours * 0.75 + devs * 2);
  const annualHoursSaved = hoursSavedPerMonth * 12;
  const annualDollarSavings = annualHoursSaved * 80;

  return (
    <section id="roi" className="py-24 bg-[#07090e] border-t border-white/5 relative">
      <div className="max-w-7xl mx-auto px-6">
        <motion.div
          initial={{ opacity: 0, y: 50 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center max-w-3xl mx-auto mb-16"
        >
          <span className="text-emerald-400 font-mono text-xs uppercase tracking-wider font-semibold">
            Executive Business Case
          </span>
          <h2 className="text-4xl md:text-5xl font-extrabold text-white mt-3 tracking-tight">
            Calculate Your Engineering ROI
          </h2>
          <p className="text-slate-400 text-lg mt-4">
            See how automating vulnerability triage and pull request generation saves hundreds of engineering hours every sprint.
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 40 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="max-w-4xl mx-auto rounded-3xl p-8 md:p-12 bg-gradient-to-br from-[#0f172a] via-[#090e1a] to-[#07090e] border border-emerald-500/30 shadow-2xl shadow-emerald-950/40"
        >
          <div className="grid grid-cols-1 md:grid-cols-2 gap-10 items-center">
            {/* Sliders Area */}
            <div className="space-y-8">
              <div>
                <div className="flex justify-between text-sm font-semibold text-white mb-2">
                  <span className="flex items-center gap-2">
                    <Users className="w-4 h-4 text-emerald-400" />
                    Engineering Team Size
                  </span>
                  <span className="text-emerald-400 font-mono font-bold text-base">
                    {devs} Developers
                  </span>
                </div>
                <input
                  type="range"
                  min="5"
                  max="250"
                  value={devs}
                  onChange={(e) => setDevs(parseInt(e.target.value, 10))}
                  className="w-full accent-emerald-400 cursor-pointer h-2 bg-slate-800 rounded-lg"
                />
              </div>

              <div>
                <div className="flex justify-between text-sm font-semibold text-white mb-2">
                  <span className="flex items-center gap-2">
                    <Clock className="w-4 h-4 text-cyan-400" />
                    Active API Endpoints
                  </span>
                  <span className="text-cyan-400 font-mono font-bold text-base">
                    {endpoints} Routes
                  </span>
                </div>
                <input
                  type="range"
                  min="10"
                  max="300"
                  value={endpoints}
                  onChange={(e) => setEndpoints(parseInt(e.target.value, 10))}
                  className="w-full accent-cyan-400 cursor-pointer h-2 bg-slate-800 rounded-lg"
                />
              </div>

              <p className="text-xs text-slate-500 leading-relaxed">
                *Model based on industry metrics: 4.5 engineering hours spent per manual vulnerability confirmation, 
                reproduction, patch authoring, and review cycle.
              </p>
            </div>

            {/* Big Results Box */}
            <div className="p-8 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-center flex flex-col justify-center items-center">
              <span className="text-xs font-mono uppercase tracking-wider text-emerald-400 font-bold mb-2">
                Estimated Annual Savings
              </span>
              <div className="text-5xl md:text-6xl font-extrabold text-white font-mono tracking-tight text-emerald-400 drop-shadow-[0_0_25px_rgba(16,185,129,0.4)]">
                ${annualDollarSavings.toLocaleString()}
              </div>

              <div className="text-slate-300 font-medium text-sm mt-3 flex items-center gap-1.5">
                <Clock className="w-4 h-4 text-emerald-400" />
                <span>{hoursSavedPerMonth.toLocaleString()} dev hours saved / month</span>
              </div>

              <div className="mt-6 inline-flex items-center gap-1 text-xs font-bold text-emerald-400 bg-emerald-500/20 px-3 py-1.5 rounded-full border border-emerald-500/30">
                <span>Instant 14x ROI vs StackHawk</span>
                <ArrowUpRight className="w-3.5 h-3.5" />
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
