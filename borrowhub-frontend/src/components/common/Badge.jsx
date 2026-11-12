import React from 'react';

export const Badge = ({ children, variant = 'default', size = 'md' }) => {
  const variants = {
    default: 'bg-slate-100 text-slate-800 border-slate-300',
    primary: 'bg-blue-50 text-blue-900 border-blue-300 font-semibold',
    success: 'bg-emerald-50 text-emerald-900 border-emerald-300 font-semibold',
    warning: 'bg-amber-50 text-amber-950 border-amber-300 font-semibold',
    danger: 'bg-rose-50 text-rose-900 border-rose-300 font-semibold',
    info: 'bg-indigo-50 text-indigo-900 border-indigo-300 font-semibold',
  };

  const sizes = {
    sm: 'text-xs px-2 py-0.5',
    md: 'text-xs font-medium px-2.5 py-1',
    lg: 'text-sm font-semibold px-3 py-1.5',
  };

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full border ${variants[variant] || variants.default} ${sizes[size]}`}>
      {children}
    </span>
  );
};
