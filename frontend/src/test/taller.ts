import { screen, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { expect } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';

type Router = ReturnType<typeof renderApp>;

let router: Router;
let user: UserEvent;

export const elRouterDelTaller = () => router;

export const laPersonaDelTaller = () => user;

export const entrarAlTaller = async () => {
  await verifyHeadingPresentAsync(/En qué puedo/);
  user = userEvent.setup();
  await clickButton(user, /Crear con Sami/);
  await verifyHeadingPresentAsync('Construyamos juntos');
};

export const dadoQueSeAbreElTaller = async () => {
  setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  router = renderApp('/inicio');
  await entrarAlTaller();
};

export const dadoQueSamiArmoUnaPropuestaDeAlmacen = async () => {
  await dadoQueSeAbreElTaller();
  await clickButton(user, /Almacén o kiosco/);
  await screen.findByLabelText('Nombre de tu sección', {}, ESPERA);
};

let sistemaAjustado: Sistema;
let emailDeAna: string;

export const elSistemaQueSeAjusta = () => sistemaAjustado;

export const elEmailDeAna = () => emailDeAna;

/** Abre un almacén de Ana en su pantalla; `preparar` le deja los datos que la prueba necesita. */
export const dadoQueSeAbreUnAlmacenDeAna = async (
  preparar: (sistema: Sistema) => void = () => undefined,
  { conEjemplos = false }: { conEjemplos?: boolean } = {},
) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  emailDeAna = usuario.email;
  sistemaAjustado = makeTemplate('shop', {}, conEjemplos);
  sistemaAjustado.name = 'Almacén de Ana';
  preparar(sistemaAjustado);
  estado.sistemas.set(usuario.email, [sistemaAjustado]);
  router = renderApp(`/sistemas/${sistemaAjustado.id}?modo=usar`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de Ana/ }, ESPERA);
  user = userEvent.setup();
};

export const cuandoSePideAjustarConSami = async () => {
  await clickButton(user, /Ajustar con Sami/);
  await verifyHeadingPresentAsync('Construyamos juntos');
};

/** Abre «Ajustar con Sami» sobre un almacén de Ana. */
export const dadoQueSeAjustaConSamiUnAlmacenDeAna = async (
  preparar: (sistema: Sistema) => void = () => undefined,
) => {
  await dadoQueSeAbreUnAlmacenDeAna(preparar);
  await cuandoSePideAjustarConSami();
};

export const campoDeLaConversacion = () =>
  screen.getByLabelText('Contame un poco de tu negocio') as HTMLTextAreaElement;

export const campoDeLosAjustes = () => screen.getByLabelText('¿Qué le falta? ¿Qué cambiarías?');

export const lienzoDeLaPropuesta = () => {
  const nodo = document.querySelector<HTMLElement>('#draft-canvas');
  if (!nodo) throw new Error('Falta el lienzo de la propuesta');
  return nodo;
};

export const piezasDeLaPropuesta = () =>
  [...lienzoDeLaPropuesta().querySelectorAll('.block .block-nombre')]
    .map((nodo) => nodo.textContent?.trim())
    .filter(Boolean);

export const cantidadDePiezasDeLaPropuesta = () =>
  document.querySelectorAll('#draft-canvas > .block').length;

export const piezaDeLaPropuesta = (nombre: RegExp) => {
  const pieza = screen.getByRole('button', { name: nombre }).closest<HTMLElement>('.block');
  if (!pieza) throw new Error('Falta la pieza');
  return pieza;
};

export const selectorDeFoto = () => {
  const selector = document.querySelector<HTMLInputElement>('input[type="file"]');
  if (!selector) throw new Error('Falta el selector de la foto');
  return selector;
};

export const botonParaVolverAlInicio = () =>
  within(screen.getByRole('main')).getByRole('button', { name: /Inicio/ });

export const entoncesLaConversacionYElLienzoEstanVacios = () => {
  expect(screen.getByText('TU IDEA VA A APARECER ACÁ')).toBeVisible();
};
