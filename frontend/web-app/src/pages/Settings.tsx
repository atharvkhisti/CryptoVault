import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { kycApi } from '../services/kycApi';
import { riskApi } from '../services/riskApi';
import { authApi } from '../services/authApi';
import { auditApi } from '../services/auditApi';
import { useToast } from '../context/ToastContext';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { cn } from '../utils/cn';
import { Input } from '../components/ui/Input';
import { getKycBadge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import { 
  FileText, 
  Upload, 
  ShieldCheck, 
  LogOut, 
  ShieldAlert,
  Check,
  XCircle,
  Bell,
  Mail,
  Key
} from 'lucide-react';

export const Settings: React.FC = () => {
  const queryClient = useQueryClient();
  const { user, logout } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();

  // Document upload state
  const [docType, setDocType] = useState<'PAN' | 'AADHAAR' | 'PASSPORT' | 'DRIVING_LICENSE'>('PAN');
  
  // Custom states
  const [isUploading, setIsUploading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSimulating, setIsSimulating] = useState(false);

  // Preference mock states
  const [emailAlerts, setEmailAlerts] = useState(true);
  const [pushAlerts, setPushAlerts] = useState(false);
  const [riskAlerts, setRiskAlerts] = useState(true);

  // Admin compliance portal states
  const [reviewRequests, setReviewRequests] = useState<any[]>([]);
  const [isAdminLoading, setIsAdminLoading] = useState(false);
  const [adminRejectingKycId, setAdminRejectingKycId] = useState<string | null>(null);
  const [adminRejectRemarks, setAdminRejectRemarks] = useState('');

  const loadAdminKycData = async () => {
    setIsAdminLoading(true);
    try {
      const logsRes = await auditApi.getAllLogs();
      if (logsRes.success && logsRes.data) {
        const uniqueUserIds = Array.from(new Set(logsRes.data.map((log: any) => log.userId).filter(Boolean)));
        
        const requests: any[] = [];
        await Promise.all(
          uniqueUserIds.map(async (userId: string) => {
            try {
              const kycRes = await kycApi.getUserKycStatus(userId);
              if (kycRes.success && kycRes.data) {
                if (kycRes.data.status === 'UNDER_REVIEW') {
                  const userRes = await authApi.getUserProfile(userId);
                  requests.push({
                    userId,
                    name: userRes.success && userRes.data ? userRes.data.name : 'Unknown User',
                    email: userRes.success && userRes.data ? userRes.data.email : 'Unknown Email',
                    kycRecord: kycRes.data
                  });
                }
              }
            } catch (err) {
              console.error('Error fetching admin KYC data for user', userId, err);
            }
          })
        );
        setReviewRequests(requests);
      }
    } catch (err) {
      console.error('Error loading admin KYC dashboard', err);
      toast({ title: 'Dashboard Error', description: 'Failed to load user compliance requests.', variant: 'error' });
    } finally {
      setIsAdminLoading(false);
    }
  };

  React.useEffect(() => {
    if (user?.role === 'ADMIN') {
      loadAdminKycData();
    }
  }, [user]);

  const handleAdminApprove = async (kycId: string) => {
    setIsSimulating(true);
    try {
      const res = await kycApi.approveKyc(kycId);
      if (res.success) {
        toast({ title: 'KYC Approved', description: 'User KYC status resolved successfully.', variant: 'success' });
        loadAdminKycData();
      }
    } catch (err) {
      toast({ title: 'Approval Error', description: 'Failed to approve KYC request.', variant: 'error' });
    } finally {
      setIsSimulating(false);
    }
  };

  const handleAdminReject = async (e: React.FormEvent, kycId: string) => {
    e.preventDefault();
    if (!adminRejectRemarks) return;
    setIsSimulating(true);
    try {
      const res = await kycApi.rejectKyc(kycId, adminRejectRemarks);
      if (res.success) {
        toast({ title: 'KYC Rejected', description: 'User KYC status updated to REJECTED.', variant: 'success' });
        setAdminRejectingKycId(null);
        setAdminRejectRemarks('');
        loadAdminKycData();
      }
    } catch (err) {
      toast({ title: 'Rejection Error', description: 'Failed to reject KYC request.', variant: 'error' });
    } finally {
      setIsSimulating(false);
    }
  };

  // Queries
  const { data: kycData, isLoading: isKycLoading } = useQuery({
    queryKey: ['kyc-status', user?.id],
    queryFn: () => kycApi.getUserKycStatus(user?.id || ''),
    enabled: !!user?.id,
    retry: false,
  });

  const { data: riskData, isLoading: isRiskLoading } = useQuery({
    queryKey: ['risk-history', user?.id],
    queryFn: () => riskApi.getUserRiskHistory(user?.id || ''),
    enabled: !!user?.id,
  });

  const kyc = kycData?.data || null;
  const riskLogs = riskData?.data || [];

  // Mutations
  const initKycMutation = useMutation({
    mutationFn: kycApi.initializeKyc,
    onSuccess: (res) => {
      if (res.success) {
        toast({ title: 'KYC Record Configured', description: 'Initialized compliance record on servers.', variant: 'success' });
        queryClient.invalidateQueries({ queryKey: ['kyc-status'] });
      } else {
        toast({ title: 'Setup Failed', description: res.message, variant: 'error' });
      }
    },
    onError: (err: any) => {
      toast({ title: 'KYC Error', description: err.response?.data?.message || 'Failed to initialize KYC record.', variant: 'error' });
    }
  });

  const uploadDocMutation = useMutation({
    mutationFn: ({ file, type }: { file: File; type: typeof docType }) => kycApi.uploadDocument(file, type),
    onSuccess: (res, variables) => {
      if (res.success) {
        toast({ title: 'Document Saved', description: `Successfully uploaded ${variables.type}.`, variant: 'success' });
        queryClient.invalidateQueries({ queryKey: ['kyc-status'] });
      } else {
        toast({ title: 'Upload Failed', description: res.message, variant: 'error' });
      }
    },
    onError: (err: any) => {
      toast({ title: 'Upload Error', description: err.response?.data?.message || 'Upload rejected. Limit 5MB.', variant: 'error' });
    }
  });

  const submitKycMutation = useMutation({
    mutationFn: kycApi.submitForReview,
    onSuccess: (res) => {
      if (res.success) {
        toast({ title: 'KYC Submitted', description: 'Verification profile is now under administrative review.', variant: 'success' });
        queryClient.invalidateQueries({ queryKey: ['kyc-status'] });
      } else {
        toast({ title: 'Submission Failed', description: res.message, variant: 'error' });
      }
    },
    onError: (err: any) => {
      toast({ title: 'Submission Error', description: err.response?.data?.message || 'PAN and AADHAAR cards are required.', variant: 'error' });
    }
  });

  const handleDirectUpload = async (file: File, type: typeof docType) => {
    setIsUploading(true);
    try {
      await uploadDocMutation.mutateAsync({ file, type });
    } catch (err: any) {
      console.error(err);
    } finally {
      setIsUploading(false);
    }
  };

  const panDoc = kyc?.documents.find(doc => doc.documentType === 'PAN');
  const aadhaarDoc = kyc?.documents.find(doc => doc.documentType === 'AADHAAR');

  const handleInitKyc = async () => {
    await initKycMutation.mutateAsync();
  };


  const handleSubmitKyc = async () => {
    setIsSubmitting(true);
    await submitKycMutation.mutateAsync();
    setIsSubmitting(false);
  };



  const handleLogout = () => {
    logout();
    navigate('/login');
    toast({ title: 'Signed Out', description: 'Your session has been terminated.', variant: 'success' });
  };

  return (
    <div className="space-y-6">
      
      {/* Title Header */}
      <div className="flex items-center justify-between border-b border-zinc-900 pb-4 select-none text-left">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">Profile</h1>
          <p className="text-xs text-zinc-400">Configure identity files, alerts, and credential keys</p>
        </div>
      </div>

      {/* ADAPTIVE MULTI COLUMN GRID */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 text-left items-start">
        
        {/* LEFT COLUMN: Profile info, preferences & controls (takes col-span-1) */}
        <div className="lg:col-span-1 space-y-6">
          
          {/* User profile card */}
          <Card className="border-zinc-800 bg-zinc-900/40 p-5">
            <CardContent className="p-0 flex flex-col space-y-4">
              <div className="flex items-center space-x-3.5 pb-4 border-b border-zinc-900">
                <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-primary to-secondary flex items-center justify-center text-white font-extrabold text-base shadow shadow-primary/20">
                  {user?.name?.slice(0, 2).toUpperCase() || 'US'}
                </div>
                <div>
                  <h4 className="text-sm font-extrabold text-white leading-none">{user?.name}</h4>
                  <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-zinc-800 text-zinc-400 border border-zinc-750 font-bold uppercase mt-1 inline-block">
                    {user?.role} Access
                  </span>
                </div>
              </div>

              <div className="space-y-3.5 text-xs">
                <div className="flex items-center space-x-2 text-zinc-300">
                  <Mail size={14} className="text-zinc-500 shrink-0" />
                  <span className="truncate">{user?.email}</span>
                </div>
                <div className="flex items-center space-x-2 text-zinc-300">
                  <Key size={14} className="text-zinc-500 shrink-0" />
                  <span className="font-mono text-zinc-500 truncate">ID: {user?.id}</span>
                </div>
              </div>

              <button 
                onClick={handleLogout}
                className="w-full mt-2 flex items-center justify-center space-x-2 py-2.5 px-4 rounded-full bg-rose-500/10 border border-rose-900/20 hover:bg-rose-500/20 text-rose-400 text-xs font-bold transition active:scale-[0.97] cursor-pointer"
              >
                <LogOut size={14} />
                <span>Sign Out Wallet</span>
              </button>
            </CardContent>
          </Card>

          {/* Preferences toggles */}
          <Card className="border-zinc-800 bg-zinc-900/40 p-5">
            <CardContent className="p-0 space-y-4">
              <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-widest block">Notification Preferences</span>
              
              <div className="space-y-3.5 text-xs">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Bell size={14} className="text-zinc-500" />
                    <span className="text-zinc-200 font-semibold">Email Transactions</span>
                  </div>
                  <input 
                    type="checkbox" 
                    checked={emailAlerts} 
                    onChange={() => setEmailAlerts(!emailAlerts)}
                    className="accent-primary w-4 h-4 rounded cursor-pointer"
                  />
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Bell size={14} className="text-zinc-500" />
                    <span className="text-zinc-200 font-semibold">Push Activity Alerts</span>
                  </div>
                  <input 
                    type="checkbox" 
                    checked={pushAlerts} 
                    onChange={() => setPushAlerts(!pushAlerts)}
                    className="accent-primary w-4 h-4 rounded cursor-pointer"
                  />
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <ShieldAlert size={14} className="text-zinc-500" />
                    <span className="text-zinc-200 font-semibold">Critical Risk warnings</span>
                  </div>
                  <input 
                    type="checkbox" 
                    checked={riskAlerts} 
                    onChange={() => setRiskAlerts(!riskAlerts)}
                    className="accent-primary w-4 h-4 rounded cursor-pointer"
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Risk logs feed */}
          <div className="flex flex-col space-y-2">
            <span className="text-xs font-bold uppercase tracking-wider text-zinc-400 px-1">Security Audits</span>
            
            {isRiskLoading ? (
              <Skeleton className="h-16 w-full rounded-2xl" />
            ) : riskLogs.length === 0 ? (
              <p className="text-[10px] text-zinc-500 px-1">No automated risk audits recorded.</p>
            ) : (
              <div className="space-y-2.5">
                {riskLogs.slice(0, 3).map((log) => (
                  <div key={log.id} className="p-3.5 rounded-2xl bg-zinc-900 border border-zinc-850 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="font-extrabold text-white">Risk Audit</span>
                      <span className="text-[9px] font-semibold text-zinc-500">{new Date(log.createdAt).toLocaleDateString()}</span>
                    </div>
                    <div className="flex items-center justify-between mt-2 pt-2 border-t border-zinc-900">
                      <span className="text-[10px] text-zinc-400 font-semibold">Score: {log.riskScore}/100</span>
                      <span className="font-bold text-accent uppercase text-[9px] px-1.5 py-0.2 rounded bg-emerald-950/20 border border-emerald-900/30">
                        {log.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

        </div>

        {/* RIGHT COLUMN: Identity compliance verification form and lists (takes col-span-2) */}
        <div className="lg:col-span-2 space-y-4">
          
          {user?.role === 'ADMIN' ? (
            <div className="space-y-4">
              <div className="p-5 rounded-2xl bg-zinc-900/60 border border-zinc-850 flex flex-col space-y-3">
                <div className="flex items-center justify-between border-b border-zinc-900 pb-3">
                  <div>
                    <span className="text-[10px] font-bold text-primary uppercase tracking-widest block">Compliance Administration</span>
                    <h2 className="text-sm font-extrabold text-white mt-0.5">Pending KYC Evaluations</h2>
                  </div>
                  <span className="text-[9px] px-2 py-0.5 rounded-full bg-purple-500/10 text-purple-400 font-bold border border-purple-900/20 uppercase">
                    Admin Portal
                  </span>
                </div>

                {isAdminLoading ? (
                  <div className="space-y-3 pt-3">
                    <Skeleton className="h-24 w-full rounded-2xl" />
                    <Skeleton className="h-24 w-full rounded-2xl" />
                  </div>
                ) : reviewRequests.length === 0 ? (
                  <div className="py-12 text-center text-xs text-zinc-500 italic">
                    No pending compliance review requests.
                  </div>
                ) : (
                  <div className="space-y-4 pt-3">
                    {reviewRequests.map((req) => (
                      <div key={req.kycRecord.kycId} className="p-4 rounded-2xl bg-[#09090b] border border-zinc-850 space-y-3 text-left">
                        <div className="flex items-center justify-between">
                          <div>
                            <h4 className="text-xs font-bold text-white">{req.name}</h4>
                            <p className="text-[10px] text-zinc-500">{req.email}</p>
                          </div>
                          <span className="text-[9px] px-1.5 py-0.5 rounded bg-amber-500/10 text-amber-400 font-bold uppercase">
                            {req.kycRecord.status}
                          </span>
                        </div>
                        
                        <div className="text-[9px] text-zinc-550 font-mono">
                          User ID: {req.userId} <br />
                          KYC ID: {req.kycRecord.kycId}
                        </div>

                        {/* Documents attached */}
                        <div className="space-y-1.5 pt-2 border-t border-zinc-900">
                          <span className="text-[9px] font-bold text-zinc-400 uppercase tracking-wider block">Attached Identification Scan Documents:</span>
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                            {req.kycRecord.documents.map((doc: any) => (
                              <div key={doc.documentId} className="p-2.5 rounded-xl bg-zinc-900 border border-zinc-850 flex items-center justify-between text-[10px]">
                                <div className="flex items-center space-x-2 min-w-0">
                                  <FileText size={12} className="text-primary shrink-0" />
                                  <div className="min-w-0">
                                    <p className="font-semibold text-zinc-350 truncate block">{doc.fileName}</p>
                                    <p className="text-[8px] text-zinc-500 uppercase">{doc.documentType} • {(doc.fileSize / 1024).toFixed(0)} KB</p>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>

                        {/* Administrative Clearance Decisions */}
                        <div className="flex gap-2 pt-3 border-t border-zinc-900">
                          <button
                            onClick={() => handleAdminApprove(req.kycRecord.kycId)}
                            disabled={isSimulating}
                            className="flex-1 flex items-center justify-center space-x-1.5 py-2 px-3 rounded-full bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 text-xs font-bold transition border border-emerald-500/20 cursor-pointer disabled:opacity-50"
                          >
                            <Check size={13} />
                            <span>Approve KYC Request</span>
                          </button>
                          <button
                            onClick={() => {
                              if (adminRejectingKycId === req.kycRecord.kycId) {
                                setAdminRejectingKycId(null);
                              } else {
                                setAdminRejectingKycId(req.kycRecord.kycId);
                              }
                            }}
                            disabled={isSimulating}
                            className="flex-1 flex items-center justify-center space-x-1.5 py-2 px-3 rounded-full bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 text-xs font-bold transition border border-rose-500/20 cursor-pointer disabled:opacity-50"
                          >
                            <XCircle size={13} />
                            <span>Reject KYC Request</span>
                          </button>
                        </div>

                        {adminRejectingKycId === req.kycRecord.kycId && (
                          <form onSubmit={(e) => handleAdminReject(e, req.kycRecord.kycId)} className="space-y-3 pt-3">
                            <Input
                              placeholder="Reason for identity document rejection..."
                              value={adminRejectRemarks}
                              onChange={(e) => setAdminRejectRemarks(e.target.value)}
                              required
                              className="py-2.5 text-xs rounded-xl"
                            />
                            <Button type="submit" className="w-full text-xs h-9 bg-rose-600 hover:bg-rose-700 text-white font-bold rounded-full border-0" isLoading={isSimulating}>
                              Submit Rejection Decision
                            </Button>
                          </form>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ) : (
            // Customer identity compliance verification upload panel
            isKycLoading ? (
              <Skeleton className="h-48 w-full rounded-3xl" />
            ) : !kyc ? (
              <div className="py-12 text-center border border-dashed border-zinc-800 rounded-3xl space-y-4 bg-zinc-950/40">
                <ShieldCheck size={32} className="mx-auto text-zinc-600" />
                <div>
                  <h4 className="text-sm font-semibold text-white">KYC Verification Record Required</h4>
                  <p className="text-xs text-zinc-400 mt-1 max-w-sm mx-auto leading-normal">
                    You have not initialized a compliance file. Initialize your record first to unlock document uploads.
                  </p>
                </div>
                <button 
                  onClick={handleInitKyc}
                  className="px-4 py-2 text-xs font-bold bg-primary text-white rounded-full hover:opacity-90 transition active:scale-95 cursor-pointer shadow-md"
                >
                  Setup Verification Profile
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                
                {/* KYC Status indicators and review state */}
                <div className="p-5 rounded-2xl bg-zinc-900/60 border border-zinc-850 flex flex-col space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex flex-col">
                      <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-widest">Compliance Status</span>
                      <span className="text-[9px] text-zinc-500 font-mono mt-0.5 select-all uppercase">File: {kyc.kycId}</span>
                    </div>
                    {getKycBadge(kyc.status)}
                  </div>

                  {kyc.status === 'PENDING' && (
                    <div className="pt-2">
                      <p className="text-[11px] text-zinc-450 leading-relaxed mb-3">
                        Please upload PAN and AADHAAR identification cards below. Once uploaded, submit your compliance file for review.
                      </p>
                      <Button
                        onClick={handleSubmitKyc}
                        className="w-full text-xs h-10 bg-primary hover:opacity-90 text-white font-bold border-0 rounded-full"
                        isLoading={isSubmitting}
                      >
                        Submit for Evaluation
                      </Button>
                    </div>
                  )}

                  {kyc.status === 'UNDER_REVIEW' && (
                    <div className="space-y-3 pt-2 border-t border-zinc-900">
                      <p className="text-xs text-amber-450 font-semibold leading-relaxed">
                        Your identity documents are currently undergoing evaluation by our compliance controllers. Please wait for an administrator to review your documents.
                      </p>
                    </div>
                  )}

                  {kyc.status === 'APPROVED' && (
                    <p className="text-xs text-accent leading-relaxed font-bold border-t border-zinc-900 pt-3">
                      Verification Resolved. Compliance audits passed successfully.
                    </p>
                  )}

                  {kyc.status === 'REJECTED' && (
                    <div className="space-y-3 pt-2 border-t border-zinc-900">
                      <div className="p-3 bg-rose-950/20 border border-rose-900/30 rounded-xl text-xs text-rose-300">
                        <span className="font-extrabold block mb-0.5">Rejection Remarks:</span>
                        <span>"{kyc.remarks}"</span>
                      </div>
                      <Button onClick={handleInitKyc} className="w-full text-xs h-10 bg-rose-500 hover:bg-rose-650 text-white font-bold border-0 rounded-full">
                        Reset Verification File
                      </Button>
                    </div>
                  )}
                </div>

                {/* Document Slot Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  
                  {/* Slot 1: PAN Card */}
                  <div className={cn(
                    "p-5 rounded-2xl border transition-all flex flex-col justify-between h-44",
                    panDoc 
                      ? "bg-zinc-900/60 border-zinc-800" 
                      : "bg-zinc-950/20 border-dashed border-zinc-800 hover:border-primary/40"
                  )}>
                    <div>
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-white flex items-center space-x-1.5">
                          <FileText size={14} className="text-primary" />
                          <span>PAN Card</span>
                        </span>
                        {panDoc ? (
                          <span className="text-[9px] px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 font-bold uppercase">Uploaded</span>
                        ) : (
                          <span className="text-[9px] px-1.5 py-0.5 rounded bg-red-500/10 text-red-400 font-bold uppercase">Required</span>
                        )}
                      </div>
                      <p className="text-[10px] text-zinc-500 mt-2.5 leading-relaxed">Permanent Account Number card for tax & identity validation.</p>
                    </div>
                    
                    {panDoc ? (
                      <div className="flex items-center justify-between pt-3.5 border-t border-zinc-900 mt-auto">
                        <div className="flex items-center space-x-2 min-w-0">
                          <Check size={14} className="text-accent shrink-0" />
                          <span className="text-[10px] text-zinc-300 font-medium truncate block max-w-[130px]">{panDoc.fileName}</span>
                        </div>
                        <span className="text-[9px] text-zinc-500 font-bold shrink-0">{(panDoc.fileSize / 1024).toFixed(0)} KB</span>
                      </div>
                    ) : (
                      <div className="pt-3.5 mt-auto">
                        {(kyc.status === 'PENDING' || kyc.status === 'REJECTED') ? (
                          <div className="relative">
                            <input
                              type="file"
                              accept="image/png, image/jpeg, application/pdf"
                              onChange={(e) => {
                                if (e.target.files && e.target.files[0]) {
                                  handleDirectUpload(e.target.files[0], 'PAN');
                                }
                              }}
                              className="hidden"
                              id="pan-upload-input"
                              disabled={isUploading}
                            />
                            <label
                              htmlFor="pan-upload-input"
                              className="flex items-center justify-center space-x-1.5 py-2 px-4 bg-[#09090b] hover:bg-zinc-900 border border-zinc-800 rounded-full cursor-pointer text-xs font-bold text-white transition active:scale-95"
                            >
                              <Upload size={13} className="text-primary" />
                              <span>Attach PAN Card</span>
                            </label>
                          </div>
                        ) : (
                          <span className="text-[10px] text-zinc-500 italic block">No file uploaded</span>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Slot 2: Aadhaar Card */}
                  <div className={cn(
                    "p-5 rounded-2xl border transition-all flex flex-col justify-between h-44",
                    aadhaarDoc 
                      ? "bg-zinc-900/60 border-zinc-800" 
                      : "bg-zinc-950/20 border-dashed border-zinc-800 hover:border-primary/40"
                  )}>
                    <div>
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-white flex items-center space-x-1.5">
                          <FileText size={14} className="text-primary" />
                          <span>Aadhaar Card</span>
                        </span>
                        {aadhaarDoc ? (
                          <span className="text-[9px] px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 font-bold uppercase">Uploaded</span>
                        ) : (
                          <span className="text-[9px] px-1.5 py-0.5 rounded bg-red-500/10 text-red-400 font-bold uppercase">Required</span>
                        )}
                      </div>
                      <p className="text-[10px] text-zinc-500 mt-2.5 leading-relaxed">Unique identification card (UIDAI) for national citizen registry.</p>
                    </div>

                    {aadhaarDoc ? (
                      <div className="flex items-center justify-between pt-3.5 border-t border-zinc-900 mt-auto">
                        <div className="flex items-center space-x-2 min-w-0">
                          <Check size={14} className="text-accent shrink-0" />
                          <span className="text-[10px] text-zinc-300 font-medium truncate block max-w-[130px]">{aadhaarDoc.fileName}</span>
                        </div>
                        <span className="text-[9px] text-zinc-500 font-bold shrink-0">{(aadhaarDoc.fileSize / 1024).toFixed(0)} KB</span>
                      </div>
                    ) : (
                      <div className="pt-3.5 mt-auto">
                        {(kyc.status === 'PENDING' || kyc.status === 'REJECTED') ? (
                          <div className="relative">
                            <input
                              type="file"
                              accept="image/png, image/jpeg, application/pdf"
                              onChange={(e) => {
                                if (e.target.files && e.target.files[0]) {
                                  handleDirectUpload(e.target.files[0], 'AADHAAR');
                                }
                              }}
                              className="hidden"
                              id="aadhaar-upload-input"
                              disabled={isUploading}
                            />
                            <label
                              htmlFor="aadhaar-upload-input"
                              className="flex items-center justify-center space-x-1.5 py-2 px-4 bg-[#09090b] hover:bg-zinc-900 border border-zinc-800 rounded-full cursor-pointer text-xs font-bold text-white transition active:scale-95"
                            >
                              <Upload size={13} className="text-primary" />
                              <span>Attach Aadhaar Card</span>
                            </label>
                          </div>
                        ) : (
                          <span className="text-[10px] text-zinc-500 italic block">No file uploaded</span>
                        )}
                      </div>
                    )}
                  </div>

                </div>

                {/* Optional / Other Documents Slot */}
                {(kyc.status === 'PENDING' || kyc.status === 'REJECTED') && (
                  <div className="p-5 border border-zinc-850 rounded-3xl bg-zinc-900/40 space-y-4">
                    <span className="text-[10px] font-bold text-zinc-450 uppercase tracking-widest block">Upload Optional Supporting Documents</span>
                    <div className="flex flex-col sm:flex-row gap-4 items-end">
                      <div className="flex-1 w-full space-y-1">
                        <label className="text-[9px] font-bold text-zinc-500 uppercase tracking-wider block">Document Type</label>
                        <select
                          value={docType}
                          onChange={(e) => setDocType(e.target.value as any)}
                          className="w-full px-4 py-2.5 rounded-[16px] border border-zinc-800 bg-[#09090b] text-xs text-white focus:outline-none focus:ring-1 focus:ring-primary h-11"
                        >
                          <option value="PASSPORT">Passport</option>
                          <option value="DRIVING_LICENSE">Driving License</option>
                        </select>
                      </div>
                      
                      <div className="relative flex-1 w-full">
                        <input
                          type="file"
                          accept="image/png, image/jpeg, application/pdf"
                          onChange={(e) => {
                            if (e.target.files && e.target.files[0]) {
                              handleDirectUpload(e.target.files[0], docType);
                            }
                          }}
                          className="hidden"
                          id="optional-upload-input"
                          disabled={isUploading}
                        />
                        <label
                          htmlFor="optional-upload-input"
                          className="w-full h-11 flex items-center justify-center space-x-2 py-2 px-4 bg-[#09090b] hover:bg-zinc-900 border border-zinc-800 rounded-[16px] cursor-pointer text-xs font-bold text-white transition active:scale-95"
                        >
                          <Upload size={13} className="text-zinc-500" />
                          <span>Select & Upload Optional File</span>
                        </label>
                      </div>
                    </div>
                  </div>
                )}

                {/* Additional attached files details list */}
                {kyc.documents.length > 0 && (
                  <div className="space-y-2.5">
                    <span className="text-[10px] font-bold text-zinc-450 uppercase tracking-widest block px-1">Attached Records ({kyc.documents.length})</span>
                    <div className="space-y-2">
                      {kyc.documents.map((doc) => (
                        <div key={doc.documentId} className="p-3.5 flex items-center justify-between bg-zinc-900/40 border border-zinc-900 rounded-2xl text-[10px]">
                          <div className="flex items-center space-x-3 min-w-0">
                            <div className="w-7 h-7 rounded bg-zinc-950 border border-zinc-900 flex items-center justify-center text-zinc-500">
                              <FileText size={13} />
                            </div>
                            <div className="min-w-0 text-left">
                              <p className="font-extrabold text-white truncate block">{doc.fileName}</p>
                              <p className="text-[9px] text-zinc-500 uppercase mt-0.5">{doc.documentType} • {(doc.fileSize / 1024).toFixed(0)} KB</p>
                            </div>
                          </div>
                          <span className="text-[9px] text-zinc-500 font-bold">{new Date(doc.uploadedAt).toLocaleDateString()}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

              </div>
            )
          )}
        </div>

      </div>

    </div>
  );
};
export default Settings;
