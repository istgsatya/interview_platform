import { NextResponse, type NextRequest } from "next/server";

import { TOKEN_KEY } from "@/lib/auth-constants";

const protectedPrefixes = ["/dashboard", "/interview", "/sessions"];
const publicAuthRoutes = ["/login", "/register"];

function isProtectedPath(pathname: string) {
  return protectedPrefixes.some((prefix) => pathname.startsWith(prefix));
}

function isPublicAuthPath(pathname: string) {
  return publicAuthRoutes.includes(pathname);
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get(TOKEN_KEY)?.value;

  if (isProtectedPath(pathname) && !token) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  if (isPublicAuthPath(pathname) && token) {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/dashboard/:path*", "/interview/:path*", "/sessions/:path*", "/login", "/register"],
};
