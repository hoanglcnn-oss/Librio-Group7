import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json'
  }
});

export const resourceApi = {
  checkHealth: async () => {
    const response = await apiClient.get('/health');
    return response.data;
  },

  getResources: async (keyword = '') => {
    const params = {};
    if (keyword && keyword.trim()) {
      params.q = keyword.trim();
    }
    const response = await apiClient.get('/resources', { params });
    return response.data;
  },

  getResourceById: async (id) => {
    const response = await apiClient.get(`/resources/${id}`);
    return response.data;
  }
};

export default apiClient;
