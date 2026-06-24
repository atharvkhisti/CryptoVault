import { axiosInstance, type ApiResponse } from './axios';

export interface AuditRecord {
  auditId: string;
  userId: string;
  eventType: string;
  serviceName: string;
  action: string;
  description: string;
  ipAddress: string;
  eventTimestamp: string;
}

export const auditApi = {
  getAllLogs: async (): Promise<ApiResponse<AuditRecord[]>> => {
    const response = await axiosInstance.get<ApiResponse<AuditRecord[]>>('/api/audit');
    return response.data;
  },

  getUserLogs: async (userId: string): Promise<ApiResponse<AuditRecord[]>> => {
    const response = await axiosInstance.get<ApiResponse<AuditRecord[]>>(`/api/audit/user/${userId}`);
    return response.data;
  },

  getLogsByType: async (eventType: string): Promise<ApiResponse<AuditRecord[]>> => {
    const response = await axiosInstance.get<ApiResponse<AuditRecord[]>>(`/api/audit/type/${eventType}`);
    return response.data;
  },

  getLogsByDateRange: async (start: string, end: string): Promise<ApiResponse<AuditRecord[]>> => {
    const response = await axiosInstance.get<ApiResponse<AuditRecord[]>>('/api/audit/date-range', {
      params: { start, end }
    });
    return response.data;
  },
};
