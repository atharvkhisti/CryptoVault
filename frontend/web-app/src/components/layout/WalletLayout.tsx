import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { kycApi, type KycRecord } from '../../services/kycApi';
import { getKycBadge } from '../ui/Badge';
import { HeaderLogo, Logo } from '../ui/Logo';
import { 
  Home, 
  Coins, 
  Send, 
  Clock, 
  User,
  LogOut
} from 'lucide-react';
import { cn } from '../../utils/cn';
import { motion, AnimatePresence } from 'framer-motion';

interface WalletLayoutProps {
  children: React.ReactNode;
}

export const WalletLayout: React.FC<WalletLayoutProps> = ({ children }) => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [kycRecord, setKycRecord] = useState<KycRecord | null>(null);

  useEffect(() => {
    if (user?.id) {
      kycApi
        .getUserKycStatus(user.id)
        .then((res) => {
          if (res.success) {
            setKycRecord(res.data);
          }
        })
        .catch((err) => {
          console.error('KYC loading failed', err);
        });
    }
  }, [user]);

  const navItems = [
    { name: 'Home', href: '/', icon: Home },
    { name: 'Assets', href: '/assets', icon: Coins },
    { name: 'Transfer', href: '/transfer', icon: Send },
    { name: 'Activity', href: '/activity', icon: Clock },
    { name: 'Profile', href: '/profile', icon: User },
  ];

  const currentPath = location.pathname;

  const handleLogoutClick = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-[#09090B] text-[#FAFAFA] flex flex-col md:flex-row relative overflow-x-hidden font-sans">
      
      {/* Background radial glows for aesthetic depth */}
      <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-gradient-to-tr from-primary/5 to-secondary/5 rounded-full blur-[140px] pointer-events-none -z-10" />
      <div className="absolute bottom-0 left-0 w-[400px] h-[400px] bg-primary/5 rounded-full blur-[120px] pointer-events-none -z-10" />

      {/* 1. DESKTOP / TABLET SIDEBAR NAVIGATION */}
      <aside className="hidden md:flex flex-col w-64 border-r border-zinc-900 bg-zinc-950/40 backdrop-blur-xl shrink-0 h-screen sticky top-0 z-30 select-none">
        
        {/* Branding header */}
        <div className="h-16 flex items-center px-6 border-b border-zinc-900">
          <Link to="/">
            <HeaderLogo size={28} />
          </Link>
        </div>

        {/* Navigation list */}
        <nav className="flex-1 py-6 px-4 space-y-1.5">
          {navItems.map((item) => {
            const isActive = currentPath === item.href || (item.href !== '/' && currentPath.startsWith(item.href));
            const Icon = item.icon;
            return (
              <Link
                key={item.name}
                to={item.href}
                className={cn(
                  'flex items-center space-x-3.5 px-4 py-3 rounded-full text-sm font-semibold transition-all relative group',
                  isActive 
                    ? 'text-white bg-zinc-900/60 border border-zinc-800' 
                    : 'text-zinc-400 hover:text-white hover:bg-zinc-900/30'
                )}
              >
                {isActive && (
                  <motion.div
                    layoutId="desktop-active-dot"
                    className="absolute left-2 w-1.5 h-1.5 rounded-full bg-accent"
                    transition={{ type: 'spring', stiffness: 300, damping: 30 }}
                  />
                )}
                <Icon 
                  size={18} 
                  className={cn(
                    "transition-transform group-hover:scale-105",
                    isActive ? "text-primary" : "text-zinc-500 group-hover:text-zinc-400"
                  )} 
                />
                <span>{item.name}</span>
              </Link>
            );
          })}
        </nav>

        {/* Connected Net Status */}
        <div className="px-5 py-2.5 mx-4 mb-2 bg-[#121214]/60 border border-zinc-900 rounded-2xl flex items-center justify-between text-[10px]">
          <div className="flex items-center space-x-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-accent animate-pulse" />
            <span className="text-zinc-400 font-bold">Mainnet Gateway</span>
          </div>
          {kycRecord?.status && (
            <div className="scale-75 origin-right">
              {getKycBadge(kycRecord.status)}
            </div>
          )}
        </div>

        {/* User profile footer block */}
        <div className="p-4 border-t border-zinc-900 bg-zinc-950/20 flex flex-col space-y-3">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-primary to-secondary flex items-center justify-center text-xs font-bold text-white shadow shadow-primary/10">
              {user?.name?.slice(0, 2).toUpperCase() || 'US'}
            </div>
            <div className="flex-1 min-w-0 text-left">
              <span className="text-xs font-extrabold text-white truncate block">{user?.name}</span>
              <span className="text-[10px] text-zinc-500 font-semibold truncate block capitalize">{user?.role} Access</span>
            </div>
          </div>
          <button
            onClick={handleLogoutClick}
            className="w-full flex items-center justify-center space-x-2 py-2 px-3 rounded-full text-xs font-bold text-rose-400 hover:bg-rose-950/20 border border-transparent hover:border-rose-900/30 transition-all active:scale-[0.97]"
          >
            <LogOut size={14} />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* 2. MOBILE HEADER (md:hidden) */}
      <header className="md:hidden h-16 flex items-center justify-between px-5 border-b border-zinc-900 bg-zinc-950/40 backdrop-blur-xl z-30 sticky top-0 shrink-0 select-none">
        <Link to="/" className="flex items-center space-x-2">
          <Logo size={24} />
          <span className="font-extrabold text-sm tracking-tight text-white">CryptoVault</span>
        </Link>

        <div className="flex items-center space-x-3">
          {kycRecord?.status && (
            <div className="scale-80">
              {getKycBadge(kycRecord.status)}
            </div>
          )}
          <div 
            onClick={() => navigate('/profile')} 
            className="w-7 h-7 rounded-full bg-zinc-800 border border-zinc-700 flex items-center justify-center text-[10px] font-bold text-zinc-200 cursor-pointer hover:bg-zinc-750 transition"
          >
            {user?.name?.slice(0, 2).toUpperCase() || 'US'}
          </div>
        </div>
      </header>

      {/* Main Workspace Body */}
      <div className="flex-1 flex flex-col min-w-0 w-full relative">
        
        {/* Warning Alert banner if KYC status is Rejected */}
        {kycRecord && kycRecord.status === 'REJECTED' && (
          <div className="bg-rose-950/40 border-b border-rose-900/40 px-6 py-2 text-[11px] text-rose-300 flex items-center justify-between shrink-0 z-20">
            <span className="truncate pr-2">Compliance Alert: Verification rejected ("{kycRecord.remarks}"). Please update documents.</span>
            <button 
              onClick={() => navigate('/profile')} 
              className="px-2.5 py-0.5 bg-rose-800 hover:bg-rose-700 text-white rounded-full font-bold transition text-[10px] whitespace-nowrap active:scale-95"
            >
              Fix Profile
            </button>
          </div>
        )}

        {/* Dynamic content canvas */}
        <main className="flex-1 w-full max-w-7xl mx-auto px-4 md:px-8 py-6 pb-24 md:pb-8 relative">
          <AnimatePresence mode="wait">
            <motion.div
              key={currentPath}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.15 }}
              className="w-full h-full"
            >
              {children}
            </motion.div>
          </AnimatePresence>
        </main>

        {/* 3. MOBILE FLOATING BOTTOM NAVIGATION BAR (md:hidden) */}
        <nav className="md:hidden fixed bottom-4 left-4 right-4 h-16 rounded-full border border-zinc-900 bg-[#121214]/90 backdrop-blur-xl shadow-xl flex items-center justify-around px-2 z-30">
          {navItems.map((item) => {
            const isActive = currentPath === item.href || (item.href !== '/' && currentPath.startsWith(item.href));
            const Icon = item.icon;
            return (
              <Link
                key={item.name}
                to={item.href}
                className="flex flex-col items-center justify-center flex-1 h-full py-1 text-zinc-500 relative transition-all group"
              >
                {isActive && (
                  <motion.div
                    layoutId="mobile-active-indicator"
                    className="absolute -top-1 w-8 h-1 bg-gradient-to-r from-primary to-secondary rounded-full"
                    transition={{ type: 'spring', stiffness: 350, damping: 30 }}
                  />
                )}
                <Icon 
                  size={20} 
                  className={cn(
                    "transition-all duration-200",
                    isActive ? "text-primary scale-110" : "text-zinc-400 group-hover:text-zinc-200"
                  )} 
                />
                <span className={cn(
                  "text-[10px] mt-1 font-semibold transition-colors duration-200",
                  isActive ? "text-zinc-200" : "text-zinc-500 group-hover:text-zinc-300"
                )}>
                  {item.name}
                </span>
              </Link>
            );
          })}
        </nav>
      </div>

    </div>
  );
};
export default WalletLayout;
