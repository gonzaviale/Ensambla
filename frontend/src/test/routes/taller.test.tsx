import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { allBlocks } from '@/lib/sistema/core';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  verifyToastAsync,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

const abrirTaller = async () => {
  setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  const user = userEvent.setup();
  await clickButton(user, /Crear con Sami/);
  await verifyHeadingPresentAsync('Lo armamos juntos.');
  return { router, user };
};

const conPropuesta = async () => {
  const taller = await abrirTaller();
  await clickButton(taller.user, /Almacén o kiosco/);
  await screen.findByLabelText('Nombre de tu sistema', {}, ESPERA);
  return taller;
};

const lienzo = () => {
  const nodo = document.querySelector<HTMLElement>('#draft-canvas');
  if (!nodo) throw new Error('Falta el lienzo de la propuesta');
  return nodo;
};

const piezas = () =>
  [
    ...document.querySelectorAll(
      '#draft-canvas .preview-block .preview-name, #draft-canvas .preview-block strong',
    ),
  ]
    .map((nodo) => nodo.textContent?.trim())
    .filter(Boolean);

describe('el taller de Sami', () => {
  it('empieza con el saludo, los ejemplos y el lienzo vacío', async () => {
    await abrirTaller();

    expect(screen.getByText('Hola, Ana Pérez.')).toBeVisible();
    expect(screen.getByText('Contame qué hacés y cómo te organizás hoy.')).toBeVisible();
    expect(screen.getByText('O probá con un ejemplo:')).toBeVisible();
    expect(screen.getByText('TU IDEA VA A APARECER ACÁ')).toBeVisible();
    expect(screen.getByText('Tu idea es el punto de partida')).toBeVisible();
  });

  it('pide un poco más cuando lo escrito es muy corto', async () => {
    const { user } = await abrirTaller();

    await user.type(screen.getByLabelText('Contame un poco de tu negocio'), 'a');
    await clickButton(user, /Pensalo conmigo/);

    await verifyToastAsync(/Contame un poquito más/);
    expect(useSistemaStore.getState().taller?.borrador).toBeNull();
  });

  it('con un ejemplo Sami arma una base que se ve a la derecha', async () => {
    await conPropuesta();

    expect(screen.getByLabelText('Nombre de tu sistema')).toHaveValue('Mi almacén');
    expect(screen.getByText('Propuesta sin guardar')).toBeVisible();
    expect(screen.getByText(/^Te armé los productos/)).toBeVisible();
    expect(screen.getByText('Ya lo podés ver a la derecha.')).toBeVisible();
    expect(screen.getByText('3 piezas conectadas')).toBeVisible();
    expect(screen.getByLabelText('¿Qué le falta? ¿Qué cambiarías?')).toBeVisible();
  });

  it('lo que se escribe después ajusta la propuesta', async () => {
    const { user } = await conPropuesta();

    await user.type(screen.getByLabelText('¿Qué le falta? ¿Qué cambiarías?'), 'sumá gastos');
    await clickButton(user, /Ajustalo conmigo/);

    expect(await screen.findByText('Listo, sumé los gastos.', {}, ESPERA)).toBeVisible();
    expect(screen.getByText('4 piezas conectadas')).toBeVisible();
  });

  it('se puede cambiar el nombre, el ancho y quitar piezas', async () => {
    const { user } = await conPropuesta();

    await user.click(within(lienzo()).getByRole('button', { name: /^Clientes/ }));
    const campo = await screen.findByLabelText('Nombre de la pieza');
    await user.clear(campo);
    await user.type(campo, 'Los de la cuadra');
    await clickButton(user, 'Guardar nombre');
    expect(await screen.findByRole('button', { name: /Los de la cuadra/ })).toBeVisible();

    const bloque = screen
      .getByRole('button', { name: /Los de la cuadra/ })
      .closest<HTMLElement>('.preview-block');
    if (!bloque) throw new Error('Falta la pieza');
    await user.click(within(bloque).getByRole('button', { name: 'Fila completa' }));
    expect(bloque).toHaveAttribute('data-width', 'full');

    await user.click(within(bloque).getByRole('button', { name: 'Quitar de la propuesta' }));
    expect(screen.queryByRole('button', { name: /Los de la cuadra/ })).toBeNull();
    expect(screen.getByText('2 piezas conectadas')).toBeVisible();
  });

  it('suma una pieza de la paleta y cambia la distribución', async () => {
    const { user } = await conPropuesta();

    await user.click(screen.getByRole('button', { name: /^Gastos/ }));
    expect(screen.getByText('4 piezas conectadas')).toBeVisible();

    await clickButton(user, /1 columna/);
    const anchos = [...document.querySelectorAll('#draft-canvas .preview-block')].map((nodo) =>
      nodo.getAttribute('data-width'),
    );
    expect(new Set(anchos)).toEqual(new Set(['full']));
  });

  it('arrastrar una pieza sobre otra la reubica', async () => {
    await conPropuesta();
    const antes = piezas();
    const [primera, segunda] = antes;
    const tirador = screen.getAllByRole('button', { name: 'Arrastrar esta pieza' })[1];
    const destino = document.querySelectorAll<HTMLElement>('[data-draft-block]')[0];
    if (!tirador || !destino) throw new Error('Faltan piezas');
    const dataTransfer = { setData: () => undefined, effectAllowed: '' };

    fireEvent.dragStart(tirador, { dataTransfer });
    fireEvent.dragOver(destino, { dataTransfer });
    fireEvent.drop(destino, { dataTransfer });

    await waitFor(() => expect(piezas().slice(0, 2)).toEqual([segunda, primera]), ESPERA);
  });

  it('crear el sistema lo guarda sin los ejemplos y lo abre listo para usar', async () => {
    const { user, router } = await conPropuesta();
    const nombre = screen.getByLabelText('Nombre de tu sistema');
    await user.clear(nombre);
    await user.type(nombre, 'Almacén Don Luis');

    await clickButton(user, /Sí, crear mi sistema/);

    await waitFor(() => expect(useSistemaStore.getState().sistemas).toHaveLength(1), ESPERA);
    const [sistema] = useSistemaStore.getState().sistemas;
    expect(sistema?.name).toBe('Almacén Don Luis');
    expect(allBlocks(sistema).every((bloque) => bloque.rows.length === 0)).toBe(true);
    expect(useSistemaStore.getState().listo).toBe(true);
    await verifyPathAsync(router, `/sistemas/${sistema?.id}`);
  });

  it('volver a entrar empieza una conversación nueva', async () => {
    const { user } = await conPropuesta();

    const volver = document.querySelector<HTMLElement>('.studio-top button');
    if (!volver) throw new Error('Falta el botón para volver');
    await user.click(volver);
    await verifyHeadingPresentAsync(/En qué puedo/);
    await clickButton(user, /Crear con Sami/);

    await verifyHeadingPresentAsync('Lo armamos juntos.');
    expect(useSistemaStore.getState().taller?.borrador).toBeNull();
    expect(screen.getByText('TU IDEA VA A APARECER ACÁ')).toBeVisible();
  });
});
