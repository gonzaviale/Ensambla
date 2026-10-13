import { describe, expect, it } from 'vitest';
import {
  describirValorInicial,
  esValorInicialValido,
  prepararCampo,
  type BorradorCampo,
} from '@/lib/campos';

const TABLA = '00000000-0000-4000-8000-0000000000b1';

const borrador = (cambios: Partial<BorradorCampo>): BorradorCampo => ({
  nombre: 'Producto',
  tipo: 'RELACION',
  obligatorio: false,
  valorInicial: '',
  opciones: [],
  bloqueRelacionadoId: '',
  ...cambios,
});

describe('prepararCampo: conexión con otra tabla', () => {
  it('pide la tabla con la que se conecta', () => {
    expect(prepararCampo(borrador({}))).toEqual({
      ok: false,
      errores: { bloqueRelacionadoId: 'Elegí la tabla con la que se conecta.' },
    });
  });

  it('envía la tabla, sin valor inicial ni opciones', () => {
    expect(
      prepararCampo(
        borrador({ bloqueRelacionadoId: TABLA, valorInicial: 'algo', opciones: ['a'] }),
      ),
    ).toEqual({
      ok: true,
      datos: {
        nombre: 'Producto',
        tipo: 'RELACION',
        obligatorio: false,
        valorInicial: null,
        opciones: [],
        bloqueRelacionadoId: TABLA,
      },
    });
  });

  it('los demás tipos no se conectan con ninguna tabla', () => {
    const preparado = prepararCampo(
      borrador({ tipo: 'TEXTO', bloqueRelacionadoId: TABLA, valorInicial: 'Hola' }),
    );
    expect(preparado).toMatchObject({
      ok: true,
      datos: { tipo: 'TEXTO', valorInicial: 'Hola', bloqueRelacionadoId: null },
    });
  });
});

describe('valor inicial de una conexión', () => {
  it('no admite valor inicial ni lo describe', () => {
    expect(esValorInicialValido('RELACION', TABLA, [])).toBe(false);
    expect(describirValorInicial('RELACION', TABLA)).toBeNull();
  });
});
