import { describe, expect, it } from 'vitest';
import { makeBlock, makeSystem } from '@/lib/sistema/core';
import { lecturaDeEjemplo } from '@/lib/sistema/lectura';
import {
  aIncorporacion,
  camposDeLasTarjetas,
  camposSinDecidir,
  campoDeColumnaNueva,
  cantidadDeDudas,
  erroresDeLosRegistros,
  errorDeNombre,
  iniciarRevision,
  motivosPendientes,
  opcionesDeLaColumna,
  reducirRevision,
  type AccionDeRevision,
  type Revision,
} from '@/lib/sistema/revision';

const destino = () => makeBlock('inventory');

const columna = (revision: Revision) => {
  const campo = revision.camposNuevos[0];
  if (!campo) throw new Error('Falta la columna nueva');
  return campo;
};

const lectura = () => {
  const bloque = destino();
  return lecturaDeEjemplo({
    tipo: 'inventory',
    campos: bloque.fields,
    vinculos: {},
    destinoExistente: true,
  });
};

const revisar = (...acciones: AccionDeRevision[]): Revision =>
  acciones.reduce(reducirRevision, iniciarRevision(lectura()));

describe('iniciar la revisión', () => {
  it('arma un registro por fila leída, con sus datos dudosos, y deja pendientes las columnas nuevas', () => {
    const revision = iniciarRevision(lectura());

    expect(revision.filas.map((fila) => fila.id)).toEqual([
      'registro-0',
      'registro-1',
      'registro-2',
    ]);
    expect(revision.filas[1]?.dudas).toEqual({ name: 'La letra no se lee con claridad.' });
    expect(revision.filas[2]?.dudas).toEqual({ quantity: 'No estamos seguros de este número.' });
    expect(revision.filas[0]?.dudas).toEqual({});
    expect(cantidadDeDudas(revision)).toBe(2);
    expect(revision.camposNuevos).toEqual([
      { clave: 'proveedor', label: 'Proveedor', tipo: 'text', estado: 'pendiente' },
    ]);
  });

  it('una lectura de un bloque nuevo no trae columnas nuevas', () => {
    const bloque = destino();
    const lecturaNueva = lecturaDeEjemplo({
      tipo: 'inventory',
      campos: bloque.fields,
      vinculos: {},
      destinoExistente: false,
    });

    expect(iniciarRevision(lecturaNueva).camposNuevos).toEqual([]);
  });

  it('no propone una columna que el bloque ya tiene, aunque la escriba distinto', () => {
    const bloque = destino();
    bloque.fields.push({ key: 'otro', label: 'PROVEEDOR ', type: 'text', required: false });
    const conCampo = lecturaDeEjemplo({
      tipo: 'inventory',
      campos: bloque.fields,
      vinculos: {},
      destinoExistente: true,
    });

    expect(conCampo.camposNuevos).toEqual([]);
  });
});

describe('corregir los registros', () => {
  it('corregir un dato dudoso lo resuelve y el registro deja de ser de ejemplo', () => {
    const revision = revisar({
      tipo: 'editar',
      filaId: 'registro-1',
      clave: 'name',
      valor: 'Arroz',
    });

    expect(revision.filas[1]?.valores.name).toBe('Arroz');
    expect(revision.filas[1]?.dudas).toEqual({});
    expect(revision.filas[1]?.editada).toBe(true);
    expect(revision.filas[0]?.editada).toBe(false);
  });

  it('«Está bien» resuelve la duda sin cambiar el dato', () => {
    const antes = iniciarRevision(lectura());
    const revision = reducirRevision(antes, {
      tipo: 'estaBien',
      filaId: 'registro-2',
      clave: 'quantity',
    });

    expect(revision.filas[2]?.dudas).toEqual({});
    expect(revision.filas[2]?.valores).toEqual(antes.filas[2]?.valores);
    expect(revision.filas[2]?.editada).toBe(false);
  });

  it('quitar un registro lo saca y restaurarlo lo devuelve a su lugar', () => {
    const antes = iniciarRevision(lectura());
    const quitado = reducirRevision(antes, { tipo: 'quitarFila', filaId: 'registro-1' });
    const fila = antes.filas[1];
    if (!fila) throw new Error('Falta la fila');

    expect(quitado.filas.map((item) => item.id)).toEqual(['registro-0', 'registro-2']);
    expect(
      reducirRevision(quitado, { tipo: 'restaurarFila', fila, indice: 1 }).filas.map(
        (item) => item.id,
      ),
    ).toEqual(['registro-0', 'registro-1', 'registro-2']);
  });
});

