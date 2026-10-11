import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupServerError,
  setupServerResponse,
  setupUsuarioConSesion,
  verifyAlertPresentAsync,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';

const ERROR_TIPO = 'La foto 1 no es una imagen JPEG, PNG, WEBP o HEIC ni un PDF';

const foto = (nombre: string, tipo = 'image/jpeg') =>
  new File([new Uint8Array([0xff, 0xd8, 0xff, 0xe0])], nombre, { type: tipo });

const paginas = (cantidad: number) =>
  Array.from({ length: cantidad }, (_, i) => foto(`pagina-${i + 1}.jpg`));

let router: ReturnType<typeof renderApp>;

async function abrirCuaderno() {
  setupUsuarioConSesion();
  const user = userEvent.setup({ applyAccept: false });
  router = renderApp('/cuaderno');
  await verifyHeadingPresentAsync('Subir foto de mi cuaderno');
  return user;
}

const galeria = () => screen.getByLabelText('Elegir de la galería');
const miniaturas = () =>
  within(screen.getByRole('list', { name: 'Hojas para leer' })).getAllByRole('listitem');

describe('T-091: subir foto de mi cuaderno', () => {
  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:vista-previa');
    URL.revokeObjectURL = vi.fn();
  });

  it('ofrece cámara, galería y consejos antes de elegir fotos', async () => {
    await abrirCuaderno();

    expect(screen.getByLabelText('Sacar foto')).toHaveAttribute('capture', 'environment');
    expect(galeria()).toHaveAttribute('multiple');
    expect(
      screen.getByRole('heading', { name: 'Consejos para una buena foto' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Leer mi cuaderno/ })).not.toBeInTheDocument();
  });

  it('muestra una miniatura por hoja elegida, en orden', async () => {
    const user = await abrirCuaderno();

    await user.upload(galeria(), paginas(3));

    expect(miniaturas()).toHaveLength(3);
    expect(screen.getByRole('img', { name: 'Hoja 3' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Leer mi cuaderno (3 hojas)' })).toBeEnabled();
  });

  it('ignora los archivos que no son imágenes', async () => {
    const user = await abrirCuaderno();

    await user.upload(galeria(), [foto('pagina.jpg'), foto('notas.txt', 'text/plain')]);

    expect(miniaturas()).toHaveLength(1);
  });

  it('gira una hoja de a 90° desde su miniatura', async () => {
    const user = await abrirCuaderno();
    await user.upload(galeria(), paginas(1));

    await user.click(screen.getByRole('button', { name: 'Girar hoja 1' }));

    expect(screen.getByRole('img', { name: 'Hoja 1' })).toHaveStyle({
      transform: 'rotate(90deg)',
    });
  });

  it('quita una hoja y renumera las demás', async () => {
    const user = await abrirCuaderno();
    await user.upload(galeria(), paginas(3));

    await user.click(screen.getByRole('button', { name: 'Quitar hoja 2' }));

    expect(miniaturas()).toHaveLength(2);
    expect(screen.queryByRole('img', { name: 'Hoja 3' })).not.toBeInTheDocument();
    expect(URL.revokeObjectURL).toHaveBeenCalled();
  });

  it('acepta hasta 10 hojas y avisa si se eligen más', async () => {
    const user = await abrirCuaderno();

    await user.upload(galeria(), paginas(12));

    expect(miniaturas()).toHaveLength(10);
    await verifyAlertPresentAsync('Podés subir hasta 10 hojas por vez');
  });

  it('manda las hojas a leer y confirma cuántas recibió', async () => {
    const user = await abrirCuaderno();
    await user.upload(galeria(), paginas(3));

    await user.click(screen.getByRole('button', { name: 'Leer mi cuaderno (3 hojas)' }));

    await verifyHeadingPresentAsync('Estamos leyendo tu cuaderno');
    expect(screen.getByText(/Recibimos 3 hojas/)).toBeInTheDocument();
  });

  it('lleva a la dirección de la lectura y, al terminar, deja revisar lo que leyó la IA', async () => {
    const user = await abrirCuaderno();
    await user.upload(galeria(), paginas(1));
    await user.click(screen.getByRole('button', { name: 'Leer mi cuaderno (1 hoja)' }));

    await verifyHeadingPresentAsync('Esto es lo que leímos de tu cuaderno');

    expect(router.state.location.pathname).toMatch(/^\/cuaderno\/[0-9a-f-]{36}$/);
    expect(screen.getByRole('textbox', { name: 'Producto, fila 1' })).toHaveValue('Pan');
    expect(screen.getByRole('textbox', { name: 'Monto, fila 2' })).toHaveValue('850');
    expect(screen.getByText('No estamos seguros de este dato.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Descartar y subir otras fotos' }));
    expect(await screen.findByLabelText('Elegir de la galería', {}, ESPERA)).toBeInTheDocument();
  }, 10000);

  it('ofrece reintentar cuando la lectura falló', async () => {
    const user = await abrirCuaderno();
    await user.upload(galeria(), paginas(1));
    setupServerResponse('get', '/lotes/:id/lectura', 200, {
      loteId: crypto.randomUUID(),
      estado: 'ERROR',
      motivoError: 'No se pudo leer la foto',
      intentos: 3,
    });
    await user.click(screen.getByRole('button', { name: 'Leer mi cuaderno (1 hoja)' }));

    await verifyHeadingPresentAsync('No pudimos leer tu cuaderno');
    expect(screen.getByRole('button', { name: 'Volver a intentar' })).toBeEnabled();
  });

  it('muestra el error del servidor y conserva las hojas para reintentar', async () => {
    const user = await abrirCuaderno();
    setupServerError('post', '/lotes', 400, ERROR_TIPO);
    await user.upload(galeria(), paginas(1));

    await user.click(screen.getByRole('button', { name: 'Leer mi cuaderno (1 hoja)' }));

    await verifyAlertPresentAsync(ERROR_TIPO);
    expect(miniaturas()).toHaveLength(1);
  });
});
