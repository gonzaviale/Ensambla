import { describe, expect, it } from 'vitest';
import { girar, medidasRotadas, prepararFoto } from '@/lib/imagen';

describe('T-091: preparación de las fotos', () => {
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

  it('manda el archivo original si el navegador no puede decodificar la imagen', async () => {
    const archivo = new File([new Uint8Array([1, 2, 3])], 'pagina.heic', { type: 'image/heic' });

    expect(await prepararFoto(archivo, 90)).toBe(archivo);
  });
});
