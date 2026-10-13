import { describe, expect, it } from 'vitest';
import {
  describirFormula,
  describirValorInicial,
  esValorInicialValido,
  piezasDeFormula,
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
  formula: '',
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
        formula: null,
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

describe('prepararCampo: cálculo', () => {
  const ID = '00000000-0000-4000-8000-0000000000c1';

  it('pide armar el cálculo', () => {
    expect(prepararCampo(borrador({ tipo: 'FORMULA', formula: '  ' }))).toEqual({
      ok: false,
      errores: { formula: 'Armá el cálculo con al menos una columna.' },
    });
  });

  it('envía la fórmula sin valor inicial, opciones ni tabla conectada', () => {
    const resultado = prepararCampo(
      borrador({ tipo: 'FORMULA', formula: ` {${ID}} * 2 `, valorInicial: '5' }),
    );

    expect(resultado).toEqual({
      ok: true,
      datos: {
        nombre: 'Producto',
        tipo: 'FORMULA',
        obligatorio: false,
        valorInicial: null,
        opciones: [],
        bloqueRelacionadoId: null,
        formula: `{${ID}} * 2`,
      },
    });
  });

  it('descarta la fórmula de otro tipo', () => {
    const resultado = prepararCampo(borrador({ tipo: 'TEXTO', formula: `{${ID}}` }));

    expect(resultado.ok && resultado.datos.formula).toBeNull();
  });

  it('no admite valor inicial en un cálculo', () => {
    expect(esValorInicialValido('FORMULA', '1', [])).toBe(false);
    expect(describirValorInicial('FORMULA', '1')).toBeNull();
  });
});

describe('describirFormula y piezasDeFormula', () => {
  const ID = '00000000-0000-4000-8000-0000000000c1';

  it('separa columnas, números, operadores y paréntesis', () => {
    expect(piezasDeFormula(`( {${ID}} + 2.5 ) * 3`)).toEqual([
      '(',
      `{${ID}}`,
      '+',
      '2.5',
      ')',
      '*',
      '3',
    ]);
    expect(piezasDeFormula('')).toEqual([]);
  });

  it('la cuenta con nombres, símbolos de pantalla y coma decimal', () => {
    const columnas = [{ id: ID, nombre: 'Cantidad' }];

    expect(describirFormula(`{${ID}} * 2.5 - 1 / 4`, columnas)).toBe('«Cantidad» × 2,5 − 1 ÷ 4');
  });

  it('avisa si una columna ya no existe', () => {
    expect(describirFormula(`{${ID}} + 1`, [])).toBe('«columna eliminada» + 1');
  });
});
