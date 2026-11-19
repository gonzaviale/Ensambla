import { expect } from 'vitest';
import {
  aplicarAcciones,
  pendientesVigentes,
  sumarPendientes,
  type VinculoPendiente,
} from '@/lib/sistema/acciones';
import { upsertRow } from '@/lib/sistema/core';
import type { Fila } from '@/lib/sistema/tipos';
import {
  agregarPieza,
  cargarEjemplos,
  cargarRegistros,
  conectar,
  cuandoSamiPide,
  dadoQueSamiYaPidio,
  dadoUnSistemaVacio,
  piezaLlamada,
  taller,
} from './accionesDeSami';

let realCargadaAMano: Fila | undefined;
let ventaDeMarta: Fila | undefined;
let martaCargadaAMano: Fila | undefined;
let pendientes: VinculoPendiente[] = [];
let vigentes: VinculoPendiente[] = [];

const sinId = (fila: Fila | undefined) => {
  const resto: Partial<Fila> = { ...fila };
  delete resto.id;
  return resto;
};

const nombresDe = (pieza: string) => piezaLlamada(pieza).rows.map((fila) => fila.name);

const vinculosDe = (pieza: string) =>
  piezaLlamada(pieza).fields.filter((campo) => campo.type === 'relation');

const dadaUnaPiezaVaciaDeCadaUno = () => {
  dadoUnSistemaVacio();
  dadoQueSamiYaPidio(
    agregarPieza('inventory', { ejemplos: [] }),
    agregarPieza('clients', { ejemplos: [] }),
    agregarPieza('sales', { ejemplos: [] }),
  );
};

export const cuandoSeSumanVelasConAromaYEjemplos = () => {
  dadoUnSistemaVacio();
  cuandoSamiPide(
    agregarPieza('inventory', {
      nombre: 'Velas',
      campos: [
        { etiqueta: 'Aroma', tipo: 'text' },
        { etiqueta: 'producto', tipo: 'text' },
        { etiqueta: 'Color', tipo: 'color' },
      ],
      ejemplos: [
        { Producto: 'Vela de lavanda', Cantidad: '12', Precio: 6500, Aroma: 'Lavanda', Otra: 1 },
        { 'Algo que no existe': 'x' },
      ],
    }),
  );
};

export const entoncesLasVelasSonUnInventarioConElAromaAlFinal = () => {
  const velas = piezaLlamada('Velas');
  expect(velas.type).toBe('inventory');
  expect(velas.fields.map((campo) => campo.label)).toEqual([
    'Producto',
    'Cantidad',
    'Precio',
    'Costo',
    'Stock mínimo',
    'Aroma',
  ]);
  expect(velas.fields.at(-1)).toMatchObject({ key: 'aroma', type: 'text', required: false });
};

export const entoncesLasVelasTienenSoloElEjemploDeSami = () => {
  expect(piezaLlamada('Velas').rows.map(sinId)).toEqual([
    { __example: true, name: 'Vela de lavanda', quantity: 12, price: 6500, aroma: 'Lavanda' },
  ]);
};

export const cuandoSeSumanTurnosConEjemplosDeMas = () => {
  dadoUnSistemaVacio();
  cuandoSamiPide(
    agregarPieza('form', {
      nombre: 'Turnos',
      campos: [
        { etiqueta: 'Hora', tipo: 'time' },
        { etiqueta: 'Seña', tipo: 'checkbox' },
        { etiqueta: 'Servicio', tipo: 'select', opciones: ['Corte', 'Color'] },
        { etiqueta: 'Precio', tipo: 'currency' },
      ],
      ejemplos: [
        {
          Nombre: 'Sofía',
          Hora: '9:05',
          Seña: 'Sí',
          Servicio: 'color',
          Fecha: 'mañana',
          Precio: 'gratis',
        },
        ...Array.from({ length: 6 }, (_, indice) => ({ Nombre: `Turno ${indice}` })),
      ],
    }),
  );
};

