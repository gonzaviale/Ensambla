import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { FormatoDeImagen } from '@/lib/promociones/formatosDeImagen';
import { allBlocks, makeSystem } from '@/lib/sistema/core';
import { hoy } from '@/lib/sistema/formato';
import type { Fila, Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupServerError,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';

const guardarArchivo = vi.hoisted(() => vi.fn());
vi.mock('@/lib/descargas', () => ({ guardarArchivo }));

const imagenDePromocion = vi.hoisted(() => vi.fn());
vi.mock('@/lib/promociones/imagenDePromocion', () => ({ imagenDePromocion }));

const INVENTARIO: Fila[] = [
  { id: 'p-yerba', name: 'Yerba Playadito 1 kg', price: 3600, quantity: 10 },
  { id: 'p-fideos', name: 'Fideos 500 g', price: 1100, quantity: 30 },
];

const sistemaConVentas = (): Sistema => {
  const sistema = makeSystem('Almacén de Ana', ['inventory', 'sales']);
  const [inventario, ventas] = allBlocks(sistema);
  if (!inventario || !ventas) throw new Error('El sistema de prueba no tiene sus tablas');
  inventario.rows = INVENTARIO;
  ventas.rows = [{ id: 'v1', product: 'p-yerba', quantity: 8, date: hoy() }];
  return sistema;
};

const sistemaSinVentas = (): Sistema => {
  const sistema = makeSystem('Almacén de Ana', ['inventory']);
  const [inventario] = allBlocks(sistema);
  if (inventario) inventario.rows = INVENTARIO;
  return sistema;
};

const abrirPromociones = async (sistemas: Sistema[]) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  estado.sistemas.set(usuario.email, sistemas);
  renderApp('/promociones');
  await verifyHeadingPresentAsync('Promocioná tu comercio');
  return userEvent.setup();
};

const formatoUsado = () =>
  (imagenDePromocion.mock.calls.at(-1)?.[0] as FormatoDeImagen | undefined)?.clave;

