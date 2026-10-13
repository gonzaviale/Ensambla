import { z } from 'zod';
import { env } from '@/config/env';
import { ApiError } from './apiError';
import { errorResponseSchema } from './types';

const MENSAJE_SIN_CONEXION = 'No pudimos conectarnos con el servidor. Revisá tu conexión.';
const MENSAJE_INESPERADO = 'Ocurrió un error inesperado. Probá de nuevo en unos minutos.';

export const EVENTO_SESION_EXPIRADA = 'auth:expirada';

const csrfSchema = z.object({ token: z.string(), headerName: z.string() });
type Csrf = z.infer<typeof csrfSchema>;

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

type RequestOptions = {
  method?: Method;
  body?: unknown;
  signal?: AbortSignal;
};

function buildUrl(path: string) {
  return new URL(`${env.apiUrl}${path}`, window.location.origin);
}

async function toApiError(response: Response) {
  const body: unknown = await response.json().catch(() => null);
  const parsed = errorResponseSchema.safeParse(body);
  if (!parsed.success) return new ApiError(response.status, MENSAJE_INESPERADO);
  const { status, message, errors, traceId } = parsed.data;
  return new ApiError(status, message, errors, traceId);
}

async function fetchRed(url: URL, init: RequestInit) {
  try {
    return await fetch(url, { credentials: 'include', ...init });
  } catch (error) {
    if (init.signal?.aborted) throw error;
    throw new ApiError(0, MENSAJE_SIN_CONEXION);
  }
}

// El backend protege POST/PUT/PATCH/DELETE con un token CSRF ligado a la sesión: se pide una vez,
// se cachea y se vuelve a pedir cuando la sesión cambia (login, logout) o el backend lo rechaza.
let csrf: Csrf | null = null;
let csrfEnVuelo: Promise<Csrf> | null = null;

export function invalidarCsrf() {
  csrf = null;
}

async function pedirCsrf(): Promise<Csrf> {
  const response = await fetchRed(buildUrl('/auth/csrf'), { method: 'GET' });
  if (!response.ok) throw await toApiError(response);
  return csrfSchema.parse(await response.json());
}

function obtenerCsrf(): Promise<Csrf> {
  if (csrf) return Promise.resolve(csrf);
  csrfEnVuelo ??= pedirCsrf()
    .then((valor) => (csrf = valor))
    .finally(() => {
      csrfEnVuelo = null;
    });
  return csrfEnVuelo;
}

async function send(
  path: string,
  { method = 'GET', body, signal }: RequestOptions,
  puedeReintentar = true,
): Promise<Response> {
  const esMutacion = method !== 'GET';
  const headers: Record<string, string> = {};
  // Con FormData el navegador arma el Content-Type multipart con su boundary.
  const esFormulario = body instanceof FormData;
  if (body !== undefined && !esFormulario) headers['Content-Type'] = 'application/json';
  if (esMutacion) {
    const { token, headerName } = await obtenerCsrf();
    headers[headerName] = token;
  }

  const response = await fetchRed(buildUrl(path), {
    method,
    signal,
    headers,
    body: body === undefined || esFormulario ? body : JSON.stringify(body),
  });

  if (response.status === 403 && esMutacion && puedeReintentar) {
    invalidarCsrf();
    return send(path, { method, body, signal }, false);
  }
  return response;
}

async function exigirOk(path: string, response: Response) {
  if (response.ok) return;
  // Un 401 en /auth/* es una respuesta esperable (credenciales, "no hay sesión").
  if (response.status === 401 && !path.startsWith('/auth/')) {
    window.dispatchEvent(new CustomEvent(EVENTO_SESION_EXPIRADA));
  }
  throw await toApiError(response);
}

async function request<S extends z.ZodTypeAny>(
  path: string,
  schema: S,
  options: RequestOptions = {},
): Promise<z.output<S>> {
  const response = await send(path, options);
  await exigirOk(path, response);
  const data: unknown = response.status === 204 ? undefined : await response.json();
  return schema.parse(data);
}

/** Un archivo que el servidor manda para descargar, con el nombre que sugiere. */
export type Archivo = { blob: Blob; nombre: string };

const NOMBRE_POR_DEFECTO = 'descarga';

// «attachment; filename="reporte.xlsx"» (o la variante filename*=UTF-8''...).
export function nombreDeArchivo(disposicion: string | null) {
  const codificado = /filename\*=UTF-8''([^;]+)/i.exec(disposicion ?? '')?.[1];
  if (codificado) return decodeURIComponent(codificado);
  return /filename="?([^";]+)"?/i.exec(disposicion ?? '')?.[1] ?? NOMBRE_POR_DEFECTO;
}

async function requestArchivo(path: string, signal?: AbortSignal): Promise<Archivo> {
  const response = await send(path, { signal });
  await exigirOk(path, response);
  return {
    blob: await response.blob(),
    nombre: nombreDeArchivo(response.headers.get('Content-Disposition')),
  };
}

export const http = {
  getArchivo: (path: string, signal?: AbortSignal) => requestArchivo(path, signal),
  get: <S extends z.ZodTypeAny>(path: string, schema: S, signal?: AbortSignal) =>
    request(path, schema, { signal }),
  post: <S extends z.ZodTypeAny>(path: string, body: unknown, schema: S) =>
    request(path, schema, { method: 'POST', body }),
  postForm: <S extends z.ZodTypeAny>(path: string, body: FormData, schema: S) =>
    request(path, schema, { method: 'POST', body }),
  put: <S extends z.ZodTypeAny>(path: string, body: unknown, schema: S) =>
    request(path, schema, { method: 'PUT', body }),
  patch: <S extends z.ZodTypeAny>(path: string, body: unknown, schema: S) =>
    request(path, schema, { method: 'PATCH', body }),
  delete: <S extends z.ZodTypeAny>(path: string, schema: S) =>
    request(path, schema, { method: 'DELETE' }),
};
