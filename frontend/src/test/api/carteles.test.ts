import { describe, expect, it } from 'vitest';
import {
  armarOpcionesDeCartel,
  exportarCartel,
  obtenerPromocionesSugeridas,
  redactarTextosDeCartel,
} from '@/api/carteles';
import { PROMOCION_DE_PRUEBA } from '@/mocks/db/carteles';
import { pedidoDeTextos } from '@/lib/carteles';
import { setupUsuarioConSesion } from '@/test/testHelpers';
import type { ContenidoDeCartel } from '@/types/carteles';

const CONTENIDO: ContenidoDeCartel = {
  tipo: 'PROMO_PRECIO',
  formato: 'A4',
  titular: 'Aceite a precio de vecino',
  bajada: '20 % menos',
  protagonista: '$2.000',
  detalle: 'Antes $2.500',
  lineas: [],
};

describe('API de carteles', () => {
  it('lista las promociones sugeridas del negocio de la sesión', async () => {
    setupUsuarioConSesion();

    const { promociones } = await obtenerPromocionesSugeridas();

    expect(promociones).toEqual([PROMOCION_DE_PRUEBA]);
  });

  it('redacta los textos con los números de la promoción', async () => {
    setupUsuarioConSesion();

    const textos = await redactarTextosDeCartel(pedidoDeTextos(PROMOCION_DE_PRUEBA));

    expect(textos).toMatchObject({ plantilla: 'PROMO_PRECIO', redactadoConIa: true });
  });

  it('arma las tres opciones del cartel', async () => {
    setupUsuarioConSesion();

    const { opciones } = await armarOpcionesDeCartel(CONTENIDO);

    expect(opciones.map((opcion) => opcion.variante)).toEqual(['BLOQUE', 'FRANJA', 'MARCO']);
  });

  it('baja el cartel elegido como archivo con el nombre del servidor', async () => {
    setupUsuarioConSesion();

    const archivo = await exportarCartel({ ...CONTENIDO, variante: 'FRANJA' });

    expect(archivo.nombre).toBe('cartel-promo-precio-a4.pdf');
    expect(archivo.blob.size).toBeGreaterThan(0);
  });
});