describe('promocioná tu comercio', () => {
  beforeEach(() => {
    guardarArchivo.mockClear();
    imagenDePromocion.mockReset();
    imagenDePromocion.mockResolvedValue(new Blob(['png'], { type: 'image/png' }));
    URL.createObjectURL = vi.fn(() => 'blob:imagen-de-prueba');
    URL.revokeObjectURL = vi.fn();
  });
  afterEach(() => {
    Reflect.deleteProperty(navigator, 'clipboard');
    vi.restoreAllMocks();
  });

  it('propone qué promocionar con lo que se vendió, lo que está parado y la lista de precios', async () => {
    await abrirPromociones([sistemaConVentas()]);

    const ideas = await screen.findByRole('list', { name: 'Qué promocionar' }, ESPERA);

    expect(within(ideas).getByRole('button', { name: /Tu producto estrella/ })).toHaveTextContent('Yerba Playadito 1 kg');
    expect(within(ideas).getByRole('button', { name: /Liquidá lo que no se mueve/ })).toHaveTextContent('Fideos 500 g');
    expect(within(ideas).getByRole('button', { name: /Lista de precios/ })).toBeVisible();
  });

  it('al elegir una idea y un canal Sami escribe el texto para ese lugar y arma la imagen', async () => {
    const user = await abrirPromociones([sistemaConVentas()]);
    await user.click(await screen.findByRole('button', { name: /Tu producto estrella/ }, ESPERA));

    await user.click(screen.getByRole('button', { name: 'Instagram' }));

    const texto = await screen.findByLabelText<HTMLTextAreaElement>('Texto (podés cambiarlo)', {}, ESPERA);
    expect(texto.value).toContain('Yerba Playadito 1 kg');
    expect(await screen.findByRole('img', { name: /Tu producto estrella/ }, ESPERA)).toHaveAttribute(
      'src',
      'blob:imagen-de-prueba',
    );
    expect(formatoUsado()).toBe('cuadrado');
  });

  it('el estado de WhatsApp se arma en formato historia y se puede cambiar el formato', async () => {
    const user = await abrirPromociones([sistemaConVentas()]);
    await user.click(await screen.findByRole('button', { name: /Tu producto estrella/ }, ESPERA));
    await user.click(screen.getByRole('button', { name: 'Estado de WhatsApp' }));
    await screen.findByLabelText('Texto (podés cambiarlo)', {}, ESPERA);
    await waitFor(() => expect(formatoUsado()).toBe('historia'), ESPERA);

    await user.selectOptions(screen.getByLabelText('Formato de la imagen'), 'cuadrado');

    await waitFor(() => expect(formatoUsado()).toBe('cuadrado'), ESPERA);
  });

  it('la lista de precios siempre se dibuja como lista y no ofrece elegir formato', async () => {
    const user = await abrirPromociones([sistemaConVentas()]);
    await user.click(await screen.findByRole('button', { name: /Lista de precios/ }, ESPERA));

    await user.click(screen.getByRole('button', { name: 'Estado de WhatsApp' }));

    await screen.findByLabelText('Texto (podés cambiarlo)', {}, ESPERA);
    await waitFor(() => expect(formatoUsado()).toBe('lista'), ESPERA);
    expect(screen.queryByLabelText('Formato de la imagen')).not.toBeInTheDocument();
  });

  it('copia el texto que la persona dejó, con sus cambios', async () => {
    const user = await abrirPromociones([sistemaConVentas()]);
    const escribir = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: escribir }, configurable: true });
    await user.click(await screen.findByRole('button', { name: /Tu producto estrella/ }, ESPERA));
    await user.click(screen.getByRole('button', { name: 'Instagram' }));
    const texto = await screen.findByLabelText('Texto (podés cambiarlo)', {}, ESPERA);

    await user.clear(texto);
    await user.type(texto, 'Hoy hay yerba');
    await user.click(screen.getByRole('button', { name: /Copiar texto/ }));

    await waitFor(() => expect(escribir).toHaveBeenCalledWith('Hoy hay yerba'), ESPERA);
    expect(await screen.findByText('Texto copiado.')).toBeVisible();
  });

  it('si no se puede copiar lo avisa', async () => {
    const user = await abrirPromociones([sistemaConVentas()]);
    const escribir = vi.fn().mockRejectedValue(new Error('sin permiso'));
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: escribir }, configurable: true });
    await user.click(await screen.findByRole('button', { name: /Tu producto estrella/ }, ESPERA));
    await user.click(screen.getByRole('button', { name: 'Instagram' }));
    await screen.findByLabelText('Texto (podés cambiarlo)', {}, ESPERA);

    await user.click(screen.getByRole('button', { name: /Copiar texto/ }));

    expect(await screen.findByText(/No pudimos copiar el texto/)).toBeVisible();
  });

  it('en la computadora «Compartir» baja la imagen y abre WhatsApp con el texto', async () => {
    const abrir = vi.spyOn(window, 'open').mockReturnValue(null);
    const user = await abrirPromociones([sistemaConVentas()]);
    await user.click(await screen.findByRole('button', { name: /Tu producto estrella/ }, ESPERA));
    await user.click(screen.getByRole('button', { name: 'Mensaje a clientes' }));
    await screen.findByRole('img', { name: /Tu producto estrella/ }, ESPERA);

    await user.click(screen.getByRole('button', { name: /Compartir/ }));

    await waitFor(() => expect(abrir).toHaveBeenCalledTimes(1), ESPERA);
    expect(String(abrir.mock.calls[0]?.[0])).toContain('https://wa.me/?text=');
    expect(guardarArchivo).toHaveBeenCalledWith(expect.objectContaining({ nombre: 'promocion-estrella.png' }));
  });

  it('si no se puede abrir WhatsApp lo dice', async () => {
    vi.spyOn(window, 'open').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    const user = await abrirPromociones([sistemaConVentas()]);
    await user.click(await screen.findByRole('button', { name: /Tu producto estrella/ }, ESPERA));
    await user.click(screen.getByRole('button', { name: 'Mensaje a clientes' }));
    await screen.findByRole('img', { name: /Tu producto estrella/ }, ESPERA);

    await user.click(screen.getByRole('button', { name: /Compartir/ }));

    expect(await screen.findByText(/No pudimos abrir WhatsApp/)).toBeVisible();
  });

  it('«Descargar imagen» baja el PNG de la promoción', async () => {
    const user = await abrirPromociones([sistemaConVentas()]);
    await user.click(await screen.findByRole('button', { name: /Tu producto estrella/ }, ESPERA));
    await user.click(screen.getByRole('button', { name: 'Instagram' }));
    await screen.findByRole('img', { name: /Tu producto estrella/ }, ESPERA);

    await user.click(screen.getByRole('button', { name: /Descargar imagen/ }));

    expect(guardarArchivo).toHaveBeenCalledWith(expect.objectContaining({ nombre: 'promocion-estrella.png' }));
  });

  it('si el navegador no puede armar la imagen lo avisa y deja copiar el texto', async () => {
    imagenDePromocion.mockRejectedValue(new Error('sin lienzo'));
    const user = await abrirPromociones([sistemaConVentas()]);
    await user.click(await screen.findByRole('button', { name: /Tu producto estrella/ }, ESPERA));
    await user.click(screen.getByRole('button', { name: 'Instagram' }));

    expect(await screen.findByText(/No pudimos armar la imagen/)).toBeVisible();
    expect(screen.getByRole('button', { name: /Compartir/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Copiar texto/ })).toBeEnabled();
  });

  it('cambiar de idea vuelve a pedir el canal y borra el texto anterior', async () => {
    const user = await abrirPromociones([sistemaConVentas()]);
    await user.click(await screen.findByRole('button', { name: /Tu producto estrella/ }, ESPERA));
    await user.click(screen.getByRole('button', { name: 'Instagram' }));
    await screen.findByLabelText('Texto (podés cambiarlo)', {}, ESPERA);

    await user.click(screen.getByRole('button', { name: /Liquidá lo que no se mueve/ }));

    expect(screen.queryByLabelText('Texto (podés cambiarlo)')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Instagram' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('sin ventas anotadas solo ofrece la lista de precios', async () => {
    await abrirPromociones([sistemaSinVentas()]);

    const ideas = await screen.findByRole('list', { name: 'Qué promocionar' }, ESPERA);

    expect(within(ideas).getAllByRole('button')).toHaveLength(1);
    expect(within(ideas).getByRole('button', { name: /Lista de precios/ })).toBeVisible();
  });

  it('sin productos con precio explica qué cargar', async () => {
    const sistema = makeSystem('Almacén de Ana', ['inventory']);

    await abrirPromociones([sistema]);

    expect(await screen.findByText(/Todavía no hay nada para promocionar/, {}, ESPERA)).toBeVisible();
  });

  it('sin ningún cuaderno con inventario lleva a armarlo', async () => {
    await abrirPromociones([makeSystem('Clientes', ['clients'])]);

    expect(await screen.findByText(/armá un cuaderno con tu inventario/, {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Armar mi cuaderno' })).toBeVisible();
  });

  it('si el servidor no puede ver las ventas ofrece reintentar', async () => {
    setupServerError('get', '/proyectos/:sistemaId/promociones', 500, 'Error interno');
    const user = await abrirPromociones([sistemaConVentas()]);

    expect(await screen.findByText('No pudimos ver tus ventas. Probá nuevamente.', {}, ESPERA)).toBeVisible();
    server.resetHandlers();
    await user.click(screen.getByRole('button', { name: 'Reintentar' }));

    await waitFor(() => expect(screen.getByRole('button', { name: /Lista de precios/ })).toBeVisible(), ESPERA);
  });

  it('si Sami no puede escribir el texto lo dice', async () => {
    setupServerError('post', '/promociones/texto', 500, 'Error interno');
    const user = await abrirPromociones([sistemaConVentas()]);
    await user.click(await screen.findByRole('button', { name: /Tu producto estrella/ }, ESPERA));

    await user.click(screen.getByRole('button', { name: 'Instagram' }));

    expect(await screen.findByRole('alert')).toBeVisible();
    expect(screen.queryByLabelText('Texto (podés cambiarlo)')).not.toBeInTheDocument();
  });

  it('se llega desde la barra de arriba', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    estado.sistemas.set(usuario.email, [sistemaConVentas()]);
    renderApp('/inicio');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Promociones' }, ESPERA));

    await verifyHeadingPresentAsync('Promocioná tu comercio');
  });
});