export const entoncesLaPiezaTieneFilas = (pieza: string, cantidad: number) => {
  expect(piezaLlamada(pieza).rows).toHaveLength(cantidad);
};

export const entoncesElPrimerTurnoQuedaConCadaValorEnSuTipo = () => {
  expect(sinId(piezaLlamada('Turnos').rows[0])).toEqual({
    __example: true,
    name: 'Sofía',
    hora: '09:05',
    sena: true,
    servicio: 'Color',
  });
};

export const entoncesTodasLasFilasSonEjemplos = (pieza: string) => {
  expect(piezaLlamada(pieza).rows.length).toBeGreaterThan(0);
  expect(piezaLlamada(pieza).rows.every((fila) => fila.__example)).toBe(true);
};

export const cuandoSeSumanTablasYClientesRepetidos = () => {
  dadoUnSistemaVacio();
  cuandoSamiPide(
    agregarPieza('form', { nombre: 'Turnos' }),
    agregarPieza('form', { nombre: 'Encargos', seccion: 'Agenda' }),
    agregarPieza('form', { nombre: 'turnos' }),
    agregarPieza('clients', { nombre: 'Encargos' }),
    agregarPieza('clients'),
    agregarPieza('clients', { nombre: 'Otra' }),
  );
};

export const entoncesLosEncargosQuedanSolosEnLaPestanaAgenda = () => {
  expect(taller.sistema.sections.map((seccion) => seccion.name)).toEqual([
    'Vista general',
    'Agenda',
  ]);
  expect(taller.sistema.sections[1]?.blocks.map((bloque) => bloque.name)).toEqual(['Encargos']);
};

export const dadasVelasConUnSoloEjemploValido = () => {
  dadoUnSistemaVacio();
  dadoQueSamiYaPidio(
    agregarPieza('inventory', {
      nombre: 'Velas',
      ejemplos: [{ Producto: 'Vela lavanda', Cantidad: 8 }, { Cantidad: 3 }],
    }),
  );
};

export const entoncesLosNombresDe = (pieza: string, nombres: unknown[]) => {
  expect(nombresDe(pieza)).toEqual(nombres);
};

export const dadosClientesConUnaClientaReal = (ejemplos?: Record<string, unknown>[]) => {
  dadoUnSistemaVacio();
  dadoQueSamiYaPidio(agregarPieza('clients', ejemplos ? { ejemplos } : {}));
  realCargadaAMano = upsertRow(
    piezaLlamada('Clientes'),
    { name: 'Rosa' },
    null,
    taller.sistema,
  ).row;
};

export const cuandoSamiCargaSieteEjemplosDeClientes = () => {
  const filas = Array.from({ length: 7 }, (_, indice) => ({ Nombre: `Cliente ${indice}` }));
  cuandoSamiPide(cargarEjemplos('clientes', filas));
};

export const entoncesLaClientaRealSigueAntesQueLosEjemplos = (ejemplos: string[]) => {
  const [primera, ...resto] = piezaLlamada('Clientes').rows;
  expect(primera).toBe(realCargadaAMano);
  expect(resto.map((fila) => fila.name)).toEqual(ejemplos);
};

export const dadosClientesYUnaNota = () => {
  dadoUnSistemaVacio();
  dadoQueSamiYaPidio(agregarPieza('clients'), agregarPieza('note'));
};

export const dadosGastosSinEjemplos = () => {
  dadoUnSistemaVacio();
  dadoQueSamiYaPidio(agregarPieza('expenses', { ejemplos: [] }));
};

export const cuandoSeCarganGastosConFechasComoEnUnCuaderno = () => {
  cuandoSamiPide(
    cargarRegistros('expenses', [
      { Concepto: 'Luz', Fecha: '02/10' },
      { Concepto: 'Gas', Fecha: '5/3/25' },
      { Concepto: 'Agua', Fecha: '09-10-2024' },
      { Concepto: 'Internet', Fecha: '2026-01-31' },
      { Concepto: 'Alquiler', Fecha: '31/02' },
    ]),
  );
};

