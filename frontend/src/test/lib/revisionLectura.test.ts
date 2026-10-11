import { beforeEach, describe, expect, it } from 'vitest';
import {
  aConfirmacion,
  borrarBorrador,
  errorDeCelda,
  errorDeColumna,
  estaBloqueada,
  guardarBorrador,
  iniciarBorrador,
  leerBorradorGuardado,
  puedeGuardar,
  reducirBorrador,
  type BorradorRevision,
  type ColumnaRevision,
} from '@/lib/revisionLectura';
import type { PropuestaLectura } from '@/types/lectura';

const CAMPO_EXISTENTE = '11111111-1111-4111-8111-111111111111';

const propuesta: PropuestaLectura = {
  nombre: 'Ventas',
  bloqueExistenteId: null,
  campos: [
    { clave: 'producto', nombre: 'Producto', tipo: 'TEXTO' },
    { clave: 'monto', nombre: 'Monto', tipo: 'NUMERO' },
    { clave: 'pagado', nombre: 'Pagado', tipo: 'SI_NO' },
  ],
  filas: [
    {
      pagina: 1,
      renglon: 1,
      datos: {
        producto: { valor: 'Pan', confianza: 0.98 },
        monto: { valor: '1200', confianza: 0.95 },
        pagado: { valor: 'true', confianza: 0.9 },
      },
    },
    {
      pagina: 2,
      renglon: 1,
      datos: {
        producto: { valor: 'Leche', confianza: 0.9 },
        monto: { valor: '850', confianza: 0.4 },
      },
    },
  ],
  mapeoCampos: { monto: CAMPO_EXISTENTE },
};

function en<T>(items: readonly T[], indice = 0): T {
  const item = items[indice];
  if (item === undefined) throw new Error(`Falta el elemento de prueba ${indice}`);
  return item;
}

let contador = 0;
const nuevoId = () => `fila-${++contador}`;
const borradorInicial = () => iniciarBorrador(propuesta, nuevoId);

const columna = (cambios: Partial<ColumnaRevision> = {}): ColumnaRevision => ({
  clave: 'monto',
  nombre: 'Monto',
  tipo: 'NUMERO',
  opciones: [],
  campoExistenteId: null,
  ...cambios,
});

const celda = (texto: string, confianza: number | null = null, revisada = false) => ({
  texto,
  confianza,
  revisada,
});

describe('iniciarBorrador', () => {
  beforeEach(() => {
    contador = 0;
  });

  it('arma columnas y filas con lo que leyó la IA', () => {
    const borrador = borradorInicial();

    expect(borrador.nombreBloque).toBe('Ventas');
    expect(borrador.columnas.map((c) => c.nombre)).toEqual(['Producto', 'Monto', 'Pagado']);
    expect(borrador.columnas[1]?.campoExistenteId).toBe(CAMPO_EXISTENTE);
    expect(borrador.filas.map((f) => f.id)).toEqual(['fila-1', 'fila-2']);
    expect(borrador.filas[0]?.celdas.monto).toEqual({
      texto: '1200',
      confianza: 0.95,
      revisada: false,
    });
  });

  it('un dato que la IA no leyó queda vacío y con confianza cero', () => {
    const borrador = borradorInicial();

    expect(borrador.filas[1]?.celdas.pagado).toEqual({ texto: '', confianza: 0, revisada: false });
  });

  it('sin nombre propuesto usa uno genérico', () => {
    const borrador = iniciarBorrador({ ...propuesta, nombre: null, mapeoCampos: null }, nuevoId);

    expect(borrador.nombreBloque).toBe('Datos del cuaderno');
    expect(borrador.columnas[1]?.campoExistenteId).toBeNull();
  });
});



