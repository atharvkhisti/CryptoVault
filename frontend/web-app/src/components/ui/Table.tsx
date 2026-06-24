import React, { type HTMLAttributes, type TableHTMLAttributes } from 'react';
import { cn } from '../../utils/cn';

export const Table: React.FC<TableHTMLAttributes<HTMLTableElement>> = ({ children, className, ...props }) => {
  return (
    <div className="w-full overflow-x-auto rounded-lg border border-zinc-800 bg-zinc-950/40">
      <table className={cn('w-full border-collapse text-sm text-left text-zinc-300', className)} {...props}>
        {children}
      </table>
    </div>
  );
};

export const TableHeader: React.FC<HTMLAttributes<HTMLTableSectionElement>> = ({ children, className, ...props }) => {
  return (
    <thead className={cn('border-b border-zinc-800 bg-zinc-900/40 text-xs font-semibold uppercase tracking-wider text-muted-foreground', className)} {...props}>
      {children}
    </thead>
  );
};

export const TableBody: React.FC<HTMLAttributes<HTMLTableSectionElement>> = ({ children, className, ...props }) => {
  return <tbody className={cn('[&_tr:last-child]:border-0', className)} {...props}>{children}</tbody>;
};

export const TableRow: React.FC<HTMLAttributes<HTMLTableRowElement>> = ({ children, className, ...props }) => {
  return (
    <tr className={cn('border-b border-zinc-800/80 hover:bg-zinc-900/25 transition-colors', className)} {...props}>
      {children}
    </tr>
  );
};

export const TableHead: React.FC<HTMLAttributes<HTMLTableCellElement>> = ({ children, className, ...props }) => {
  return <th className={cn('px-4 py-3 font-semibold text-zinc-400 select-none', className)} {...props}>{children}</th>;
};

export const TableCell: React.FC<HTMLAttributes<HTMLTableCellElement>> = ({ children, className, ...props }) => {
  return <td className={cn('px-4 py-3.5 align-middle', className)} {...props}>{children}</td>;
};
