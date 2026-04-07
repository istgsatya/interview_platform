"use client";

import Cookies from "js-cookie";
import { TOKEN_KEY } from "@/lib/auth-constants";

const COOKIE_OPTIONS = {
  expires: 1,
  sameSite: "lax" as const,
  secure: process.env.NODE_ENV === "production",
};

export const tokenStore = {
  getToken: () => Cookies.get(TOKEN_KEY),
  setToken: (token: string) => Cookies.set(TOKEN_KEY, token, COOKIE_OPTIONS),
  clearToken: () => Cookies.remove(TOKEN_KEY),
};
