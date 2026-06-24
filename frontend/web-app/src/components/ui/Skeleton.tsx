import React, { type HTMLAttributes } from 'react';
import { cn } from '../../utils/cn';

export const Skeleton: React.FC<HTMLAttributes<HTMLDivElement>> = ({ className, ...props }) => {
  return (
    <div
      className={cn('animate-pulse rounded-md bg-zinc-800/80', className)}
      {...props}
    />
  );
};
