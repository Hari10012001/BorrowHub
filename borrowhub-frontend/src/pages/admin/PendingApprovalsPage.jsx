import React, { useState, useEffect } from 'react';
import api from '../../api/axios';
import { CheckSquare, Check, X, AlertCircle, CheckCircle2, User, Calendar } from 'lucide-react';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export const PendingApprovalsPage = () => {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [reviewNotes, setReviewNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionSuccess, setActionSuccess] = useState('');
  const [actionError, setActionError] = useState('');

  useEffect(() => {
    fetchPending();
  }, []);

  const fetchPending = async () => {
    setLoading(true);
    try {
      const res = await api.get('/borrowings/requests/pending');
      if (res.data?.success) {
        setRequests(res.data.data || []);
      }
    } catch (err) {
      console.error('Failed to load pending requests', err);
    } finally {
      setLoading(false);
    }
  };

  const handleReview = async (approved) => {
    if (!selectedRequest) return;
    setIsSubmitting(true);
    setActionError('');
    setActionSuccess('');

    try {
      const res = await api.post(`/borrowings/requests/${selectedRequest.id}/review`, {
        approved,
        reviewNotes,
      });

      if (res.data?.success) {
        setActionSuccess(`Request ${approved ? 'Approved and HELD reservation created' : 'Rejected'}.`);
        setSelectedRequest(null);
        setReviewNotes('');
        fetchPending();
      }
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || 'Review submission failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Pending Borrowing Approvals</h1>
          <p className="text-sm text-slate-500">Review faculty & student borrowing applications requiring authorization</p>
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
        <LoadingSpinner text="Fetching pending review queue..." />
      ) : requests.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-slate-200">
          <CheckSquare className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-800">No requests pending review</h3>
          <p className="text-xs text-slate-500 mt-1">All borrower requests have been processed</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {requests.map((req) => (
            <div key={req.id} className="bg-white rounded-2xl border border-slate-200 p-5 space-y-4 shadow-sm">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-xs font-mono font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
                    REQ #{req.id}
                  </span>
                  <h3 className="font-bold text-slate-900 mt-1">{req.resourceName}</h3>
                </div>
                <Badge variant="warning">Pending Review</Badge>
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs pt-2 border-t border-slate-100">
                <div>
                  <span className="text-slate-400 block">Requester</span>
                  <span className="font-semibold text-slate-800 mt-0.5 block">{req.userName}</span>
                </div>
                <div>
                  <span className="text-slate-400 block">Quantity</span>
                  <span className="font-semibold text-slate-800 mt-0.5 block">{req.quantity} Units</span>
                </div>
                <div className="col-span-2">
                  <span className="text-slate-400 block">Requested Window</span>
                  <span className="font-semibold text-slate-800 mt-0.5 block">
                    {req.requestedStartDate} to {req.requestedEndDate}
                  </span>
                </div>
                {req.purpose && (
                  <div className="col-span-2 p-2 bg-slate-50 rounded-lg text-slate-700">
                    <span className="font-semibold block text-slate-500 text-[10px] uppercase">Purpose:</span>
                    <span>{req.purpose}</span>
                  </div>
                )}
              </div>

              <div className="pt-2 flex justify-end gap-2">
                <button
                  onClick={() => setSelectedRequest(req)}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold shadow-sm transition"
                >
                  Review Request
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Review Modal */}
      <Modal
        isOpen={!!selectedRequest}
        onClose={() => setSelectedRequest(null)}
        title={`Review Request REQ #${selectedRequest?.id}`}
      >
        {selectedRequest && (
          <div className="space-y-4">
            <div className="text-xs space-y-2 p-3 bg-slate-50 rounded-xl">
              <div>
                <span className="text-slate-400">Borrower: </span>
                <span className="font-bold text-slate-800">{selectedRequest.userName}</span>
              </div>
              <div>
                <span className="text-slate-400">Resource: </span>
                <span className="font-bold text-slate-800">{selectedRequest.resourceName} ({selectedRequest.quantity} units)</span>
              </div>
              <div>
                <span className="text-slate-400">Window: </span>
                <span className="font-semibold text-slate-800">{selectedRequest.requestedStartDate} to {selectedRequest.requestedEndDate}</span>
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Review Notes / Justification</label>
              <textarea
                rows="3"
                value={reviewNotes}
                onChange={(e) => setReviewNotes(e.target.value)}
                placeholder="Optional comments or rejection rationale..."
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>

            <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
              <button
                type="button"
                disabled={isSubmitting}
                onClick={() => handleReview(false)}
                className="px-4 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 border border-rose-200 rounded-xl transition"
              >
                Reject Request
              </button>
              <button
                type="button"
                disabled={isSubmitting}
                onClick={() => handleReview(true)}
                className="px-5 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl shadow-sm transition"
              >
                Approve &amp; Hold Inventory
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
