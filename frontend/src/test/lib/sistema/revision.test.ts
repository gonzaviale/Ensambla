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
import type { Bloque } from '@/lib/sistema/tipos';

const FILAS = ['registro-0', 'registro-1', 'registro-2'];
const OPCIONES_DE_PROVEEDOR = ['Distribuidora Sur', 'Mayorista Norte'];

const destino = () => makeBlock('inventory');

const lecturaDe = (destinoExistente: boolean, bloque = destino()) =>
  lecturaDeEjemplo({
    tipo: 'inventory',
    campos: bloque.fields,
    vinculos: {},
    destinoExistente,
  });

const lectura = () => lecturaDe(true);

const dosColumnasNuevas = (marca2 = 'marca') =>
  iniciarRevision({
    filas: [{ a: 'x', b: 'y' }],
    camposNuevos: [
      { key: 'a', label: 'Marca', type: 'text', required: false },
      { key: 'b', label: marca2, type: 'text', required: false },
    ],
    dudas: [],
  });

const elemento = <T>(lista: readonly T[], indice: number): T => {
  const encontrado = lista[indice];
  if (encontrado === undefined) throw new Error(`Falta el elemento ${indice}`);
  return encontrado;
};

const quitarTodas = (): AccionDeRevision[] =>
  FILAS.map((filaId): AccionDeRevision => ({ tipo: 'quitarFila', filaId }));

let revision: Revision;
let revisionOriginal: Revision;
let incorporacion: ReturnType<typeof aIncorporacion>;
let bloqueOriginal: Bloque;
let resultado: unknown;

describe('iniciar la revisión', () => {
  it('arma un registro por fila leída', () => {
    cuandoSeIniciaLaRevision();

    entoncesLasFilasSon(FILAS);
  });

  it('cada registro trae sus datos dudosos', () => {
    cuandoSeIniciaLaRevision();

    entoncesLasDudasPorRegistroSon([
      {},
      { name: 'La letra no se lee con claridad.' },
      { quantity: 'No estamos seguros de este número.' },
    ]);
    entoncesHayDudas(2);
  });

  it('deja pendientes las columnas nuevas', () => {
    cuandoSeIniciaLaRevision();

    entoncesLasColumnasNuevasSon([
      { clave: 'proveedor', label: 'Proveedor', tipo: 'text', estado: 'pendiente' },
    ]);
  });

  it('una lectura de un bloque nuevo no trae columnas nuevas', () => {
    cuandoSeCalculan(() => iniciarRevision(lecturaDe(false)).camposNuevos);

    entoncesElResultadoEs([]);
  });

  it('no propone una columna que el bloque ya tiene, aunque la escriba distinto', () => {
    const bloque = destino();
    bloque.fields.push({ key: 'otro', label: 'PROVEEDOR ', type: 'text', required: false });

    cuandoSeCalculan(() => lecturaDe(true, bloque).camposNuevos);

    entoncesElResultadoEs([]);
  });
});

describe('corregir los registros', () => {
  it('corregir un dato dudoso cambia el dato', () => {
    cuandoSeAplican({ tipo: 'editar', filaId: 'registro-1', clave: 'name', valor: 'Arroz' });

    entoncesElRegistroTieneElNombre('registro-1', 'Arroz');
  });

  it('corregir un dato dudoso resuelve la duda', () => {
    cuandoSeAplican({ tipo: 'editar', filaId: 'registro-1', clave: 'name', valor: 'Arroz' });

    entoncesElRegistroNoTieneDudas(1);
  });

  it('corregir un dato deja de ser un registro de ejemplo y no toca a los otros', () => {
    cuandoSeAplican({ tipo: 'editar', filaId: 'registro-1', clave: 'name', valor: 'Arroz' });

    entoncesSoloElRegistroQuedaEditado(1);
  });

  it('«Está bien» resuelve la duda sin cambiar el dato', () => {
    cuandoSeMarcaQueEstaBien('registro-2', 'quantity');

    entoncesLaDudaSeResolvioSinCambiarElDato(2);
  });

  it('quitar un registro lo saca', () => {
    cuandoSeAplican({ tipo: 'quitarFila', filaId: 'registro-1' });

    entoncesLasFilasSon(['registro-0', 'registro-2']);
  });

  it('restaurar un registro lo devuelve a su lugar', () => {
    cuandoSeQuitaYSeRestauraElRegistro(1);

    entoncesLasFilasSon(FILAS);
  });
});

