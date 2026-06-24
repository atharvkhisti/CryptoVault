import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { walletApi, type Wallet } from '../services/walletApi';
import { useToast } from '../context/ToastContext';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { Skeleton } from '../components/ui/Skeleton';
import { 
  Plus, 
  ArrowDownLeft, 
  ArrowUpRight, 
  Send, 
  Wallet as WalletIcon
} from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

const CONVERSION_RATES = {
  BTC: 65000,
  ETH: 35000,
  USDT: 1,
  INR: 0.012,
};

const BRAND_COLORS = {
  BTC: '#F7931A',
  ETH: '#8B5CF6',
  USDT: '#10B981',
  INR: '#3B82F6',
};

export const Wallets: React.FC = () => {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const navigate = useNavigate();

  // Dialog State controls
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isDepositOpen, setIsDepositOpen] = useState(false);
  const [isWithdrawOpen, setIsWithdrawOpen] = useState(false);

  const [selectedWallet, setSelectedWallet] = useState<Wallet | null>(null);
  const [actionAmount, setActionAmount] = useState<string>('');
  const [selectedCurrency, setSelectedCurrency] = useState<'BTC' | 'ETH' | 'USDT' | 'INR'>('USDT');
  const [isSubmitLoading, setIsSubmitLoading] = useState(false);

  // Queries
  const { data: walletsData, isLoading } = useQuery({
    queryKey: ['wallets'],
    queryFn: walletApi.getWallets,
  });

  const wallets = walletsData?.data || [];

  // Mutations
  const createWalletMutation = useMutation({
    mutationFn: (currency: 'BTC' | 'ETH' | 'USDT' | 'INR') => walletApi.createWallet(currency),
    onSuccess: (res) => {
      if (res.success) {
        toast({ title: 'Wallet Configured', description: `Successfully created ${res.data.currency} wallet.`, variant: 'success' });
        queryClient.invalidateQueries({ queryKey: ['wallets'] });
        setIsCreateOpen(false);
      } else {
        toast({ title: 'Creation Failed', description: res.message, variant: 'error' });
      }
    },
    onError: (err: any) => {
      const errMsg = err.response?.data?.message || 'Failed to initialize wallet.';
      toast({ title: 'Creation Failed', description: errMsg, variant: 'error' });
    }
  });

  const depositMutation = useMutation({
    mutationFn: ({ walletId, amount }: { walletId: string; amount: number }) => walletApi.deposit(walletId, amount),
    onSuccess: (res) => {
      if (res.success) {
        toast({ title: 'Deposit Successful', description: `Deposited ${actionAmount} ${res.data.currency}.`, variant: 'success' });
        queryClient.invalidateQueries({ queryKey: ['wallets'] });
        setIsDepositOpen(false);
        setActionAmount('');
      } else {
        toast({ title: 'Deposit Failed', description: res.message, variant: 'error' });
      }
    },
    onError: (err: any) => {
      toast({ title: 'Deposit Error', description: err.response?.data?.message || 'Deposit simulation failed.', variant: 'error' });
    }
  });

  const withdrawMutation = useMutation({
    mutationFn: ({ walletId, amount }: { walletId: string; amount: number }) => walletApi.withdraw(walletId, amount),
    onSuccess: (res) => {
      if (res.success) {
        toast({ title: 'Withdrawal Success', description: `Withdrew ${actionAmount} ${res.data.currency}.`, variant: 'success' });
        queryClient.invalidateQueries({ queryKey: ['wallets'] });
        setIsWithdrawOpen(false);
        setActionAmount('');
      } else {
        toast({ title: 'Withdrawal Failed', description: res.message, variant: 'error' });
      }
    },
    onError: (err: any) => {
      toast({ title: 'Withdrawal Error', description: err.response?.data?.message || 'Withdrawal simulation failed.', variant: 'error' });
    }
  });

  const handleCreateWallet = async (e: React.FormEvent) => {
    e.preventDefault();
    const exists = wallets.some(w => w.currency === selectedCurrency);
    if (exists) {
      toast({ title: 'Duplicate Wallet', description: `You already have a ${selectedCurrency} wallet.`, variant: 'warning' });
      return;
    }
    setIsSubmitLoading(true);
    await createWalletMutation.mutateAsync(selectedCurrency);
    setIsSubmitLoading(false);
  };

  const handleDepositSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedWallet || !actionAmount || isNaN(Number(actionAmount)) || Number(actionAmount) <= 0) return;
    setIsSubmitLoading(true);
    await depositMutation.mutateAsync({
      walletId: selectedWallet.walletId,
      amount: parseFloat(actionAmount),
    });
    setIsSubmitLoading(false);
  };

  const handleWithdrawSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedWallet || !actionAmount || isNaN(Number(actionAmount)) || Number(actionAmount) <= 0) return;
    if (selectedWallet.balance < parseFloat(actionAmount)) {
      toast({ title: 'Insufficient Funds', description: 'Available balance is lower than input amount.', variant: 'error' });
      setIsSubmitLoading(false);
      return;
    }
    setIsSubmitLoading(true);
    await withdrawMutation.mutateAsync({
      walletId: selectedWallet.walletId,
      amount: parseFloat(actionAmount),
    });
    setIsSubmitLoading(false);
  };

  const totalUSD = wallets.reduce((sum, wallet) => {
    const rate = CONVERSION_RATES[wallet.currency] || 0;
    return sum + wallet.balance * rate;
  }, 0);

  const chartData = wallets
    .map(w => {
      const rate = CONVERSION_RATES[w.currency] || 0;
      const value = w.balance * rate;
      return {
        name: w.currency,
        value: value > 0 ? value : 0.001,
        color: BRAND_COLORS[w.currency] || '#71717a',
      };
    })
    .filter(item => item.value > 0);

  return (
    <div className="space-y-6">
      
      {/* Title Header */}
      <div className="flex items-center justify-between border-b border-zinc-900 pb-4 select-none text-left">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">Holdings</h1>
          <p className="text-xs text-zinc-400">Initialize wallets, execute ledger actions, and track allocation</p>
        </div>
        <button 
          onClick={() => setIsCreateOpen(true)}
          className="flex items-center space-x-1.5 px-4 py-2 rounded-full bg-primary text-white text-xs font-bold hover:opacity-90 transition active:scale-95 cursor-pointer shadow-md shadow-primary/20"
        >
          <Plus size={14} />
          <span>Add Asset</span>
        </button>
      </div>

      {/* ADAPTIVE LAYOUT CONTAINER */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 text-left items-start">
        
        {/* LEFT COMPONENT: Wallet grid holding (2 columns on desktop) */}
        <div className="lg:col-span-2 space-y-4">
          {isLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Skeleton className="h-32 rounded-3xl" />
              <Skeleton className="h-32 rounded-3xl" />
            </div>
          ) : wallets.length === 0 ? (
            <div className="py-16 text-center text-xs text-zinc-500 border border-dashed border-zinc-800 rounded-3xl bg-zinc-950/40">
              <WalletIcon className="mx-auto text-zinc-600 mb-2" size={32} />
              No token accounts configured yet.
              <button 
                onClick={() => setIsCreateOpen(true)}
                className="mt-4 block mx-auto px-4 py-2 text-xs font-bold bg-primary/20 text-primary border border-primary/30 rounded-full hover:bg-primary/30 transition cursor-pointer"
              >
                Create Asset Wallet
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {wallets.map((w) => {
                const rate = CONVERSION_RATES[w.currency] || 0;
                const usdVal = w.balance * rate;
                
                const cardStyles = {
                  BTC: 'border-amber-500/20 bg-gradient-to-br from-amber-500/5 to-zinc-950 hover:border-amber-500/30',
                  ETH: 'border-purple-500/20 bg-gradient-to-br from-purple-500/5 to-zinc-950 hover:border-purple-500/30',
                  USDT: 'border-emerald-500/20 bg-gradient-to-br from-emerald-500/5 to-zinc-950 hover:border-emerald-500/30',
                  INR: 'border-blue-500/20 bg-gradient-to-br from-blue-500/5 to-zinc-950 hover:border-blue-500/30'
                };

                return (
                  <Card key={w.walletId} className={`border-zinc-900 shadow-md ${cardStyles[w.currency] || 'bg-zinc-900/40'}`}>
                    <CardContent className="p-5 flex flex-col space-y-4">
                      {/* Currency and balance */}
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-2.5">
                          <div 
                            className="w-8 h-8 rounded-full flex items-center justify-center font-bold text-white text-[10px] shadow"
                            style={{ backgroundColor: BRAND_COLORS[w.currency] }}
                          >
                            {w.currency}
                          </div>
                          <div className="flex flex-col">
                            <span className="font-extrabold text-sm text-zinc-100">{w.currency === 'INR' ? 'Fiat Rupee' : w.currency === 'USDT' ? 'Tether USD' : w.currency === 'BTC' ? 'Bitcoin' : 'Ethereum'}</span>
                            <span className="text-[9px] text-zinc-500 font-mono tracking-tighter uppercase">{w.walletId.substring(0, 10)}...</span>
                          </div>
                        </div>
                        <div className="text-right">
                          <span className="text-sm font-extrabold text-white tracking-tight">
                            {w.balance.toLocaleString(undefined, { maximumFractionDigits: 6 })}
                          </span>
                          <span className="text-[10px] text-zinc-400 font-semibold mt-0.5 block">
                            ${usdVal.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                          </span>
                        </div>
                      </div>

                      {/* Card buttons bar */}
                      <div className="grid grid-cols-3 gap-2 pt-3.5 border-t border-zinc-900/80">
                        <button
                          onClick={() => {
                            setSelectedWallet(w);
                            setIsDepositOpen(true);
                          }}
                          className="flex items-center justify-center space-x-1.5 py-2 rounded-xl bg-zinc-900/80 hover:bg-zinc-800/80 border border-zinc-800 text-[10px] font-bold text-zinc-300 transition cursor-pointer"
                        >
                          <ArrowDownLeft size={12} className="text-emerald-400" />
                          <span>Deposit</span>
                        </button>
                        <button
                          onClick={() => {
                            setSelectedWallet(w);
                            setIsWithdrawOpen(true);
                          }}
                          className="flex items-center justify-center space-x-1.5 py-2 rounded-xl bg-zinc-900/80 hover:bg-zinc-800/80 border border-zinc-800 text-[10px] font-bold text-zinc-300 transition cursor-pointer"
                        >
                          <ArrowUpRight size={12} className="text-rose-400" />
                          <span>Withdraw</span>
                        </button>
                        <button
                          onClick={() => navigate(`/transfer?source=${w.walletId}`)}
                          className="flex items-center justify-center space-x-1.5 py-2 rounded-xl bg-zinc-900/80 hover:bg-zinc-800/80 border border-zinc-800 text-[10px] font-bold text-zinc-300 transition cursor-pointer"
                        >
                          <Send size={12} className="text-primary" />
                          <span>Send</span>
                        </button>
                      </div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          )}
        </div>

        {/* RIGHT COMPONENT: Portfolio Breakdown & Analytics */}
        {wallets.length > 0 && totalUSD > 0 && (
          <div className="lg:col-span-1 space-y-4">
            
            {/* Allocation donut chart card */}
            <Card className="border-zinc-850 bg-zinc-900/40 p-5 shadow-lg">
              <CardContent className="p-0 flex flex-col space-y-5">
                <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-widest block">Portfolio Share</span>
                
                <div className="w-full h-36 flex items-center justify-center relative">
                  {/* Absolute balance marker in center */}
                  <div className="absolute flex flex-col items-center justify-center pointer-events-none">
                    <span className="text-[9px] font-bold uppercase text-zinc-500 tracking-wider">Total</span>
                    <span className="text-lg font-extrabold text-white">${totalUSD.toLocaleString(undefined, { maximumFractionDigits: 0 })}</span>
                  </div>
                  
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={chartData}
                        cx="50%"
                        cy="50%"
                        innerRadius={36}
                        outerRadius={50}
                        paddingAngle={4}
                        dataKey="value"
                      >
                        {chartData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip 
                        formatter={(value: any) => [`$${Number(value).toFixed(2)}`, 'Allocated']}
                        contentStyle={{ background: '#121214', borderColor: '#27272a', borderRadius: '12px', fontSize: '10px' }}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                
                {/* Visual Legend */}
                <div className="space-y-2 pt-2 border-t border-zinc-900">
                  {wallets.map((w) => {
                    const rate = CONVERSION_RATES[w.currency] || 0;
                    const share = totalUSD > 0 ? ((w.balance * rate) / totalUSD) * 100 : 0;
                    return (
                      <div key={w.walletId} className="flex items-center justify-between text-xs font-semibold">
                        <div className="flex items-center space-x-2">
                          <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: BRAND_COLORS[w.currency] }} />
                          <span className="text-zinc-200">{w.currency === 'INR' ? 'Indian Rupee (INR)' : w.currency === 'USDT' ? 'Tether USD (USDT)' : w.currency === 'BTC' ? 'Bitcoin (BTC)' : 'Ethereum (ETH)'}</span>
                        </div>
                        <span className="text-zinc-400 font-bold">{share.toFixed(1)}%</span>
                      </div>
                    );
                  })}
                </div>
              </CardContent>
            </Card>

          </div>
        )}

      </div>

      {/* Modal 1: Create Wallet */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Configure Asset Wallet">
        <form onSubmit={handleCreateWallet} className="space-y-4">
          <div className="text-left space-y-1">
            <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wide">Select Currency Asset</label>
            <select
              value={selectedCurrency}
              onChange={(e) => setSelectedCurrency(e.target.value as any)}
              className="w-full px-4 py-3 rounded-[16px] border border-zinc-800 bg-zinc-900/60 text-sm text-white focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary"
            >
              <option value="USDT" className="bg-zinc-950">USDT (Tether USD)</option>
              <option value="BTC" className="bg-zinc-950">BTC (Bitcoin)</option>
              <option value="ETH" className="bg-zinc-950">ETH (Ethereum)</option>
              <option value="INR" className="bg-zinc-950">INR (Indian Rupee)</option>
            </select>
          </div>

          <Button type="submit" className="w-full h-12 text-xs bg-primary hover:opacity-90 text-white font-bold border-0 rounded-full mt-2" isLoading={isSubmitLoading}>
            Create Asset Wallet
          </Button>
        </form>
      </Modal>

      {/* Modal 2: Deposit Simulator */}
      <Modal isOpen={isDepositOpen} onClose={() => setIsDepositOpen(false)} title={`Deposit ${selectedWallet?.currency}`}>
        <form onSubmit={handleDepositSubmit} className="space-y-4">
          <div className="p-3 bg-zinc-900/40 rounded-2xl border border-zinc-800/80 text-xs text-left">
            <span className="text-zinc-400 block mb-0.5">Asset Address (Wallet ID)</span>
            <span className="font-mono text-white select-all text-[10px] break-all">{selectedWallet?.walletId}</span>
          </div>

          <Input
            label="Amount to Deposit"
            placeholder="0.00"
            type="number"
            step="any"
            value={actionAmount}
            onChange={(e) => setActionAmount(e.target.value)}
            required
          />

          <Button type="submit" className="w-full h-12 text-xs bg-accent hover:opacity-90 text-black font-bold border-0 rounded-full mt-2" isLoading={isSubmitLoading}>
            Simulate Deposit
          </Button>
        </form>
      </Modal>

      {/* Modal 3: Withdraw Simulator */}
      <Modal isOpen={isWithdrawOpen} onClose={() => setIsWithdrawOpen(false)} title={`Withdraw ${selectedWallet?.currency}`}>
        <form onSubmit={handleWithdrawSubmit} className="space-y-4">
          <div className="p-3 bg-zinc-900/40 rounded-2xl border border-zinc-800/80 text-xs text-left">
            <span className="text-zinc-400 block mb-0.5">Available Balance</span>
            <span className="font-bold text-white text-sm">{selectedWallet?.balance} {selectedWallet?.currency}</span>
          </div>

          <Input
            label="Amount to Withdraw"
            placeholder="0.00"
            type="number"
            step="any"
            value={actionAmount}
            onChange={(e) => setActionAmount(e.target.value)}
            required
          />

          <Button type="submit" className="w-full h-12 text-xs bg-rose-500 hover:bg-rose-600 text-white font-bold border-0 rounded-full mt-2" isLoading={isSubmitLoading}>
            Simulate Withdrawal
          </Button>
        </form>
      </Modal>

    </div>
  );
};
export default Wallets;
