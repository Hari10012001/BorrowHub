import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Layout } from './components/layout/Layout';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { ResourceCatalogPage } from './pages/catalog/ResourceCatalogPage';
import { ResourceDetailPage } from './pages/catalog/ResourceDetailPage';
import { MyBorrowingsPage } from './pages/borrowing/MyBorrowingsPage';
import { MyReservationsPage } from './pages/reservation/MyReservationsPage';
import { MyWaitlistPage } from './pages/waitlist/MyWaitlistPage';
import { CirculationDeskPage } from './pages/circulation/CirculationDeskPage';
import { PendingApprovalsPage } from './pages/admin/PendingApprovalsPage';
import { ResourceManagementPage } from './pages/admin/ResourceManagementPage';
import { LoadingSpinner } from './components/common/LoadingSpinner';

const ProtectedRoute = ({ children, allowedRoles }) => {
  const { user, isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen bg-slate-50">
        <LoadingSpinner size="lg" text="Verifying authentication..." />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && (!user || !allowedRoles.includes(user.role))) {
    return <Navigate to="/catalog" replace />;
  }

  return children;
};

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public Auth Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Authenticated Application Layout */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/catalog" replace />} />
            <Route path="catalog" element={<ResourceCatalogPage />} />
            <Route path="catalog/:id" element={<ResourceDetailPage />} />
            <Route path="borrowings" element={<MyBorrowingsPage />} />
            <Route path="reservations" element={<MyReservationsPage />} />
            <Route path="waitlist" element={<MyWaitlistPage />} />

            {/* Circulation Desk & Approvals (Admin & Faculty) */}
            <Route
              path="circulation"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'FACULTY']}>
                  <CirculationDeskPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="approvals"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'FACULTY']}>
                  <PendingApprovalsPage />
                </ProtectedRoute>
              }
            />

            {/* Resource & Inventory Management (Admin Only) */}
            <Route
              path="admin/resources"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <ResourceManagementPage />
                </ProtectedRoute>
              }
            />

            {/* Catch-all fallback */}
            <Route path="*" element={<Navigate to="/catalog" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
