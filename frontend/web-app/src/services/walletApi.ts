import { axiosInstance, type ApiResponse } from './axios';

export interface Wallet {
  walletId: string;
  currency: 'BTC' | 'ETH' | 'USDT' | 'INR';
  balance: number;
}

export const walletApi = {
  getWallets: async (): Promise<ApiResponse<Wallet[]>> => {
    const response = await axiosInstance.get<ApiResponse<Wallet[]>>('/api/wallets');
    return response.data;
  },

  createWallet: async (currency: 'BTC' | 'ETH' | 'USDT' | 'INR'): Promise<ApiResponse<Wallet>> => {
    const response = await axiosInstance.post<ApiResponse<Wallet>>('/api/wallets', { currency });
    return response.data;
  },

  deposit: async (walletId: string, amount: number): Promise<ApiResponse<Wallet>> => {
    const response = await axiosInstance.post<ApiResponse<Wallet>>('/api/wallets/deposit', { walletId, amount });
    return response.data;
  },

  withdraw: async (walletId: string, amount: number): Promise<ApiResponse<Wallet>> => {
    const response = await axiosInstance.post<ApiResponse<Wallet>>('/api/wallets/withdraw', { walletId, amount });
    return response.data;
  },
};