describe('errorDeCelda', () => {
  it.each([
    ['NUMERO', '12,5', undefined],
    ['NUMERO', 'doce', 'Escribí un número, por ejemplo 12 o 12,5.'],
    ['FECHA', '2026-10-03', undefined],
    ['FECHA', '31/12/2026', 'Elegí una fecha válida.'],
    ['FECHA', '2026-02-31', 'Elegí una fecha válida.'],
    ['SI_NO', 'false', undefined],
    ['SI_NO', 'quizás', 'Elegí Sí o No.'],
    ['TEXTO', 'cualquier cosa', undefined],
  ] as const)('%s con «%s»', (tipo, texto, esperado) => {
    expect(errorDeCelda(columna({ tipo }), celda(texto))).toBe(esperado);
  });

  it('una celda vacía nunca es un error', () => {
    expect(errorDeCelda(columna(), celda('  '))).toBeUndefined();
  });

  it('una opción tiene que estar en la lista', () => {
    const lista = columna({ tipo: 'OPCION', opciones: ['Chico', ' Grande ', ''] });

    expect(errorDeCelda(lista, celda('Grande'))).toBeUndefined();
    expect(errorDeCelda(lista, celda('Mediano'))).toBe('Elegí una de las opciones.');
  });

  it('un texto no puede pasar de 1000 caracteres', () => {
    expect(errorDeCelda(columna({ tipo: 'TEXTO' }), celda('a'.repeat(1001)))).toBe(
      'Usá hasta 1000 caracteres.',
    );
  });
});

describe('errorDeColumna', () => {
  const todas = [columna({ clave: 'a', nombre: 'Monto' }), columna({ clave: 'b', nombre: 'Rubro' })];

  it('acepta una columna correcta', () => {
    expect(errorDeColumna(en(todas), todas)).toBeUndefined();
  });

  it('exige un nombre', () => {
    expect(errorDeColumna(columna({ nombre: '  ' }), todas)).toBe('Escribí un nombre para la columna.');
  });

  it('limita el largo del nombre', () => {
    expect(errorDeColumna(columna({ nombre: 'a'.repeat(121) }), todas)).toBe(
      'Usá hasta 120 caracteres.',
    );
  });

  it('no admite dos columnas con el mismo nombre', () => {
    const repetida = columna({ clave: 'c', nombre: ' monto ' });

    expect(errorDeColumna(repetida, [...todas, repetida])).toBe('Ya hay otra columna con este nombre.');
  });

  it('una lista necesita opciones y sin repetir', () => {
    const lista = (opciones: string[]) => columna({ clave: 'z', nombre: 'Lista', tipo: 'OPCION', opciones });

    expect(errorDeColumna(lista(['', ' ']), todas)).toBe('Agregá al menos una opción.');
    expect(errorDeColumna(lista(['A', 'a']), todas)).toBe('La opción «a» está repetida.');
    expect(errorDeColumna(lista(['A', 'B']), todas)).toBeUndefined();
  });
});

