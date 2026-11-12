import React, { useState, useEffect } from 'react';
import api from '../../api/axios';
import { ListOrdered, AlertCircle, CheckCircle2, Clock, Sparkles } from 'lucide-react';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export const MyWaitlistPage = () => {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionSuccess, setActionSuccess] = useState('');
  const [actionError, setActionError] = useState('');

  useEffect(() => {
    fetchWaitlist();
  }, []);

  const fetchWaitlist = async () => {
    setLoading(true);
    try {
      const res = await api.get('/waitlist/my');
      if (res.data?.success) {
        setEntries(res.data.data || []);
      }
    } catch (err) {
      console.error('Failed to load waitlist', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async (id) => {
    if (!window.confirm('Cancel your waitlist queue position?')) return;
    setActionError('');
    setActionSuccess('');
    try {
      const res = await api.post(`/waitlist/${id}/cancel`);
      if (res.data?.success) {
        setActionSuccess('Waitlist entry cancelled.');
        fetchWaitlist();
      }
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || 'Failed to cancel waitlist entry');
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'WAITING':
        return <Badge variant="warning">In Queue</Badge>;
      case 'NOTIFIED':
        return <Badge variant="success">Claim Notification Active!</Badge>;
      case 'CLAIMED':
        return <Badge variant="info">Claimed</Badge>;
      case 'CANCELLED':
        return <Badge variant="default">Cancelled</Badge>;
      default:
        return <Badge variant="default">{status}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">My Waitlist Positions</h1>
          <p className="text-sm text-slate-500">Autonomous queue promotion backed by MySQL Transactional Outbox</p>
        </div>
      </div>

      {actionSuccess && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          <span>{actionSuccess}</span>
        </div>
      )}

      {actionError && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-sm flex items-center gap-2">
          <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
          <span>{actionError}</span>
        </div>
      )}

      {loading ? (
        <LoadingSpinner text="Loading waitlist queue..." />
      ) : entries.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-slate-200">
          <ListOrdered className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-800">No active waitlist positions</h3>
          <p className="text-xs text-slate-500 mt-1">When an item is out of stock, join the queue from the catalog</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {entries.map((item) => (
            <div key={item.id} className="bg-white rounded-2xl border border-slate-200 p-5 space-y-4 shadow-sm">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-xs font-mono font-bold text-amber-600 bg-amber-50 px-2 py-0.5 rounded">
                    Position #{item.positionInQueue}
                  </span>
                  <h3 className="font-bold text-slate-900 mt-1">{item.resourceName}</h3>
                </div>
                {getStatusBadge(item.status)}
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs pt-2 border-t border-slate-100">
                <div>
                  <span className="text-slate-400 block">Priority Tier</span>
                  <span className="font-semibold text-slate-800 mt-0.5 block">
                    Tier {item.priorityTier} {item.priorityTier === 1 ? '(Faculty/Staff)' : '(Student)'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 block">Needed Period</span>
                  <span className="font-semibold text-slate-800 mt-0.5 block">
                    {item.neededFrom} to {item.neededUntil}
                  </span>
                </div>
                {item.claimDeadline && (
                  <div className="col-span-2 p-2 bg-emerald-50 rounded-lg border border-emerald-200 text-emerald-800">
                    <span className="font-bold">Claim Window Closes: </span>
                    <span>{item.claimDeadline}</span>
                  </div>
                )}
              </div>

              {item.status === 'WAITING' && (
                <div className="pt-2 flex justify-end">
                  <button
                    onClick={() => handleCancel(item.id)}
                    className="px-3 py-1.5 rounded-lg text-xs font-medium text-rose-600 hover:bg-rose-50 border border-rose-200 transition"
                  >
                    Leave Queue
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
