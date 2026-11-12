import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  Grid,
  BookMarked,
  Clock,
  ListOrdered,
  ScanLine,
  CheckSquare,
  Settings,
  Layers,
  Sparkles,
} from 'lucide-react';

export const Sidebar = () => {
  const { user } = useAuth();
  const isAdminOrFaculty = user?.role === 'ADMIN' || user?.role === 'FACULTY';
  const isAdmin = user?.role === 'ADMIN';

  const navItemClass = ({ isActive }) =>
    `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
      isActive
        ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20 font-semibold'
        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
    }`;

  return (
    <aside className="w-64 border-r border-slate-200 bg-white/70 min-h-[calc(100vh-65px)] p-4 flex flex-col justify-between">
      <div className="space-y-6">
        <div>
          <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider px-3">
            Resource Catalog
          </span>
          <nav className="mt-2 space-y-1">
            <NavLink to="/catalog" className={navItemClass}>
              <Grid className="w-4 h-4" />
              <span>Browse Catalog</span>
            </NavLink>
          </nav>
        </div>

        <div>
          <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider px-3">
            My Activity
          </span>
          <nav className="mt-2 space-y-1">
            <NavLink to="/borrowings" className={navItemClass}>
              <BookMarked className="w-4 h-4" />
              <span>My Borrowings</span>
              {user?.activeBorrowingsCount > 0 && (
                <span className="ml-auto px-2 py-0.5 text-[10px] font-bold rounded-full bg-blue-100 text-blue-800">
                  {user.activeBorrowingsCount}
                </span>
              )}
            </NavLink>
            <NavLink to="/reservations" className={navItemClass}>
              <Clock className="w-4 h-4" />
              <span>My Reservations</span>
              {user?.activeReservationsCount > 0 && (
                <span className="ml-auto px-2 py-0.5 text-[10px] font-bold rounded-full bg-emerald-100 text-emerald-800">
                  {user.activeReservationsCount}
                </span>
              )}
            </NavLink>
            <NavLink to="/waitlist" className={navItemClass}>
              <ListOrdered className="w-4 h-4" />
              <span>My Waitlist</span>
            </NavLink>
          </nav>
        </div>

        {isAdminOrFaculty && (
          <div>
            <span className="text-[11px] font-bold text-indigo-500 uppercase tracking-wider px-3">
              Circulation Desk
            </span>
            <nav className="mt-2 space-y-1">
              <NavLink to="/circulation" className={navItemClass}>
                <ScanLine className="w-4 h-4" />
                <span>Issue & Returns</span>
              </NavLink>
              <NavLink to="/approvals" className={navItemClass}>
                <CheckSquare className="w-4 h-4" />
                <span>Pending Approvals</span>
              </NavLink>
            </nav>
          </div>
        )}

        {isAdmin && (
          <div>
            <span className="text-[11px] font-bold text-purple-600 uppercase tracking-wider px-3">
              Administration
            </span>
            <nav className="mt-2 space-y-1">
              <NavLink to="/admin/resources" className={navItemClass}>
                <Layers className="w-4 h-4" />
                <span>Inventory & Assets</span>
              </NavLink>
            </nav>
          </div>
        )}
      </div>

      <div className="p-3 bg-gradient-to-br from-slate-50 to-blue-50 border border-slate-200/80 rounded-xl text-xs text-slate-600">
        <div className="flex items-center gap-1.5 font-semibold text-slate-800 mb-1">
          <Sparkles className="w-3.5 h-3.5 text-blue-600" />
          <span>Central Time Arbiter</span>
        </div>
        <p className="text-[11px] text-slate-500">
          Enforcing MySQL <code>CURRENT_DATE()</code> &amp; strict Level 1-6 InnoDB locking (C42, C43).
        </p>
      </div>
    </aside>
  );
};
