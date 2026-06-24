import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { notificationApi } from '../services/notificationApi';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import { Button } from '../components/ui/Button';
import { Bell, Mail, RefreshCw } from 'lucide-react';

export const Notifications: React.FC = () => {
  const { user } = useAuth();

  const { data: notificationsData, isLoading, refetch, isRefetching } = useQuery({
    queryKey: ['notifications', user?.id],
    queryFn: () => notificationApi.getUserNotifications(user?.id || ''),
    enabled: !!user?.id,
  });

  const notifications = notificationsData?.data || [];

  const getStatusBadge = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'SENT':
        return <Badge variant="success">Sent</Badge>;
      case 'FAILED':
        return <Badge variant="destructive">Failed</Badge>;
      case 'PENDING':
      default:
        return <Badge variant="warning">Pending</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between text-left">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-white m-0">Notifications</h1>
          <p className="text-sm text-muted-foreground mt-1">
            System dispatch alerts, security email tracking status, and account activity statements.
          </p>
        </div>
        <Button
          variant="glass"
          size="sm"
          onClick={() => refetch()}
          className="flex items-center space-x-1"
          disabled={isLoading || isRefetching}
        >
          <RefreshCw size={14} className={isRefetching ? 'animate-spin' : ''} />
          <span>Refresh</span>
        </Button>
      </div>

      <Card className="text-left">
        <CardHeader>
          <CardTitle>Security & Alert Notifications</CardTitle>
          <CardDescription>Audited list of SMTP messages sent to {user?.email}</CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-4">
              <Skeleton className="h-16 w-full" />
              <Skeleton className="h-16 w-full" />
              <Skeleton className="h-16 w-full" />
            </div>
          ) : notifications.length === 0 ? (
            <div className="py-20 text-center text-xs text-muted-foreground border border-dashed border-zinc-800 rounded-lg">
              <Bell size={24} className="mx-auto text-zinc-600 mb-2" />
              <p>No notifications logs available for this account.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {notifications.map((n) => (
                <div
                  key={n.notificationId}
                  className="p-4 rounded-xl bg-zinc-950/60 border border-zinc-900/80 hover:border-zinc-800/80 transition-all flex items-start space-x-4"
                >
                  <div className="w-10 h-10 rounded-lg bg-zinc-900 border border-zinc-800 flex items-center justify-center text-primary/80 shrink-0">
                    <Mail size={18} />
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1.5">
                      <h4 className="text-sm font-semibold text-white truncate">{n.subject}</h4>
                      <div className="flex items-center space-x-2 text-[10px] text-muted-foreground font-semibold shrink-0">
                        <span>{new Date(n.sentAt).toLocaleString()}</span>
                        {getStatusBadge(n.status)}
                      </div>
                    </div>
                    <p className="text-xs text-zinc-400 mt-1 leading-normal font-mono break-all bg-zinc-900/30 p-2 rounded border border-zinc-900/40">
                      {n.message}
                    </p>
                    <div className="mt-2.5 flex items-center space-x-1.5 text-[10px] text-muted-foreground">
                      <span className="bg-zinc-900 px-2 py-0.5 rounded font-mono border border-zinc-800">
                        Type: {n.type}
                      </span>
                      <span>•</span>
                      <span className="font-mono text-zinc-600">ID: {n.notificationId}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};
export default Notifications;
