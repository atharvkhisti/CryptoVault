import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { kycApi, type KycRecord } from '../../services/kycApi';
import { Badge, getKycBadge } from '../ui/Badge';
import {
  LayoutDashboard,
  Wallet,
  ArrowLeftRight,
  Bell,
  FileSpreadsheet,
  Settings,
  LogOut,
  Menu,
  X,
  UserCheck,
} from 'lucide-react';
import { cn } from '../../utils/cn';

interface DashboardLayoutProps {
  children: React.ReactNode;
}

export const DashboardLayout: React.FC<DashboardLayoutProps> = ({ children }) => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [kycRecord, setKycRecord] = useState<KycRecord | null>(null);
  const [kycError, setKycError] = useState(false);

  useEffect(() => {
    if (user?.id) {
      kycApi
        .getUserKycStatus(user.id)
        .then((res) => {
          if (res.success) {
            setKycRecord(res.data);
          }
        })
        .catch(() => {
          setKycError(true);
        });
    }
  }, [user]);

  const navigation = [
    { name: 'Dashboard', href: '/', icon: LayoutDashboard },
    { name: 'Wallets', href: '/wallets', icon: Wallet },
    { name: 'Transactions', href: '/transactions', icon: ArrowLeftRight },
    { name: 'Notifications', href: '/notifications', icon: Bell },
    { name: 'Audit Logs', href: '/audit', icon: FileSpreadsheet },
    { name: 'Settings', href: '/settings', icon: Settings },
  ];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getKycBanner = () => {
    if (kycError) {
      return (
        <div className="bg-amber-950/30 border border-amber-900/50 px-4 py-2 text-xs text-amber-300 flex items-center justify-between">
          <span>Identity Verification (KYC) details are missing. Please configure your verification documents.</span>
          <button
            onClick={() => navigate('/settings')}
            className="px-2.5 py-1 bg-amber-800 text-white rounded font-medium hover:bg-amber-700 transition"
          >
            Start Verification
          </button>
        </div>
      );
    }
    if (kycRecord && kycRecord.status === 'REJECTED') {
      return (
        <div className="bg-red-950/30 border border-red-900/50 px-4 py-2 text-xs text-red-300 flex items-center justify-between">
          <span>Identity Verification rejected: "{kycRecord.remarks}". Please upload valid credentials.</span>
          <button
            onClick={() => navigate('/settings')}
            className="px-2.5 py-1 bg-red-800 text-white rounded font-medium hover:bg-red-700 transition"
          >
            Re-submit
          </button>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="min-h-screen bg-zinc-950 text-foreground flex relative overflow-hidden">
      {/* Background Neon Glows */}
      <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-primary/5 rounded-full blur-[140px] pointer-events-none -z-10" />
      <div className="absolute bottom-0 left-0 w-[400px] h-[400px] bg-purple-500/5 rounded-full blur-[120px] pointer-events-none -z-10" />

      {/* Desktop Sidebar */}
      <aside className="hidden md:flex flex-col w-64 border-r border-zinc-800/80 bg-zinc-950/60 backdrop-blur-md">
        <div className="h-16 flex items-center px-6 border-b border-zinc-800/50 space-x-2.5">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-primary to-purple-400 flex items-center justify-center shadow-lg shadow-primary/20">
            <span className="font-bold text-white text-base">C</span>
          </div>
          <span className="font-bold text-lg tracking-tight text-white">CryptoVault</span>
        </div>

        <nav className="flex-1 py-6 px-4 space-y-1.5">
          {navigation.map((item) => {
            const isActive = location.pathname === item.href;
            const Icon = item.icon;
            return (
              <Link
                key={item.name}
                to={item.href}
                className={cn(
                  'flex items-center space-x-3 px-3 py-2.5 rounded-md text-sm font-medium transition-all',
                  isActive
                    ? 'bg-primary/10 text-primary border-l-2 border-primary pl-2.5'
                    : 'text-muted-foreground hover:text-white hover:bg-zinc-900/60'
                )}
              >
                <Icon size={18} className={isActive ? 'text-primary' : 'text-zinc-500'} />
                <span>{item.name}</span>
              </Link>
            );
          })}
        </nav>

        {/* User profile section at the bottom */}
        <div className="p-4 border-t border-zinc-800/50 bg-zinc-900/10">
          <div className="flex items-center space-x-3 mb-3">
            <div className="w-9 h-9 rounded-full bg-zinc-800 flex items-center justify-center border border-zinc-700 text-white font-semibold">
              {user?.name?.charAt(0).toUpperCase() || 'U'}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold text-white truncate">{user?.name}</p>
              <p className="text-[10px] text-muted-foreground truncate">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center space-x-2 py-2 px-3 rounded-md text-xs font-medium text-red-400 hover:bg-red-950/20 border border-transparent hover:border-red-900/30 transition-all"
          >
            <LogOut size={14} />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* Mobile Header / Sidebar */}
      <div className="md:hidden fixed top-0 left-0 right-0 h-16 border-b border-zinc-800/80 bg-zinc-950/80 backdrop-blur-md z-40 flex items-center justify-between px-4">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-primary to-purple-400 flex items-center justify-center">
            <span className="font-bold text-white text-base">C</span>
          </div>
          <span className="font-bold text-base text-white">CryptoVault</span>
        </div>
        <button
          onClick={() => setIsMobileOpen(!isMobileOpen)}
          className="p-1 rounded text-zinc-400 hover:text-white"
        >
          {isMobileOpen ? <X size={22} /> : <Menu size={22} />}
        </button>
      </div>

      {/* Mobile Drawer */}
      {isMobileOpen && (
        <div className="md:hidden fixed inset-0 z-30 flex">
          <div className="fixed inset-0 bg-black/60 backdrop-blur-xs" onClick={() => setIsMobileOpen(false)} />
          <aside className="w-64 bg-zinc-950 border-r border-zinc-800 flex flex-col relative z-10 pt-16">
            <nav className="flex-1 py-6 px-4 space-y-1.5">
              {navigation.map((item) => {
                const isActive = location.pathname === item.href;
                const Icon = item.icon;
                return (
                  <Link
                    key={item.name}
                    to={item.href}
                    onClick={() => setIsMobileOpen(false)}
                    className={cn(
                      'flex items-center space-x-3 px-3 py-2.5 rounded-md text-sm font-medium transition-all',
                      isActive ? 'bg-primary/10 text-primary border-l-2 border-primary pl-2.5' : 'text-zinc-400'
                    )}
                  >
                    <Icon size={18} />
                    <span>{item.name}</span>
                  </Link>
                );
              })}
            </nav>
            <div className="p-4 border-t border-zinc-800 bg-zinc-900/10">
              <button
                onClick={handleLogout}
                className="w-full flex items-center justify-center space-x-2 py-2 px-3 rounded-md text-xs font-medium text-red-400 hover:bg-red-950/20 border border-transparent hover:border-red-900/30 transition-all"
              >
                <LogOut size={14} />
                <span>Sign Out</span>
              </button>
            </div>
          </aside>
        </div>
      )}

      {/* Main Workspace */}
      <div className="flex-1 flex flex-col min-w-0 pt-16 md:pt-0">
        {/* Banner area */}
        {getKycBanner()}

        {/* Top Navbar */}
        <header className="hidden md:flex h-16 border-b border-zinc-800/50 items-center justify-between px-8 bg-zinc-950/20">
          <div className="flex items-center space-x-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            <UserCheck size={14} className="text-primary" />
            <span>Role: {user?.role || 'USER'}</span>
          </div>

          <div className="flex items-center space-x-4">
            {/* Display verification status */}
            <div className="flex items-center space-x-2 text-sm">
              <span className="text-xs text-muted-foreground">KYC:</span>
              {kycRecord ? getKycBadge(kycRecord.status) : <Badge variant="info">Unverified</Badge>}
            </div>
          </div>
        </header>

        {/* Main page content area */}
        <main className="flex-1 overflow-y-auto p-6 md:p-8">
          <div className="max-w-6xl mx-auto space-y-8">{children}</div>
        </main>
      </div>
    </div>
  );
};
export default DashboardLayout;
