import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import { Search, Filter, Layers, ArrowUpRight, CheckCircle2, AlertTriangle, Clock } from 'lucide-react';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export const ResourceCatalogPage = () => {
  const [resources, setResources] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [selectedMode, setSelectedMode] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchCategories();
  }, []);

  useEffect(() => {
    fetchResources();
  }, [selectedCategory, selectedMode, searchTerm]);

  const fetchCategories = async () => {
    try {
      const res = await api.get('/categories');
      if (res.data?.success) {
        setCategories(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load categories', err);
    }
  };

  const fetchResources = async () => {
    setLoading(true);
    try {
      const params = {};
      if (selectedCategory) params.categoryId = selectedCategory;
      if (selectedMode) params.mode = selectedMode;
      if (searchTerm.trim()) params.search = searchTerm.trim();

      const res = await api.get('/resources', { params });
      if (res.data?.success) {
        setResources(res.data.data.content || []);
      }
    } catch (err) {
      console.error('Failed to load resources', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Resource Catalog</h1>
          <p className="text-sm text-slate-500">Discover and borrow laboratory assets, computing hardware, and multimedia gear</p>
        </div>
      </div>

      {/* Filters Bar */}
      <div className="flex flex-col md:flex-row items-center gap-3">
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search resources by name, model, specifications or code..."
            className="w-full pl-10 pr-4 py-2.5 text-sm rounded-xl border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600 transition"
          />
        </div>

        <div className="flex items-center gap-2 w-full md:w-auto overflow-x-auto pb-1 md:pb-0">
          <button
            onClick={() => setSelectedCategory(null)}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
              selectedCategory === null
                ? 'bg-blue-600 text-white shadow-sm shadow-blue-500/20'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            All Categories
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              onClick={() => setSelectedCategory(c.id)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
                selectedCategory === c.id
                  ? 'bg-blue-600 text-white shadow-sm shadow-blue-500/20'
                  : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
              }`}
            >
              {c.name}
            </button>
          ))}
        </div>
      </div>

      {/* Mode Pills */}
      <div className="flex items-center gap-2 text-xs">
        <span className="font-semibold text-slate-400 uppercase tracking-wider text-[10px]">Tracking Mode:</span>
        <button
          onClick={() => setSelectedMode('')}
          className={`px-2.5 py-1 rounded-lg ${selectedMode === '' ? 'bg-slate-800 text-white font-medium' : 'text-slate-600 hover:bg-slate-200'}`}
        >
          All
        </button>
        <button
          onClick={() => setSelectedMode('INDIVIDUAL')}
          className={`px-2.5 py-1 rounded-lg ${selectedMode === 'INDIVIDUAL' ? 'bg-slate-800 text-white font-medium' : 'text-slate-600 hover:bg-slate-200'}`}
        >
          Serialized Assets
        </button>
        <button
          onClick={() => setSelectedMode('QUANTITY')}
          className={`px-2.5 py-1 rounded-lg ${selectedMode === 'QUANTITY' ? 'bg-slate-800 text-white font-medium' : 'text-slate-600 hover:bg-slate-200'}`}
        >
          Bulk Components
        </button>
      </div>

      {/* Resource Grid */}
      {loading ? (
        <LoadingSpinner text="Fetching real-time inventory balances..." />
      ) : resources.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-slate-200">
          <Layers className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-base font-semibold text-slate-800">No resources found</h3>
          <p className="text-xs text-slate-500 mt-1">Try adjusting your filters or search keywords</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {resources.map((item) => {
            const isAvailable = item.availableQuantity > 0;
            return (
              <div
                key={item.id}
                onClick={() => navigate(`/catalog/${item.id}`)}
                className="group cursor-pointer bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm hover:shadow-md hover:border-blue-300 transition-all flex flex-col justify-between"
              >
                <div>
                  <div className="relative h-44 bg-slate-100 overflow-hidden">
                    <img
                      src={item.imageUrl || 'https://images.unsplash.com/photo-1581092335397-9583fe92d232?auto=format&fit=crop&w=600&q=80'}
                      alt={item.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                    <div className="absolute top-3 left-3 flex gap-1.5">
                      <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-slate-900/80 backdrop-blur-sm text-white">
                        {item.resourceCode}
                      </span>
                      <span className="px-2 py-0.5 rounded-md text-[10px] font-semibold bg-white/90 backdrop-blur-sm text-slate-800">
                        {item.categoryName}
                      </span>
                    </div>

                    <div className="absolute bottom-3 right-3">
                      <Badge variant={isAvailable ? 'success' : 'danger'}>
                        {isAvailable ? (
                          <>
                            <CheckCircle2 className="w-3 h-3" />
                            <span>{item.availableQuantity} Available</span>
                          </>
                        ) : (
                          <>
                            <AlertTriangle className="w-3 h-3" />
                            <span>Out of Stock</span>
                          </>
                        )}
                      </Badge>
                    </div>
                  </div>

                  <div className="p-5 space-y-2">
                    <h3 className="font-semibold text-slate-900 group-hover:text-blue-600 transition-colors line-clamp-1">
                      {item.name}
                    </h3>
                    <p className="text-xs text-slate-500 line-clamp-2 leading-relaxed">
                      {item.description}
                    </p>
                  </div>
                </div>

                <div className="px-5 pb-5 pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-600">
                  <div className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    <span>Max {item.maxBorrowDays} days</span>
                  </div>
                  <div className="flex items-center gap-1 font-semibold text-blue-600 group-hover:translate-x-0.5 transition-transform">
                    <span>View Details</span>
                    <ArrowUpRight className="w-3.5 h-3.5" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
