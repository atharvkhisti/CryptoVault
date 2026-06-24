import React, { type HTMLAttributes } from 'react';
import { cn } from '../../utils/cn';

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  hoverGlow?: boolean;
}

export const Card: React.FC<CardProps> = ({ children, className, hoverGlow = false, ...props }) => {
  return (
    <div
      className={cn(
        'glass-card rounded-[24px] p-5 border border-zinc-800/80 bg-zinc-900/60 overflow-hidden relative',
        hoverGlow && 'hover-glow',
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
};

export const CardHeader: React.FC<HTMLAttributes<HTMLDivElement>> = ({ children, className, ...props }) => {
  return <div className={cn('flex flex-col space-y-1 mb-4 text-left', className)} {...props}>{children}</div>;
};

export const CardTitle: React.FC<HTMLAttributes<HTMLHeadingElement>> = ({ children, className, ...props }) => {
  return (
    <h3 className={cn('font-semibold text-lg tracking-tight text-white', className)} {...props}>
      {children}
    </h3>
  );
};

export const CardDescription: React.FC<HTMLAttributes<HTMLParagraphElement>> = ({ children, className, ...props }) => {
  return (
    <p className={cn('text-xs text-muted-foreground', className)} {...props}>
      {children}
    </p>
  );
};

export const CardContent: React.FC<HTMLAttributes<HTMLDivElement>> = ({ children, className, ...props }) => {
  return <div className={cn('text-sm text-zinc-300', className)} {...props}>{children}</div>;
};

export const CardFooter: React.FC<HTMLAttributes<HTMLDivElement>> = ({ children, className, ...props }) => {
  return <div className={cn('flex items-center mt-4 pt-4 border-t border-zinc-800/50', className)} {...props}>{children}</div>;
};