export const entoncesLasFechasSeEntiendenYLasQueNoExistenSeDescartan = () => {
  const anio = new Date().getFullYear();
  const reales = piezaLlamada('Gastos').rows.filter((fila) => !fila.__example);
  expect(reales.map((fila) => fila.date)).toEqual([
    `${anio}-10-02`,
    '2025-03-05',
    '2024-10-09',
    '2026-01-31',
    undefined,
  ]);
};

export const cuandoSeCarganTresClientes = () => {
  cuandoSamiPide(
    cargarRegistros('Clientes', [
      { Nombre: 'Lucía', Teléfono: '11 5555-0101' },
      { Nombre: 'Marta' },
      { Nombre: 'Pedro' },
    ]),
  );
};

export const entoncesLosRegistrosNuevosSonRealesConSusDatos = () => {
  const nuevas = piezaLlamada('Clientes').rows.slice(1);
  expect(nuevas.every((fila) => !('__example' in fila))).toBe(true);
  expect(sinId(nuevas[0])).toEqual({ name: 'Lucía', phone: '11 5555-0101' });
};

export const dadoUnInventarioVacio = () => {
  dadoUnSistemaVacio();
  dadoQueSamiYaPidio(agregarPieza('inventory', { ejemplos: [] }));
  piezaLlamada('Inventario').rows = [];
};

export const cuandoSeCarganMasDeTreintaProductos = () => {
  cuandoSamiPide(
    cargarRegistros('inventory', [
      { Producto: 'Vela lavanda', Cantidad: '8', Precio: '6500,5', Costo: 'mucho' },
      { Cantidad: 3 },
      'no es una fila' as unknown as Record<string, unknown>,
      ...Array.from({ length: 40 }, (_, indice) => ({ Producto: `Vela ${indice}` })),
    ]),
  );
};

export const entoncesSeCargaronLosPrimerosTreinta = () => {
  expect(piezaLlamada('Inventario').rows).toHaveLength(30);
  expect(piezaLlamada('Inventario').rows.at(-1)?.name).toBe('Vela 28');
};

export const entoncesElPrimerProductoQuedaConCadaValorEnSuTipo = () => {
  expect(sinId(piezaLlamada('Inventario').rows[0])).toEqual({
    name: 'Vela lavanda',
    quantity: 8,
    price: 6500.5,
  });
};

export const dadaUnaVentaPendienteDeMartaCargadaAManoYOtraDeNadiaBorrada = () => {
  dadaUnaPiezaVaciaDeCadaUno();
  pendientes = dadoQueSamiYaPidio(
    cargarRegistros('sales', [
      { Producto: 'Yerba', Cliente: 'Marta', Cantidad: 1 },
      { Producto: 'Yerba', Cliente: 'Nadia', Cantidad: 2 },
    ]),
  ).pendientes;
  const ventas = piezaLlamada('Ventas');
  const [deMarta, deNadia] = ventas.rows;
  ventaDeMarta = deMarta;
  martaCargadaAMano = upsertRow(
    piezaLlamada('Clientes'),
    { name: 'marta' },
    null,
    taller.sistema,
  ).row;
  ventas.rows = ventas.rows.filter((fila) => fila !== deNadia);
};

export const cuandoSeSumanEsosPendientes = () => {
  sumarPendientes(taller.sistema, pendientes);
};

export const entoncesLaVentaQuedaConLaClientaQueSeCargoAMano = () => {
  expect(ventaDeMarta?.customer).toBe(martaCargadaAMano?.id);
};

export const entoncesNoSeCreaLaClientaDeUnaVentaBorrada = () => {
  expect(piezaLlamada('Clientes').rows.filter((fila) => fila.name === 'Nadia')).toEqual([]);
};