describe('las columnas nuevas', () => {
  it.each([
    ['se aceptan', [{ tipo: 'aceptarCampo', clave: 'proveedor' }], 'aceptado'],
    ['se descartan', [{ tipo: 'descartarCampo', clave: 'proveedor' }], 'descartado'],
    [
      'se reabren',
      [
        { tipo: 'descartarCampo', clave: 'proveedor' },
        { tipo: 'reabrirCampo', clave: 'proveedor' },
      ],
      'pendiente',
    ],
  ] as [string, AccionDeRevision[], string][])('%s', (_nombre, acciones, estado) => {
    cuandoSeAplican(...acciones);

    entoncesLaPrimeraColumnaEsta(estado);
  });

  it('se renombran y cambian de tipo', () => {
    cuandoSeAplican(
      { tipo: 'renombrarCampo', clave: 'proveedor', label: 'Quién lo vende' },
      { tipo: 'cambiarTipoDeCampo', clave: 'proveedor', tipoDeCampo: 'textarea' },
    );

    entoncesLaPrimeraColumnaCoincideCon({ label: 'Quién lo vende', tipo: 'textarea' });
  });

  it('renombrar una columna que no existe no toca las otras', () => {
    cuandoSeAplican({ tipo: 'renombrarCampo', clave: 'otra', label: 'Nada' });

    entoncesQuedaUnaColumnaSinDecidir();
  });

  it('una columna de tipo «Opciones» ofrece los valores distintos que se leyeron', () => {
    cuandoSeAplican({ tipo: 'cambiarTipoDeCampo', clave: 'proveedor', tipoDeCampo: 'select' });

    entoncesLaColumnaOfreceLasOpciones(OPCIONES_DE_PROVEEDOR);
  });

  it('el campo que se crearía para una columna de opciones las incluye', () => {
    cuandoSeAplican({ tipo: 'cambiarTipoDeCampo', clave: 'proveedor', tipoDeCampo: 'select' });

    entoncesElCampoNuevoEsDeOpciones(OPCIONES_DE_PROVEEDOR);
  });

  it.each([
    ['pendiente', [], true],
    ['aceptada', [{ tipo: 'aceptarCampo', clave: 'proveedor' }], true],
    ['descartada', [{ tipo: 'descartarCampo', clave: 'proveedor' }], false],
  ] as [string, AccionDeRevision[], boolean][])(
    'una columna %s: las tarjetas la muestran = %s',
    (_estado, acciones, seMuestra) => {
      cuandoSeAplican(...acciones);

      entoncesLasTarjetasMuestranLaColumna(seMuestra);
    },
  );
});

describe('el nombre de una columna nueva', () => {
  const existentes = destino().fields;

  it('tiene que existir', () => {
    cuandoSeRenombraLaColumna('  ');

    entoncesElErrorDeNombreEs(existentes, 'Elegí un nombre para el campo.');
  });

  it('no puede repetir un campo del bloque, sin importar tildes ni mayúsculas', () => {
    cuandoSeRenombraLaColumna('PRÉCIO');

    entoncesElErrorDeNombreEs(existentes, 'Ya hay un campo con ese nombre.');
  });

  it('un nombre libre no tiene error', () => {
    cuandoSeRenombraLaColumna('Marca');

    entoncesElErrorDeNombreEs(existentes, undefined);
  });

  it('no puede repetir otra columna nueva', () => {
    cuandoSeCalculan(() => {
      const base = dosColumnasNuevas();
      return errorDeNombre(elemento(base.camposNuevos, 1), base, []);
    });

    entoncesElResultadoEs('Ya hay un campo con ese nombre.');
  });

  it('puede repetir una columna nueva que está descartada', () => {
    cuandoSeCalculan(() => {
      const base = dosColumnasNuevas();
      const sinLaPrimera = reducirRevision(base, { tipo: 'descartarCampo', clave: 'a' });
      return errorDeNombre(elemento(base.camposNuevos, 1), sinLaPrimera, []);
    });

    entoncesElResultadoEs(undefined);
  });
});

