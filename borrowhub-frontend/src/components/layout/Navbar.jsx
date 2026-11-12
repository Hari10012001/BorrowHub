import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { Package2, LogOut, User as UserIcon, Shield, BookOpen, Clock } from 'lucide-react';
import { Badge } from '../common/Badge';

export const Navbar = () => {
  const { user, logout } = useAuth();

  const getRoleBadgeVariant = (role) => {
    switch (role) {
      case 'ADMIN': return 'danger';
      case 'FACULTY': return 'warning';
      case 'STUDENT': return 'primary';
      default: return 'default';
    }
  };

  return (
    <header className="sticky top-0 z-40 w-full bg-white/90 backdrop-blur-md border-b border-slate-200">
      <div className="flex items-center justify-between px-6 py-3">
        <div className="flex items-center gap-3">
          <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-md shadow-blue-500/20">
            <Package2 className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xl font-bold bg-gradient-to-r from-blue-700 to-indigo-800 bg-clip-text text-transparent">
                BorrowHub
              </span>
              <span className="text-[10px] font-semibold uppercase tracking-wider px-1.5 py-0.5 rounded bg-blue-100 text-blue-700">
                v2.4.8
              </span>
            </div>
            <p className="text-xs text-slate-500 hidden sm:block">Campus Resource & Inventory Management</p>
          </div>
        </div>

        {user && (
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3 pr-4 border-r border-slate-200">
              <div className="flex items-center justify-center w-9 h-9 rounded-full bg-slate-100 text-slate-600 border border-slate-200">
                <UserIcon className="w-5 h-5" />
              </div>
              <div className="text-left hidden md:block">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-slate-800">{user.fullName || user.username}</span>
                  <Badge variant={getRoleBadgeVariant(user.role)} size="sm">
                    {user.role}
                  </Badge>
                </div>
                <span className="text-xs text-slate-500">{user.department} • {user.institutionalId}</span>
              </div>
            </div>

            <button
              onClick={logout}
              title="Logout"
              className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-600 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors border border-transparent hover:border-rose-200"
            >
              <LogOut className="w-4 h-4" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
