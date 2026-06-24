import React from 'react';

interface LogoProps {
  className?: string;
  size?: number;
  variant?: 'color' | 'white' | 'black';
}

/**
 * CryptoVault — Professional brand logo mark.
 *
 * Design language: Geometric hexagonal vault shield with an angular
 * unlock-key glyph cut through the center. The outer hex uses a
 * purple→indigo gradient (brand primary); the inner lock arc uses an
 * emerald accent stroke to signal "unlocked / active access".
 *
 * Suitable for: sidebar header, login page, favicon, app icon.
 */
export const Logo: React.FC<LogoProps> = ({ className, size = 32, variant = 'color' }) => {
  const isColor = variant === 'color';
  const iid = `cv-${size}`; // unique gradient id prefix per size to avoid SVG conflicts

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-label="CryptoVault"
    >
      <defs>
        {/* Main hex fill gradient — deep purple to indigo */}
        <linearGradient id={`${iid}-hex`} x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor={isColor ? '#5B21B6' : variant === 'white' ? '#fff' : '#000'} />
          <stop offset="100%" stopColor={isColor ? '#3730A3' : variant === 'white' ? '#fff' : '#000'} />
        </linearGradient>

        {/* Hex border gradient — brighter purple to violet */}
        <linearGradient id={`${iid}-border`} x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor={isColor ? '#8B5CF6' : variant === 'white' ? '#fff' : '#333'} />
          <stop offset="100%" stopColor={isColor ? '#6366F1' : variant === 'white' ? '#ccc' : '#555'} />
        </linearGradient>

        {/* Glow filter for the emerald accent */}
        <filter id={`${iid}-glow`} x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation="2.5" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>

        {/* Subtle drop shadow on the whole mark */}
        <filter id={`${iid}-shadow`} x="-10%" y="-10%" width="120%" height="120%">
          <feDropShadow dx="0" dy="4" stdDeviation="6" floodColor="#7C3AED" floodOpacity={isColor ? '0.35' : '0'} />
        </filter>
      </defs>

      {/* ── Outer hexagon ── */}
      {/*
        Regular flat-top hexagon with 6 vertices at angles 0°,60°,120°,180°,240°,300°
        Center (50,50), radius 46 → vertices:
          (96,50),(73,90),(27,90),(4,50),(27,10),(73,10)
        We round inwards by 2px for a slightly softened premium feel.
      */}
      <path
        d="M 73 8 L 96 50 L 73 92 L 27 92 L 4 50 L 27 8 Z"
        fill={`url(#${iid}-hex)`}
        stroke={`url(#${iid}-border)`}
        strokeWidth="2.5"
        filter={`url(#${iid}-shadow)`}
      />

      {/*
        ── Inner facet lines (give a "cut gem / vault door" appearance) ──
        Subtle inner hex at 60% scale + diagonal cross-lines
      */}
      <path
        d="M 65.8 18.4 L 81.6 50 L 65.8 81.6 L 34.2 81.6 L 18.4 50 L 34.2 18.4 Z"
        fill="none"
        stroke={isColor ? 'rgba(139,92,246,0.18)' : 'transparent'}
        strokeWidth="1"
      />

      {/*
        ── Shackle (arc of the padlock) ──
        An open-arc sitting above centre — the "unlocked" position
        signals open access / active vault.
        Arc center (50, 48), radius 15, from 210° to 330° (top arc, open on right)
      */}
      <path
        d="M 37 46 C 37 32, 63 32, 63 46"
        stroke={isColor ? '#10B981' : variant === 'white' ? '#fff' : '#444'}
        strokeWidth="5.5"
        strokeLinecap="round"
        fill="none"
        filter={isColor ? `url(#${iid}-glow)` : 'none'}
      />

      {/*
        ── Lock body ──
        Rounded rectangle centered around (50,57)
      */}
      <rect
        x="33"
        y="48"
        width="34"
        height="26"
        rx="5"
        ry="5"
        fill={isColor ? '#7C3AED' : variant === 'white' ? '#fff' : '#222'}
        stroke={isColor ? '#A78BFA' : variant === 'white' ? '#ddd' : '#555'}
        strokeWidth="1.5"
      />

      {/*
        ── Keyhole ──
        Circle + narrow downward triangle forming a classic keyhole
      */}
      <circle
        cx="50"
        cy="57"
        r="5"
        fill={isColor ? '#1E1B4B' : variant === 'white' ? '#888' : '#000'}
      />
      <path
        d="M 47.5 60.5 L 52.5 60.5 L 51 68 L 49 68 Z"
        fill={isColor ? '#1E1B4B' : variant === 'white' ? '#888' : '#000'}
      />

      {/*
        ── Subtle corner tick marks (premium detail) ──
        Four small diagonal notches at hex corners for a "precision engineered" look
      */}
      {isColor && (
        <>
          <line x1="69" y1="12" x2="73" y2="8" stroke="#6366F1" strokeWidth="1.5" strokeLinecap="round" opacity="0.5" />
          <line x1="31" y1="12" x2="27" y2="8" stroke="#6366F1" strokeWidth="1.5" strokeLinecap="round" opacity="0.5" />
          <line x1="69" y1="88" x2="73" y2="92" stroke="#6366F1" strokeWidth="1.5" strokeLinecap="round" opacity="0.5" />
          <line x1="31" y1="88" x2="27" y2="92" stroke="#6366F1" strokeWidth="1.5" strokeLinecap="round" opacity="0.5" />
        </>
      )}
    </svg>
  );
};

export const FaviconLogo: React.FC = () => <Logo size={16} />;

export const AppIconLogo: React.FC = () => (
  <div className="w-16 h-16 rounded-[22px] bg-[#0E0E11] border border-zinc-800/80 flex items-center justify-center shadow-xl shadow-black/40">
    <Logo size={44} />
  </div>
);

interface HeaderLogoProps {
  className?: string;
  size?: number;
}

export const HeaderLogo: React.FC<HeaderLogoProps> = ({ className, size = 28 }) => {
  return (
    <div className={`flex items-center space-x-2.5 ${className ?? ''}`}>
      {/* Logo mark in a subtle frosted container */}
      <div className="flex items-center justify-center w-9 h-9 rounded-xl bg-gradient-to-br from-violet-950/80 to-indigo-950/80 border border-violet-800/40 shadow-lg shadow-violet-900/20">
        <Logo size={size} />
      </div>
      <div className="flex flex-col leading-none">
        <span className="font-black text-[15px] tracking-tight text-white select-none">
          Crypto<span className="text-violet-400">Vault</span>
        </span>
        <span className="text-[8px] text-zinc-500 font-bold tracking-widest uppercase select-none">
          Secure · Digital · Finance
        </span>
      </div>
    </div>
  );
};

export default Logo;