describe('qué falta para incorporar', () => {
  const existentes = destino().fields;

  it('pide decidir las columnas nuevas', () => {
    cuandoSeAplican();

    entoncesFaltaPara(existentes, ['Aceptá o descartá los campos nuevos.']);
  });

  it('pide dejar algún registro', () => {
    cuandoSeAplican({ tipo: 'descartarCampo', clave: 'proveedor' }, ...quitarTodas());

    entoncesFaltaPara(existentes, ['Dejá al menos un registro.']);
  });

  it('pide un nombre válido para las columnas aceptadas', () => {
    cuandoSeAplican(
      { tipo: 'aceptarCampo', clave: 'proveedor' },
      { tipo: 'renombrarCampo', clave: 'proveedor', label: 'Precio' },
    );

    entoncesFaltaPara(existentes, ['Corregí el nombre de los campos nuevos.']);
  });

  it.each([
    ['aceptada', 'aceptarCampo'],
    ['descartada', 'descartarCampo'],
  ] as const)('no pide nada cuando la columna está %s', (_estado, tipo) => {
    cuandoSeAplican({ tipo, clave: 'proveedor' });

    entoncesFaltaPara(existentes, []);
  });

  it('un nombre repetido de una columna descartada no molesta', () => {
    cuandoSeAplican(
      { tipo: 'renombrarCampo', clave: 'proveedor', label: 'Precio' },
      { tipo: 'descartarCampo', clave: 'proveedor' },
    );

    entoncesFaltaPara(existentes, []);
  });
});

describe('los errores de cada registro', () => {
  it('aparecen en el registro y el campo que no cumplen, con los tipos ya elegidos', () => {
    cuandoSeAplican(
      { tipo: 'aceptarCampo', clave: 'proveedor' },
      { tipo: 'cambiarTipoDeCampo', clave: 'proveedor', tipoDeCampo: 'number' },
    );

    entoncesLosErroresIncluyen({
      'registro-0': { proveedor: 'Escribí un número válido.' },
      'registro-1': { proveedor: 'Escribí un número válido.' },
    });
  });

  it('un registro sin problemas no aparece', () => {
    cuandoSeAplican();

    entoncesLosErroresSon({});
  });
});

describe('lo que se incorpora', () => {
  it('suma al final del bloque las columnas aceptadas, con clave propia', () => {
    cuandoSeIncorporaConLaColumnaRenombradaYAceptada();

    entoncesElBloqueFinalTieneLaColumnaNueva();
  });

  it('no toca el bloque original', () => {
    cuandoSeIncorporaConLaColumnaRenombradaYAceptada();

    entoncesElBloqueOriginalSigueConCincoCampos();
  });

  it('los registros llevan los datos de la columna aceptada con su clave nueva', () => {
    cuandoSeIncorporaConLaColumnaRenombradaYAceptada();

    entoncesLosRegistrosUsanLaClaveNueva();
  });

  it('una columna descartada no se crea y sus datos se ignoran', () => {
    cuandoSeIncorpora(destino(), [{ tipo: 'descartarCampo', clave: 'proveedor' }]);

    entoncesNoSeCreaLaColumnaNiSusDatos();
  });

  it('sólo van los registros que quedaron', () => {
    cuandoSeIncorpora(destino(), [
      { tipo: 'descartarCampo', clave: 'proveedor' },
      { tipo: 'quitarFila', filaId: 'registro-0' },
      { tipo: 'editar', filaId: 'registro-1', clave: 'name', valor: 'Arroz' },
    ]);

    entoncesVanDosRegistros();
  });

  it('los registros corregidos dejan de ser de ejemplo', () => {
    cuandoSeIncorpora(destino(), [
      { tipo: 'descartarCampo', clave: 'proveedor' },
      { tipo: 'quitarFila', filaId: 'registro-0' },
      { tipo: 'editar', filaId: 'registro-1', clave: 'name', valor: 'Arroz' },
    ]);

    entoncesSoloElRegistroCorregidoDejaDeSerDeEjemplo();
  });

  it('con una clave por defecto cada columna nueva recibe una clave distinta', () => {
    cuandoSeIncorporanDosColumnasConLaClavePorDefecto();

    entoncesLasClavesSonDistintas();
  });
});

