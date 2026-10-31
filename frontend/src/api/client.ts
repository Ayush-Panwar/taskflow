import { loadSession } from '../auth/sessionStorage';

const API_BASE = '/api';

interface ServerError {
  code?: string;
  message?: string;
  fieldErrors?: { field: string; message: string }[];
}

/** The one error type the rest of the app deals with, whatever went wrong. */
export class ApiError extends Error {
  /** The HTTP status, or 0 if the server could not be reached. */
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, code: string, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

/** Turns anything caught in a catch block into an ApiError. */
export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }
  return new ApiError(0, 'UNKNOWN', 'Something went wrong. Please try again.');
}

let onUnauthorized: () => void = () => {};

/** The auth layer registers here, so it can end the session when the server rejects a token. */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

type QueryParams = object;

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  params?: QueryParams;
  signal?: AbortSignal;
}

function buildUrl(path: string, params?: QueryParams): string {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params ?? {})) {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value));
    }
  }
  const queryString = query.toString();
  return `${API_BASE}${path}${queryString ? `?${queryString}` : ''}`;
}

function parseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return null; // Empty bodies (204) and non-JSON errors from a proxy.
  }
}

function errorFromResponse(status: number, body: unknown): ApiError {
  const server = (body ?? {}) as ServerError;
  const fieldErrors: Record<string, string> = {};
  for (const { field, message } of server.fieldErrors ?? []) {
    fieldErrors[field] ??= message; // Keep the first message for each field.
  }
  const fallback = status >= 500
    ? 'The server is having problems. Please try again shortly.'
    : 'The request could not be completed.';
  return new ApiError(status, server.code ?? 'UNKNOWN', server.message ?? fallback, fieldErrors);
}

/** Every API call in the app goes through here: it adds the token and normalises errors. */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, params, signal } = options;
  const token = loadSession()?.accessToken;

  const headers: Record<string, string> = {};
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(buildUrl(path, params), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    if (signal?.aborted) {
      throw error; // React Query cancelled this request on purpose, so leave its error alone.
    }
    throw new ApiError(0, 'NETWORK_ERROR', 'Could not reach the server. Check your connection and try again.');
  }

  const data = parseJson(await response.text());
  if (!response.ok) {
    // No token means this was a failed login, which is not an expired session.
    // A different current token means the user has already logged in again since this request began.
    if (response.status === 401 && token && loadSession()?.accessToken === token) {
      onUnauthorized();
    }
    throw errorFromResponse(response.status, data);
  }
  return data as T;
}
