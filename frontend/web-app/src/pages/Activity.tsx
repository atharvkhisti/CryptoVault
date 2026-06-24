import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { transactionApi } from '../services/transactionApi';
import { notificationApi } from '../services/notificationApi';
import { auditApi } from '../services/auditApi';
import { Skeleton } from '../components/ui/Skeleton';
import { 
  ArrowUpRight, 
  ArrowDownLeft, 
  Bell, 
  ShieldCheck, 
  Clock, 
  Calendar,
  HelpCircle
} from 'lucide-react';
import { cn } from '../utils/cn';

interface TimelineItem {
  id: string;
  type: 'TRANSACTION' | 'NOTIFICATION' | 'AUDIT';
  title: string;
  description: string;
  timestamp: string;
  status?: string;
  rawType?: string;
}

export const Activity: React.FC = () => {
  const { user } = useAuth();
  const [filterType, setFilterType] = useState<'ALL' | 'TRANSACTION' | 'NOTIFICATION' | 'AUDIT'>('ALL');

  // Queries
  const { data: txData, isLoading: isTxLoading } = useQuery({
    queryKey: ['transactions'],
    queryFn: transactionApi.getTransactions,
  });

  const { data: notifData, isLoading: isNotifLoading } = useQuery({
    queryKey: ['notifications', user?.id],
    queryFn: () => notificationApi.getUserNotifications(user?.id || ''),
    enabled: !!user?.id,
  });

  const { data: auditData, isLoading: isAuditLoading } = useQuery({
    queryKey: ['audit-logs', user?.id],
    queryFn: () => auditApi.getUserLogs(user?.id || ''),
    enabled: !!user?.id,
  });

  const isLoading = isTxLoading || isNotifLoading || isAuditLoading;

  // Flatten and unify timeline elements
  const getTimelineItems = (): TimelineItem[] => {
    const items: TimelineItem[] = [];

    // Transactions
    if (txData?.success && txData.data) {
      txData.data.forEach((tx) => {
        const isOut = tx.type === 'WITHDRAW' || tx.type === 'TRANSFER';
        items.push({
          id: tx.transactionId,
          type: 'TRANSACTION',
          title: tx.type === 'TRANSFER' ? 'Sent Token' : tx.type === 'WITHDRAW' ? 'Withdrew Assets' : 'Deposited Funds',
          description: `${isOut ? '-' : '+'}${tx.amount} ${tx.currency} (Ref: ${tx.referenceNumber.substring(0,8)}...)`,
          timestamp: tx.timestamp,
          status: tx.status,
          rawType: tx.type,
        });
      });
    }

    // Notifications
    if (notifData?.success && notifData.data) {
      notifData.data.forEach((n) => {
        items.push({
          id: n.notificationId,
          type: 'NOTIFICATION',
          title: n.subject || 'System Alert',
          description: n.message,
          timestamp: n.sentAt,
          status: n.status,
        });
      });
    }

    // Audit logs
    if (auditData?.success && auditData.data) {
      auditData.data.forEach((a) => {
        items.push({
          id: a.auditId,
          type: 'AUDIT',
          title: a.action || 'Security Audit',
          description: `${a.description} (${a.serviceName} • IP: ${a.ipAddress})`,
          timestamp: a.eventTimestamp,
          status: 'COMPLETED',
        });
      });
    }

    // Filter type checking
    const filtered = filterType === 'ALL' ? items : items.filter(item => item.type === filterType);

    // Sort chronologically descending
    return filtered.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
  };

  const timelineItems = getTimelineItems();

  // Grouping helper (Today, Yesterday, This Week, Older)
  const groupTimelineByDate = (items: TimelineItem[]) => {
    const groups: { [key: string]: TimelineItem[] } = {};

    items.forEach((item) => {
      const date = new Date(item.timestamp);
      const today = new Date();
      const yesterday = new Date();
      yesterday.setDate(today.getDate() - 1);

      // Start of this week
      const diffTime = today.getTime() - date.getTime();
      const diffDays = diffTime / (1000 * 60 * 60 * 24);

      let key = '';
      if (isNaN(date.getTime())) {
        key = 'Other Activity';
      } else if (date.toDateString() === today.toDateString()) {
        key = 'Today';
      } else if (date.toDateString() === yesterday.toDateString()) {
        key = 'Yesterday';
      } else if (diffDays > 0 && diffDays <= 7) {
        key = 'This Week';
      } else {
        key = date.toLocaleDateString(undefined, { month: 'long', day: 'numeric', year: 'numeric' });
      }

      if (!groups[key]) {
        groups[key] = [];
      }
      groups[key].push(item);
    });

    return groups;
  };

  const groupedTimeline = groupTimelineByDate(timelineItems);
  const groupKeys = Object.keys(groupedTimeline);

  const getIcon = (item: TimelineItem) => {
    switch (item.type) {
      case 'TRANSACTION':
        if (item.rawType === 'WITHDRAW' || item.rawType === 'TRANSFER') {
          return (
            <div className="w-8 h-8 rounded-full bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400 shrink-0">
              <ArrowUpRight size={14} />
            </div>
          );
        }
        return (
          <div className="w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-accent shrink-0">
            <ArrowDownLeft size={14} />
          </div>
        );
      case 'NOTIFICATION':
        return (
          <div className="w-8 h-8 rounded-full bg-violet-500/10 border border-violet-500/20 flex items-center justify-center text-primary shrink-0">
            <Bell size={14} />
          </div>
        );
      case 'AUDIT':
        return (
          <div className="w-8 h-8 rounded-full bg-zinc-800 border border-zinc-700 flex items-center justify-center text-zinc-300 shrink-0">
            <ShieldCheck size={14} />
          </div>
        );
      default:
        return (
          <div className="w-8 h-8 rounded-full bg-zinc-800 border border-zinc-700 flex items-center justify-center text-zinc-400 shrink-0">
            <HelpCircle size={14} />
          </div>
        );
    }
  };

  return (
    <div className="space-y-6 text-left">
      
      {/* Title Header with inline Filter Pills */}
      <div className="flex flex-col md:flex-row md:items-center justify-between border-b border-zinc-900 pb-4 gap-4 select-none">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">Timeline</h1>
          <p className="text-xs text-zinc-400">Chronological list of transactions, audits, and account alerts</p>
        </div>
        
        {/* Responsive filter pills */}
        <div className="flex items-center space-x-1 bg-zinc-950 p-1 rounded-full border border-zinc-900 overflow-x-auto shrink-0 scrollbar-none">
          {(['ALL', 'TRANSACTION', 'NOTIFICATION', 'AUDIT'] as const).map((type) => (
            <button
              key={type}
              onClick={() => setFilterType(type)}
              className={cn(
                "px-3 py-1.5 rounded-full text-[10px] font-bold tracking-wide uppercase transition cursor-pointer whitespace-nowrap",
                filterType === type 
                  ? "bg-zinc-900 text-white border border-zinc-800" 
                  : "text-zinc-500 hover:text-zinc-300"
              )}
            >
              {type === 'ALL' ? 'All Feed' : type + 's'}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-10 w-28 rounded-full" />
          <Skeleton className="h-14 w-full rounded-2xl" />
          <Skeleton className="h-14 w-full rounded-2xl" />
        </div>
      ) : timelineItems.length === 0 ? (
        <div className="py-16 text-center text-xs text-zinc-500 border border-dashed border-zinc-800 rounded-3xl bg-zinc-950/40">
          <Clock className="mx-auto text-zinc-600 mb-2" size={32} />
          No recent activity logs found matching the filters.
        </div>
      ) : (
        <div className="space-y-6">
          {groupKeys.map((groupTitle) => (
            <div key={groupTitle} className="space-y-2.5">
              {/* Group Date Title */}
              <div className="flex items-center space-x-1.5 px-1 py-0.5 select-none">
                <Calendar size={12} className="text-zinc-500" />
                <span className="text-[10px] font-extrabold text-zinc-400 uppercase tracking-widest">{groupTitle}</span>
              </div>

              {/* Items listing */}
              <div className="space-y-2">
                {groupedTimeline[groupTitle].map((item) => {
                  const dateObj = new Date(item.timestamp);
                  const timeString = isNaN(dateObj.getTime()) 
                    ? '' 
                    : dateObj.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });

                  return (
                    <div 
                      key={item.id} 
                      className="flex items-center justify-between p-4 rounded-2xl bg-zinc-900 border border-zinc-850 hover:border-zinc-800 transition"
                    >
                      <div className="flex items-center space-x-3.5 min-w-0 flex-1">
                        {getIcon(item)}
                        <div className="min-w-0 flex-1 text-left">
                          <div className="flex items-center space-x-2">
                            <span className="text-xs font-extrabold text-white truncate block leading-none">{item.title}</span>
                            {item.status && item.status !== 'COMPLETED' && item.status !== 'SENT' && (
                              <span className={cn(
                                "text-[8px] px-2 py-0.2 rounded-full border uppercase font-bold tracking-wider scale-90 shrink-0",
                                item.status === 'PENDING' 
                                  ? "bg-amber-950/40 text-amber-400 border-amber-800/40" 
                                  : "bg-red-950/40 text-red-400 border-red-800/40"
                              )}>
                                {item.status.toLowerCase()}
                              </span>
                            )}
                          </div>
                          <span className="text-[10px] text-zinc-400 mt-1 block truncate max-w-lg leading-relaxed">{item.description}</span>
                        </div>
                      </div>
                      <div className="text-right pl-3 shrink-0 select-none">
                        <span className="text-[10px] text-zinc-500 font-semibold">{timeString}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}

    </div>
  );
};
export default Activity;