// --- cuando ---

function cuandoSeIniciaLaRevision() {
  revision = iniciarRevision(lectura());
}

function cuandoSeAplican(...acciones: AccionDeRevision[]) {
  revision = acciones.reduce(reducirRevision, iniciarRevision(lectura()));
}

function cuandoSeCalculan(calculo: () => unknown) {
  resultado = calculo();
}

function cuandoSeMarcaQueEstaBien(filaId: string, clave: string) {
  revisionOriginal = iniciarRevision(lectura());
  revision = reducirRevision(revisionOriginal, { tipo: 'estaBien', filaId, clave });
}

function cuandoSeQuitaYSeRestauraElRegistro(indice: number) {
  const antes = iniciarRevision(lectura());
  const fila = elemento(antes.filas, indice);
  const quitado = reducirRevision(antes, { tipo: 'quitarFila', filaId: fila.id });
  revision = reducirRevision(quitado, { tipo: 'restaurarFila', fila, indice });
}

function cuandoSeRenombraLaColumna(label: string) {
  cuandoSeAplican({ tipo: 'renombrarCampo', clave: 'proveedor', label });
}

function cuandoSeIncorpora(bloque: Bloque, acciones: AccionDeRevision[]) {
  bloqueOriginal = bloque;
  cuandoSeAplican(...acciones);
  incorporacion = aIncorporacion(bloque, revision, () => 'clave-nueva');
}

function cuandoSeIncorporaConLaColumnaRenombradaYAceptada() {
  cuandoSeIncorpora(destino(), [
    { tipo: 'renombrarCampo', clave: 'proveedor', label: '  Quién lo vende ' },
    { tipo: 'aceptarCampo', clave: 'proveedor' },
  ]);
}

function cuandoSeIncorporanDosColumnasConLaClavePorDefecto() {
  const base = dosColumnasNuevas('Dos');
  const aceptadas = ['a', 'b'].reduce(
    (actual, clave) => reducirRevision(actual, { tipo: 'aceptarCampo', clave }),
    base,
  );
  incorporacion = aIncorporacion(makeBlock('note'), aceptadas);
}

// --- entonces ---

function entoncesElResultadoEs(esperado: unknown) {
  expect(resultado).toEqual(esperado);
}

function entoncesLasFilasSon(ids: string[]) {
  expect(revision.filas.map((fila) => fila.id)).toEqual(ids);
}

function entoncesLasDudasPorRegistroSon(dudas: unknown[]) {
  expect(revision.filas.map((fila) => fila.dudas)).toEqual(dudas);
}

function entoncesHayDudas(cantidad: number) {
  expect(cantidadDeDudas(revision)).toBe(cantidad);
}

function entoncesLasColumnasNuevasSon(columnas: unknown[]) {
  expect(revision.camposNuevos).toEqual(columnas);
}

function entoncesElRegistroTieneElNombre(filaId: string, nombre: string) {
  expect(revision.filas.find((fila) => fila.id === filaId)?.valores.name).toBe(nombre);
}

function entoncesElRegistroNoTieneDudas(indice: number) {
  expect(revision.filas[indice]?.dudas).toEqual({});
}

function entoncesSoloElRegistroQuedaEditado(indice: number) {
  expect(revision.filas.map((fila) => fila.editada)).toEqual(
    revision.filas.map((_fila, posicion) => posicion === indice),
  );
}