describe('reducirBorrador', () => {
  const base = () => {
    contador = 0;
    return borradorInicial();
  };

  it('quita una fila', () => {
    const borrador = reducirBorrador(base(), { tipo: 'quitarFila', filaId: 'fila-1' });

    expect(borrador.filas.map((f) => f.id)).toEqual(['fila-2']);
  });

  it('agrega una fila a mano, vacía y sin dudas', () => {
    const borrador = reducirBorrador(base(), { tipo: 'agregarFila', id: 'nueva' });
    const fila = borrador.filas.at(-1);

    expect(fila?.pagina).toBeNull();
    expect(Object.keys(fila?.celdas ?? {})).toEqual(['producto', 'monto', 'pagado']);
  });

  it('renombra la tabla y edita una columna', () => {
    let borrador = reducirBorrador(base(), { tipo: 'renombrarBloque', nombre: 'Mis ventas' });
    borrador = reducirBorrador(borrador, {
      tipo: 'editarColumna',
      clave: 'monto',
      cambios: { nombre: 'Importe', tipo: 'TEXTO' },
    });

    expect(borrador.nombreBloque).toBe('Mis ventas');
    expect(borrador.columnas[1]).toMatchObject({ nombre: 'Importe', tipo: 'TEXTO' });
  });

  it('cambiar el tipo conserva los valores y los marca inválidos si no encajan', () => {
    const borrador = reducirBorrador(base(), {
      tipo: 'editarColumna',
      clave: 'producto',
      cambios: { tipo: 'NUMERO' },
    });

    expect(borrador.filas[0]?.celdas.producto?.texto).toBe('Pan');
    expect(puedeGuardar(borrador)).toBe(false);
  });

  it('quita una columna con sus celdas', () => {
    const borrador = reducirBorrador(base(), { tipo: 'quitarColumna', clave: 'monto' });

    expect(borrador.columnas.map((c) => c.clave)).toEqual(['producto', 'pagado']);
    expect(Object.keys(borrador.filas[0]?.celdas ?? {})).toEqual(['producto', 'pagado']);
  });

  it('agrega una columna de texto con un nombre libre y celdas vacías', () => {
    let borrador = reducirBorrador(base(), { tipo: 'agregarColumna' });
    borrador = reducirBorrador(borrador, { tipo: 'agregarColumna' });

    expect(borrador.columnas.slice(3).map((c) => c.nombre)).toEqual(['Columna 4', 'Columna 5']);
    expect(borrador.columnas[3]).toMatchObject({ tipo: 'TEXTO', campoExistenteId: null });
    expect(borrador.filas[0]?.celdas.columna_4).toEqual(celda('', null, true));
    expect(puedeGuardar(borrador)).toBe(true);
  });

  it('una columna nueva no choca con un nombre que ya usa otra', () => {
    let borrador = reducirBorrador(base(), {
      tipo: 'editarColumna',
      clave: 'producto',
      cambios: { nombre: 'Columna 4' },
    });
    borrador = reducirBorrador(borrador, { tipo: 'agregarColumna' });

    expect(borrador.columnas.at(-1)?.nombre).toBe('Columna 5');
  });

  it('ignora una celda que no existe', () => {
    const inicial = base();

    expect(reducirBorrador(inicial, { tipo: 'marcarRevisada', filaId: 'x', clave: 'monto' })).toEqual(
      inicial,
    );
  });
});

describe('puedeGuardar', () => {
  it('con todo en orden se puede guardar', () => {
    expect(puedeGuardar(borradorInicial())).toBe(true);
  });

  it('no se puede sin nombre de tabla ni sin columnas', () => {
    const sinNombre = reducirBorrador(borradorInicial(), { tipo: 'renombrarBloque', nombre: ' ' });
    let sinColumnas = borradorInicial();
    for (const clave of ['producto', 'monto', 'pagado']) {
      sinColumnas = reducirBorrador(sinColumnas, { tipo: 'quitarColumna', clave });
    }

    expect(puedeGuardar(sinNombre)).toBe(false);
    expect(puedeGuardar(sinColumnas)).toBe(false);
  });

  it('no se puede con una columna inválida', () => {
    const borrador = reducirBorrador(borradorInicial(), {
      tipo: 'editarColumna',
      clave: 'monto',
      cambios: { nombre: '' },
    });

    expect(puedeGuardar(borrador)).toBe(false);
  });
});