export const dadosPendientesDeDosTurnosConUnaVentaYaVinculada = () => {
  dadaUnaPiezaVaciaDeCadaUno();
  const primero = aplicarAcciones(taller.sistema, [
    cargarRegistros('sales', [{ Producto: 'Yerba', Cliente: 'Marta', Cantidad: 1 }]),
  ]).pendientes;
  const segundo = aplicarAcciones(taller.sistema, [
    cargarRegistros('sales', [
      { Producto: 'Mate', Cliente: 'marta', Cantidad: 1 },
      { Producto: 'Mate', Cliente: 'Nadia', Cantidad: 1 },
    ]),
  ]).pendientes;
  const conMate = piezaLlamada('Ventas').rows[1];
  if (conMate) conMate.customer = 'e00000000-ya-vinculada';
  pendientes = [...primero, ...segundo, { destinoId: 'nada', nombre: 'X', filas: [] }];
};

export const cuandoSeBuscanLosPendientesVigentes = () => {
  vigentes = pendientesVigentes(taller.sistema, pendientes);
};

export const entoncesLosPendientesVigentesSon = (esperados: [string, number][]) => {
  expect(vigentes.map((pendiente) => [pendiente.nombre, pendiente.filas.length])).toEqual(
    esperados,
  );
};

export const cuandoSeConectanPedidosConVelasDeVariasFormas = () => {
  dadoUnSistemaVacio();
  cuandoSamiPide(
    agregarPieza('inventory', { nombre: 'Velas' }),
    agregarPieza('orders'),
    conectar('orders', 'Velas'),
    conectar('Pedidos', 'inventory', { etiqueta: 'Otra vela' }),
    conectar('Pedidos', 'Pedidos'),
    conectar('Pedidos', 'nada'),
  );
};

export const entoncesSeAplicoEntreOtras = (descripcion: string) => {
  expect(taller.resultado.aplicadas).toContain(descripcion);
};

export const entoncesLosPedidosTienenUnSoloVinculoConLasVelas = () => {
  expect(vinculosDe('Pedidos')).toEqual([
    {
      key: 'velas',
      label: 'Velas',
      type: 'relation',
      required: false,
      target: piezaLlamada('Velas').id,
    },
  ]);
};

export const cuandoSeConectanPedidosConLosClientesQueYaTraeDeFabrica = () => {
  dadoUnSistemaVacio();
  cuandoSamiPide(agregarPieza('clients'), agregarPieza('orders'), conectar('orders', 'clients'));
};

export const entoncesLaPiezaTieneVinculos = (pieza: string, cantidad: number) => {
  expect(vinculosDe(pieza)).toHaveLength(cantidad);
};

export const cuandoSeConectanTurnosConClientesPorUnDatoEscrito = () => {
  dadoUnSistemaVacio();
  cuandoSamiPide(
    agregarPieza('clients'),
    agregarPieza('form', { nombre: 'Turnos', campos: [{ etiqueta: 'Cliente', tipo: 'text' }] }),
    conectar('Turnos', 'Clientes', { etiqueta: 'Nombre' }),
    conectar('Turnos', 'Clientes', { etiqueta: 'cliente' }),
  );
};

export const entoncesElClienteEscritoDeLosTurnosPasaASerElVinculo = () => {
  expect(piezaLlamada('Turnos').fields.filter((campo) => campo.label === 'Cliente')).toEqual([
    {
      key: 'cliente',
      label: 'Cliente',
      type: 'relation',
      required: false,
      target: piezaLlamada('Clientes').id,
    },
  ]);
};

export const cuandoSeArmanClientasYTurnosConEjemplosQueLasNombran = () => {
  dadoUnSistemaVacio();
  cuandoSamiPide(
    agregarPieza('clients', {
      nombre: 'Clientas',
      ejemplos: [{ Nombre: 'Carla Gómez' }, { Nombre: 'Juli Sosa' }],
    }),
    agregarPieza('form', { nombre: 'Turnos', campos: [{ etiqueta: 'Clienta', tipo: 'text' }] }),
    conectar('turnos', 'CLIENTAS', { etiqueta: 'Clienta' }),
    cargarEjemplos('Turnos', [
      { Nombre: 'Corte', Clienta: 'carla gomez' },
      { Nombre: 'Color', Clienta: 'Alguien más' },
    ]),
  );
};

