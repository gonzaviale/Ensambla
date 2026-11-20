import { describe, expect, it } from 'vitest';
import { allBlocks, makeTemplate, removeRow, upsertRow } from '@/lib/sistema/core';
import {
  agregarLinea,
  cambiarCantidad,
  crearLinea,
  guardarArticuloCaja,
  idCaja,
  leerPrecio,
  prepararCaja,
} from '@/lib/caja';
import {
  cuandoSeCalcula,
  cuandoSeIntentaCalcular,
  entoncesElCalculoFalla,
  entoncesElResultadoCoincideCon,
  entoncesElResultadoEs,
  entoncesElResultadoExiste,
} from '@/test/calculo';
import type { Bloque, Fila, Sistema } from '@/lib/sistema/tipos';

const pan: Fila = { id: 'pan', name: 'Pan francés', saleMode: 'peso', quantity: 2, price: 4000 };
const coca: Fila = { id: 'coca', name: 'Coca-Cola', quantity: 10, price: 2700 };

let sistema: Sistema;
let inventario: Bloque;
let ventas: Bloque;
let ventasPreparadas: ReturnType<typeof prepararCaja> | undefined;
let lineas: ReturnType<typeof agregarLinea>;
let primeraLinea: ReturnType<typeof agregarLinea>;

describe('caja con precios por unidad y kilo', () => {
  it('calcula los gramos y el precio por kilo de un renglón', () => {
    cuandoSeCalcula(() => crearLinea(pan, '500'));

    entoncesElResultadoCoincideCon({ cantidad: 0.5, importeCentavos: 200000 });
  });

  it('redondea el importe de un renglón por peso', () => {
    cuandoSeCalcula(() => crearLinea({ ...pan, price: 10999 }, '125').importeCentavos);

    entoncesElResultadoEs(137488);
  });

  it('calcula el importe de un renglón por unidad', () => {
    cuandoSeCalcula(() => crearLinea(coca, '2').importeCentavos);

    entoncesElResultadoEs(540000);
  });

  it('redondea hacia arriba exactamente medio centavo, como el servidor', () => {
    cuandoSeCalcula(() => crearLinea({ ...pan, price: 1 }, '145').importeCentavos);

    entoncesElResultadoEs(15);
  });

  it('redondea hacia abajo menos de medio centavo, como el servidor', () => {
    cuandoSeCalcula(() => crearLinea({ ...pan, price: 1 }, '144').importeCentavos);

    entoncesElResultadoEs(14);
  });

  it.each(['', '-1', '0', '1.5', 'NaN'])('rechaza cantidad inválida %s', (cantidad) => {
    cuandoSeIntentaCalcular(() => crearLinea(pan, cantidad));

    entoncesElCalculoFalla();
  });

  it.each(['', '-1', '0', '1.234,50', '1,2,3', '3.001'])('rechaza precio ambiguo %s', (precio) => {
    cuandoSeIntentaCalcular(() => leerPrecio(precio));

    entoncesElCalculoFalla();
  });

  it('acepta coma decimal sin separador de miles', () => {
    cuandoSeCalcula(() => leerPrecio('1250,50'));

    entoncesElResultadoEs(125050);
  });

  it('agrupa las unidades en un solo renglón', () => {
    cuandoSeAgregaDosVecesLaMismaUnidad();

    entoncesHayUnRenglonConTresUnidades();
  });

  it('no modifica los renglones anteriores al agrupar', () => {
    cuandoSeAgregaDosVecesLaMismaUnidad();

    entoncesElRenglonOriginalSigueConUnaUnidad();
  });

  it('conserva las pesadas independientes', () => {
    cuandoSeAgreganDosPesadasIguales();

    entoncesHayDosRenglones();
  });

  it('corrige los gramos sin cambiar el precio histórico ni el identificador de línea', () => {
    const linea = crearLinea(pan, '100');

    cuandoSeCalcula(() => cambiarCantidad(linea, 0.108));

    entoncesElResultadoCoincideCon({ id: linea.id, cantidad: 0.108, importeCentavos: 43200 });
  });

  it('prepara las tablas conectadas sin repetir la configuración', () => {
    dadoUnComercioConInventario();

    cuandoSePreparaLaCajaDosVeces();

    entoncesLaCajaPreparadaEsLaMisma();
  });

  it('prepara la caja sin duplicar productos ni el campo de modo de venta', () => {
    dadoUnComercioConInventario();

    cuandoSePreparaLaCajaDosVeces();

    entoncesElInventarioSigueConDosProductosYUnSoloModoDeVenta();
  });

  it('prepara cada producto con su modo de venta y control de stock', () => {
    dadoUnComercioConInventario();

    cuandoSePreparaLaCajaDosVeces();

    entoncesLosProductosTienenSuModoDeVentaYSuStock();
  });

  it('el alta rápida guarda una ficha reutilizable sin inventar costo ni existencias', () => {
    dadoUnComercioConLaCajaPreparada();

    cuandoSeGuardaUnArticuloNuevoEnElAltaRapida();

    entoncesLaFichaTieneElNombreElPrecioYElCodigoPeroNoCostoNiExistencias();
  });

  it('conserva campos propios y conteo al modificar la ficha', () => {
    dadoUnComercioConLaCajaPreparada();

    cuandoSeModificaLaFichaDelPan();

    entoncesLaFichaConservaElConteoYTomaElPrecioNuevo();
  });

  it('no permite modificar una venta ya confirmada desde el editor', () => {
    dadoUnaVentaConfirmada();

    cuandoSeCalcula(() => upsertRow(ventas, {}, 'venta', sistema).errors);

    entoncesElResultadoExiste();
  });

  it('no permite eliminar una venta ya confirmada desde el editor', () => {
    dadoUnaVentaConfirmada();

    cuandoSeCalcula(() => removeRow(ventas, 'venta', sistema).errors);

    entoncesElResultadoExiste();
  });

  it('envía los ids UUID del backend sin el prefijo local de las tablas', () => {
    const uuid = crypto.randomUUID();

    cuandoSeCalcula(() => idCaja(`e${uuid}`));

    entoncesElResultadoEs(uuid);
  });

  it('deja intacto un id que ya es UUID', () => {
    const uuid = crypto.randomUUID();

    cuandoSeCalcula(() => idCaja(uuid));

    entoncesElResultadoEs(uuid);
  });
});

