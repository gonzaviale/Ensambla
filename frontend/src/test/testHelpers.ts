import { screen, waitFor } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { expect } from 'vitest';
import { estado, sembrarUsuario, type UsuarioMock } from '@/mocks/db/estado';
import { PREFERENCIAS_INICIALES } from '@/types/preferencias';
import { server } from '@/mocks/node';

export const ESPERA = { timeout: 5000 };

type Metodo = 'get' | 'post' | 'put' | 'patch' | 'delete';

/* setup: backend simulado */

export const setupServerResponse = (
  metodo: Metodo,
  path: string,
  status: number,
  body?: unknown,
) => {
  server.use(
    http[metodo](`/api${path}`, () =>
      body === undefined ? new HttpResponse(null, { status }) : HttpResponse.json(body, { status }),
    ),
  );
};

export const setupServerError = (
  metodo: Metodo,
  path: string,
  status: number,
  message: string,
  errors?: Record<string, string>,
) => {
  setupServerResponse(metodo, path, status, {
    timestamp: '2026-10-09T10:00:00',
    status,
    error: 'Error',
    message,
    path: `/api${path}`,
    traceId: 'traza-de-prueba',
    ...(errors && { errors }),
  });
};

// La bienvenida de la primera vez tapa la pantalla con un modal: salvo que la prueba la quiera ver, ya fue vista.
export const setupUsuarioConSesion = (
  datos: Partial<UsuarioMock> = {},
  { bienvenidaVista = true }: { bienvenidaVista?: boolean } = {},
) => {
  const usuario = sembrarUsuario({ ...datos, conSesion: true });
  estado.preferencias.set(usuario.email, { ...PREFERENCIAS_INICIALES, bienvenidaVista });
  return usuario;
};

export const setupUsuarioSinSesion = (datos: Partial<UsuarioMock> = {}) => sembrarUsuario(datos);

/* interacciones */

export const clickButton = (user: UserEvent, name: string | RegExp) =>
  user.click(screen.getByRole('button', { name }));

export const clickLink = (user: UserEvent, name: string | RegExp) =>
  user.click(screen.getByRole('link', { name }));

export const typeLabel = (user: UserEvent, label: string | RegExp, texto: string) =>
  user.type(screen.getByLabelText(label), texto);

/* verificaciones */

export const verifyTextPresentAsync = async (texto: string | RegExp) => {
  expect(await screen.findByText(texto, {}, ESPERA)).toBeInTheDocument();
};

export const verifyHeadingPresentAsync = async (name: string | RegExp) => {
  expect(await screen.findByRole('heading', { name }, ESPERA)).toBeInTheDocument();
};

export const verifyAlertPresentAsync = async (texto: string | RegExp) => {
  expect(await screen.findByRole('alert', {}, ESPERA)).toHaveTextContent(texto);
};

export const verifyPathAsync = async (
  router: { state: { location: { pathname: string } } },
  path: string,
) => {
  await waitFor(() => expect(router.state.location.pathname).toBe(path), ESPERA);
};

/** El aviso breve de abajo (`#toast`); hay otros `status` en pantalla, como «Sami vio un cambio». */
export const verifyToastAsync = (texto: string | RegExp) =>
  waitFor(() => {
    const toast = document.getElementById('toast');
    if (!toast) throw new Error('Falta el aviso breve');
    if (typeof texto === 'string') expect(toast).toHaveTextContent(texto);
    else expect(toast.textContent).toMatch(texto);
  }, ESPERA);
