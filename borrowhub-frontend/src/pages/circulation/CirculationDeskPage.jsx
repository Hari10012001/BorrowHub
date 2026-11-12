import React, { useState, useEffect } from 'react';
import api from '../../api/axios';
import {
  ScanLine,
  ArrowRight,
  RotateCcw,
  CheckCircle2,
  AlertCircle,
  QrCode,
  ShieldAlert,
  Sparkles,
  Search,
} from 'lucide-react';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export const CirculationDeskPage = () => {
  const [activeTab, setActiveTab] = useState('checkout'); // 'checkout' | 'return' | 'records'
  const [records, setRecords] = useState([]);
  const [loadingRecords, setLoadingRecords] = useState(false);

  // Checkout Form
  const [checkoutData, setCheckoutData] = useState({
    reservationId: '',
    userId: '4', // student1 default ID
    resourceId: '1',
    quantity: '1',
    dueDate: new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0],
    unitIds: '1',
    notes: 'Desk checkout',
  });

  // Return Form
  const [returnData, setFormData] = useState({
    borrowingRecordId: '',
    quantityReturned: '1',
    returnCondition: 'GOOD',
    damageNotes: '',
    notes: 'Regular desk check-in',
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionSuccess, setActionSuccess] = useState('');
  const [actionError, setActionError] = useState('');

  useEffect(() => {
    if (activeTab === 'records') {
      fetchRecords();
    }
  }, [activeTab]);

  const fetchRecords = async () => {
    setLoadingRecords(true);
    try {
      const res = await api.get('/circulation/records');
      if (res.data?.success) {
        setRecords(res.data.data.content || []);
      }
    } catch (err) {
      console.error('Failed to load desk records', err);
    } finally {
      setLoadingRecords(false);
    }
  };

  const handleCheckoutSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setActionError('');
    setActionSuccess('');

    try {
      const idempotencyKey = 'IDEM-' + Math.random().toString(36).substring(2, 12);
      const payload = {
        userId: parseInt(checkoutData.userId, 10),
        resourceId: parseInt(checkoutData.resourceId, 10),
        quantity: parseInt(checkoutData.quantity, 10),
        dueDate: checkoutData.dueDate,
        notes: checkoutData.notes,
      };

      if (checkoutData.reservationId) {
        payload.reservationId = parseInt(checkoutData.reservationId, 10);
      }
      if (checkoutData.unitIds) {
        payload.unitIds = checkoutData.unitIds.split(',').map((id) => parseInt(id.trim(), 10));
      }

      const res = await api.post('/circulation/checkout', payload, {
        headers: { 'Idempotency-Key': idempotencyKey },
      });

      if (res.data?.success) {
        const d = res.data.data;
        let msg = `Checkout successful! Transaction Loan #${d.borrowingRecordId} created.`;
        if (d.reservationRemainderReleased > 0) {
          msg += ` Partial checkout: ${d.reservationRemainderReleased} remainder units released back to stock (C30/C38).`;
        }
        setActionSuccess(msg);
      }
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || 'Checkout failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReturnSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setActionError('');
    setActionSuccess('');

    try {
      const idempotencyKey = 'IDEM-RET-' + Math.random().toString(36).substring(2, 12);
      const payload = {
        borrowingRecordId: parseInt(returnData.borrowingRecordId, 10),
        quantityReturned: parseInt(returnData.quantityReturned, 10),
        returnCondition: returnData.returnCondition,
        damageNotes: returnData.damageNotes,
        notes: returnData.notes,
      };

      const res = await api.post('/circulation/return', payload, {
        headers: { 'Idempotency-Key': idempotencyKey },
      });

      if (res.data?.success) {
        const d = res.data.data;
        setActionSuccess(`Return processed! Status: ${d.status} • Outstanding balance: ${d.quantityOutstanding}`);
      }
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || 'Return failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Circulation Desk</h1>
          <p className="text-sm text-slate-500">Asset checkouts, returns, physical asset tracking, and condition audit</p>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-1 p-1 bg-slate-200/60 rounded-xl text-xs font-semibold">
          <button
            onClick={() => setActiveTab('checkout')}
            className={`px-3 py-1.5 rounded-lg transition ${activeTab === 'checkout' ? 'bg-white text-blue-700 shadow-sm' : 'text-slate-600 hover:text-slate-900'}`}
          >
            Issue / Checkout
          </button>
          <button
            onClick={() => setActiveTab('return')}
            className={`px-3 py-1.5 rounded-lg transition ${activeTab === 'return' ? 'bg-white text-blue-700 shadow-sm' : 'text-slate-600 hover:text-slate-900'}`}
          >
            Process Return
          </button>
          <button
            onClick={() => setActiveTab('records')}
            className={`px-3 py-1.5 rounded-lg transition ${activeTab === 'records' ? 'bg-white text-blue-700 shadow-sm' : 'text-slate-600 hover:text-slate-900'}`}
          >
            Audit Log Records
          </button>
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

      {activeTab === 'checkout' && (
        <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm max-w-2xl space-y-6">
          <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
            <ScanLine className="w-5 h-5 text-blue-600" />
            <h2 className="font-bold text-slate-800 text-base">Issue Resource to Borrower</h2>
          </div>

          <form onSubmit={handleCheckoutSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-700">Reservation ID (Optional for walk-in)</label>
                <input
                  type="number"
                  placeholder="e.g. 1"
                  value={checkoutData.reservationId}
                  onChange={(e) => setCheckoutData({ ...checkoutData, reservationId: e.target.value })}
                  className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-700">Borrower User ID</label>
                <input
                  type="number"
                  required
                  placeholder="e.g. 4 (student1)"
                  value={checkoutData.userId}
                  onChange={(e) => setCheckoutData({ ...checkoutData, userId: e.target.value })}
                  className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-700">Resource ID</label>
                <input
                  type="number"
                  required
                  placeholder="e.g. 1 (Dell Laptop)"
                  value={checkoutData.resourceId}
                  onChange={(e) => setCheckoutData({ ...checkoutData, resourceId: e.target.value })}
                  className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-700">Quantity to Issue</label>
                <input
                  type="number"
                  min="1"
                  required
                  value={checkoutData.quantity}
                  onChange={(e) => setCheckoutData({ ...checkoutData, quantity: e.target.value })}
                  className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-700">Due Date</label>
                <input
                  type="date"
                  required
                  value={checkoutData.dueDate}
                  onChange={(e) => setCheckoutData({ ...checkoutData, dueDate: e.target.value })}
                  className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-700">Physical Unit IDs (Comma separated for serialized)</label>
                <input
                  type="text"
                  placeholder="e.g. 1, 2"
                  value={checkoutData.unitIds}
                  onChange={(e) => setCheckoutData({ ...checkoutData, unitIds: e.target.value })}
                  className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
                />
              </div>
            </div>

            <div className="p-3 bg-blue-50 rounded-xl border border-blue-200 text-xs text-blue-900 space-y-1">
              <div className="font-semibold flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-blue-600" />
                <span>Architecture Guarantees (C42, C43, C50)</span>
              </div>
              <p className="text-[11px] text-blue-800">
                Level 1-4 DAG lock ordering is strictly enforced. The issue timestamp is generated server-side by MySQL <code>CURRENT_DATE()</code> with zero JVM clock dependency.
              </p>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold shadow-md shadow-blue-500/20 transition disabled:opacity-50"
            >
              {isSubmitting ? 'Acquiring Level 1-4 Locks...' : 'Authorize & Complete Checkout'}
            </button>
          </form>
        </div>
      )}

      {activeTab === 'return' && (
        <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm max-w-2xl space-y-6">
          <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
            <RotateCcw className="w-5 h-5 text-indigo-600" />
            <h2 className="font-bold text-slate-800 text-base">Check-In Returned Assets</h2>
          </div>

          <form onSubmit={handleReturnSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-700">Borrowing Loan ID</label>
                <input
                  type="number"
                  required
                  placeholder="e.g. 1"
                  value={returnData.borrowingRecordId}
                  onChange={(e) => setFormData({ ...returnData, borrowingRecordId: e.target.value })}
                  className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-700">Quantity Returned</label>
                <input
                  type="number"
                  min="1"
                  required
                  value={returnData.quantityReturned}
                  onChange={(e) => setFormData({ ...returnData, quantityReturned: e.target.value })}
                  className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Condition on Return</label>
              <select
                value={returnData.returnCondition}
                onChange={(e) => setFormData({ ...returnData, returnCondition: e.target.value })}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              >
                <option value="GOOD">Good / Functional (Return to available stock)</option>
                <option value="NEW">Like New</option>
                <option value="FAIR">Fair (Minor wear)</option>
                <option value="DAMAGED">Damaged (Routes to Maintenance Bay)</option>
                <option value="POOR">Poor</option>
              </select>
            </div>

            {returnData.returnCondition === 'DAMAGED' && (
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-700">Damage Diagnostic Notes</label>
                <textarea
                  rows="2"
                  placeholder="Describe damage..."
                  value={returnData.damageNotes}
                  onChange={(e) => setFormData({ ...returnData, damageNotes: e.target.value })}
                  className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
                />
              </div>
            )}

            <div className="p-3 bg-indigo-50 rounded-xl border border-indigo-200 text-xs text-indigo-900 space-y-1">
              <div className="font-semibold flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
                <span>Return Reconciliation (C12, C13, C17)</span>
              </div>
              <p className="text-[11px] text-indigo-800">
                If partially returned, the remaining balance retains its custody window against total inventory. On complete return, stock is replenished and waitlists are notified via outbox events.
              </p>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-sm font-semibold shadow-md shadow-indigo-500/20 transition disabled:opacity-50"
            >
              {isSubmitting ? 'Reconciling Return...' : 'Process Return & Reconcile Inventory'}
            </button>
          </form>
        </div>
      )}

      {activeTab === 'records' && (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
          {loadingRecords ? (
            <LoadingSpinner text="Loading circulation audit records..." />
          ) : records.length === 0 ? (
            <div className="text-center py-12 text-slate-500 text-sm">No circulation records found</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-600">
                <thead className="bg-slate-50 text-[11px] font-bold text-slate-400 uppercase tracking-wider border-b border-slate-200">
                  <tr>
                    <th className="px-6 py-3.5">Loan ID</th>
                    <th className="px-4 py-3.5">Borrower</th>
                    <th className="px-4 py-3.5">Resource</th>
                    <th className="px-4 py-3.5">Issued / Outstanding</th>
                    <th className="px-4 py-3.5">Due Date</th>
                    <th className="px-4 py-3.5">Status</th>
                    <th className="px-6 py-3.5 text-right">Actual Return</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {records.map((r) => (
                    <tr key={r.id} className="hover:bg-slate-50 transition">
                      <td className="px-6 py-4 font-mono font-bold text-slate-800">#{r.id}</td>
                      <td className="px-4 py-4 font-semibold text-slate-800">{r.userName}</td>
                      <td className="px-4 py-4">{r.resourceName}</td>
                      <td className="px-4 py-4 font-medium text-slate-700">
                        {r.quantityIssued} issued • {r.quantityOutstanding} outstanding
                      </td>
                      <td className="px-4 py-4 font-semibold text-slate-800">{r.dueDate}</td>
                      <td className="px-4 py-4">
                        <Badge variant={r.status === 'ACTIVE' ? 'primary' : r.status === 'RETURNED' ? 'success' : 'warning'}>
                          {r.status}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-right font-medium text-slate-700">{r.actualReturnDate || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