const clientaLlamada = (nombre: string) =>
  piezaLlamada('Clientas').rows.find((fila) => fila.name === nombre);

export const entoncesQuienNoEstabaSeSumaComoClientaDeEjemplo = () => {
  expect(clientaLlamada('Alguien más')).toMatchObject({ __example: true });
};

export const entoncesLosTurnosApuntanASusClientas = () => {
  expect(piezaLlamada('Turnos').rows.map((fila) => fila.clienta)).toEqual([
    clientaLlamada('Carla Gómez')?.id,
    clientaLlamada('Alguien más')?.id,
  ]);
};

export const dadasVelasClientasYVentasSinFilas = () => {
  dadoUnSistemaVacio();
  dadoQueSamiYaPidio(
    agregarPieza('inventory', { nombre: 'Velas', ejemplos: [] }),
    agregarPieza('clients', { nombre: 'Clientas', ejemplos: [] }),
    agregarPieza('sales'),
  );
  piezaLlamada('Velas').rows = [];
  piezaLlamada('Clientas').rows = [];
};

export const cuandoSeCarganVentasDeEjemploPorNombreYPorUnIdQueYaNoExiste = () => {
  cuandoSamiPide(
    cargarEjemplos('sales', [
      { Producto: 'Vela lavanda', Cliente: 'Lucía', Cantidad: 2 },
      { Producto: 'e1234abcd-0000-4000-8000-000000000000', Cantidad: 1 },
    ]),
  );
};

export const entoncesLaVelaYLaClientaNombradasSeSumanComoEjemplo = () => {
  expect(piezaLlamada('Velas').rows[0]).toMatchObject({ name: 'Vela lavanda', __example: true });
  expect(piezaLlamada('Clientas').rows[0]).toMatchObject({ name: 'Lucía', __example: true });
};

export const entoncesLasVentasApuntanALoSumadoYLaDelIdViejoQuedaVacia = () => {
  const [lavanda] = piezaLlamada('Velas').rows;
  const [lucia] = piezaLlamada('Clientas').rows;
  expect(piezaLlamada('Ventas').rows.map((fila) => [fila.product, fila.customer ?? ''])).toEqual([
    [lavanda?.id, lucia?.id],
    ['', ''],
  ]);
};

export const cuandoSeArmanInventarioVentasYClientesConEjemplosQueSeNombran = () => {
  dadoUnSistemaVacio();
  cuandoSamiPide(
    agregarPieza('inventory', { ejemplos: [{ Producto: 'Yerba 1 kg', Cantidad: 10 }] }),
    agregarPieza('sales', {
      ejemplos: [{ Producto: 'Yerba 1 kg', Cantidad: 2, Cliente: 'Martín', Fiado: true }],
    }),
    agregarPieza('clients', { ejemplos: [{ Nombre: 'Martín' }] }),
  );
};

export const entoncesLaVentaApuntaAlProductoYAlClienteYSigueFiada = () => {
  const [venta] = piezaLlamada('Ventas').rows;
  expect(venta?.product).toBe(piezaLlamada('Inventario').rows[0]?.id);
  expect(venta?.customer).toBe(piezaLlamada('Clientes').rows[0]?.id);
  expect(venta?.credit).toBe(true);
};

export const dadasVentasDeEjemploDeYerba = () => {
  dadoUnSistemaVacio();
  dadoQueSamiYaPidio(
    agregarPieza('inventory', { ejemplos: [{ Producto: 'Yerba' }] }),
    agregarPieza('sales', { ejemplos: [{ Producto: 'Yerba', Cantidad: 1 }] }),
  );
};

export const entoncesElProductoDeLaVentaQuedaVacio = () => {
  expect(piezaLlamada('Ventas').rows[0]?.product).toBe('');
};
