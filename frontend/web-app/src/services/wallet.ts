import { api, type ApiResponse } from './api';

export interface Wallet {
  walletId: string;
  currency: 'BTC' | 'ETH' | 'USDT' | 'INR';
  balance: number;
}

export const walletService = {
  getWallets: async (): Promise<ApiResponse<Wallet[]>> => {
    const response = await api.get<ApiResponse<Wallet[]>>('/api/wallets');
    return response.data;
  },

  createWallet: async (currency: 'BTC' | 'ETH' | 'USDT' | 'INR'): Promise<ApiResponse<Wallet>> => {
    const response = await api.post<ApiResponse<Wallet>>('/api/wallets', { currency });
    return response.data;
  },

  deposit: async (walletId: string, amount: number): Promise<ApiResponse<Wallet>> => {
    const response = await api.post<ApiResponse<Wallet>>('/api/wallets/deposit', { walletId, amount });
    return response.data;
  },

  withdraw: async (walletId: string, amount: number): Promise<ApiResponse<Wallet>> => {
    const response = await api.post<ApiResponse<Wallet>>('/api/wallets/withdraw', { walletId, amount });
    return response.data;
  },
};