function dadoUnComercioConInventario() {
  sistema = makeTemplate('shop', {}, false);
  inventario = allBlocks(sistema).find((b) => b.type === 'inventory') as Bloque;
  inventario.rows = [{ ...pan }, { ...coca }];
}

function dadoUnComercioConLaCajaPreparada() {
  dadoUnComercioConInventario();
  prepararCaja(sistema, inventario.id);
}

function dadoUnaVentaConfirmada() {
  dadoUnComercioConInventario();
  ventas = allBlocks(sistema).find((b) => b.type === 'sales') as Bloque;
  ventas.rows.push({
    id: 'venta',
    product: inventario.rows[0]?.id,
    quantity: 1,
    price: 4000,
    __source: 'Caja operación',
  });
}

function cuandoSeAgregaDosVecesLaMismaUnidad() {
  primeraLinea = agregarLinea([], crearLinea(coca));
  lineas = agregarLinea(primeraLinea, crearLinea(coca, '2'));
}

function cuandoSeAgreganDosPesadasIguales() {
  lineas = agregarLinea([crearLinea(pan, '100')], crearLinea(pan, '100'));
}

function cuandoSePreparaLaCajaDosVeces() {
  ventasPreparadas = prepararCaja(sistema, inventario.id);
}

function cuandoSeGuardaUnArticuloNuevoEnElAltaRapida() {
  cuandoSeCalcula(() =>
    guardarArticuloCaja(
      sistema,
      inventario,
      {
        nombre: 'Jamón',
        precio: '12000',
        modo: 'peso',
        favorito: true,
        controlarStock: false,
        cantidad: '100',
        extras: {},
      },
      '77903754',
    ),
  );
}

function cuandoSeModificaLaFichaDelPan() {
  cuandoSeCalcula(() =>
    guardarArticuloCaja(
      sistema,
      inventario,
      {
        nombre: 'Pan francés',
        precio: '4500',
        modo: 'peso',
        favorito: true,
        controlarStock: true,
        cantidad: '100',
        extras: {},
      },
      '',
      'pan',
    ),
  );
}

function entoncesHayUnRenglonConTresUnidades() {
  expect(lineas).toHaveLength(1);
  expect(lineas[0]).toMatchObject({ cantidad: 3, importeCentavos: 810000 });
}

function entoncesElRenglonOriginalSigueConUnaUnidad() {
  expect(primeraLinea[0]?.cantidad).toBe(1);
}

function entoncesHayDosRenglones() {
  expect(lineas).toHaveLength(2);
}

function entoncesLaCajaPreparadaEsLaMisma() {
  expect(prepararCaja(sistema, inventario.id)).toBe(ventasPreparadas);
}

function entoncesElInventarioSigueConDosProductosYUnSoloModoDeVenta() {
  expect(inventario.rows).toHaveLength(2);
  expect(inventario.fields.filter((f) => f.key === 'saleMode')).toHaveLength(1);
}

function entoncesLosProductosTienenSuModoDeVentaYSuStock() {
  expect(inventario.rows[0]).toMatchObject({
    quantity: 2,
    saleMode: 'peso',
    stockControl: true,
    favorite: false,
  });
  expect(inventario.rows[1]?.saleMode).toBe('unidad');
}

function entoncesLaFichaTieneElNombreElPrecioYElCodigoPeroNoCostoNiExistencias() {
  entoncesElResultadoCoincideCon({
    name: 'Jamón',
    price: 12000,
    saleMode: 'peso',
    favorite: true,
    stockControl: false,
    quantity: 0,
    barcode: '77903754',
    cost: '',
  });
}

function entoncesLaFichaConservaElConteoYTomaElPrecioNuevo() {
  entoncesElResultadoCoincideCon({ quantity: 2, price: 4500 });
}