describe('las columnas nuevas', () => {
  it('se aceptan, se descartan y se reabren', () => {
    expect(revisar({ tipo: 'aceptarCampo', clave: 'proveedor' }).camposNuevos[0]?.estado).toBe(
      'aceptado',
    );
    expect(revisar({ tipo: 'descartarCampo', clave: 'proveedor' }).camposNuevos[0]?.estado).toBe(
      'descartado',
    );
    expect(
      revisar(
        { tipo: 'descartarCampo', clave: 'proveedor' },
        { tipo: 'reabrirCampo', clave: 'proveedor' },
      ).camposNuevos[0]?.estado,
    ).toBe('pendiente');
  });

  it('se renombran y cambian de tipo; otras columnas no se tocan', () => {
    const revision = revisar(
      { tipo: 'renombrarCampo', clave: 'proveedor', label: 'Quién lo vende' },
      { tipo: 'cambiarTipoDeCampo', clave: 'proveedor', tipoDeCampo: 'textarea' },
      { tipo: 'renombrarCampo', clave: 'otra', label: 'Nada' },
    );

    expect(revision.camposNuevos[0]).toMatchObject({ label: 'Quién lo vende', tipo: 'textarea' });
    expect(camposSinDecidir(revision)).toHaveLength(1);
  });

  it('una columna de tipo «Opciones» ofrece los valores distintos que se leyeron', () => {
    const revision = revisar({
      tipo: 'cambiarTipoDeCampo',
      clave: 'proveedor',
      tipoDeCampo: 'select',
    });

    expect(opcionesDeLaColumna(revision, 'proveedor')).toEqual([
      'Distribuidora Sur',
      'Mayorista Norte',
    ]);
    const campo = revision.camposNuevos[0];
    if (!campo) throw new Error('Falta la columna');
    expect(campoDeColumnaNueva(campo, revision)).toMatchObject({
      type: 'select',
      options: ['Distribuidora Sur', 'Mayorista Norte'],
      required: false,
    });
  });

  it('las tarjetas muestran las columnas nuevas hasta que se descartan', () => {
    const existentes = destino().fields;
    const pendiente = revisar();
    const aceptada = revisar({ tipo: 'aceptarCampo', clave: 'proveedor' });
    const descartada = revisar({ tipo: 'descartarCampo', clave: 'proveedor' });

    expect(camposDeLasTarjetas(existentes, pendiente).map((campo) => campo.key)).toContain(
      'proveedor',
    );
    expect(camposDeLasTarjetas(existentes, aceptada).map((campo) => campo.key)).toContain(
      'proveedor',
    );
    expect(camposDeLasTarjetas(existentes, descartada).map((campo) => campo.key)).toEqual(
      existentes.map((campo) => campo.key),
    );
  });
});

describe('el nombre de una columna nueva', () => {
  const existentes = destino().fields;

  it('tiene que existir y no repetir un campo del bloque, sin importar tildes ni mayúsculas', () => {
    const vacio = revisar({ tipo: 'renombrarCampo', clave: 'proveedor', label: '  ' });
    const repetido = revisar({ tipo: 'renombrarCampo', clave: 'proveedor', label: 'PRÉCIO' });
    const libre = revisar({ tipo: 'renombrarCampo', clave: 'proveedor', label: 'Marca' });

    expect(errorDeNombre(columna(vacio), vacio, existentes)).toBe('Elegí un nombre para el campo.');
    expect(errorDeNombre(columna(repetido), repetido, existentes)).toBe(
      'Ya hay un campo con ese nombre.',
    );
    expect(errorDeNombre(columna(libre), libre, existentes)).toBeUndefined();
  });

  it('no puede repetir otra columna nueva, salvo que esa esté descartada', () => {
    const base = iniciarRevision({
      filas: [{ a: 'x', b: 'y' }],
      camposNuevos: [
        { key: 'a', label: 'Marca', type: 'text', required: false },
        { key: 'b', label: 'marca', type: 'text', required: false },
      ],
      dudas: [],
    });
    const [primera, segunda] = base.camposNuevos;
    if (!primera || !segunda) throw new Error('Faltan columnas');

    expect(errorDeNombre(segunda, base, [])).toBe('Ya hay un campo con ese nombre.');
    const sinLaPrimera = reducirRevision(base, { tipo: 'descartarCampo', clave: 'a' });
    expect(errorDeNombre(segunda, sinLaPrimera, [])).toBeUndefined();
  });
});

