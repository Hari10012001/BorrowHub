import React, { useState, useEffect } from 'react';
import api from '../../api/axios';
import {
  Layers,
  Plus,
  Tag,
  Search,
  CheckCircle2,
  AlertCircle,
  Eye,
  Trash2,
} from 'lucide-react';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export const ResourceManagementPage = () => {
  const [resources, setResources] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modals
  const [isAddResourceOpen, setIsAddResourceOpen] = useState(false);
  const [isAddUnitOpen, setIsAddUnitOpen] = useState(false);
  const [isViewUnitsOpen, setIsViewUnitsOpen] = useState(false);
  const [selectedResource, setSelectedResource] = useState(null);
  const [selectedResourceUnits, setSelectedResourceUnits] = useState([]);

  // Form state
  const [resourceForm, setResourceForm] = useState({
    name: '',
    resourceCode: '',
    description: '',
    categoryId: '1',
    inventoryMode: 'INDIVIDUAL',
    totalQuantity: '1',
    maxBorrowDays: '7',
    isReservable: true,
    isActive: true,
    imageUrl: '',
  });

  const [unitForm, setUnitForm] = useState({
    assetCode: '',
    serialNumber: '',
    brandModel: '',
    physicalCondition: 'NEW',
    status: 'AVAILABLE',
    storageLocation: 'Depot Shelf A-1',
    notes: '',
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionSuccess, setActionSuccess] = useState('');
  const [actionError, setActionError] = useState('');

  useEffect(() => {
    fetchInitial();
  }, []);

  const fetchInitial = async () => {
    setLoading(true);
    try {
      const [resList, catList] = await Promise.all([
        api.get('/resources?size=50'),
        api.get('/categories'),
      ]);

      if (resList.data?.success) {
        setResources(resList.data.data.content || []);
      }
      if (catList.data?.success) {
        setCategories(catList.data.data || []);
      }
    } catch (err) {
      console.error('Failed to load inventory', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateResource = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setActionError('');
    setActionSuccess('');

    try {
      const payload = {
        ...resourceForm,
        categoryId: parseInt(resourceForm.categoryId, 10),
        totalQuantity: parseInt(resourceForm.totalQuantity, 10),
        maxBorrowDays: parseInt(resourceForm.maxBorrowDays, 10),
      };

      const res = await api.post('/resources', payload);
      if (res.data?.success) {
        setActionSuccess('Resource created successfully in catalog!');
        setIsAddResourceOpen(false);
        fetchInitial();
      }
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || 'Failed to create resource');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCreateUnit = async (e) => {
    e.preventDefault();
    if (!selectedResource) return;
    setIsSubmitting(true);
    setActionError('');
    setActionSuccess('');

    try {
      const res = await api.post(`/resources/${selectedResource.id}/units`, unitForm);
      if (res.data?.success) {
        setActionSuccess(`Unit ${unitForm.assetCode} added successfully!`);
        setIsAddUnitOpen(false);
        fetchInitial();
      }
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || 'Failed to add unit');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleViewUnits = async (resource) => {
    setSelectedResource(resource);
    try {
      const res = await api.get(`/resources/${resource.id}/units`);
      if (res.data?.success) {
        setSelectedResourceUnits(res.data.data || []);
        setIsViewUnitsOpen(true);
      }
    } catch (err) {
      console.error('Failed to load units', err);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Inventory & Assets Management</h1>
          <p className="text-sm text-slate-500">Configure catalog resources, physical asset units, and categories</p>
        </div>

        <button
          onClick={() => setIsAddResourceOpen(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold shadow-md shadow-blue-500/20 transition"
        >
          <Plus className="w-4 h-4" />
          <span>Add New Resource</span>
        </button>
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
        <LoadingSpinner text="Loading catalog inventory..." />
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-600">
              <thead className="bg-slate-50 text-[11px] font-bold text-slate-400 uppercase tracking-wider border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3.5">Code</th>
                  <th className="px-4 py-3.5">Name</th>
                  <th className="px-4 py-3.5">Category</th>
                  <th className="px-4 py-3.5">Tracking Mode</th>
                  <th className="px-4 py-3.5">Available / Total</th>
                  <th className="px-4 py-3.5">Max Days</th>
                  <th className="px-6 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {resources.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50 transition">
                    <td className="px-6 py-4 font-mono font-bold text-slate-800">{r.resourceCode}</td>
                    <td className="px-4 py-4 font-semibold text-slate-800">{r.name}</td>
                    <td className="px-4 py-4">{r.categoryName}</td>
                    <td className="px-4 py-4">
                      <span className="px-2 py-0.5 rounded font-medium bg-slate-100 text-slate-700">
                        {r.inventoryMode}
                      </span>
                    </td>
                    <td className="px-4 py-4 font-medium text-slate-700">
                      <span className="text-emerald-700 font-bold">{r.availableQuantity}</span> / {r.totalQuantity}
                    </td>
                    <td className="px-4 py-4">{r.maxBorrowDays} Days</td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        {r.inventoryMode === 'INDIVIDUAL' && (
                          <>
                            <button
                              onClick={() => {
                                setSelectedResource(r);
                                setIsAddUnitOpen(true);
                              }}
                              className="px-2.5 py-1 text-[11px] font-semibold text-blue-600 hover:bg-blue-50 border border-blue-200 rounded-lg transition"
                            >
                              + Unit
                            </button>
                            <button
                              onClick={() => handleViewUnits(r)}
                              className="px-2.5 py-1 text-[11px] font-semibold text-slate-600 hover:bg-slate-100 border border-slate-200 rounded-lg transition"
                            >
                              Units ({r.totalQuantity})
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add Resource Modal */}
      <Modal
        isOpen={isAddResourceOpen}
        onClose={() => setIsAddResourceOpen(false)}
        title="Add New Resource to Catalog"
      >
        <form onSubmit={handleCreateResource} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Resource Name</label>
              <input
                type="text"
                required
                placeholder="e.g. 4K Drone Kit"
                value={resourceForm.name}
                onChange={(e) => setResourceForm({ ...resourceForm, name: e.target.value })}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Resource Code</label>
              <input
                type="text"
                required
                placeholder="e.g. DRN-DJI-001"
                value={resourceForm.resourceCode}
                onChange={(e) => setResourceForm({ ...resourceForm, resourceCode: e.target.value })}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Category</label>
              <select
                value={resourceForm.categoryId}
                onChange={(e) => setResourceForm({ ...resourceForm, categoryId: e.target.value })}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Tracking Mode</label>
              <select
                value={resourceForm.inventoryMode}
                onChange={(e) => setResourceForm({ ...resourceForm, inventoryMode: e.target.value })}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              >
                <option value="INDIVIDUAL">INDIVIDUAL (Asset Tag & Serial)</option>
                <option value="QUANTITY">QUANTITY (Bulk Consumables/Kits)</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Total Quantity</label>
              <input
                type="number"
                min="0"
                required
                value={resourceForm.totalQuantity}
                onChange={(e) => setResourceForm({ ...resourceForm, totalQuantity: e.target.value })}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Max Borrow Days</label>
              <input
                type="number"
                min="1"
                required
                value={resourceForm.maxBorrowDays}
                onChange={(e) => setResourceForm({ ...resourceForm, maxBorrowDays: e.target.value })}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-slate-700">Description</label>
            <textarea
              rows="2"
              placeholder="Hardware specs, inclusions..."
              value={resourceForm.description}
              onChange={(e) => setResourceForm({ ...resourceForm, description: e.target.value })}
              className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
            />
          </div>

          <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsAddResourceOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-sm transition disabled:opacity-50"
            >
              {isSubmitting ? 'Creating...' : 'Save Resource'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Add Unit Modal */}
      <Modal
        isOpen={isAddUnitOpen}
        onClose={() => setIsAddUnitOpen(false)}
        title={`Add Physical Unit to ${selectedResource?.name}`}
      >
        <form onSubmit={handleCreateUnit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Asset Code (Barcode / Tag)</label>
              <input
                type="text"
                required
                placeholder="e.g. BH-DRN-001"
                value={unitForm.assetCode}
                onChange={(e) => setUnitForm({ ...unitForm, assetCode: e.target.value })}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Serial Number</label>
              <input
                type="text"
                placeholder="Manufacturer S/N"
                value={unitForm.serialNumber}
                onChange={(e) => setUnitForm({ ...unitForm, serialNumber: e.target.value })}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Brand / Model</label>
              <input
                type="text"
                placeholder="e.g. DJI Mini 4 Pro"
                value={unitForm.brandModel}
                onChange={(e) => setUnitForm({ ...unitForm, brandModel: e.target.value })}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-700">Storage Location</label>
              <input
                type="text"
                required
                placeholder="e.g. Lab Bay 3"
                value={unitForm.storageLocation}
                onChange={(e) => setUnitForm({ ...unitForm, storageLocation: e.target.value })}
                className="w-full px-3.5 py-2 text-sm rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600"
              />
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setIsAddUnitOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-sm transition disabled:opacity-50"
            >
              {isSubmitting ? 'Registering...' : 'Register Asset Unit'}
            </button>
          </div>
        </form>
      </Modal>

      {/* View Units Modal */}
      <Modal
        isOpen={isViewUnitsOpen}
        onClose={() => setIsViewUnitsOpen(false)}
        title={`Asset Units for ${selectedResource?.name}`}
      >
        <div className="space-y-3">
          {selectedResourceUnits.length === 0 ? (
            <p className="text-xs text-slate-500 py-4 text-center">No physical asset units registered yet</p>
          ) : (
            <div className="divide-y divide-slate-100 max-h-96 overflow-y-auto">
              {selectedResourceUnits.map((u) => (
                <div key={u.id} className="py-2.5 flex items-center justify-between text-xs">
                  <div>
                    <div className="font-bold text-slate-800">{u.assetCode}</div>
                    <div className="text-slate-500 text-[11px]">{u.storageLocation} • {u.physicalCondition}</div>
                  </div>
                  <Badge variant={u.status === 'AVAILABLE' ? 'success' : 'default'} size="sm">
                    {u.status}
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
};
