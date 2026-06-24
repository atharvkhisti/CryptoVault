import { api, type ApiResponse } from './api';

export interface KycDocument {
  documentId: string;
  documentType: 'PAN' | 'AADHAAR' | 'PASSPORT' | 'DRIVING_LICENSE';
  fileName: string;
  mimeType: string;
  fileSize: number;
  uploadedAt: string;
}

export interface KycRecord {
  kycId: string;
  userId: string;
  status: 'PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
  remarks: string | null;
  submittedAt: string | null;
  reviewedAt: string | null;
  createdAt: string;
  documents: KycDocument[];
}

export const kycService = {
  initializeKyc: async (): Promise<ApiResponse<KycRecord>> => {
    const response = await api.post<ApiResponse<KycRecord>>('/api/kyc');
    return response.data;
  },

  uploadDocument: async (file: File, documentType: 'PAN' | 'AADHAAR' | 'PASSPORT' | 'DRIVING_LICENSE'): Promise<ApiResponse<KycRecord>> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    const response = await api.post<ApiResponse<KycRecord>>('/api/kyc/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  submitForReview: async (): Promise<ApiResponse<KycRecord>> => {
    const response = await api.post<ApiResponse<KycRecord>>('/api/kyc/submit');
    return response.data;
  },

  approveKyc: async (kycId: string): Promise<ApiResponse<KycRecord>> => {
    // Authenticate as administrative user to authorize simulator approval actions
    const loginRes = await api.post('/api/auth/login', {
      email: 'admin@cryptovault.com',
      password: 'AdminPassword123'
    });
    const adminToken = loginRes.data.data.accessToken;

    const response = await api.post<ApiResponse<KycRecord>>('/api/kyc/approve', null, {
      params: { kycId },
      headers: {
        Authorization: `Bearer ${adminToken}`
      }
    });
    return response.data;
  },

  rejectKyc: async (kycId: string, remarks: string): Promise<ApiResponse<KycRecord>> => {
    // Authenticate as administrative user to authorize simulator rejection actions
    const loginRes = await api.post('/api/auth/login', {
      email: 'admin@cryptovault.com',
      password: 'AdminPassword123'
    });
    const adminToken = loginRes.data.data.accessToken;

    const response = await api.post<ApiResponse<KycRecord>>('/api/kyc/reject', null, {
      params: { kycId, remarks },
      headers: {
        Authorization: `Bearer ${adminToken}`
      }
    });
    return response.data;
  },

  getUserKycStatus: async (userId: string): Promise<ApiResponse<KycRecord>> => {
    const response = await api.get<ApiResponse<KycRecord>>(`/api/kyc/status/${userId}`);
    return response.data;
  },

  getKycDetails: async (id: string): Promise<ApiResponse<KycRecord>> => {
    const response = await api.get<ApiResponse<KycRecord>>(`/api/kyc/${id}`);
    return response.data;
  },
};
