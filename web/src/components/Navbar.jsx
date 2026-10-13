import React, { useState, useEffect } from 'react';
import { Shield, Github, Menu, X, ArrowRight } from 'lucide-react';

export default function Navbar() {
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleGitHubSignIn = () => {
    // Direct GitHub OAuth trigger or Registration redirect
    window.open('https://github.com/login/oauth/authorize', '_blank');
  };

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        isScrolled
          ? 'bg-[#07090e]/85 backdrop-blur-xl border-b border-white/10 shadow-lg shadow-black/40 py-3.5'
          : 'bg-transparent py-5'
      }`}
    >
      <div className="max-w-7xl mx-auto px-6 flex items-center justify-between">
        {/* Brand Logo */}
        <a href="#" className="flex items-center gap-3 group">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-500 to-cyan-400 flex items-center justify-center text-black font-extrabold shadow-lg shadow-emerald-500/25 group-hover:scale-105 transition-transform">
            <Shield className="w-5 h-5 fill-black" />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-white font-extrabold text-lg tracking-tight">
              API Security Scanner
            </span>
            <span className="hidden sm:inline-block bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 font-mono text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider">
              v1.2.0 Hybrid
            </span>
          </div>
        </a>

        {/* Desktop Navigation Links */}
        <nav className="hidden md:flex items-center gap-8 text-sm font-medium text-slate-300">
          <a href="#features" className="hover:text-emerald-400 transition">Features</a>
          <a href="#comparison" className="hover:text-emerald-400 transition">vs StackHawk</a>
          <a href="#playground" className="hover:text-emerald-400 transition">Interactive Demo</a>
          <a href="#pricing" className="hover:text-emerald-400 transition">Pricing</a>
          <a href="#roi" className="hover:text-emerald-400 transition">ROI Calculator</a>
        </nav>

        {/* Auth CTA & GitHub Registration */}
        <div className="hidden md:flex items-center gap-3">
          <button
            onClick={handleGitHubSignIn}
            className="relative group p-[1px] rounded-xl overflow-hidden focus:outline-none"
          >
            {/* Glowing animated border gradient */}
            <span className="absolute inset-0 bg-gradient-to-r from-emerald-500 via-cyan-400 to-indigo-500 rounded-xl animate-pulse-glow" />
            
            <div className="relative px-4 py-2 bg-[#090d16] rounded-xl flex items-center gap-2 text-white text-xs font-semibold hover:bg-[#0f1422] transition-colors">
              <Github className="w-4 h-4 text-emerald-400" />
              <span>Sign in with GitHub</span>
              <ArrowRight className="w-3.5 h-3.5 text-slate-400 group-hover:translate-x-0.5 transition-transform" />
            </div>
          </button>
        </div>

        {/* Mobile Menu Button */}
        <button
          onClick={() => setMobileOpen(!mobileOpen)}
          className="md:hidden text-slate-300 hover:text-white p-2"
        >
          {mobileOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>
      </div>

      {/* Mobile Drawer */}
      {mobileOpen && (
        <div className="md:hidden bg-[#07090e]/95 backdrop-blur-2xl border-b border-slate-800 px-6 py-6 space-y-4">
          <a
            href="#features"
            onClick={() => setMobileOpen(false)}
            className="block text-slate-300 hover:text-emerald-400 text-base font-medium"
          >
            Killer Features
          </a>
          <a
            href="#comparison"
            onClick={() => setMobileOpen(false)}
            className="block text-slate-300 hover:text-emerald-400 text-base font-medium"
          >
            vs StackHawk
          </a>
          <a
            href="#playground"
            onClick={() => setMobileOpen(false)}
            className="block text-slate-300 hover:text-emerald-400 text-base font-medium"
          >
            Interactive Demo
          </a>
          <a
            href="#pricing"
            onClick={() => setMobileOpen(false)}
            className="block text-slate-300 hover:text-emerald-400 text-base font-medium"
          >
            Pricing & Early Bird
          </a>
          <a
            href="#roi"
            onClick={() => setMobileOpen(false)}
            className="block text-slate-300 hover:text-emerald-400 text-base font-medium"
          >
            ROI Calculator
          </a>

          <div className="pt-4 border-t border-slate-800">
            <button
              onClick={handleGitHubSignIn}
              className="w-full py-2.5 bg-emerald-500 text-black font-bold text-sm rounded-xl flex items-center justify-center gap-2"
            >
              <Github className="w-4 h-4" />
              Sign in with GitHub
            </button>
          </div>
        </div>
      )}
    </header>
  );
}
