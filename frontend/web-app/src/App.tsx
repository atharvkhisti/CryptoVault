import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { ProtectedRoute } from './components/layout/ProtectedRoute';
import { WalletLayout } from './components/layout/WalletLayout';
import { ErrorBoundary } from './components/layout/ErrorBoundary';

// Pages
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Dashboard } from './pages/Dashboard';
import { Wallets } from './pages/Wallets';
import { Transactions } from './pages/Transactions';
import { Activity } from './pages/Activity';
import { Settings } from './pages/Settings';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <AuthProvider>
            <BrowserRouter>
              <Routes>
                {/* Public Auth routes */}
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />

                {/* Wallet layout routes protected by JWT */}
                <Route
                  path="/*"
                  element={
                    <ProtectedRoute>
                      <WalletLayout>
                        <Routes>
                          <Route path="/" element={<Dashboard />} />
                          <Route path="/assets" element={<Wallets />} />
                          <Route path="/transfer" element={<Transactions />} />
                          <Route path="/activity" element={<Activity />} />
                          <Route path="/profile" element={<Settings />} />
                          {/* Fallback route */}
                          <Route path="*" element={<Dashboard />} />
                        </Routes>
                      </WalletLayout>
                    </ProtectedRoute>
                  }
                />
              </Routes>
            </BrowserRouter>
          </AuthProvider>
        </ToastProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
