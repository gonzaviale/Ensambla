import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { sistemaParaCrear } from '@/lib/sistema/acciones';
import type * as ConsejosDeFoto from '@/lib/sistema/consejosDeFoto';
import { debeMostrarConsejosDeFoto } from '@/lib/sistema/consejosDeFoto';
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

// Por defecto vale lo real (siempre mostrar); un test lo cambia para probar el otro camino.
vi.mock('@/lib/sistema/consejosDeFoto', async (original) => {
  const real = await original<typeof ConsejosDeFoto>();
  return { ...real, debeMostrarConsejosDeFoto: vi.fn(real.debeMostrarConsejosDeFoto) };
});

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

  it('la cámara abre un pop up con los consejos y «Tomar foto» abre el selector', async () => {
    const { user } = await abrirTaller();
    const selector = document.getElementById('guide-photo-file') as HTMLInputElement;
    const abrirSelector = vi.spyOn(selector, 'click');

    await user.click(screen.getByRole('button', { name: 'Mandarle una foto a Sami' }));

    const dialogo = await screen.findByRole('dialog', { name: 'Para que Sami lea bien tu foto' });
    expect(within(dialogo).getAllByRole('listitem').length).toBeGreaterThanOrEqual(4);
    expect(abrirSelector).not.toHaveBeenCalled();

    await user.click(within(dialogo).getByRole('button', { name: 'Tomar foto' }));

    expect(abrirSelector).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('el pop up de la foto se cierra con Escape sin abrir el selector', async () => {
    const { user } = await abrirTaller();
    const selector = document.getElementById('guide-photo-file') as HTMLInputElement;
    const abrirSelector = vi.spyOn(selector, 'click');
    await user.click(screen.getByRole('button', { name: 'Mandarle una foto a Sami' }));
    await screen.findByRole('dialog', { name: 'Para que Sami lea bien tu foto' });

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(abrirSelector).not.toHaveBeenCalled();
  });

  it('si se decide no mostrar los consejos, la cámara abre el selector directo', async () => {
    const { user } = await abrirTaller();
    const selector = document.getElementById('guide-photo-file') as HTMLInputElement;
    const abrirSelector = vi.spyOn(selector, 'click');
    vi.mocked(debeMostrarConsejosDeFoto).mockReturnValueOnce(false);

    await user.click(screen.getByRole('button', { name: 'Mandarle una foto a Sami' }));

    expect(abrirSelector).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('el chat flota: se minimiza a un botón y se vuelve a abrir sin perder la conversación', async () => {
    const { user } = await abrirTaller();

    await clickButton(user, 'Minimizar el chat');
    expect(screen.getByText(/Hola, Ana Pérez\. Contame qué hacés/)).not.toBeVisible();
    expect(screen.getByText('TU IDEA VA A APARECER ACÁ')).toBeVisible();
    const abrir = screen.getByRole('button', { name: 'Abrir el chat con Sami' });
    await waitFor(() => expect(abrir).toHaveFocus());

    await user.click(abrir);
    expect(screen.getByText(/Hola, Ana Pérez\. Contame qué hacés/)).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Abrir el chat con Sami' })).toBeNull();
    await waitFor(() =>
      expect(screen.getByLabelText('Contame un poco de tu negocio')).toHaveFocus(),
    );
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

  it('se puede editar un registro de ejemplo y agregar uno propio, y al crear el sistema pasan', async () => {
    const { user } = await conPropuesta();
    const bloque = screen
      .getByRole('button', { name: /^Productos/ })
      .closest<HTMLElement>('.preview-block');
    if (!bloque) throw new Error('Falta la pieza');
    expect(within(bloque).getAllByText('Ejemplo').length).toBeGreaterThan(0);

    const [primerLapiz] = within(bloque).getAllByRole('button', { name: 'Editar registro' });
    if (!primerLapiz) throw new Error('Falta el lápiz del primer registro');
    await user.click(primerLapiz);
    expect(await screen.findByRole('heading', { name: 'Editar producto' })).toBeVisible();
    expect(screen.queryByRole('button', { name: /Eliminar/ })).toBeNull();
    const nombre = screen.getByLabelText(/^Producto/);
    await user.clear(nombre);
    await user.type(nombre, 'Yerba Taragüí 1 kg');
    await clickButton(user, 'Guardar');
    await verifyToastAsync('Registro actualizado.');

    const editada = within(bloque).getByText('Yerba Taragüí 1 kg').closest('tr');
    expect(editada).not.toHaveClass('example-row');

    await user.click(within(bloque).getByRole('button', { name: '+ Agregar registro' }));
    expect(await screen.findByRole('heading', { name: 'Registrar producto' })).toBeVisible();
    await user.type(screen.getByLabelText(/^Producto/), 'Mate de calabaza');
    await user.type(screen.getByLabelText(/^Cantidad/), '8');
    await clickButton(user, 'Guardar');
    await verifyToastAsync('Registro guardado.');
    expect(within(bloque).getByText('Mate de calabaza')).toBeVisible();

    const borrador = useSistemaStore.getState().taller?.borrador;
    if (!borrador) throw new Error('Falta la propuesta');
    const creado = sistemaParaCrear(borrador);
    const productos = allBlocks(creado).find((pieza) => pieza.type === 'inventory');
    expect(productos?.rows.map((fila) => fila.name)).toEqual([
      'Yerba Taragüí 1 kg',
      'Mate de calabaza',
    ]);
  });

  it('un registro sin los datos obligatorios no se guarda y el formulario sigue abierto', async () => {
    const { user } = await conPropuesta();
    const bloque = screen
      .getByRole('button', { name: /^Productos/ })
      .closest<HTMLElement>('.preview-block');
    if (!bloque) throw new Error('Falta la pieza');
    const antes = within(bloque).getAllByRole('row').length;

    await user.click(within(bloque).getByRole('button', { name: '+ Agregar registro' }));
    await screen.findByRole('heading', { name: 'Registrar producto' });
    await clickButton(user, 'Guardar');

    expect((await screen.findAllByText('Completá este campo.')).length).toBeGreaterThan(0);
    expect(screen.getByRole('heading', { name: 'Registrar producto' })).toBeVisible();
    expect(within(bloque).getAllByRole('row')).toHaveLength(antes);
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