describe('aConfirmacion', () => {
  const revisado = (): BorradorRevision => {
    contador = 0;
    let borrador = borradorInicial();
    borrador = reducirBorrador(borrador, { tipo: 'editarCelda', filaId: 'fila-2', clave: 'monto', texto: '850,5' });
    borrador = reducirBorrador(borrador, { tipo: 'agregarFila', id: 'manual' });
    return borrador;
  };

  it('tipa los valores: números, sí/no y texto; los vacíos no viajan', () => {
    const datos = aConfirmacion(revisado(), { modo: 'nuevo', nombreProyecto: ' Mi negocio ' });

    expect(datos.nombreProyecto).toBe('Mi negocio');
    expect(datos.nombreBloque).toBe('Ventas');
    expect(datos.proyectoId).toBeNull();
    expect(datos.bloqueId).toBeNull();
    expect(datos.filas[0]).toEqual({
      pagina: 1,
      valores: { producto: 'Pan', monto: 1200, pagado: true },
    });
    expect(datos.filas[1]).toEqual({ pagina: 2, valores: { producto: 'Leche', monto: 850.5 } });
    expect(datos.filas[2]).toEqual({ pagina: null, valores: {} });
  });

  it('en un proyecto existente manda su id y el nombre de la tabla', () => {
    const datos = aConfirmacion(revisado(), { modo: 'proyecto', proyectoId: 'p-1' });

    expect(datos).toMatchObject({ proyectoId: 'p-1', nombreProyecto: null, nombreBloque: 'Ventas' });
    expect(datos.campos.every((campo) => campo.campoId === null)).toBe(true);
  });

  it('al sumar a una tabla existente manda su id y los campos que ya tiene', () => {
    const datos = aConfirmacion(revisado(), { modo: 'bloque', bloqueId: 'b-1' });

    expect(datos).toMatchObject({ bloqueId: 'b-1', nombreBloque: null, proyectoId: null });
    expect(datos.campos.map((campo) => campo.campoId)).toEqual([null, CAMPO_EXISTENTE, null]);
  });

  it('manda las opciones limpias sólo en las listas', () => {
    const borrador = reducirBorrador(borradorInicial(), {
      tipo: 'editarColumna',
      clave: 'producto',
      cambios: { tipo: 'OPCION', opciones: [' Pan ', '', 'Leche'] },
    });

    const campos = aConfirmacion(borrador, { modo: 'nuevo', nombreProyecto: 'X' }).campos;

    expect(campos[0]).toMatchObject({ tipo: 'OPCION', opciones: ['Pan', 'Leche'] });
    expect(campos[1]?.opciones).toEqual([]);
  });

  it('con destino en una tabla existente sus columnas mapeadas quedan bloqueadas', () => {
    const borrador = borradorInicial();
    const destino = { modo: 'bloque', bloqueId: 'b-1' } as const;

    expect(estaBloqueada(en(borrador.columnas, 1), destino)).toBe(true);
    expect(estaBloqueada(en(borrador.columnas), destino)).toBe(false);
    expect(estaBloqueada(en(borrador.columnas, 1), { modo: 'nuevo', nombreProyecto: '' })).toBe(false);
  });
});

describe('persistencia del borrador', () => {
  beforeEach(() => sessionStorage.clear());

  it('guarda, lee y borra el borrador de un lote', () => {
    const guardado = { borrador: borradorInicial(), destino: { modo: 'nuevo', nombreProyecto: 'X' } } as const;

    guardarBorrador('lote-1', guardado);
    expect(leerBorradorGuardado('lote-1')).toEqual(guardado);
    expect(leerBorradorGuardado('lote-2')).toBeUndefined();

    borrarBorrador('lote-1');
    expect(leerBorradorGuardado('lote-1')).toBeUndefined();
  });

  it('ignora lo guardado que no tiene la forma esperada', () => {
    sessionStorage.setItem('revision-lectura:lote-1', '{"borrador":{"columnas":1},"destino":{}}');
    sessionStorage.setItem('revision-lectura:lote-2', 'no es json');

    expect(leerBorradorGuardado('lote-1')).toBeUndefined();
    expect(leerBorradorGuardado('lote-2')).toBeUndefined();
  });

  it('funciona aunque el navegador no deje usar el almacenamiento', () => {
    const original = Storage.prototype.getItem;
    Storage.prototype.getItem = () => {
      throw new Error('bloqueado');
    };
    const guardar = Storage.prototype.setItem;
    Storage.prototype.setItem = () => {
      throw new Error('bloqueado');
    };
    const quitar = Storage.prototype.removeItem;
    Storage.prototype.removeItem = () => {
      throw new Error('bloqueado');
    };

    try {
      expect(leerBorradorGuardado('lote-1')).toBeUndefined();
      expect(() =>
        guardarBorrador('lote-1', { borrador: borradorInicial(), destino: { modo: 'proyecto', proyectoId: 'p' } }),
      ).not.toThrow();
      expect(() => borrarBorrador('lote-1')).not.toThrow();
    } finally {
      Storage.prototype.getItem = original;
      Storage.prototype.setItem = guardar;
      Storage.prototype.removeItem = quitar;
    }
  });
});
