import { api, type ApiResponse } from './api';

export interface RiskAssessment {
  id: string;
  userId: string;
  transactionId: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'APPROVED' | 'FLAGGED' | 'BLOCKED';
  riskScore: number;
  triggeredRule?: string;
  comments?: string;
  createdAt: string;
}

export const riskService = {
  evaluateRisk: async (data: {
    userId: string;
    transactionId: string;
    amount: number;
    currency: string;
    type: string;
  }): Promise<ApiResponse<RiskAssessment>> => {
    const response = await api.post<ApiResponse<RiskAssessment>>('/api/risk/evaluate', data);
    return response.data;
  },

  getAssessmentById: async (id: string): Promise<ApiResponse<RiskAssessment>> => {
    const response = await api.get<ApiResponse<RiskAssessment>>(`/api/risk/${id}`);
    return response.data;
  },

  getUserRiskHistory: async (userId: string): Promise<ApiResponse<RiskAssessment[]>> => {
    const response = await api.get<ApiResponse<RiskAssessment[]>>(`/api/risk/user/${userId}`);
    return response.data;
  },

  getAllRiskHistory: async (): Promise<ApiResponse<RiskAssessment[]>> => {
    const response = await api.get<ApiResponse<RiskAssessment[]>>('/api/risk/history');
    return response.data;
  },
};
