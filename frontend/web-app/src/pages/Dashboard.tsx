import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { walletApi } from '../services/walletApi';
import { transactionApi } from '../services/transactionApi';
import { riskApi } from '../services/riskApi';
import { notificationApi } from '../services/notificationApi';
import { useToast } from '../context/ToastContext';
import { Modal } from '../components/ui/Modal';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { Skeleton } from '../components/ui/Skeleton';
import QRCode from 'qrcode';
import { 
  ArrowUpRight, 
  ArrowDownLeft, 
  Send, 
  QrCode, 
  ShieldAlert, 
  Bell,
  TrendingUp, 
  ChevronRight,
  Plus
} from 'lucide-react';
import { cn } from '../utils/cn';

const CONVERSION_RATES = {
  BTC: 65000,
  ETH: 35000,
  USDT: 1,
  INR: 0.012,
};

export const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();
  const queryClient = useQueryClient();

  // Modals state
  const [isReceiveOpen, setIsReceiveOpen] = useState(false);
  const [isDepositOpen, setIsDepositOpen] = useState(false);
  const [isWithdrawOpen, setIsWithdrawOpen] = useState(false);
  const [selectedReceiveWalletId, setSelectedReceiveWalletId] = useState('');

  // QR canvas ref
  const qrCanvasRef = useRef<HTMLCanvasElement>(null);

  // Generate QR code whenever selected receive wallet changes
  // Small delay ensures AnimatePresence has mounted the canvas in the DOM
  useEffect(() => {
    if (!isReceiveOpen || !selectedReceiveWalletId) return;
    const timer = setTimeout(() => {
      if (qrCanvasRef.current) {
        QRCode.toCanvas(qrCanvasRef.current, selectedReceiveWalletId, {
          width: 200,
          margin: 2,
          color: {
            dark: '#000000',
            light: '#ffffff',
          },
        }).catch((err) => console.error('QR generation error:', err));
      }
    }, 100);
    return () => clearTimeout(timer);
  }, [isReceiveOpen, selectedReceiveWalletId]);

  // Form states for deposit / withdraw
  const [selectedWalletId, setSelectedWalletId] = useState('');
  const [amountVal, setAmountVal] = useState('');
  const [isActionLoading, setIsActionLoading] = useState(false);

  // Queries
  const { data: walletsData, isLoading: isWalletsLoading } = useQuery({
    queryKey: ['wallets'],
    queryFn: walletApi.getWallets,
  });

  const { data: txData, isLoading: isTxLoading } = useQuery({
    queryKey: ['recent-transactions'],
    queryFn: transactionApi.getTransactions,
  });

  const { data: riskData } = useQuery({
    queryKey: ['risk-history', user?.id],
    queryFn: () => riskApi.getUserRiskHistory(user?.id || ''),
    enabled: !!user?.id,
  });

  const { data: notificationsData } = useQuery({
    queryKey: ['recent-notifications', user?.id],
    queryFn: () => notificationApi.getUserNotifications(user?.id || ''),
    enabled: !!user?.id,
  });


  const wallets = walletsData?.data || [];
  const transactions = txData?.data || [];
  const riskReports = riskData?.data || [];
  const notifications = notificationsData?.data || [];


  // Compute balance
  const totalBalanceUSD = wallets.reduce((sum, w) => {
    const rate = CONVERSION_RATES[w.currency] || 0;
    return sum + w.balance * rate;
  }, 0);

  const activeRisk = riskReports[0] || {
    riskScore: 5,
    riskLevel: 'LOW',
    status: 'APPROVED',
  };

  const latestNotification = notifications[0];

  // Deposit Submit handler
  const handleDepositSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedWalletId || !amountVal || isNaN(Number(amountVal)) || Number(amountVal) <= 0) {
      toast({ title: 'Invalid Fields', description: 'Please fill valid wallet and amount.', variant: 'error' });
      return;
    }
    setIsActionLoading(true);
    try {
      const res = await transactionApi.deposit({ walletId: selectedWalletId, amount: Number(amountVal) });
      if (res.success) {
        toast({ title: 'Deposit Successful', description: `Added ${amountVal} to wallet!`, variant: 'success' });
        setIsDepositOpen(false);
        setAmountVal('');
        queryClient.invalidateQueries({ queryKey: ['wallets'] });
        queryClient.invalidateQueries({ queryKey: ['recent-transactions'] });
      } else {
        toast({ title: 'Deposit Failed', description: res.message || 'Error occurred.', variant: 'error' });
      }
    } catch (err: any) {
      toast({ title: 'Deposit Error', description: err.response?.data?.message || 'Failed to complete transaction.', variant: 'error' });
    } finally {
      setIsActionLoading(false);
    }
  };

  // Withdraw Submit handler
  const handleWithdrawSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedWalletId || !amountVal || isNaN(Number(amountVal)) || Number(amountVal) <= 0) {
      toast({ title: 'Invalid Fields', description: 'Please fill valid wallet and amount.', variant: 'error' });
      return;
    }
    const currentWallet = wallets.find(w => w.walletId === selectedWalletId);
    if (currentWallet && currentWallet.balance < Number(amountVal)) {
      toast({ title: 'Insufficient Balance', description: 'You do not have enough assets in this wallet.', variant: 'error' });
      return;
    }
    setIsActionLoading(true);
    try {
      const res = await transactionApi.withdraw({ walletId: selectedWalletId, amount: Number(amountVal) });
      if (res.success) {
        toast({ title: 'Withdrawal Successful', description: `Withdrew ${amountVal} from wallet!`, variant: 'success' });
        setIsWithdrawOpen(false);
        setAmountVal('');
        queryClient.invalidateQueries({ queryKey: ['wallets'] });
        queryClient.invalidateQueries({ queryKey: ['recent-transactions'] });
      } else {
        toast({ title: 'Withdrawal Failed', description: res.message || 'Error occurred.', variant: 'error' });
      }
    } catch (err: any) {
      toast({ title: 'Withdrawal Error', description: err.response?.data?.message || 'Failed to complete transaction.', variant: 'error' });
    } finally {
      setIsActionLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      
      {/* Title Header (Desktop display helper) */}
      <div className="hidden md:flex items-center justify-between border-b border-zinc-900 pb-4 select-none">
        <div className="text-left">
          <h1 className="text-2xl font-extrabold text-white tracking-tight">Portfolio</h1>
          <p className="text-xs text-zinc-400">Track balance, tokens allocation, and security logs</p>
        </div>
      </div>

      {/* ADAPTIVE RESPONSIBLE GRID SYSTEM */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 text-left items-start">
        
        {/* LEFT MAIN WORKSPACE: Columns 1 & 2 */}
        <div className="lg:col-span-2 space-y-6">
          
          {/* Portfolio Value Hero Card */}
          <div className="relative rounded-[28px] overflow-hidden bg-gradient-to-br from-[#1c133a] via-[#100c25] to-[#0a0a0f] border border-zinc-800/60 p-6 md:p-8 shadow-xl flex flex-col items-center justify-center text-center">
            {/* Glow behind value */}
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-56 h-56 bg-primary/20 rounded-full blur-[50px] pointer-events-none -z-10" />

            <span className="text-[10px] md:text-xs font-bold uppercase tracking-widest text-purple-400 select-none">Total Balance</span>
            
            {isWalletsLoading ? (
              <Skeleton className="h-10 w-48 mt-2 rounded-full" />
            ) : (
              <h1 className="text-4xl md:text-5xl font-extrabold text-white tracking-tight mt-1 mb-1">
                ${totalBalanceUSD.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </h1>
            )}
            
            <span className="text-[11px] text-zinc-400 font-medium flex items-center select-none">
              <TrendingUp size={12} className="mr-1 text-accent animate-pulse" />
              Live portfolio evaluation
            </span>
          </div>

          {/* Quick Action Buttons */}
          <div className="grid grid-cols-4 gap-3">
            <button
              onClick={() => navigate('/transfer')}
              className="flex flex-col items-center justify-center p-3.5 md:p-4 rounded-2xl bg-zinc-900 border border-zinc-800/80 hover:bg-zinc-850 hover:border-zinc-700/80 transition hover:scale-[1.02] active:scale-[0.98] group cursor-pointer"
            >
              <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary group-hover:scale-105 transition-transform mb-1.5">
                <Send size={18} />
              </div>
              <span className="text-xs font-bold text-zinc-200">Send</span>
            </button>

            <button
              onClick={() => {
                if (wallets.length === 0) {
                  toast({ title: 'No Wallets', description: 'Create an asset wallet first.', variant: 'info' });
                  return;
                }
                setSelectedReceiveWalletId(wallets[0].walletId);
                setIsReceiveOpen(true);
              }}
              className="flex flex-col items-center justify-center p-3.5 md:p-4 rounded-2xl bg-zinc-900 border border-zinc-800/80 hover:bg-zinc-850 hover:border-zinc-700/80 transition hover:scale-[1.02] active:scale-[0.98] group cursor-pointer"
            >
              <div className="w-10 h-10 rounded-full bg-secondary/10 flex items-center justify-center text-secondary group-hover:scale-105 transition-transform mb-1.5">
                <QrCode size={18} />
              </div>
              <span className="text-xs font-bold text-zinc-200">Receive</span>
            </button>

            <button
              onClick={() => {
                if (wallets.length === 0) {
                  toast({ title: 'No Wallets', description: 'Create an asset wallet first.', variant: 'info' });
                  return;
                }
                setSelectedWalletId(wallets[0].walletId);
                setIsDepositOpen(true);
              }}
              className="flex flex-col items-center justify-center p-3.5 md:p-4 rounded-2xl bg-zinc-900 border border-zinc-800/80 hover:bg-zinc-850 hover:border-zinc-700/80 transition hover:scale-[1.02] active:scale-[0.98] group cursor-pointer"
            >
              <div className="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center text-accent group-hover:scale-105 transition-transform mb-1.5">
                <Plus size={18} />
              </div>
              <span className="text-xs font-bold text-zinc-200">Deposit</span>
            </button>

            <button
              onClick={() => {
                if (wallets.length === 0) {
                  toast({ title: 'No Wallets', description: 'Create an asset wallet first.', variant: 'info' });
                  return;
                }
                setSelectedWalletId(wallets[0].walletId);
                setIsWithdrawOpen(true);
              }}
              className="flex flex-col items-center justify-center p-3.5 md:p-4 rounded-2xl bg-zinc-900 border border-zinc-800/80 hover:bg-zinc-850 hover:border-zinc-700/80 transition hover:scale-[1.02] active:scale-[0.98] group cursor-pointer"
            >
              <div className="w-10 h-10 rounded-full bg-rose-500/10 flex items-center justify-center text-rose-400 group-hover:scale-105 transition-transform mb-1.5">
                <ArrowUpRight size={18} />
              </div>
              <span className="text-xs font-bold text-zinc-200">Withdraw</span>
            </button>
          </div>

          {/* Tokens list holding breakdown */}
          <div className="flex flex-col space-y-2">
            <div className="flex items-center justify-between px-1 select-none">
              <span className="text-xs font-bold uppercase tracking-wider text-zinc-400">Tokens</span>
              <span className="text-xs text-primary font-bold hover:underline cursor-pointer" onClick={() => navigate('/assets')}>View holdings</span>
            </div>

            {isWalletsLoading ? (
              <div className="space-y-2">
                <Skeleton className="h-16 w-full rounded-2xl" />
                <Skeleton className="h-16 w-full rounded-2xl" />
              </div>
            ) : wallets.length === 0 ? (
              <div className="text-center py-8 border border-dashed border-zinc-850 rounded-2xl text-xs text-zinc-500">
                No active wallets found. Go to Assets tab to create one.
              </div>
            ) : (
              <div className="space-y-2">
                {wallets.map((w) => {
                  const rate = CONVERSION_RATES[w.currency] || 0;
                  const valUSD = w.balance * rate;
                  
                  const brandColors = {
                    BTC: 'bg-[#F7931A]',
                    ETH: 'bg-[#8B5CF6]',
                    USDT: 'bg-[#10B981]',
                    INR: 'bg-[#3B82F6]'
                  };
                  
                  return (
                    <div 
                      key={w.walletId} 
                      className="flex items-center justify-between p-4 rounded-2xl bg-zinc-900 border border-zinc-850 hover:border-zinc-800 transition cursor-pointer"
                      onClick={() => navigate('/assets')}
                    >
                      <div className="flex items-center space-x-3">
                        <div className={cn(
                          "w-9 h-9 rounded-full flex items-center justify-center text-white text-[10px] font-bold shadow-md",
                          brandColors[w.currency] || 'bg-zinc-700'
                        )}>
                          {w.currency}
                        </div>
                        <div>
                          <span className="text-sm font-extrabold text-white leading-none block">
                            {w.currency === 'INR' ? 'Fiat Rupee' : w.currency === 'USDT' ? 'Tether USD' : w.currency === 'BTC' ? 'Bitcoin' : 'Ethereum'}
                          </span>
                          <span className="text-[10px] text-zinc-500 font-mono tracking-tighter uppercase">{w.walletId.substring(0, 10)}...</span>
                        </div>
                      </div>
                      <div className="text-right">
                        <span className="text-sm font-extrabold text-white leading-none block">
                          {w.balance.toLocaleString(undefined, { maximumFractionDigits: 6 })} {w.currency}
                        </span>
                        <span className="text-[10px] text-zinc-400 font-semibold mt-0.5 block">
                          ${valUSD.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

        </div>

        {/* RIGHT SIDEBAR PANEL: Column 3 */}
        <div className="lg:col-span-1 space-y-6">
          
          {/* Risk Score summary */}
          <div className="rounded-[24px] border border-zinc-800/80 bg-zinc-900/40 p-5 flex flex-col space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-widest">Compliance Level</span>
              <span className={cn(
                "text-[9px] font-bold px-2 py-0.5 rounded border uppercase",
                activeRisk.riskLevel === 'CRITICAL' || activeRisk.riskLevel === 'HIGH' 
                  ? "bg-red-950/30 border-red-900/40 text-red-400" 
                  : activeRisk.riskLevel === 'MEDIUM' 
                  ? "bg-amber-950/30 border-amber-900/40 text-amber-400" 
                  : "bg-emerald-950/30 border-emerald-900/40 text-emerald-400"
              )}>
                {activeRisk.riskLevel}
              </span>
            </div>
            
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-xl bg-zinc-950 border border-zinc-900 flex items-center justify-center text-zinc-300">
                <ShieldAlert size={18} />
              </div>
              <div className="text-left flex-1 min-w-0">
                <span className="text-sm font-extrabold text-white block">Score: {activeRisk.riskScore}/100</span>
                <span className="text-[10px] text-zinc-400 truncate block mt-0.5">Automated safety threshold checks verified</span>
              </div>
              <button 
                onClick={() => navigate('/profile')} 
                className="text-zinc-500 hover:text-white p-1 rounded-full hover:bg-zinc-800 transition"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>

          {/* Notification Preview banner */}
          {latestNotification && (
            <div className="rounded-[24px] border border-zinc-850 bg-zinc-900/40 p-5 flex flex-col space-y-3 relative">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-widest">Latest Message</span>
                <span className="w-1.5 h-1.5 rounded-full bg-primary" />
              </div>
              <div className="flex items-start space-x-3">
                <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-primary shrink-0 mt-0.5">
                  <Bell size={14} className="animate-bounce" />
                </div>
                <div className="flex-1 text-xs">
                  <span className="font-bold text-white block">{latestNotification.subject}</span>
                  <p className="text-[11px] text-zinc-400 mt-1 line-clamp-2 leading-relaxed">{latestNotification.message}</p>
                </div>
              </div>
              <button 
                onClick={() => navigate('/activity')}
                className="absolute right-4 top-4 text-zinc-500 hover:text-white transition"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          )}

          {/* Recent Activity Timeline Feed */}
          <div className="flex flex-col space-y-2">
            <div className="flex items-center justify-between px-1 select-none">
              <span className="text-xs font-bold uppercase tracking-wider text-zinc-400">Activity History</span>
              <span className="text-xs text-primary font-bold hover:underline cursor-pointer" onClick={() => navigate('/activity')}>Timeline</span>
            </div>

            {isTxLoading ? (
              <Skeleton className="h-24 w-full rounded-2xl" />
            ) : transactions.length === 0 ? (
              <div className="text-center py-8 border border-dashed border-zinc-850 rounded-2xl text-xs text-zinc-500">
                No transactions executed.
              </div>
            ) : (
              <div className="space-y-2.5">
                {transactions.slice(0, 4).map((tx) => {
                  const isOut = tx.type === 'WITHDRAW' || tx.type === 'TRANSFER';
                  return (
                    <div 
                      key={tx.transactionId} 
                      className="flex items-center justify-between p-3.5 rounded-2xl bg-zinc-900 border border-zinc-850 text-xs hover:border-zinc-800 transition cursor-pointer"
                      onClick={() => navigate('/activity')}
                    >
                      <div className="flex items-center space-x-3">
                        <div className={cn(
                          "w-8 h-8 rounded-full flex items-center justify-center border",
                          isOut ? "bg-rose-950/20 border-rose-900/30 text-rose-400" : "bg-emerald-950/20 border-emerald-900/30 text-accent"
                        )}>
                          {isOut ? <ArrowUpRight size={14} /> : <ArrowDownLeft size={14} />}
                        </div>
                        <div className="text-left">
                          <span className="font-extrabold text-white block capitalize">{tx.type.toLowerCase()}</span>
                          <span className="text-[9px] text-zinc-500 block">{new Date(tx.timestamp).toLocaleDateString()}</span>
                        </div>
                      </div>
                      <div className="text-right">
                        <span className={cn(
                          "font-extrabold block",
                          isOut ? "text-rose-400" : "text-accent"
                        )}>
                          {isOut ? '-' : '+'}{tx.amount} {tx.currency}
                        </span>
                        <span className="text-[9px] text-zinc-500 block uppercase font-bold tracking-tight">{tx.status}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

        </div>

      </div>

      {/* Modal 1: Receive Wallet Address */}
      <Modal isOpen={isReceiveOpen} onClose={() => setIsReceiveOpen(false)} title="Receive Assets">
        <div className="flex flex-col items-center text-center space-y-4 pt-2">
          {/* Real QR Code Canvas */}
          <div className="relative">
            <div className="w-52 h-52 bg-white rounded-[20px] flex items-center justify-center shadow-lg shadow-white/5 overflow-hidden">
              <canvas ref={qrCanvasRef} className="rounded-[16px]" />
            </div>
            {/* Currency badge overlay */}
            {selectedReceiveWalletId && (() => {
              const w = wallets.find(x => x.walletId === selectedReceiveWalletId);
              return w ? (
                <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full bg-zinc-950 border border-zinc-800 text-[10px] font-bold text-white shadow-md">
                  {w.currency} Address
                </div>
              ) : null;
            })()}
          </div>

          <div className="w-full text-left space-y-2 mt-4">
            <span className="text-xs font-bold text-zinc-400 block">Select Wallet — QR updates automatically:</span>
            <div className="space-y-1.5 max-h-40 overflow-y-auto pr-1">
              {wallets.map((w) => (
                <div 
                  key={w.walletId} 
                  onClick={() => {
                    setSelectedReceiveWalletId(w.walletId);
                    navigator.clipboard.writeText(w.walletId);
                    toast({ title: 'Address Copied', description: `${w.currency} wallet address copied to clipboard!`, variant: 'success' });
                  }}
                  className={`p-2.5 rounded-[16px] hover:bg-zinc-800 transition border cursor-pointer flex items-center justify-between text-xs ${
                    selectedReceiveWalletId === w.walletId
                      ? 'bg-zinc-800 border-primary/50'
                      : 'bg-zinc-900 border-zinc-800'
                  }`}
                >
                  <span className="font-bold text-white">{w.currency}</span>
                  <span className="font-semibold text-zinc-400 select-all font-mono text-[10px] truncate max-w-[200px]">{w.walletId}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </Modal>

      {/* Modal 2: Deposit Simulator */}
      <Modal isOpen={isDepositOpen} onClose={() => setIsDepositOpen(false)} title="Simulate Deposit">
        <form onSubmit={handleDepositSubmit} className="space-y-4">
          <div className="text-left space-y-1">
            <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wide">Destination Asset Wallet</label>
            <select
              value={selectedWalletId}
              onChange={(e) => setSelectedWalletId(e.target.value)}
              className="w-full px-4 py-3 rounded-[16px] border border-zinc-800 bg-zinc-900/60 text-sm text-white focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary"
            >
              {wallets.map((w) => (
                <option key={w.walletId} value={w.walletId} className="bg-zinc-950">
                  {w.currency} - {w.walletId.substring(0, 8)}... ({w.balance} {w.currency} Available)
                </option>
              ))}
            </select>
          </div>

          <Input
            label="Amount to Deposit"
            placeholder="0.00"
            type="number"
            step="any"
            value={amountVal}
            onChange={(e) => setAmountVal(e.target.value)}
            required
          />

          <Button type="submit" className="w-full h-12 text-xs bg-accent hover:opacity-90 text-black font-bold border-0 rounded-full mt-2" isLoading={isActionLoading}>
            Deposit Funds
          </Button>
        </form>
      </Modal>

      {/* Modal 3: Withdraw Simulator */}
      <Modal isOpen={isWithdrawOpen} onClose={() => setIsWithdrawOpen(false)} title="Simulate Withdrawal">
        <form onSubmit={handleWithdrawSubmit} className="space-y-4">
          <div className="text-left space-y-1">
            <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wide">Source Asset Wallet</label>
            <select
              value={selectedWalletId}
              onChange={(e) => setSelectedWalletId(e.target.value)}
              className="w-full px-4 py-3 rounded-[16px] border border-zinc-800 bg-zinc-900/60 text-sm text-white focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary"
            >
              {wallets.map((w) => (
                <option key={w.walletId} value={w.walletId} className="bg-zinc-950">
                  {w.currency} - {w.walletId.substring(0, 8)}... ({w.balance} {w.currency} Available)
                </option>
              ))}
            </select>
          </div>

          <Input
            label="Amount to Withdraw"
            placeholder="0.00"
            type="number"
            step="any"
            value={amountVal}
            onChange={(e) => setAmountVal(e.target.value)}
            required
          />

          <Button type="submit" className="w-full h-12 text-xs bg-rose-500 hover:bg-rose-600 text-white font-bold border-0 rounded-full mt-2" isLoading={isActionLoading}>
            Withdraw Funds
          </Button>
        </form>
      </Modal>

    </div>
  );
};
export default Dashboard;
