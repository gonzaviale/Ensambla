import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

// El almacén de ejemplo tiene 6 avisos; de más a menos urgente: sin Aceite, Yerba bajo el mínimo, Lavandina
// bajo el mínimo, Azúcar caro y estancado, Fideos barato frente a la zona y Yerba que se vende mucho.
// En la home entra uno de cada tema: Aceite (falta), Azúcar (estancado) y Fideos (precio frente a la zona).
// La sección arranca cerrada: salvo que se pida lo contrario, los tests la abren como lo haría la persona.
const abrirInicio = async (sistemas: Sistema[], { abierta = true } = {}) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  estado.sistemas.set(usuario.email, sistemas);
  const router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  await waitFor(() => expect(useSistemaStore.getState().accesoAvisos).not.toBeNull(), ESPERA);
  const user = userEvent.setup();
  if (abierta) await user.click(await screen.findByRole('heading', { name: 'Para decidir hoy' }));
  return { router, user };
};

const seccion = () =>
  screen.queryByRole('heading', { name: 'Para decidir hoy' })?.closest('details') ?? null;
const sugerencias = () =>
  Array.from(seccion()?.querySelectorAll<HTMLElement>('[data-alerta]') ?? []);
const titulos = () => sugerencias().map((fila) => within(fila).getByRole('heading').textContent);
const precioDe = (nombre: string) =>
  useSistemaStore
    .getState()
    .sistemas[0]?.sections[0]?.blocks[0]?.rows.find((fila) => fila.name === nombre)?.price;

describe('inicio: decisiones rápidas', () => {
  it('arranca cerrada y el encabezado cuenta cuántas sugerencias hay y cuántas son urgentes', async () => {
    await abrirInicio([crearAlmacenDeEjemplo(hoy())], { abierta: false });

    expect(seccion()).not.toHaveAttribute('open');
    expect(within(seccion() as HTMLElement).getByText('3 sugerencias · 1 urgente')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Registrar entrada' })).not.toBeVisible();
  });

  it('muestra una sugerencia de cada tema: lo que falta, lo que no se mueve y un precio frente a la zona', async () => {
    await abrirInicio([crearAlmacenDeEjemplo(hoy())]);

    expect(titulos()).toEqual([
      'Te quedaste sin Aceite Natura 900 ml',
      'Azúcar Ledesma 1 kg está caro y no se mueve',
      'Fideos Matarazzo 500 g está más barato que en tu zona',
    ]);
    expect(screen.getByRole('button', { name: /Ver las otras 3/ })).toBeVisible();
  });

  it('cada sugerencia es una línea con el botón de lo que propone, y sin descartar a la vista', async () => {
    await abrirInicio([crearAlmacenDeEjemplo(hoy())]);

    const [faltante, estancado] = sugerencias() as [HTMLElement, HTMLElement];

    expect(within(faltante).getByRole('button', { name: 'Registrar entrada' })).toBeVisible();
    expect(within(estancado).getByRole('button', { name: /^Bajar|^Subir|\$/ })).toBeVisible();
    expect(within(faltante).queryByRole('button', { name: /descartar/i })).toBeNull();
  });

  it('«Ver por qué» muestra el detalle de esa sugerencia y cada botón dice de cuál habla', async () => {
    const { user } = await abrirInicio([crearAlmacenDeEjemplo(hoy())]);
    const [faltante] = sugerencias() as [HTMLElement];
    expect(within(faltante).queryByText(/No te quedan unidades/)).toBeNull();

    await user.click(
      within(faltante).getByRole('button', {
        name: 'Ver por qué: Te quedaste sin Aceite Natura 900 ml',
      }),
    );

    expect(within(faltante).getByText(/No te quedan unidades/)).toBeVisible();
    expect(
      within(faltante).getByRole('button', {
        name: 'Ocultar: Te quedaste sin Aceite Natura 900 ml',
      }),
    ).toHaveAttribute('aria-expanded', 'true');
  });

  it('«Ver las otras» lleva a la pantalla de Avisos', async () => {
    const { user, router } = await abrirInicio([crearAlmacenDeEjemplo(hoy())]);

    await clickButton(user, /Ver las otras 3/);

    await verifyPathAsync(router, '/avisos');
  });

  it('aplicar una sugerencia de precio lo cambia en el cuaderno y la saca de la lista', async () => {
    const { user } = await abrirInicio([crearAlmacenDeEjemplo(hoy())]);
    const antes = precioDe('Azúcar Ledesma 1 kg');
    expect(antes).toBeDefined();
    const [, estancado] = sugerencias() as [HTMLElement, HTMLElement];

    await user.click(within(estancado).getByRole('button', { name: /\$/ }));

    await waitFor(() => expect(precioDe('Azúcar Ledesma 1 kg')).not.toBe(antes), ESPERA);
    expect(Number(precioDe('Azúcar Ledesma 1 kg'))).toBeLessThan(Number(antes));
    await waitFor(
      () => expect(titulos()).not.toContain('Azúcar Ledesma 1 kg está caro y no se mueve'),
      ESPERA,
    );
    expect(sugerencias()).toHaveLength(3);
  });

  it('descartar pide abrir el detalle primero y sube la siguiente sugerencia', async () => {
    const { user } = await abrirInicio([crearAlmacenDeEjemplo(hoy())]);
    const [faltante] = sugerencias() as [HTMLElement];

    await user.click(within(faltante).getByRole('button', { name: /^Ver por qué/ }));
    await user.click(within(faltante).getByRole('button', { name: /descartar/i }));

    await waitFor(
      () => expect(titulos()).not.toContain('Te quedaste sin Aceite Natura 900 ml'),
      ESPERA,
    );
    expect(sugerencias()).toHaveLength(3);
  });

  it('sin sugerencias la sección no aparece', async () => {
    await abrirInicio([crearAlmacenDeEjemplo(hoy())]);
    expect(seccion()).not.toBeNull();

    act(() => useSistemaStore.setState({ avisos: [] }));

    expect(seccion()).toBeNull();
  });
});