describe('qué falta para incorporar', () => {
  const existentes = destino().fields;

  it('pide decidir las columnas nuevas, dejar algún registro y un nombre válido', () => {
    expect(motivosPendientes(revisar(), existentes)).toEqual([
      'Aceptá o descartá los campos nuevos.',
    ]);
    const sinRegistros = revisar(
      { tipo: 'descartarCampo', clave: 'proveedor' },
      ...['registro-0', 'registro-1', 'registro-2'].map((filaId): AccionDeRevision => ({
        tipo: 'quitarFila',
        filaId,
      })),
    );
    expect(motivosPendientes(sinRegistros, existentes)).toEqual(['Dejá al menos un registro.']);
    const nombreMalo = revisar(
      { tipo: 'aceptarCampo', clave: 'proveedor' },
      { tipo: 'renombrarCampo', clave: 'proveedor', label: 'Precio' },
    );
    expect(motivosPendientes(nombreMalo, existentes)).toEqual([
      'Corregí el nombre de los campos nuevos.',
    ]);
  });

  it('no pide nada cuando todo está decidido', () => {
    expect(
      motivosPendientes(revisar({ tipo: 'aceptarCampo', clave: 'proveedor' }), existentes),
    ).toEqual([]);
    expect(
      motivosPendientes(revisar({ tipo: 'descartarCampo', clave: 'proveedor' }), existentes),
    ).toEqual([]);
  });

  it('un nombre repetido de una columna descartada no molesta', () => {
    const revision = revisar(
      { tipo: 'renombrarCampo', clave: 'proveedor', label: 'Precio' },
      { tipo: 'descartarCampo', clave: 'proveedor' },
    );

    expect(motivosPendientes(revision, existentes)).toEqual([]);
  });
});

describe('los errores de cada registro', () => {
  it('aparecen en el registro y el campo que no cumplen, con los tipos ya elegidos', () => {
    const bloque = destino();
    const revision = revisar(
      { tipo: 'aceptarCampo', clave: 'proveedor' },
      { tipo: 'cambiarTipoDeCampo', clave: 'proveedor', tipoDeCampo: 'number' },
    );

    const errores = erroresDeLosRegistros(bloque, revision, makeSystem('Prueba'));

    expect(errores['registro-0']).toEqual({ proveedor: 'Ingresá un número válido.' });
    expect(errores['registro-1']).toEqual({ proveedor: 'Ingresá un número válido.' });
  });

  it('un registro sin problemas no aparece', () => {
    const errores = erroresDeLosRegistros(destino(), revisar(), makeSystem('Prueba'));

    expect(errores).toEqual({});
  });
});

describe('lo que se incorpora', () => {
  it('suma al final del bloque las columnas aceptadas, con clave propia, y no toca el bloque original', () => {
    const bloque = destino();
    const revision = revisar(
      { tipo: 'renombrarCampo', clave: 'proveedor', label: '  Quién lo vende ' },
      { tipo: 'aceptarCampo', clave: 'proveedor' },
    );

    const { bloque: final, filas } = aIncorporacion(bloque, revision, () => 'clave-nueva');

    expect(bloque.fields).toHaveLength(5);
    expect(final.fields).toHaveLength(6);
    expect(final.fields.at(-1)).toEqual({
      key: 'clave-nueva',
      label: 'Quién lo vende',
      type: 'text',
      required: false,
    });
    expect(filas).toHaveLength(3);
    expect(filas[0]).toMatchObject({
      name: 'Yerba Playadito 1 kg',
      'clave-nueva': 'Distribuidora Sur',
    });
    expect(filas[0]).not.toHaveProperty('proveedor');
  });

  it('una columna descartada no se crea y sus datos se ignoran', () => {
    const bloque = destino();

    const { bloque: final, filas } = aIncorporacion(
      bloque,
      revisar({ tipo: 'descartarCampo', clave: 'proveedor' }),
    );

    expect(final.fields).toHaveLength(5);
    for (const fila of filas)
      expect(Object.keys(fila)).toEqual(bloque.fields.map((campo) => campo.key));
  });

  it('sólo van los registros que quedaron y los corregidos dejan de ser de ejemplo', () => {
    const revision = revisar(
      { tipo: 'descartarCampo', clave: 'proveedor' },
      { tipo: 'quitarFila', filaId: 'registro-0' },
      { tipo: 'editar', filaId: 'registro-1', clave: 'name', valor: 'Arroz' },
    );

    const { filas } = aIncorporacion(destino(), revision);

    expect(filas).toHaveLength(2);
    expect(filas[0]).toMatchObject({ name: 'Arroz', __example: false });
    expect(filas[1]).not.toHaveProperty('__example');
  });

  it('con una clave por defecto cada columna nueva recibe una clave distinta', () => {
    const revision = iniciarRevision({
      filas: [{ a: '1', b: '2' }],
      camposNuevos: [
        { key: 'a', label: 'Uno', type: 'text', required: false },
        { key: 'b', label: 'Dos', type: 'text', required: false },
      ],
      dudas: [],
    });
    const aceptadas = reducirRevision(
      reducirRevision(revision, { tipo: 'aceptarCampo', clave: 'a' }),
      {
        tipo: 'aceptarCampo',
        clave: 'b',
      },
    );

    const { bloque } = aIncorporacion(makeBlock('note'), aceptadas);

    const claves = bloque.fields.map((campo) => campo.key);
    expect(new Set(claves).size).toBe(claves.length);
    expect(claves.every((clave) => clave.length > 3)).toBe(true);
  });
});
