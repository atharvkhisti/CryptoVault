import { axiosInstance, type ApiResponse } from './axios';

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

export const kycApi = {
  initializeKyc: async (): Promise<ApiResponse<KycRecord>> => {
    const response = await axiosInstance.post<ApiResponse<KycRecord>>('/api/kyc');
    return response.data;
  },

  uploadDocument: async (file: File, documentType: 'PAN' | 'AADHAAR' | 'PASSPORT' | 'DRIVING_LICENSE'): Promise<ApiResponse<KycRecord>> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    const response = await axiosInstance.post<ApiResponse<KycRecord>>('/api/kyc/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  submitForReview: async (): Promise<ApiResponse<KycRecord>> => {
    const response = await axiosInstance.post<ApiResponse<KycRecord>>('/api/kyc/submit');
    return response.data;
  },

  approveKyc: async (kycId: string): Promise<ApiResponse<KycRecord>> => {
    const response = await axiosInstance.post<ApiResponse<KycRecord>>('/api/kyc/approve', null, {
      params: { kycId }
    });
    return response.data;
  },

  rejectKyc: async (kycId: string, remarks: string): Promise<ApiResponse<KycRecord>> => {
    const response = await axiosInstance.post<ApiResponse<KycRecord>>('/api/kyc/reject', null, {
      params: { kycId, remarks }
    });
    return response.data;
  },

  getUserKycStatus: async (userId: string): Promise<ApiResponse<KycRecord>> => {
    const response = await axiosInstance.get<ApiResponse<KycRecord>>(`/api/kyc/status/${userId}`);
    return response.data;
  },

  getKycDetails: async (id: string): Promise<ApiResponse<KycRecord>> => {
    const response = await axiosInstance.get<ApiResponse<KycRecord>>(`/api/kyc/${id}`);
    return response.data;
  },
};
