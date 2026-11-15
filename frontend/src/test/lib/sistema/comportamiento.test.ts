import { describe, expect, it } from 'vitest';
import { comportamientoDe } from '@/lib/sistema/comportamiento';
import { makeSystem } from '@/lib/sistema/core';
import type { Campo, TipoCampo, Valor } from '@/lib/sistema/tipos';

type CasoDeTipo = {
  tipo: TipoCampo;
  valorValido: Valor;
  valorInvalido: Valor;
  mensajeDeError: string;
  coercion: [Valor, Valor];
  textoPlano: [Valor, string];
  interpretacion: [unknown, Valor];
  noInterpretable: unknown;
};

const campoDe = (tipo: TipoCampo): Campo => ({
  key: 'campo',
  label: 'Campo',
  type: tipo,
  required: false,
  options: ['Chica', 'Grande'],
});

const sistema = makeSystem('Sistema de prueba', [], false);

const TIPOS_CON_VALIDACION: CasoDeTipo[] = [
  {
    tipo: 'number',
    valorValido: 12,
    valorInvalido: 'abc',
    mensajeDeError: 'Ingresá un número válido.',
    coercion: ['12', 12],
    textoPlano: [12, '12'],
    interpretacion: ['3,5', 3.5],
    noInterpretable: 'abc',
  },
  {
    tipo: 'currency',
    valorValido: '1500',
    valorInvalido: 'mucho',
    mensajeDeError: 'Ingresá un número válido.',
    coercion: ['1500', 1500],
    textoPlano: [1500, '1500'],
    interpretacion: [900, 900],
    noInterpretable: 'mucho',
  },
  {
    tipo: 'date',
    valorValido: '2026-10-16',
    valorInvalido: '16/10/2026',
    mensajeDeError: 'Elegí una fecha válida.',
    coercion: ['2026-10-16', '2026-10-16'],
    textoPlano: ['2026-10-16', '2026-10-16'],
    interpretacion: ['16/10/2026', '2026-10-16'],
    noInterpretable: '31/02/2026',
  },
  {
    tipo: 'time',
    valorValido: '09:30',
    valorInvalido: '25:00',
    mensajeDeError: 'Elegí un horario válido.',
    coercion: ['09:30', '09:30'],
    textoPlano: ['09:30', '09:30'],
    interpretacion: ['9:30', '09:30'],
    noInterpretable: '25:00',
  },
  {
    tipo: 'email',
    valorValido: 'ana@correo.com',
    valorInvalido: 'ana@correo',
    mensajeDeError: 'Revisá el correo.',
    coercion: ['ana@correo.com', 'ana@correo.com'],
    textoPlano: ['ana@correo.com', 'ana@correo.com'],
    interpretacion: ['ana@correo.com', 'ana@correo.com'],
    noInterpretable: '',
  },
  {
    tipo: 'select',
    valorValido: 'Chica',
    valorInvalido: 'Mediana',
    mensajeDeError: 'Elegí una de las opciones.',
    coercion: ['Chica', 'Chica'],
    textoPlano: ['Chica', 'Chica'],
    interpretacion: ['grande', 'Grande'],
    noInterpretable: 'Mediana',
  },
];

describe.each(TIPOS_CON_VALIDACION)(
  'el campo $tipo',
  ({
    tipo,
    valorValido,
    valorInvalido,
    mensajeDeError,
    coercion,
    textoPlano,
    interpretacion,
    noInterpretable,
  }) => {
    const campo = campoDe(tipo);
    const comportamiento = comportamientoDe(tipo);

    it('acepta un valor válido', () => {
      expect(comportamiento.validar(campo, valorValido, sistema)).toBeUndefined();
    });

    it('rechaza un valor inválido con su mensaje', () => {
      expect(comportamiento.validar(campo, valorInvalido, sistema)).toBe(mensajeDeError);
    });

    it('guarda el valor con su tipo', () => {
      expect(comportamiento.coercionar(coercion[0])).toBe(coercion[1]);
    });

    it('se muestra como texto', () => {
      expect(comportamiento.textoPlano(campo, textoPlano[0], sistema)).toBe(textoPlano[1]);
    });

    it('interpreta lo que se anota a mano', () => {
      expect(comportamiento.interpretar(campo, interpretacion[0])).toBe(interpretacion[1]);
    });

    it('descarta lo que no se puede interpretar', () => {
      expect(comportamiento.interpretar(campo, noInterpretable)).toBeUndefined();
    });
  },
);

describe.each<TipoCampo>(['text', 'textarea', 'tel'])('el campo de texto %s', (tipo) => {
  const campo = campoDe(tipo);
  const comportamiento = comportamientoDe(tipo);

  it('acepta cualquier texto', () => {
    expect(comportamiento.validar(campo, 'lo que sea', sistema)).toBeUndefined();
  });

  it('guarda vacío lo que no tiene valor', () => {
    expect(comportamiento.coercionar(undefined)).toBe('');
  });

  it('recorta el texto largo', () => {
    expect(String(comportamiento.interpretar(campo, 'a'.repeat(300))).length).toBe(200);
  });

  it('propone un registro numerado si es obligatorio', () => {
    const obligatorio = { ...campo, required: true };
    expect(comportamiento.valorDeEjemplo(obligatorio, 1, undefined, [])).toBe('Registro 2');
  });
});

describe('el campo checkbox', () => {
  const campo = campoDe('checkbox');
  const comportamiento = comportamientoDe('checkbox');

  it('falta si no está tildado', () => {
    expect(comportamiento.falta(false)).toBe(true);
  });

  it('se muestra como Sí o No', () => {
    expect(comportamiento.textoPlano(campo, true, sistema)).toBe('Sí');
  });

  it('interpreta «x» como tildado', () => {
    expect(comportamiento.interpretar(campo, 'x')).toBe(true);
  });
});

describe('el campo relation', () => {
  const campo = campoDe('relation');
  const comportamiento = comportamientoDe('relation');

  it('rechaza un registro que no existe en el bloque vinculado', () => {
    expect(comportamiento.validar(campo, 'inexistente', sistema)).toBe(
      'Elegí un registro del bloque vinculado.',
    );
  });

  it('reparte los vínculos disponibles entre los registros de ejemplo', () => {
    expect(comportamiento.valorDeEjemplo(campo, 3, undefined, ['a', 'b'])).toBe('b');
  });
});

describe('el mínimo de un campo', () => {
  it('rechaza un número por debajo', () => {
    const campo = { ...campoDe('number'), min: 5 };
    expect(comportamientoDe('number').validar(campo, 2, sistema)).toBe('El mínimo es 5.');
  });
});
