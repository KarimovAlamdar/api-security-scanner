import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Terminal as TerminalIcon, Play, CheckCircle2, AlertTriangle, ShieldCheck, GitPullRequest } from 'lucide-react';

const SCRIPT_STEPS = [
  { type: 'cmd', text: 'api-security-scanner --spec openapi.json --auto-pr --air-gapped' },
  { type: 'info', text: '🛡️ API Security Scanner v1.2.0 (DAST + SAST Hybrid Engine)' },
  { type: 'info', text: 'Target: https://api.store.internal | Spec: openapi.json (28 endpoints)' },
  { type: 'airgap', text: '🔒 100% Air-Gapped Mode: ACTIVE (Audit SHA-256: 8f4c9a2e... Zero external egress)' },
  { type: 'info', text: 'Analyzing Spring Controllers in ./src (34 routes mapped)...' },
  { type: 'warning', text: '⚠️ [SPEC DRIFT] SHADOW_API_EXPOSED: /admin/metrics-dump in MetricsController.java:29' },
  { type: 'info', text: 'Executing Multi-Step Stateful BOLA Matrix (User A -> ID Extraction -> User B Exploit)...' },
  { type: 'crit', text: '🚨 [CRITICAL VULNERABILITY FOUND] STATEFUL_BOLA_CONFIRMED on /api/v1/orders/{id}' },
  { type: 'source', text: '📍 Exact Source Location: OrderController.java:84 (HTTP DELETE unauthorized bypass)' },
  { type: 'fix', text: '⚡ [ZERO-CLICK AUTO-FIX APPLIED] Code Patch inserted into OrderController.java' },
  { type: 'pr', text: '🚀 Pull Request #142 opened: "🛡️ Security Auto-Fix: STATEFUL_BOLA_CONFIRMED"' },
  { type: 'done', text: '✅ Pipeline finished in 1.42s (1 Critical, 1 High, 1 Auto-PR ready to merge)' },
];

export default function TerminalSim() {
  const [lines, setLines] = useState([]);
  const [stepIndex, setStepIndex] = useState(0);
  const [typedCommand, setTypedCommand] = useState('');
  const [isRunning, setIsRunning] = useState(true);

  // Typing effect for the first command
  useEffect(() => {
    const fullCmd = SCRIPT_STEPS[0].text;
    let charIdx = 0;
    setTypedCommand('');

    const typeTimer = setInterval(() => {
      if (charIdx <= fullCmd.length) {
        setTypedCommand(fullCmd.slice(0, charIdx));
        charIdx++;
      } else {
        clearInterval(typeTimer);
        // Start streaming subsequent log lines
        setStepIndex(1);
      }
    }, 35);

    return () => clearInterval(typeTimer);
  }, [isRunning]);

  // Streaming remaining log lines
  useEffect(() => {
    if (stepIndex > 0 && stepIndex < SCRIPT_STEPS.length) {
      const timer = setTimeout(() => {
        setLines((prev) => [...prev, SCRIPT_STEPS[stepIndex]]);
        setStepIndex((curr) => curr + 1);
      }, 320);
      return () => clearTimeout(timer);
    }
  }, [stepIndex]);

  const handleRestart = () => {
    setLines([]);
    setStepIndex(0);
    setIsRunning((prev) => !prev);
  };

  return (
    <div className="w-full max-w-4xl mx-auto rounded-2xl overflow-hidden border border-slate-800 bg-[#06090f] shadow-2xl shadow-emerald-950/20">
      {/* Terminal Titlebar */}
      <div className="bg-[#0c121d] px-4 py-3 border-b border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded-full bg-rose-500/80" />
          <div className="w-3 h-3 rounded-full bg-amber-500/80" />
          <div className="w-3 h-3 rounded-full bg-emerald-500/80" />
          <span className="ml-2 font-mono text-xs text-slate-400 flex items-center gap-1.5">
            <TerminalIcon className="w-3.5 h-3.5 text-emerald-400" />
            api-security-scanner --auto-pr (CI/CD Runner)
          </span>
        </div>

        <button
          onClick={handleRestart}
          className="text-xs font-mono bg-white/5 hover:bg-white/10 text-slate-300 px-2.5 py-1 rounded border border-white/10 transition flex items-center gap-1.5"
        >
          <Play className="w-3 h-3 text-emerald-400" />
          Replay Scan
        </button>
      </div>

      {/* Terminal Output Screen */}
      <div className="p-6 font-mono text-xs md:text-sm leading-relaxed space-y-2.5 min-h-[360px] text-slate-300 overflow-y-auto">
        {/* Active Command Line */}
        <div className="flex items-center gap-2 text-sky-400 font-semibold">
          <span className="text-emerald-400">$</span>
          <span>{typedCommand}</span>
          <span className="w-2 h-4 bg-emerald-400 animate-pulse" />
        </div>

        {/* Streamed Lines */}
        {lines.map((line, idx) => (
          <motion.div
            key={idx}
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.2 }}
            className="flex items-start gap-2"
          >
            {line.type === 'crit' && (
              <span className="text-rose-400 font-bold bg-rose-950/40 px-2 py-0.5 rounded border border-rose-500/30 shadow-[0_0_15px_rgba(244,63,94,0.4)]">
                {line.text}
              </span>
            )}

            {line.type === 'fix' && (
              <span className="text-emerald-300 font-bold bg-emerald-950/40 px-2 py-0.5 rounded border border-emerald-500/40 shadow-[0_0_15px_rgba(16,185,129,0.35)] flex items-center gap-1">
                <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
                {line.text}
              </span>
            )}

            {line.type === 'pr' && (
              <span className="text-indigo-300 font-bold bg-indigo-950/40 px-2 py-0.5 rounded border border-indigo-500/30 flex items-center gap-1">
                <GitPullRequest className="w-4 h-4 text-indigo-400 shrink-0" />
                {line.text}
              </span>
            )}

            {line.type === 'warning' && (
              <span className="text-amber-400">{line.text}</span>
            )}

            {line.type === 'airgap' && (
              <span className="text-cyan-400 font-semibold">{line.text}</span>
            )}

            {line.type === 'source' && (
              <span className="text-slate-400 italic">{line.text}</span>
            )}

            {line.type === 'done' && (
              <span className="text-emerald-400 font-bold">{line.text}</span>
            )}

            {line.type === 'info' && (
              <span className="text-slate-400">{line.text}</span>
            )}
          </motion.div>
        ))}
      </div>
    </div>
  );
}
