'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { api, UserProfile } from '../lib/api';

interface AuthContextType {
  user: UserProfile | null;
  token: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (payload: {
    organizationName: string;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  }) => Promise<void>;
  logout: () => void;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    try {
      const savedUser = localStorage.getItem('emblemix_user');
      const savedToken = localStorage.getItem('emblemix_token');
      if (savedUser) setUser(JSON.parse(savedUser));
      if (savedToken) setToken(savedToken);
    } catch {
      // Ignore SSR or storage access errors
    }
  }, []);

  useEffect(() => {
    if (token) {
      api.auth.getProfile()
        .then((profile) => {
          setUser(profile);
          localStorage.setItem('emblemix_user', JSON.stringify(profile));
        })
        .catch(() => {
          logout();
        })
        .finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }
  }, [token]);

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const res = await api.auth.login({ email, password });
      setToken(res.accessToken);
      setUser(res.user);
      localStorage.setItem('emblemix_token', res.accessToken);
      localStorage.setItem('emblemix_refresh_token', res.refreshToken);
      localStorage.setItem('emblemix_user', JSON.stringify(res.user));
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (payload: {
    organizationName: string;
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  }) => {
    setIsLoading(true);
    try {
      const res = await api.auth.register(payload);
      setToken(res.accessToken);
      setUser(res.user);
      localStorage.setItem('emblemix_token', res.accessToken);
      localStorage.setItem('emblemix_refresh_token', res.refreshToken);
      localStorage.setItem('emblemix_user', JSON.stringify(res.user));
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('emblemix_token');
    localStorage.removeItem('emblemix_refresh_token');
    localStorage.removeItem('emblemix_user');
    setToken(null);
    setUser(null);
  };

  const refreshProfile = async () => {
    const profile = await api.auth.getProfile();
    setUser(profile);
    localStorage.setItem('emblemix_user', JSON.stringify(profile));
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isLoading,
        isAuthenticated: !!token && !!user,
        login,
        register,
        logout,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
