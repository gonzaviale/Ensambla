import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  PESO_MAXIMO_BYTES,
  girar,
  medidasReducidas,
  medidasRotadas,
  prepararFoto,
  type Rotacion,
} from '@/lib/imagen';

const MB = 1024 * 1024;

const archivo = (nombre = 'pagina.jpg') =>
  new File([new Uint8Array(8 * MB)], nombre, { type: 'image/jpeg' });

type Lienzo = { width: number; height: number; contexto: ContextoFalso };
type ContextoFalso = {
  translate: ReturnType<typeof vi.fn>;
  rotate: ReturnType<typeof vi.fn>;
  drawImage: ReturnType<typeof vi.fn>;
};

// Simula el navegador: una foto de cámara de 4000x3000 y un JPEG cuyo peso depende de la calidad.
function simularNavegador(pesoSegunCalidad: (calidad: number) => number | null) {
  const imagen = { width: 4000, height: 3000, close: vi.fn() };
  vi.stubGlobal(
    'createImageBitmap',
    vi.fn(() => Promise.resolve(imagen)),
  );
  const lienzos: Lienzo[] = [];
  const calidades: number[] = [];
  vi.spyOn(document, 'createElement').mockImplementation(() => {
    const contexto = { translate: vi.fn(), rotate: vi.fn(), drawImage: vi.fn() };
    const lienzo = {
      width: 0,
      height: 0,
      contexto,
      getContext: () => contexto,
      toBlob: (listo: (blob: Blob | null) => void, _tipo: string, calidad: number) => {
        calidades.push(calidad);
        const peso = pesoSegunCalidad(calidad);
        listo(peso === null ? null : new Blob([new Uint8Array(peso)], { type: 'image/jpeg' }));
      },
    };
    lienzos.push(lienzo);
    return lienzo as unknown as HTMLElement;
  });
  return { imagen, lienzos, calidades };
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('T-091: giro de las páginas', () => {
  it('gira de a 90° y vuelve al inicio después de una vuelta', () => {
    expect(girar(0)).toBe(90);
    expect(girar(90)).toBe(180);
    expect(girar(180)).toBe(270);
    expect(girar(270)).toBe(0);
  });

  it('intercambia ancho y alto sólo cuando la foto queda de costado', () => {
    expect(medidasRotadas(4000, 3000, 0)).toEqual({ ancho: 4000, alto: 3000 });
    expect(medidasRotadas(4000, 3000, 90)).toEqual({ ancho: 3000, alto: 4000 });
    expect(medidasRotadas(4000, 3000, 180)).toEqual({ ancho: 4000, alto: 3000 });
    expect(medidasRotadas(4000, 3000, 270)).toEqual({ ancho: 3000, alto: 4000 });
  });
});

describe('T-092: compresión y orientación en el cliente', () => {
  it('achica el lado mayor a 2000px sin deformar y nunca agranda', () => {
    expect(medidasReducidas(4000, 3000)).toEqual({ ancho: 2000, alto: 1500 });
    expect(medidasReducidas(3000, 4000)).toEqual({ ancho: 1500, alto: 2000 });
    expect(medidasReducidas(1200, 900)).toEqual({ ancho: 1200, alto: 900 });
  });

  it('una foto de 8 MB sube como JPEG de menos de 1,5 MB y a 2000px', async () => {
    const { imagen, lienzos } = simularNavegador(() => 900 * 1024);

    const foto = await prepararFoto(archivo(), 0);

    expect(foto.size).toBeLessThan(PESO_MAXIMO_BYTES);
    expect(foto.type).toBe('image/jpeg');
    expect(lienzos[0]).toMatchObject({ width: 2000, height: 1500 });
    expect(lienzos[0]?.contexto.drawImage).toHaveBeenCalledWith(imagen, -1000, -750, 2000, 1500);
    expect(imagen.close).toHaveBeenCalled();
  });

  it('pide al navegador la orientación EXIF para que la foto quede derecha', async () => {
    simularNavegador(() => 900 * 1024);
    const original = archivo();

    await prepararFoto(original, 0);

    expect(createImageBitmap).toHaveBeenCalledWith(original, { imageOrientation: 'from-image' });
  });

  it.each<[Rotacion, number, number]>([
    [90, 1500, 2000],
    [180, 2000, 1500],
    [270, 1500, 2000],
  ])('aplica el giro de %i° que eligió el usuario', async (rotacion, ancho, alto) => {
    const { lienzos } = simularNavegador(() => 900 * 1024);

    await prepararFoto(archivo(), rotacion);

    expect(lienzos[0]).toMatchObject({ width: ancho, height: alto });
    expect(lienzos[0]?.contexto.rotate).toHaveBeenCalledWith((rotacion * Math.PI) / 180);
  });

  it('baja la calidad hasta que la foto entra en el máximo', async () => {
    const { calidades } = simularNavegador((calidad) => (calidad > 0.7 ? 2 * MB : 1 * MB));

    const foto = await prepararFoto(archivo(), 0);

    expect(calidades).toEqual([0.85, 0.75, 0.65]);
    expect(foto.size).toBe(1 * MB);
  });

  it('si ni la calidad más baja alcanza, manda la versión más liviana', async () => {
    const { calidades } = simularNavegador(() => 2 * MB);

    const foto = await prepararFoto(archivo(), 0);

    expect(calidades).toEqual([0.85, 0.75, 0.65, 0.55]);
    expect(foto.size).toBe(2 * MB);
  });

  it('manda el original si el navegador no puede generar el JPEG', async () => {
    simularNavegador(() => null);
    const original = archivo();

    expect(await prepararFoto(original, 90)).toBe(original);
  });

  it('manda el original si el navegador no puede decodificar la imagen, como HEIC fuera de Safari', async () => {
    vi.stubGlobal(
      'createImageBitmap',
      vi.fn(() => Promise.reject(new Error('formato no soportado'))),
    );
    const original = archivo('pagina.heic');

    expect(await prepararFoto(original, 0)).toBe(original);
  });

  it('manda el original si el navegador no sabe decodificar imágenes', async () => {
    const original = archivo();

    expect(await prepararFoto(original, 90)).toBe(original);
  });
});
