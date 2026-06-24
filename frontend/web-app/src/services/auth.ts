import { api, type ApiResponse } from './api';

export interface User {
  id: string;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN';
}

export interface LoginResponse {
  accessToken: string;
  user: User;
}

export interface RegisterResponse {
  userId: string;
  name: string;
  email: string;
}

export const authService = {
  login: async (email: string, password: string): Promise<ApiResponse<LoginResponse>> => {
    const response = await api.post<ApiResponse<LoginResponse>>('/api/auth/login', { email, password });
    return response.data;
  },

  register: async (name: string, email: string, password: string): Promise<ApiResponse<RegisterResponse>> => {
    const response = await api.post<ApiResponse<RegisterResponse>>('/api/auth/register', { name, email, password });
    return response.data;
  },

  getProfile: async (): Promise<ApiResponse<User>> => {
    const response = await api.get<ApiResponse<User>>('/api/auth/me');
    return response.data;
  },
};
