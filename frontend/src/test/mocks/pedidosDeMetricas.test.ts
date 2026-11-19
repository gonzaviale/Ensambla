import { describe, it } from 'vitest';
import {
  cuandoSePideAlMock,
  dadoElAlmacenDeDemo,
  entoncesArmaLaReceta,
  entoncesElAlmacenCalculaLoQueSePidio,
  entoncesLosDiccionariosSonIguales,
  entoncesNoEntendioLasPalabras,
  entoncesNoSeEntiendePorque,
  entoncesSeArmaDe,
} from '@/test/pedidosDeMetricas';

// Los mismos diez pedidos de todos los días que InterpreteDePedidosTest del servidor, con la misma respuesta.
const PEDIDOS_DE_UN_ALMACEN = [
  ['¿Cuánto vendí esta semana?', 'VENTAS', 'SUMA', 'NINGUNA', 'SEMANA', false],
  ['qué producto vendo más este mes', 'VENTAS', 'UNIDADES', 'PRODUCTO', 'MES', false],
  ['¿Cuánto me deben por cliente?', 'VENTAS', 'SUMA', 'CLIENTE', 'TODO', true],
  ['En qué gasté más este mes', 'GASTOS', 'SUMA', 'CATEGORIA', 'MES', false],
  ['cuántas ventas hice hoy', 'VENTAS', 'CONTEO', 'NINGUNA', 'HOY', false],
  ['Venta promedio de esta semana', 'VENTAS', 'PROMEDIO', 'NINGUNA', 'SEMANA', false],
  ['¿Cuánto stock me queda por producto?', 'INVENTARIO', 'UNIDADES', 'PRODUCTO', 'TODO', false],
  ['¿Qué día de la semana vendo más?', 'VENTAS', 'SUMA', 'DIA_DE_LA_SEMANA', 'MES', false],
  ['cuánto gasté en total', 'GASTOS', 'SUMA', 'NINGUNA', 'TODO', false],
  ['¿Cuánto vale mi mercadería?', 'INVENTARIO', 'SUMA', 'NINGUNA', 'TODO', false],
] as const;

describe('el intérprete de pedidos de los mocks', () => {
  it('usa el mismo diccionario que el servidor', () => {
    entoncesLosDiccionariosSonIguales();
  });

  it.each(PEDIDOS_DE_UN_ALMACEN)(
    'entiende «%s» igual que el servidor',
    (pedido, fuente, calculo, agrupar, periodo, soloFiado) => {
      cuandoSePideAlMock(pedido);

      entoncesArmaLaReceta({ fuente, calculo, agrupar, periodo, soloFiado });
    },
  );

  it('entiende «cuantos productos tengo» sin tildes como el stock contado', () => {
    cuandoSePideAlMock('cuantos productos tengo');

    entoncesArmaLaReceta({
      fuente: 'INVENTARIO',
      calculo: 'CONTEO',
      agrupar: 'NINGUNA',
      periodo: 'TODO',
      soloFiado: false,
    });
  });

  it('avisa lo que no sabe calcular, como el finde, en vez de inventarlo', () => {
    cuandoSePideAlMock('cuanto vendi el finde');

    entoncesNoEntendioLasPalabras(['finde']);
  });

  it('si no se da cuenta de qué se habla, pide que se diga si son ventas, gastos o stock', () => {
    cuandoSePideAlMock('hola, ¿cómo andás?');

    entoncesNoSeEntiendePorque('Decime si es de tus ventas, tus gastos o tu stock');
  });

  it('si lo que se pide no se puede calcular, dice por qué', () => {
    cuandoSePideAlMock('gastos por cliente');

    entoncesNoSeEntiendePorque('No se puede repartir gastos de esa manera');
  });

  it('con el almacén de demo calcula «Cuánto vendí esta semana» sin fallar', () => {
    dadoElAlmacenDeDemo();

    cuandoSePideAlMock('Cuánto vendí esta semana');

    entoncesElAlmacenCalculaLoQueSePidio();
  });

  it('el almacén de demo, que no cargó gastos, ofrece armar de ventas y stock pero no de gastos', () => {
    dadoElAlmacenDeDemo();

    entoncesSeArmaDe(['VENTAS', 'INVENTARIO']);
  });
});
