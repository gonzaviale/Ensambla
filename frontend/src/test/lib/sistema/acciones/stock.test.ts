import { describe, it } from 'vitest';
import {
  borrarRegistros,
  cargarRegistros,
  cuandoSamiPide,
  entoncesSeAplico,
} from '@/test/accionesDeSami';
import {
  cuandoSeBorraLaVenta,
  cuandoSeBorranLaSinProductoYLaDeEjemplo,
  cuandoSeCorrigeLaVentaAProductoYCantidad,
  cuandoSePreparaParaGuardarYParaCrear,
  cuandoSeSumaElProductoPendiente,
  cuandoSeVende,
  dadaUnaVentaDeSami,
  dadaUnaVentaDeSamiSobreUnAlmacenGuardado,
  dadaUnaVentaDeUnProductoQueNoEsta,
  dadaUnaVentaGuardadaDeDosYerbas,
  dadasEntradasGuardadasDeYerbaYMate,
  dadasEntradasSinFilas,
  dadasUnaVentaDeEjemploYOtraSinProducto,
  dadoUnAlmacenConYerbaYMate,
  dadoUnClienteGuardado,
  entoncesElInventarioSigueConSusProductos,
  entoncesFaltoStockDe,
  entoncesLoGuardadoConservaElStockDescontado,
  entoncesLosStocksSon,
  entoncesNingunaFilaLlevaMarcasDeStock,
  entoncesNoFaltoStock,
  entoncesNoQuedanVentas,
} from '@/test/stockDeSami';

describe('el stock de las ventas que carga Sami', () => {
  it('una venta de un producto que está descuenta su cantidad', () => {
    dadoUnAlmacenConYerbaYMate();

    cuandoSeVende({ Producto: 'Yerba', Cantidad: 2 });

    entoncesLosStocksSon({ Yerba: 3 });
  });

  it('si alcanza, no avisa que falte stock', () => {
    dadoUnAlmacenConYerbaYMate();

    cuandoSeVende({ Producto: 'Yerba', Cantidad: 2 });

    entoncesNoFaltoStock();
  });

  it('si no alcanza, descuenta hasta 0', () => {
    dadoUnAlmacenConYerbaYMate();

    cuandoSeVende({ Producto: 'Mate', Cantidad: 3 }, { Producto: 'Yerba', Cantidad: 5 });

    entoncesLosStocksSon({ Mate: 0, Yerba: 0 });
  });

  it('si no alcanza, avisa cuánto se pedía y cuánto había', () => {
    dadoUnAlmacenConYerbaYMate();

    cuandoSeVende({ Producto: 'Mate', Cantidad: 3 }, { Producto: 'Yerba', Cantidad: 5 });

    entoncesFaltoStockDe('Mate', 3, 1);
  });

  it('corregir la cantidad o el producto deja el stock como si se hubiera cargado bien', () => {
    dadaUnaVentaDeSami('Mate', 3);

    cuandoSeCorrigeLaVentaAProductoYCantidad('Yerba', 2);

    entoncesLosStocksSon({ Mate: 1, Yerba: 3 });
  });

  it('borrar una venta de Sami devuelve el stock', () => {
    dadaUnaVentaDeSami('Yerba', 2);

    cuandoSeBorraLaVenta();

    entoncesLosStocksSon({ Yerba: 5 });
  });

  it('borrar una venta que ya estaba guardada se avisa como cualquier otra', () => {
    dadaUnaVentaGuardadaDeDosYerbas();

    cuandoSamiPide(borrarRegistros('sales', { ids: ['e3333333-vta'] }));

    entoncesSeAplico('Borré un registro de ventas.');
  });

  it('borrar una venta que ya estaba guardada también devuelve su cantidad al stock', () => {
    dadaUnaVentaGuardadaDeDosYerbas();

    cuandoSamiPide(borrarRegistros('sales', { ids: ['e3333333-vta'] }));

    entoncesLosStocksSon({ Yerba: 7 });
  });

  it('borrar una entrada guardada le resta su cantidad al stock sin dejarlo en negativo', () => {
    dadasEntradasGuardadasDeYerbaYMate();

    cuandoSamiPide(borrarRegistros('entries', { ids: ['e4444444-ent', 'e5555555-ent'] }));

    entoncesLosStocksSon({ Yerba: 3, Mate: 0 });
  });

  it('borrar una fila de ejemplo o una venta sin producto no mueve el stock', () => {
    dadasUnaVentaDeEjemploYOtraSinProducto();

    cuandoSeBorranLaSinProductoYLaDeEjemplo();

    entoncesLosStocksSon({ Yerba: 5 });
  });

  it('las dos se borran igual', () => {
    dadasUnaVentaDeEjemploYOtraSinProducto();

    cuandoSeBorranLaSinProductoYLaDeEjemplo();

    entoncesNoQuedanVentas();
  });

  it('borrar un cliente no toca el stock', () => {
    dadoUnClienteGuardado();

    cuandoSamiPide(borrarRegistros('clients', { ids: ['e8888888-cli'] }));

    entoncesLosStocksSon({ Yerba: 5, Mate: 1 });
  });

  it('una entrada suma al stock', () => {
    dadasEntradasSinFilas();

    cuandoSamiPide(cargarRegistros('entries', [{ Producto: 'Mate', Cantidad: 4 }]));

    entoncesLosStocksSon({ Mate: 5 });
  });

  it('una venta de un producto pendiente no crea el producto', () => {
    dadaUnaVentaDeUnProductoQueNoEsta();

    entoncesElInventarioSigueConSusProductos(2);
  });

  it('una venta de un producto pendiente descuenta recién al sumarlo', () => {
    dadaUnaVentaDeUnProductoQueNoEsta();

    cuandoSeSumaElProductoPendiente();

    entoncesLosStocksSon({ Bombilla: 0 });
  });

  it('al sumar el producto pendiente avisa que no había', () => {
    dadaUnaVentaDeUnProductoQueNoEsta();

    cuandoSeSumaElProductoPendiente();

    entoncesFaltoStockDe('Bombilla', 2, 0);
  });

  it('las marcas con las que se sigue el stock no se guardan', () => {
    dadaUnaVentaDeSamiSobreUnAlmacenGuardado();

    cuandoSePreparaParaGuardarYParaCrear();

    entoncesNingunaFilaLlevaMarcasDeStock();
  });

  it('lo que se guarda conserva el stock descontado', () => {
    dadaUnaVentaDeSamiSobreUnAlmacenGuardado();

    cuandoSePreparaParaGuardarYParaCrear();

    entoncesLoGuardadoConservaElStockDescontado();
  });
});
