import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { walletApi } from '../services/walletApi';
import { transactionApi } from '../services/transactionApi';
import { auditApi } from '../services/auditApi';
import { useToast } from '../context/ToastContext';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Skeleton } from '../components/ui/Skeleton';
import { ArrowLeft, MessageSquare, Copy, Users } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

export const Transactions: React.FC = () => {
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const urlSource = searchParams.get('source');

  // Form State
  const [senderWalletId, setSenderWalletId] = useState('');
  const [receiverWalletId, setReceiverWalletId] = useState('');
  const [transferAmount, setTransferAmount] = useState('');
  const [transferDesc, setTransferDesc] = useState('');
  
  // Custom Flow States
  const [isSubmitLoading, setIsSubmitLoading] = useState(false);
  const [successTx, setSuccessTx] = useState<any | null>(null);
  const [recipientSearch, setRecipientSearch] = useState('');

  // Fetch wallets
  const { data: walletsData, isLoading: isWalletsLoading } = useQuery({
    queryKey: ['wallets'],
    queryFn: walletApi.getWallets,
  });

  const wallets = walletsData?.data || [];

  // Fetch audit logs to compile network wallets
  const { data: logsData, isLoading: isLogsLoading } = useQuery({
    queryKey: ['audit-logs-wallets'],
    queryFn: auditApi.getAllLogs,
  });

  const allLogs = logsData?.data || [];

  // Parse logs to extract wallets
  const networkWallets = allLogs
    .filter((log) => log.eventType === 'WALLET_CREATED')
    .map((log) => {
      if (!log.description) return null;
      const match = log.description.match(/Wallet ID:\s*([a-fA-F0-9-]+),\s*User:\s*([^,]+),\s*Currency:\s*([A-Z]+)/);
      if (match) {
        return {
          walletId: match[1],
          email: match[2],
          currency: match[3],
        };
      }
      return null;
    })
    .filter(Boolean) as Array<{ walletId: string; email: string; currency: string }>;

  // Filter out the user's own wallets
  const filteredNetworkWallets = networkWallets.filter(
    (nw) => !wallets.some((w) => w.walletId === nw.walletId)
  );

  // Search filter
  const searchedNetworkWallets = filteredNetworkWallets.filter(
    (nw) =>
      nw.email.toLowerCase().includes(recipientSearch.toLowerCase()) ||
      nw.currency.toLowerCase().includes(recipientSearch.toLowerCase())
  );

  const handleSelectRecipient = (walletId: string) => {
    setReceiverWalletId(walletId);
    navigator.clipboard.writeText(walletId);
    toast({
      title: 'Address Copied',
      description: 'Recipient address copied to clipboard & auto-filled.',
      variant: 'success',
    });
  };

  // Set initial sender wallet from URL parameter if present
  useEffect(() => {
    if (wallets.length > 0) {
      if (urlSource && wallets.some(w => w.walletId === urlSource)) {
        setSenderWalletId(urlSource);
      } else {
        setSenderWalletId(wallets[0].walletId);
      }
    }
  }, [wallets, urlSource]);

  // Transfer Mutation
  const transferMutation = useMutation({
    mutationFn: (data: {
      senderWalletId: string;
      receiverWalletId: string;
      amount: number;
      description?: string;
    }) => transactionApi.transfer(data),
    onSuccess: (res) => {
      if (res.success && res.data) {
        setSuccessTx(res.data);
        queryClient.invalidateQueries({ queryKey: ['wallets'] });
        queryClient.invalidateQueries({ queryKey: ['transactions'] });
        toast({ title: 'Assets Transferred', description: 'Transaction resolved on blockchain gateway.', variant: 'success' });
      } else {
        toast({ title: 'Transfer Blocked', description: res.message || 'Verification or validation failed.', variant: 'error' });
      }
    },
    onError: (err: any) => {
      const errMsg = err.response?.data?.message || 'Transfer rejected by gateway compliance.';
      toast({ title: 'Transaction Failed', description: errMsg, variant: 'error' });
    }
  });

  const handleTransferSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!senderWalletId || !receiverWalletId || !transferAmount) {
      toast({ title: 'Validation Warning', description: 'Please fill in all inputs.', variant: 'warning' });
      return;
    }
    
    const sourceWallet = wallets.find(w => w.walletId === senderWalletId);
    if (sourceWallet && sourceWallet.balance < parseFloat(transferAmount)) {
      toast({ title: 'Insufficient Funds', description: 'Transfer amount exceeds current balance.', variant: 'error' });
      return;
    }

    if (senderWalletId === receiverWalletId) {
      toast({ title: 'Self Transfer Blocked', description: 'Source and destination wallets must be different.', variant: 'warning' });
      return;
    }

    setIsSubmitLoading(true);
    await transferMutation.mutateAsync({
      senderWalletId,
      receiverWalletId,
      amount: parseFloat(transferAmount),
      description: transferDesc || 'Phantom send transaction',
    });
    setIsSubmitLoading(false);
  };

  const selectedWallet = wallets.find(w => w.walletId === senderWalletId);

  const resetForm = () => {
    setSuccessTx(null);
    setTransferAmount('');
    setTransferDesc('');
    setReceiverWalletId('');
  };

  return (
    <div className="flex flex-col space-y-5 text-left">
      
      {/* Title Header */}
      <div className="flex items-center space-x-2 select-none">
        <button 
          onClick={() => navigate(-1)}
          className="p-1.5 rounded-full hover:bg-zinc-800 text-zinc-400 hover:text-white transition"
        >
          <ArrowLeft size={16} />
        </button>
        <div>
          <h1 className="text-xl font-extrabold text-white tracking-tight">Send Assets</h1>
          <p className="text-[10px] text-zinc-400">Transfer tokens securely across network nodes</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start max-w-5xl mx-auto w-full">
        {/* LEFT COLUMN: Main Send Form */}
        <div className="lg:col-span-2 w-full">
          <AnimatePresence mode="wait">
            {!successTx ? (
              <motion.div
                key="send-form"
                initial={{ opacity: 0, scale: 0.98 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.98 }}
                transition={{ duration: 0.15 }}
              >
                <Card className="border-zinc-800 bg-zinc-900/40 p-5">
                  <CardContent className="p-0">
                    <form onSubmit={handleTransferSubmit} className="space-y-4">
                      
                      {/* Source Wallet selector */}
                      <div className="text-left space-y-1">
                        <label className="text-[10px] font-bold text-zinc-400 uppercase tracking-wide">Source Token Account</label>
                        {isWalletsLoading ? (
                          <Skeleton className="h-11 w-full rounded-[16px]" />
                        ) : (
                          <select
                            value={senderWalletId}
                            onChange={(e) => setSenderWalletId(e.target.value)}
                            className="w-full px-4 py-3 rounded-[16px] border border-zinc-800 bg-[#09090b] text-sm text-white focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary"
                          >
                            {wallets.map((w) => (
                              <option key={w.walletId} value={w.walletId}>
                                {w.currency} - Balance: {w.balance}
                              </option>
                            ))}
                          </select>
                        )}
                        {selectedWallet && (
                          <span className="text-[10px] text-zinc-400 block px-1 mt-0.5">
                            Available: <b className="text-white">{selectedWallet.balance} {selectedWallet.currency}</b>
                          </span>
                        )}
                      </div>

                      {/* Destination Wallet address */}
                      <div className="relative">
                        <Input
                          label="Destination Wallet Address"
                          placeholder="Enter recipient's wallet ID (UUID)"
                          value={receiverWalletId}
                          onChange={(e) => setReceiverWalletId(e.target.value)}
                          required
                          className="pr-10 text-xs font-mono font-semibold"
                        />
                      </div>

                      {/* Amount input */}
                      <div className="relative">
                        {selectedWallet && (
                          <span 
                            onClick={() => setTransferAmount(String(selectedWallet.balance))}
                            className="absolute right-4 top-2 text-[10px] font-bold text-primary hover:text-purple-400 cursor-pointer select-none"
                          >
                            MAX
                          </span>
                        )}
                        <Input
                          label="Amount to Send"
                          placeholder="0.00"
                          type="number"
                          step="any"
                          value={transferAmount}
                          onChange={(e) => setTransferAmount(e.target.value)}
                          required
                        />
                      </div>

                      {/* Memo field */}
                      <div className="relative">
                        <MessageSquare className="absolute right-4 top-[42px] text-zinc-500" size={15} />
                        <Input
                          label="Memo (Description)"
                          placeholder="Note for transaction logs"
                          value={transferDesc}
                          onChange={(e) => setTransferDesc(e.target.value)}
                          className="pr-10 text-xs"
                        />
                      </div>

                      <Button type="submit" className="w-full h-12 text-sm bg-gradient-to-r from-primary to-secondary hover:opacity-90 shadow-md shadow-primary/25 border-0 rounded-full text-white font-semibold mt-4" isLoading={isSubmitLoading}>
                        Send Token
                      </Button>
                    </form>
                  </CardContent>
                </Card>
              </motion.div>
            ) : (
              <motion.div
                key="send-success"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.95 }}
                transition={{ type: 'spring', duration: 0.3 }}
              >
                <Card className="border-zinc-800 bg-[#09090b]/80 backdrop-blur-2xl p-6 text-center shadow-2xl relative overflow-hidden">
                  <CardContent className="p-0 py-6 flex flex-col items-center justify-center space-y-6">
                    
                    {/* Success Animated Circle & Checkmark */}
                    <motion.div
                      initial={{ scale: 0, opacity: 0 }}
                      animate={{ scale: 1, opacity: 1 }}
                      transition={{ type: 'spring', stiffness: 200, damping: 15, delay: 0.1 }}
                      className="w-16 h-16 rounded-full bg-emerald-500/10 border border-emerald-500/40 flex items-center justify-center text-emerald-400 mx-auto"
                    >
                      <motion.svg
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                        strokeWidth={3.5}
                        stroke="currentColor"
                        className="w-8 h-8"
                      >
                        <motion.path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="M4.5 12.75l6 6 9-13.5"
                          initial={{ pathLength: 0 }}
                          animate={{ pathLength: 1 }}
                          transition={{ delay: 0.3, duration: 0.4, ease: 'easeOut' }}
                        />
                      </motion.svg>
                    </motion.div>

                    <div>
                      <h3 className="text-lg font-extrabold text-white tracking-tight">Transfer Complete</h3>
                      <p className="text-[10px] text-zinc-400 mt-1">Funds successfully transferred to blockchain node</p>
                    </div>

                    {/* Receipt breakdown */}
                    <div className="w-full bg-zinc-900/40 border border-zinc-850 rounded-2xl p-4 text-left text-xs space-y-2">
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-zinc-400 font-semibold">Amount Transferred</span>
                        <span className="font-extrabold text-white">{successTx.amount} {successTx.currency}</span>
                      </div>
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-zinc-400 font-semibold">Reference Number</span>
                        <span className="font-mono text-zinc-300 select-all font-semibold uppercase">{successTx.referenceNumber.substring(0, 16)}...</span>
                      </div>
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-zinc-400 font-semibold">Gateway Status</span>
                        <span className="text-emerald-400 font-bold uppercase text-[9px] px-2 py-0.5 rounded-full bg-emerald-950/40 border border-emerald-800/40">{successTx.status}</span>
                      </div>
                    </div>

                    <div className="w-full flex gap-3 mt-2">
                      <Button 
                        onClick={resetForm} 
                        className="flex-1 h-11 text-xs bg-zinc-850 hover:bg-zinc-800 border border-zinc-800 rounded-full text-white font-bold"
                      >
                        Send More
                      </Button>
                      <Button 
                        onClick={() => navigate('/')} 
                        className="flex-1 h-11 text-xs bg-gradient-to-r from-primary to-secondary hover:opacity-90 shadow-md shadow-primary/25 border-0 rounded-full text-white font-bold"
                      >
                        Done
                      </Button>
                    </div>

                  </CardContent>
                </Card>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* RIGHT COLUMN: Active Network Wallets Address Book */}
        <div className="lg:col-span-1 w-full space-y-4">
          <Card className="border-zinc-800 bg-zinc-900/40 p-5">
            <CardContent className="p-0 space-y-4">
              <div className="flex items-center space-x-2 pb-3 border-b border-zinc-900 select-none">
                <Users size={16} className="text-primary animate-pulse" />
                <span className="text-[10px] font-bold text-white uppercase tracking-widest">Active Address Book</span>
              </div>

              {/* Recipient Search Filter */}
              <div className="relative">
                <input
                  type="text"
                  placeholder="Search by email or currency..."
                  value={recipientSearch}
                  onChange={(e) => setRecipientSearch(e.target.value)}
                  className="w-full px-4 py-2.5 text-xs bg-[#09090b] border border-zinc-800 text-white rounded-[16px] placeholder-zinc-500 focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary"
                />
              </div>

              {isLogsLoading || isWalletsLoading ? (
                <div className="space-y-2">
                  <Skeleton className="h-14 w-full rounded-2xl" />
                  <Skeleton className="h-14 w-full rounded-2xl" />
                </div>
              ) : searchedNetworkWallets.length === 0 ? (
                <p className="text-[10px] text-zinc-550 italic text-center py-4">No active network wallets found.</p>
              ) : (
                <div className="space-y-2 max-h-[300px] overflow-y-auto pr-1">
                  {searchedNetworkWallets.map((nw, idx) => (
                    <div
                      key={idx}
                      onClick={() => handleSelectRecipient(nw.walletId)}
                      className="p-3 rounded-2xl bg-[#09090b] border border-zinc-850 hover:border-primary/50 transition cursor-pointer flex flex-col space-y-1.5 text-left active:scale-[0.98]"
                      title="Click to copy address & autofill destination input"
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] font-extrabold text-white truncate max-w-[130px]">{nw.email.split('@')[0]}</span>
                        <span className="text-[8px] px-1.5 py-0.2 rounded bg-primary/10 text-primary border border-primary/20 font-extrabold uppercase">
                          {nw.currency}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-[9px] text-zinc-500 font-mono">
                        <span className="truncate block max-w-[150px]">{nw.walletId}</span>
                        <Copy size={11} className="text-zinc-500 hover:text-white shrink-0 ml-1" />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

    </div>
  );
};
export default Transactions;
