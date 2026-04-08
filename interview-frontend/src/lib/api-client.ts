"use client";

import axios from "axios";
import { tokenStore } from "@/lib/token";

const runtimeDefaultBaseUrl =
  typeof window !== "undefined"
    ? `${window.location.protocol}//${window.location.hostname}:8080`
    : "http://localhost:8080";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? runtimeDefaultBaseUrl;

export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 20000,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = tokenStore.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export type AuthPayload = {
  email: string;
  password: string;
};

export type StartInterviewPayload = {
  resumeText: string;
  targetJdText: string;
};

export type StartInterviewResponse = {
  sessionId: number;
  questionCount: number;
  status: string;
};

export type TranscriptItem = {
  questionId: number;
  difficultyLevel: number;
  questionText: string;
  idealAnswer: string;
  candidateText: string | null;
  mlSimilarityScore: number | null;
  mlLengthScore: number | null;
  finalAggregateScore: number | null;
  llmFeedback: string | null;
  asked: boolean;
};

export type TranscriptResponse = {
  sessionId: number;
  status: string;
  createdAt: string;
  items: TranscriptItem[];
};

export type SessionSummary = {
  sessionId: number;
  status: string;
  createdAt: string;
  questionCount: number;
  answeredCount: number;
  averageScore: number;
};

type RawAuthResponse = {
  token?: string;
};

function extractToken(data: RawAuthResponse | string): string {
  if (typeof data === "string") {
    return data;
  }
  return data.token ?? "";
}

export async function login(payload: AuthPayload): Promise<string> {
  const response = await apiClient.post<RawAuthResponse | string>("/api/auth/login", payload);
  return extractToken(response.data);
}

export async function register(payload: AuthPayload): Promise<string> {
  const response = await apiClient.post<RawAuthResponse | string>("/api/auth/register", payload);
  return extractToken(response.data);
}

export async function startInterview(payload: StartInterviewPayload): Promise<StartInterviewResponse> {
  const response = await apiClient.post<StartInterviewResponse>("/api/interview/start", payload);
  return response.data;
}

export async function getInterviewTranscript(sessionId: string): Promise<TranscriptResponse> {
  const response = await apiClient.get<TranscriptResponse>(`/api/interview/sessions/${sessionId}/transcript`);
  return response.data;
}

export async function listSessions(): Promise<SessionSummary[]> {
  const response = await apiClient.get<SessionSummary[]>("/api/interview/sessions");
  return response.data;
}
