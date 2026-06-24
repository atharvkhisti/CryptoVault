import { axiosInstance, type ApiResponse } from './axios';

export interface Transaction {
  transactionId: string;
  type: 'DEPOSIT' | 'WITHDRAW' | 'TRANSFER';
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'BLOCKED';
  amount: number;
  currency: string;
  referenceNumber: string;
  timestamp: string;
  description?: string;
}

export interface DepositRequest {
  walletId: string;
  amount: number;
  description?: string;
}

export interface WithdrawRequest {
  walletId: string;
  amount: number;
  description?: string;
}

export interface TransferRequest {
  senderWalletId: string;
  receiverWalletId: string;
  amount: number;
  description?: string;
}

export const transactionApi = {
  deposit: async (data: DepositRequest): Promise<ApiResponse<Transaction>> => {
    const response = await axiosInstance.post<ApiResponse<Transaction>>('/api/transactions/deposit', data);
    return response.data;
  },

  withdraw: async (data: WithdrawRequest): Promise<ApiResponse<Transaction>> => {
    const response = await axiosInstance.post<ApiResponse<Transaction>>('/api/transactions/withdraw', data);
    return response.data;
  },

  transfer: async (data: TransferRequest): Promise<ApiResponse<Transaction>> => {
    const response = await axiosInstance.post<ApiResponse<Transaction>>('/api/transactions/transfer', data);
    return response.data;
  },

  getTransactions: async (): Promise<ApiResponse<Transaction[]>> => {
    const response = await axiosInstance.get<ApiResponse<Transaction[]>>('/api/transactions');
    return response.data;
  },

  getTransactionById: async (id: string): Promise<ApiResponse<Transaction>> => {
    const response = await axiosInstance.get<ApiResponse<Transaction>>(`/api/transactions/${id}`);
    return response.data;
  },
};
