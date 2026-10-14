import axios, { AxiosError } from 'axios';
import { ApiErrorBody } from '../types';

export const TOKEN_KEY = 'hiresense_token';

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorBody>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export function extractErrorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    const body = error.response?.data;
    if (body?.message) {
      if (body.fieldErrors) {
        const first = Object.values(body.fieldErrors)[0];
        if (first) return String(first);
      }
      return body.message;
    }
    return error.message;
  }
  if (error instanceof Error) return error.message;
  return 'An unexpected error occurred';
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  const message = extractErrorMessage(error);
  return message && !message.startsWith('Request failed') && message !== 'Network Error'
    ? message
    : fallback;
}