import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import { useAuth } from '../../context/AuthContext';
import {
  ArrowLeft,
  Calendar,
  CheckCircle2,
  AlertCircle,
  Clock,
  Layers,
  MapPin,
  Tag,
  ListOrdered,
  Sparkles,
} from 'lucide-react';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export const ResourceDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [resource, setResource] = useState(null);
  const [units, setUnits] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modals
  const [isReserveModalOpen, setIsReserveModalOpen] = useState(false);
  const [isWaitlistModalOpen, setIsWaitlistModalOpen] = useState(false);

  // Form States
  const [quantity, setQuantity] = useState(1);
  const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0]);
  const [endDate, setEndDate] = useState(
    new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0]
  );
  const [selectedUnitId, setSelectedUnitId] = useState('');
  const [purpose, setPurpose] = useState('');

  // Status message
  const [actionSuccess, setActionSuccess] = useState('');
  const [actionError, setActionError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchDetail();
  }, [id]);

  const fetchDetail = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/resources/${id}`);
      if (res.data?.success) {
        setResource(res.data.data);
      }

      const unitsRes = await api.get(`/resources/${id}/units`);
      if (unitsRes.data?.success) {
        setUnits(unitsRes.data.data);
      }
    } catch (err) {
      console.error('Failed to load resource details', err);
    } finally {
      setLoading(false);
    }
  };

  const handleReserve = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setActionError('');
    setActionSuccess('');

    try {
      const payload = {
        resourceId: resource.id,
        quantity: parseInt(quantity, 10),
        startDate: startDate,
        endDate: endDate,
        requestedUnitIds: selectedUnitId ? [parseInt(selectedUnitId, 10)] : null,
      };

      const res = await api.post('/reservations', payload);
      if (res.data?.success) {
        setActionSuccess('Reservation successfully created! Your items are held for 48 hours.');
        setIsReserveModalOpen(false);
        fetchDetail();
      }
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || 'Failed to make reservation');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleJoinWaitlist = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setActionError('');
    setActionSuccess('');

    try {
      const payload = {
        resourceId: resource.id,
        quantity: parseInt(quantity, 10),
        neededFrom: startDate,
        neededUntil: endDate,
      };

      const res = await api.post('/waitlist', payload);
      if (res.data?.success) {
        setActionSuccess('Added to waitlist! You will be notified immediately when stock is returned.');
        setIsWaitlistModalOpen(false);
      }
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || 'Failed to join waitlist');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return <LoadingSpinner text="Loading resource details..." />;
  }

  if (!resource) {
    return (
      <div className="text-center py-12">
        <h2 className="text-lg font-bold text-slate-800">Resource not found</h2>
        <button onClick={() => navigate('/catalog')} className="mt-4 text-sm text-blue-600 hover:underline">
          Return to catalog
        </button>
      </div>
    );
  }

  const isAvailable = resource.availableQuantity > 0;
  const availableUnits = units.filter((u) => u.status === 'AVAILABLE');

  return (
    <div className="space-y-6">
      <button
        onClick={() => navigate('/catalog')}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-800 transition"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Back to Catalog</span>
      </button>

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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
            <div className="h-64 sm:h-80 bg-slate-100 overflow-hidden relative">
              <img
                src={resource.imageUrl || 'https://images.unsplash.com/photo-1581092335397-9583fe92d232?auto=format&fit=crop&w=600&q=80'}
                alt={resource.name}
                className="w-full h-full object-cover"
              />
              <div className="absolute top-4 left-4 flex gap-2">
                <span className="px-3 py-1 rounded-lg text-xs font-bold bg-slate-900/90 backdrop-blur-sm text-white">
                  {resource.resourceCode}
                </span>
                <span className="px-3 py-1 rounded-lg text-xs font-semibold bg-white/90 backdrop-blur-sm text-slate-800">
                  {resource.categoryName}
                </span>
              </div>
            </div>

            <div className="p-6 space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <h1 className="text-2xl font-bold text-slate-900">{resource.name}</h1>
                <Badge variant={isAvailable ? 'success' : 'danger'} size="lg">
                  {isAvailable ? `${resource.availableQuantity} in Stock` : 'Out of Stock'}
                </Badge>
              </div>

              <p className="text-sm text-slate-600 leading-relaxed">{resource.description}</p>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 pt-4 border-t border-slate-100 text-xs">
                <div>
                  <span className="text-slate-400 block font-medium">Tracking Mode</span>
                  <span className="font-semibold text-slate-800 mt-0.5 block">{resource.inventoryMode}</span>
                </div>
                <div>
                  <span className="text-slate-400 block font-medium">Max Loan Duration</span>
                  <span className="font-semibold text-slate-800 mt-0.5 block">{resource.maxBorrowDays} Days</span>
                </div>
                <div>
                  <span className="text-slate-400 block font-medium">Total Inventory</span>
                  <span className="font-semibold text-slate-800 mt-0.5 block">{resource.totalQuantity} Units</span>
                </div>
              </div>
            </div>
          </div>

          {/* Individual Units View if applicable */}
          {resource.inventoryMode === 'INDIVIDUAL' && (
            <div className="bg-white rounded-2xl border border-slate-200 p-6 space-y-4 shadow-sm">
              <div className="flex items-center justify-between">
                <h3 className="font-bold text-slate-900 text-sm flex items-center gap-2">
                  <Tag className="w-4 h-4 text-blue-600" />
                  <span>Physical Asset Fleet ({units.length} Units)</span>
                </h3>
                <span className="text-xs text-slate-500">{availableUnits.length} Available</span>
              </div>

              <div className="divide-y divide-slate-100">
                {units.map((u) => (
                  <div key={u.id} className="py-3 flex items-center justify-between text-xs">
                    <div>
                      <div className="font-semibold text-slate-800 flex items-center gap-2">
                        <span>{u.assetCode}</span>
                        {u.serialNumber && <span className="text-slate-400 font-mono text-[11px]">• {u.serialNumber}</span>}
                      </div>
                      <div className="text-slate-500 mt-0.5 flex items-center gap-2">
                        <MapPin className="w-3 h-3 text-slate-400" />
                        <span>{u.storageLocation}</span>
                        <span>• Condition: {u.physicalCondition}</span>
                      </div>
                    </div>
                    <Badge variant={u.status === 'AVAILABLE' ? 'success' : 'default'} size="sm">
                      {u.status}
                    </Badge>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Action Card */}
        <div className="space-y-6">
          <div className="bg-white rounded-2xl border border-slate-200 p-6 space-y-5 shadow-sm sticky top-20">
            <h3 className="font-bold text-slate-900 text-sm">Borrowing Actions</h3>

            <div className="space-y-2 text-xs text-slate-600">
              <div className="flex justify-between py-1 border-b border-slate-100">
                <span>Hold Period:</span>
                <span className="font-semibold text-slate-800">48 Hours</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-100">
                <span>Late Fee:</span>
                <span className="font-semibold text-slate-800">Zero Grace Period</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-100">
                <span>Central Clock:</span>
                <span className="font-semibold text-slate-800">MySQL CURRENT_DATE</span>
              </div>
            </div>

            {isAvailable ? (
              <button
                onClick={() => setIsReserveModalOpen(true)}
                className="w-full py-3 px-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white rounded-xl text-sm font-semibold shadow-md shadow-blue-500/25 transition-all flex items-center justify-center gap-2"
              >
                <Calendar className="w-4 h-4" />
                <span>Reserve Resource Now</span>
              </button>
            ) : (
              <div className="space-y-3">
                <button
                  onClick={() => setIsWaitlistModalOpen(true)}
                  className="w-full py-3 px-4 bg-gradient-to-r from-amber-600 to-orange-600 hover:from-amber-700 hover:to-orange-700 text-white rounded-xl text-sm font-semibold shadow-md shadow-amber-500/25 transition-all flex items-center justify-center gap-2"
                >
                  <ListOrdered className="w-4 h-4" />
                  <span>Join Waitlist Queue</span>
                </button>
                <p className="text-[11px] text-slate-500 text-center">
                  Our Transactional Outbox engine will automatically reserve and notify you when stock is returned.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Reservation Modal */}
      <Modal
        isOpen={isReserveModalOpen}
        onClose={() => setIsReserveModalOpen(false)}
        title={`Reserve ${resource.name}`}
      >
        <form onSubmit={handleReserve} className="space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-semibold text-slate-700">Quantity Needed</label>
            <input
              type="number"
              min="1"
              max={resource.availableQuantity}
              required
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
            />
            <span className="text-[11px] text-slate-500">Up to {resource.availableQuantity} units currently available</span>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Borrow From Date</label>
              <input
                type="date"
                required
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Expected Due Date</label>
              <input
                type="date"
                required
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
          </div>

          {resource.inventoryMode === 'INDIVIDUAL' && availableUnits.length > 0 && (
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Specific Asset Unit (Optional)</label>
              <select
                value={selectedUnitId}
                onChange={(e) => setSelectedUnitId(e.target.value)}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              >
                <option value="">Auto-assign best available unit</option>
                {availableUnits.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.assetCode} - {u.brandModel || 'Standard'} ({u.storageLocation})
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsReserveModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-sm transition disabled:opacity-50"
            >
              {isSubmitting ? 'Confirming...' : 'Confirm Hold'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Waitlist Modal */}
      <Modal
        isOpen={isWaitlistModalOpen}
        onClose={() => setIsWaitlistModalOpen(false)}
        title={`Join Waitlist for ${resource.name}`}
      >
        <form onSubmit={handleJoinWaitlist} className="space-y-4">
          <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800 space-y-1">
            <div className="font-semibold">Priority Tier: {user?.role === 'FACULTY' ? 'Tier 1 (Faculty Priority)' : 'Tier 2 (Student Queue)'}</div>
            <p className="text-[11px] text-amber-700">
              When an item is returned or an expired hold is released, candidates are promoted strictly in priority and FIFO order.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Needed From</label>
              <input
                type="date"
                required
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Needed Until</label>
              <input
                type="date"
                required
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsWaitlistModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 text-xs font-semibold bg-amber-600 hover:bg-amber-700 text-white rounded-xl shadow-sm transition disabled:opacity-50"
            >
              {isSubmitting ? 'Joining...' : 'Confirm Waitlist'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
