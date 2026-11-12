import React, { useState, useEffect } from 'react';
import api from '../../api/axios';
import { BookMarked, Search, AlertTriangle, CheckCircle2, Clock, Calendar } from 'lucide-react';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export const MyBorrowingsPage = () => {
  const [borrowings, setBorrowings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    fetchMyBorrowings();
  }, []);

  const fetchMyBorrowings = async () => {
    setLoading(true);
    try {
      const res = await api.get('/borrowings/my');
      if (res.data?.success) {
        setBorrowings(res.data.data || []);
      }
    } catch (err) {
      console.error('Failed to load borrowings', err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge variant="primary">Active Loan</Badge>;
      case 'PARTIALLY_RETURNED':
        return <Badge variant="info">Partially Returned (C12 Active)</Badge>;
      case 'OVERDUE':
        return <Badge variant="danger">Overdue</Badge>;
      case 'RETURNED':
        return <Badge variant="success">Returned</Badge>;
      case 'CLOSED':
        return <Badge variant="default">Closed (Reconciled)</Badge>;
      default:
        return <Badge variant="default">{status}</Badge>;
    }
  };

  const filtered = borrowings.filter((b) => {
    const term = searchTerm.toLowerCase();
    return (
      (b.resourceName && b.resourceName.toLowerCase().includes(term)) ||
      (b.assetTags && b.assetTags.some((t) => t.toLowerCase().includes(term)))
    );
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">My Borrowings & Loans</h1>
          <p className="text-sm text-slate-500">Track current custody windows, due dates, and return audit trails</p>
        </div>
      </div>

      <div className="relative max-w-md">
        <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Filter by resource name or asset tag..."
          className="w-full pl-10 pr-4 py-2 text-sm rounded-xl border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
        />
      </div>

      {loading ? (
        <LoadingSpinner text="Loading borrowing records..." />
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-slate-200">
          <BookMarked className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-800">No borrowing records</h3>
          <p className="text-xs text-slate-500 mt-1">You do not have any active or past borrowing records</p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-600">
              <thead className="bg-slate-50 text-[11px] font-bold text-slate-400 uppercase tracking-wider border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3.5">Resource</th>
                  <th className="px-4 py-3.5">Asset Tag(s)</th>
                  <th className="px-4 py-3.5">Issued / Outstanding</th>
                  <th className="px-4 py-3.5">Issue Date</th>
                  <th className="px-4 py-3.5">Due Date</th>
                  <th className="px-4 py-3.5">Status</th>
                  <th className="px-6 py-3.5 text-right">Actual Return</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.map((b) => (
                  <tr key={b.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-6 py-4 font-semibold text-slate-800">
                      <div>{b.resourceName}</div>
                      <div className="text-[11px] text-slate-400 font-mono">Loan ID #{b.id}</div>
                    </td>
                    <td className="px-4 py-4">
                      {b.assetTags && b.assetTags.length > 0 ? (
                        <div className="flex flex-wrap gap-1">
                          {b.assetTags.map((tag) => (
                            <span key={tag} className="px-2 py-0.5 rounded bg-slate-100 font-mono font-medium text-slate-700">
                              {tag}
                            </span>
                          ))}
                        </div>
                      ) : (
                        <span className="text-slate-400">Bulk Mode</span>
                      )}
                    </td>
                    <td className="px-4 py-4 font-medium text-slate-700">
                      <span className="text-blue-600 font-bold">{b.quantityIssued}</span> Issued •{' '}
                      <span className="text-amber-600 font-bold">{b.quantityOutstanding}</span> Outstanding
                    </td>
                    <td className="px-4 py-4">{b.issueDate || '—'}</td>
                    <td className="px-4 py-4 font-semibold text-slate-800">{b.dueDate}</td>
                    <td className="px-4 py-4">{getStatusBadge(b.status)}</td>
                    <td className="px-6 py-4 text-right font-medium text-slate-700">
                      {b.actualReturnDate ? (
                        <span className="text-emerald-700">{b.actualReturnDate}</span>
                      ) : (
                        <span className="text-slate-400">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
