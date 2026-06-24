import { forwardRef, type InputHTMLAttributes } from 'react';
import { cn } from '../../utils/cn';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, type = 'text', label, error, ...props }, ref) => {
    return (
      <div className="w-full text-left">
        {label && (
          <label className="block text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1.5">
            {label}
          </label>
        )}
        <input
          type={type}
          ref={ref}
          className={cn(
            'w-full px-4 py-3 rounded-[16px] border text-sm transition-all focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary placeholder-zinc-600',
            'bg-zinc-900/60 border-zinc-800 text-foreground',
            error && 'border-destructive focus:ring-destructive focus:border-destructive',
            className
          )}
          {...props}
        />
        {error && (
          <p className="mt-1 text-xs text-red-500 font-medium">
            {error}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
