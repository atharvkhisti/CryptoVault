import React, { type HTMLAttributes } from 'react';
import { cn } from '../../utils/cn';

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: 'default' | 'success' | 'warning' | 'destructive' | 'info' | 'secondary';
}

export const Badge: React.FC<BadgeProps> = ({ children, className, variant = 'default', ...props }) => {
  const baseStyles = 'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold select-none border tracking-wide uppercase';
  
  const variants = {
    default: 'bg-zinc-800 text-zinc-200 border-zinc-700',
    success: 'bg-emerald-950/40 text-emerald-400 border-emerald-800/60',
    warning: 'bg-amber-950/40 text-amber-400 border-amber-800/60',
    destructive: 'bg-red-950/40 text-red-400 border-red-800/60',
    info: 'bg-blue-950/40 text-blue-400 border-blue-800/60',
    secondary: 'bg-zinc-900 text-zinc-400 border-zinc-800',
  };

  return (
    <span className={cn(baseStyles, variants[variant], className)} {...props}>
      {children}
    </span>
  );
};

export const getKycBadge = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'APPROVED':
      return <Badge variant="success">Approved</Badge>;
    case 'UNDER_REVIEW':
      return <Badge variant="warning">Under Review</Badge>;
    case 'REJECTED':
      return <Badge variant="destructive">Rejected</Badge>;
    case 'PENDING':
    default:
      return <Badge variant="info">Pending</Badge>;
  }
};

export const getTransactionStatusBadge = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'COMPLETED':
      return <Badge variant="success">Completed</Badge>;
    case 'PENDING':
      return <Badge variant="warning">Pending</Badge>;
    case 'FAILED':
      return <Badge variant="destructive">Failed</Badge>;
    case 'BLOCKED':
      return <Badge variant="destructive">Blocked</Badge>;
    default:
      return <Badge variant="default">{status}</Badge>;
  }
};

export const getRiskBadge = (level: string) => {
  switch (level?.toUpperCase()) {
    case 'LOW':
      return <Badge variant="success">Low</Badge>;
    case 'MEDIUM':
      return <Badge variant="warning">Medium</Badge>;
    case 'HIGH':
      return <Badge variant="destructive">High</Badge>;
    case 'CRITICAL':
      return <Badge className="bg-purple-950/40 text-purple-400 border-purple-800/60">Critical</Badge>;
    default:
      return <Badge variant="default">{level}</Badge>;
  }
};
