import React from 'react';
import Navbar from './components/Navbar';
import Hero3D from './components/Hero3D';
import TerminalSim from './components/TerminalSim';
import SocialProof from './components/SocialProof';
import Features from './components/Features';
import BenchmarkTable from './components/BenchmarkTable';
import Pricing from './components/Pricing';
import RoiCalculator from './components/RoiCalculator';
import { ArrowRight, Shield, Terminal, Zap } from 'lucide-react';

export default function App() {
  return (
    <div className="min-h-screen bg-[#07090e] text-white selection:bg-emerald-500 selection:text-black font-sans relative">
      <Navbar />

      {/* Hero Section */}
      <section className="relative pt-36 pb-20 md:pt-44 md:pb-28 overflow-hidden">
        {/* 3D WebGL Canvas */}
        <Hero3D />

        <div className="max-w-7xl mx-auto px-6 relative z-10 text-center">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 font-mono text-xs font-semibold mb-8 shadow-lg shadow-emerald-500/10">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            Next-Gen DAST + SAST Hybrid Engine &bull; Zero-Click Auto-PR
          </div>

          {/* Heading */}
          <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight max-w-5xl mx-auto leading-[1.1] mb-6">
            Stop Reading Reports.{' '}
            <span className="bg-gradient-to-r from-emerald-400 via-teal-300 to-cyan-400 bg-clip-text text-transparent">
              Auto-Fix APIs
            </span>{' '}
            Directly in CI/CD.
          </h1>

          {/* Subtitle */}
          <p className="text-slate-400 text-lg md:text-xl max-w-3xl mx-auto font-normal leading-relaxed mb-10">
            The only security engine that bridges runtime DAST fuzzing with exact Java source code mapping. 
            Discovers Shadow APIs, confirms stateful BOLA/IDOR, tests GenAI agents, and opens ready-to-merge Pull Requests.
          </p>

          {/* CTAs */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-16">
            <a
              href="#pricing"
              className="w-full sm:w-auto px-8 py-4 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-black font-extrabold text-base transition-all duration-200 shadow-xl shadow-emerald-500/25 flex items-center justify-center gap-2 hover:scale-[1.02]"
            >
              <span>Get Started Free</span>
              <ArrowRight className="w-4 h-4" />
            </a>

            <a
              href="#terminal"
              className="w-full sm:w-auto px-8 py-4 rounded-xl bg-white/5 hover:bg-white/10 text-white font-semibold text-base border border-white/10 transition-all duration-200 flex items-center justify-center gap-2"
            >
              <Terminal className="w-4 h-4 text-emerald-400" />
              <span>Watch Live Scan</span>
            </a>
          </div>

          {/* Interactive Terminal Component */}
          <div id="terminal" className="pt-6">
            <TerminalSim />
          </div>
        </div>
      </section>

      {/* Social Proof Marquee & Testimonials */}
      <SocialProof />

      {/* 5 Killer Features */}
      <Features />

      {/* Head-to-Head Benchmark Table */}
      <BenchmarkTable />

      {/* Pricing Section with Early Bird Discount for First 10 Customers */}
      <Pricing />

      {/* Executive ROI Calculator */}
      <RoiCalculator />

      {/* Final CTA Banner */}
      <section className="py-20 bg-gradient-to-t from-[#0b101b] to-[#07090e] border-t border-white/5 text-center relative overflow-hidden">
        <div className="max-w-4xl mx-auto px-6 relative z-10">
          <h2 className="text-4xl md:text-5xl font-extrabold text-white tracking-tight mb-4">
            Ready to Automate Your API Security?
          </h2>
          <p className="text-slate-400 text-lg mb-8 max-w-2xl mx-auto">
            Integrate with GitHub Actions in 60 seconds. Zero runtime dependencies, 100% air-gapped readiness.
          </p>
          <a
            href="#pricing"
            className="inline-flex items-center gap-2 px-8 py-4 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-black font-extrabold text-base shadow-xl shadow-emerald-500/20 transition-all hover:scale-105"
          >
            Claim 50% Early Bird Pass
            <ArrowRight className="w-4 h-4" />
          </a>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 bg-[#05070b] border-t border-slate-900 text-slate-500 text-xs font-mono">
        <div className="max-w-7xl mx-auto px-6 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-slate-300 font-bold">
            <Shield className="w-4 h-4 text-emerald-400" />
            <span>API Security Scanner</span>
          </div>
          <div>Built for DevSecOps teams shipping high-velocity microservices.</div>
          <div>Java 17 &bull; SARIF 2.1.0 &bull; Apache 2.0</div>
        </div>
      </footer>
    </div>
  );
}
