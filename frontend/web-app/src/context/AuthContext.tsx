import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { type User, authApi } from '../services/authApi';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
  updateUser: (user: User) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem('cryptovault_token');
      const storedUser = localStorage.getItem('cryptovault_user');

      if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
        
        // Optionally verify token freshness against profile API
        try {
          const profileRes = await authApi.getProfile();
          if (profileRes.success) {
            setUser(profileRes.data);
            localStorage.setItem('cryptovault_user', JSON.stringify(profileRes.data));
          }
        } catch (err) {
          console.error('Failed to sync profile on load', err);
          // 401 interceptor will handle clearing local storage if actual token is invalid/expired
        }
      }
      setIsLoading(false);
    };

    initAuth();
  }, []);

  const login = (accessToken: string, loggedUser: User) => {
    localStorage.setItem('cryptovault_token', accessToken);
    localStorage.setItem('cryptovault_user', JSON.stringify(loggedUser));
    setToken(accessToken);
    setUser(loggedUser);
  };

  const logout = () => {
    localStorage.removeItem('cryptovault_token');
    localStorage.removeItem('cryptovault_user');
    setToken(null);
    setUser(null);
  };

  const updateUser = (updatedUser: User) => {
    localStorage.setItem('cryptovault_user', JSON.stringify(updatedUser));
    setUser(updatedUser);
  };

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated, isLoading, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
