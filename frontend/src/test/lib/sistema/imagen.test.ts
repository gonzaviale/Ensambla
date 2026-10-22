import { afterEach, describe, expect, it, vi } from 'vitest';
import { aBase64, dimensionesReducidas, reducirImagen } from '@/lib/sistema/imagen';

const MB = 1024 * 1024;

const archivo = (bytes: number, tipo = 'image/jpeg') =>
  new File([new Uint8Array(bytes)], 'foto', { type: tipo });

const mapa = (ancho: number, alto: number) => ({ width: ancho, height: alto, close: vi.fn() });

function givenCanvas(contexto: object | null, blob: Blob | null) {
  const canvas = {
    width: 0,
    height: 0,
    getContext: () => contexto,
    toBlob: (resolver: (valor: Blob | null) => void) => resolver(blob),
  };
  vi.spyOn(document, 'createElement').mockReturnValue(canvas as unknown as HTMLElement);
  return canvas;
}

const contexto2d = () => ({ fillStyle: '', fillRect: vi.fn(), drawImage: vi.fn() });

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('dimensionesReducidas', () => {
  it('no toca una imagen que ya entra en el lado máximo', () => {
    expect(dimensionesReducidas(1200, 800)).toEqual({ ancho: 1200, alto: 800 });
  });

  it('achica por el lado más largo y conserva la proporción', () => {
    expect(dimensionesReducidas(4000, 3000)).toEqual({ ancho: 2000, alto: 1500 });
    expect(dimensionesReducidas(3000, 6000)).toEqual({ ancho: 1000, alto: 2000 });
  });
});

describe('reducirImagen', () => {
  it('sin createImageBitmap manda la foto tal cual', async () => {
    vi.stubGlobal('createImageBitmap', undefined);
    const foto = archivo(3 * MB);

    expect(await reducirImagen(foto)).toBe(foto);
  });

  it('un GIF no se toca', async () => {
    const crear = vi.fn();
    vi.stubGlobal('createImageBitmap', crear);
    const gif = archivo(3 * MB, 'image/gif');

    expect(await reducirImagen(gif)).toBe(gif);
    expect(crear).not.toHaveBeenCalled();
  });

  it('una foto liviana y chica no se vuelve a comprimir', async () => {
    const imagen = mapa(1200, 900);
    vi.stubGlobal('createImageBitmap', vi.fn().mockResolvedValue(imagen));
    const foto = archivo(MB);

    expect(await reducirImagen(foto)).toBe(foto);
    expect(imagen.close).toHaveBeenCalled();
  });

  it('una foto grande se achica y se guarda como JPG con fondo blanco', async () => {
    const imagen = mapa(4000, 3000);
    vi.stubGlobal('createImageBitmap', vi.fn().mockResolvedValue(imagen));
    const contexto = contexto2d();
    const reducida = new Blob([new Uint8Array(MB)], { type: 'image/jpeg' });
    const canvas = givenCanvas(contexto, reducida);

    const resultado = await reducirImagen(archivo(6 * MB, 'image/png'));

    expect(resultado).toBe(reducida);
    expect([canvas.width, canvas.height]).toEqual([2000, 1500]);
    expect(contexto.fillRect).toHaveBeenCalledWith(0, 0, 2000, 1500);
    expect(contexto.drawImage).toHaveBeenCalledWith(imagen, 0, 0, 2000, 1500);
    expect(imagen.close).toHaveBeenCalled();
  });

  it('con otro lado máximo (el de las fotos para Sami) achica hasta ese lado', async () => {
    vi.stubGlobal('createImageBitmap', vi.fn().mockResolvedValue(mapa(4000, 3000)));
    const reducida = new Blob([new Uint8Array(MB)], { type: 'image/jpeg' });
    const canvas = givenCanvas(contexto2d(), reducida);

    expect(await reducirImagen(archivo(6 * MB), 1600)).toBe(reducida);
    expect([canvas.width, canvas.height]).toEqual([1600, 1200]);
  });

  it('si la versión achicada pesa más que la original, manda la original', async () => {
    vi.stubGlobal('createImageBitmap', vi.fn().mockResolvedValue(mapa(1800, 1200)));
    givenCanvas(contexto2d(), new Blob([new Uint8Array(4 * MB)]));
    const foto = archivo(2 * MB);

    expect(await reducirImagen(foto)).toBe(foto);
  });

  it('si el canvas no devuelve imagen, manda la original', async () => {
    vi.stubGlobal('createImageBitmap', vi.fn().mockResolvedValue(mapa(4000, 3000)));
    givenCanvas(contexto2d(), null);
    const foto = archivo(6 * MB);

    expect(await reducirImagen(foto)).toBe(foto);
  });

  it('si el canvas no tiene contexto, manda la original', async () => {
    const imagen = mapa(4000, 3000);
    vi.stubGlobal('createImageBitmap', vi.fn().mockResolvedValue(imagen));
    givenCanvas(null, null);
    const foto = archivo(6 * MB);

    expect(await reducirImagen(foto)).toBe(foto);
    expect(imagen.close).toHaveBeenCalled();
  });

  it('si el navegador no puede abrir la imagen, manda la original', async () => {
    vi.stubGlobal('createImageBitmap', vi.fn().mockRejectedValue(new Error('formato')));
    const foto = archivo(6 * MB);

    expect(await reducirImagen(foto)).toBe(foto);
  });
});

describe('aBase64', () => {
  it('devuelve el contenido en base64, sin el prefijo data:', async () => {
    expect(await aBase64(new Blob(['pixeles'], { type: 'image/jpeg' }))).toBe(btoa('pixeles'));
  });

  it('una imagen grande no desborda la pila', async () => {
    const bytes = new Uint8Array(300_000).fill(65);

    expect(await aBase64(new Blob([bytes]))).toBe(btoa('A'.repeat(300_000)));
  });
});
