import React, { useState, useEffect } from 'react';
import api from '../../api/axios';
import { Clock, Calendar, AlertCircle, CheckCircle2, XCircle } from 'lucide-react';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export const MyReservationsPage = () => {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionSuccess, setActionSuccess] = useState('');
  const [actionError, setActionError] = useState('');

  useEffect(() => {
    fetchMyReservations();
  }, []);

  const fetchMyReservations = async () => {
    setLoading(true);
    try {
      const res = await api.get('/reservations/my');
      if (res.data?.success) {
        setReservations(res.data.data || []);
      }
    } catch (err) {
      console.error('Failed to load reservations', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async (id) => {
    if (!window.confirm('Are you sure you want to release this held reservation?')) return;
    setActionError('');
    setActionSuccess('');
    try {
      const res = await api.post(`/reservations/${id}/cancel`, { reason: 'User requested cancellation' });
      if (res.data?.success) {
        setActionSuccess('Reservation cancelled and items released back to stock.');
        fetchMyReservations();
      }
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || 'Failed to cancel reservation');
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'HELD':
        return <Badge variant="warning">Hold Active</Badge>;
      case 'CLAIMED':
        return <Badge variant="success">Claimed & Checked Out</Badge>;
      case 'CANCELLED':
        return <Badge variant="default">Cancelled</Badge>;
      case 'EXPIRED':
        return <Badge variant="danger">Hold Expired</Badge>;
      default:
        return <Badge variant="default">{status}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">My Reservations & Holds</h1>
          <p className="text-sm text-slate-500">Active holds are retained for 48 hours or until pickup at the circulation desk</p>
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
        <LoadingSpinner text="Fetching reservations..." />
      ) : reservations.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-slate-200">
          <Clock className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-800">No active reservations</h3>
          <p className="text-xs text-slate-500 mt-1">Browse the catalog to place an item on hold</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {reservations.map((r) => {
            const isHeld = r.status === 'HELD';
            return (
              <div key={r.id} className="bg-white rounded-2xl border border-slate-200 p-5 space-y-4 shadow-sm">
                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-xs font-mono font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
                      RES #{r.id}
                    </span>
                    <h3 className="font-bold text-slate-900 mt-1">{r.resourceName}</h3>
                  </div>
                  {getStatusBadge(r.status)}
                </div>

                <div className="grid grid-cols-2 gap-3 text-xs pt-2 border-t border-slate-100">
                  <div>
                    <span className="text-slate-400 block">Quantity Held</span>
                    <span className="font-semibold text-slate-800 mt-0.5 block">{r.quantityReserved} Units</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Pickup Deadline</span>
                    <span className="font-semibold text-slate-800 mt-0.5 block">{r.holdExpiresAt || '48 Hours'}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Scheduled Start</span>
                    <span className="font-semibold text-slate-800 mt-0.5 block">{r.startDate}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Scheduled Return</span>
                    <span className="font-semibold text-slate-800 mt-0.5 block">{r.endDate}</span>
                  </div>
                </div>

                {r.reservedAssetTags && r.reservedAssetTags.length > 0 && (
                  <div className="text-xs">
                    <span className="text-slate-400 block mb-1">Allocated Units:</span>
                    <div className="flex flex-wrap gap-1">
                      {r.reservedAssetTags.map((tag) => (
                        <span key={tag} className="px-2 py-0.5 rounded bg-slate-100 font-mono text-[11px] font-semibold text-slate-700">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                {isHeld && (
                  <div className="pt-2 flex justify-end">
                    <button
                      onClick={() => handleCancel(r.id)}
                      className="px-3 py-1.5 rounded-lg text-xs font-medium text-rose-600 hover:bg-rose-50 border border-rose-200 transition"
                    >
                      Cancel Reservation
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