function entoncesLaDudaSeResolvioSinCambiarElDato(indice: number) {
  expect(revision.filas[indice]?.dudas).toEqual({});
  expect(revision.filas[indice]?.valores).toEqual(revisionOriginal.filas[indice]?.valores);
  expect(revision.filas[indice]?.editada).toBe(false);
}

function entoncesLaPrimeraColumnaEsta(estado: string) {
  expect(revision.camposNuevos[0]?.estado).toBe(estado);
}

function entoncesLaPrimeraColumnaCoincideCon(esperado: object) {
  expect(revision.camposNuevos[0]).toMatchObject(esperado);
}

function entoncesQuedaUnaColumnaSinDecidir() {
  expect(camposSinDecidir(revision)).toHaveLength(1);
}

function entoncesLaColumnaOfreceLasOpciones(opciones: string[]) {
  expect(opcionesDeLaColumna(revision, 'proveedor')).toEqual(opciones);
}

function entoncesElCampoNuevoEsDeOpciones(opciones: string[]) {
  const campo = elemento(revision.camposNuevos, 0);
  expect(campoDeColumnaNueva(campo, revision)).toMatchObject({
    type: 'select',
    options: opciones,
    required: false,
  });
}

function entoncesLasTarjetasMuestranLaColumna(seMuestra: boolean) {
  const claves = camposDeLasTarjetas(destino().fields, revision).map((campo) => campo.key);
  expect(claves.includes('proveedor')).toBe(seMuestra);
}

function entoncesElErrorDeNombreEs(existentes: Bloque['fields'], esperado: string | undefined) {
  expect(errorDeNombre(elemento(revision.camposNuevos, 0), revision, existentes)).toBe(esperado);
}

function entoncesFaltaPara(existentes: Bloque['fields'], motivos: string[]) {
  expect(motivosPendientes(revision, existentes)).toEqual(motivos);
}

function entoncesLosErroresIncluyen(esperados: object) {
  expect(erroresDeLosRegistros(destino(), revision, makeSystem('Prueba'))).toMatchObject(esperados);
}

function entoncesLosErroresSon(esperados: unknown) {
  expect(erroresDeLosRegistros(destino(), revision, makeSystem('Prueba'))).toEqual(esperados);
}

function entoncesElBloqueFinalTieneLaColumnaNueva() {
  expect(incorporacion.bloque.fields).toHaveLength(10);
  expect(incorporacion.bloque.fields.at(-1)).toEqual({
    key: 'clave-nueva',
    label: 'Quién lo vende',
    type: 'text',
    required: false,
  });
}

function entoncesElBloqueOriginalSigueConCincoCampos() {
  expect(bloqueOriginal.fields).toHaveLength(9);
}

function entoncesLosRegistrosUsanLaClaveNueva() {
  expect(incorporacion.filas).toHaveLength(3);
  expect(incorporacion.filas[0]).toMatchObject({
    name: 'Yerba Playadito 1 kg',
    'clave-nueva': 'Distribuidora Sur',
  });
  expect(incorporacion.filas[0]).not.toHaveProperty('proveedor');
}

function entoncesNoSeCreaLaColumnaNiSusDatos() {
  expect(incorporacion.bloque.fields).toHaveLength(9);
  for (const fila of incorporacion.filas)
    expect(Object.keys(fila)).toEqual(bloqueOriginal.fields.map((campo) => campo.key));
}

function entoncesVanDosRegistros() {
  expect(incorporacion.filas).toHaveLength(2);
}

function entoncesSoloElRegistroCorregidoDejaDeSerDeEjemplo() {
  expect(incorporacion.filas[0]).toMatchObject({ name: 'Arroz', __example: false });
  expect(incorporacion.filas[1]).not.toHaveProperty('__example');
}

function entoncesLasClavesSonDistintas() {
  const claves = incorporacion.bloque.fields.map((campo) => campo.key);
  expect(new Set(claves).size).toBe(claves.length);
  expect(claves.every((clave) => clave.length > 3)).toBe(true);
}
