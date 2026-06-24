import { axiosInstance, type ApiResponse } from './axios';

export interface NotificationItem {
  notificationId: string;
  userId: string;
  email: string;
  type: string;
  subject: string;
  message: string;
  status: 'SENT' | 'FAILED' | 'PENDING';
  sentAt: string;
}

export const notificationApi = {
  getUserNotifications: async (userId: string): Promise<ApiResponse<NotificationItem[]>> => {
    const response = await axiosInstance.get<ApiResponse<NotificationItem[]>>(`/api/notifications/user/${userId}`);
    return response.data;
  },

  getAllNotifications: async (): Promise<ApiResponse<NotificationItem[]>> => {
    const response = await axiosInstance.get<ApiResponse<NotificationItem[]>>('/api/notifications');
    return response.data;
  },

  getNotificationById: async (id: string): Promise<ApiResponse<NotificationItem>> => {
    const response = await axiosInstance.get<ApiResponse<NotificationItem>>(`/api/notifications/${id}`);
    return response.data;
  },
};
