import { describe, it } from 'vitest';
import type { ClavePlantilla, OpcionesPlantilla, TipoBloque } from '@/lib/sistema/tipos';
import {
  cuandoLaEntradaPasaAlSegundoProductoCon,
  cuandoSeArmaLaPlantilla,
  cuandoSeCorrigeLaSalidaA,
  cuandoSeCorrigeLaVentaA,
  cuandoSeIntentaSacar,
  cuandoSeIntentaVender,
  cuandoSePoneElStockAlLadoDeLosClientes,
  cuandoSeQuitaElBloqueDe,
  cuandoSeQuitaLaEntrada,
  cuandoSeQuitaLaVenta,
  cuandoSeQuitanAmbasVentas,
  cuandoSeQuitanLaSalidaYLaEntrada,
  cuandoSeQuitanLasSalidasYDespuesLasEntradas,
  cuandoSeQuitanLosEjemplos,
  cuandoSeSeparaYSeVuelveAlMosaico,
  cuandoSeVendenFiadoTresDelPrimerProductoSinPrecio,
  cuandoSeVendenTresConPrecioSugeridoYUnoManual,
  cuandoSeVendeUnoConPrecioManual,
  dadaHarinaConEntradasYaConsumidas,
  dadaHarinaConUnaEntradaDeEjemplo,
  dadaLaPlantilla,
  dadaUnaEntradaDelSegundoProductoYaConsumida,
  dadosDosProductosYUnaEntradaDelPrimero,
  dadosUnProductoDeEjemploYOtroRealConUnaVentaCadaUno,
  dadoUnAlmacenConAlfajoresYUnaVentaDe,
  dadoUnDepositoConAzucarConUnaEntradaYUnaSalida,
  entoncesDejoDeSerEjemplo,
  entoncesElClienteYaNoEsEjemplo,
  entoncesElPrimerPedidoEsValido,
  entoncesElPrimerProductoBajo,
  entoncesElStockVuelveAOcuparTodoElAncho,
  entoncesHayAzucar,
  entoncesHayHarina,
  entoncesLasEntradasSiguenCargadas,
  entoncesLasEntradasYSalidasApuntanAlInventario,
  entoncesLasOpcionesSon,
  entoncesLasVentasApuntanAlInventarioYALosClientesYDescuentanStock,
  entoncesLaVentaTieneElPrecio,
  entoncesLosAnchosSon,
  entoncesLosIngresosSon,
  entoncesLosPedidosApuntanALosClientes,
  entoncesLosPedidosNoCambiaron,
  entoncesLosStocksSon,
  entoncesQuedaUnaSolaSalida,
  entoncesQuedanAlfajores,
  entoncesQuedanLadoALadoEmpezandoPorLosClientes,
  entoncesSeQuito,
  entoncesSeQuitoLaEntradaDeEjemplo,
  entoncesSeRechazaPorFaltaDeStock,
  entoncesSigueSiendoEjemplo,
  entoncesTraeLasPiezas,
} from '@/test/plantillasDelMotor';

type PiezasDePlantilla = {
  clave: ClavePlantilla;
  opciones: Partial<OpcionesPlantilla>;
  piezas: TipoBloque[];
};

const PIEZAS_POR_PLANTILLA: PiezasDePlantilla[] = [
  { clave: 'shop', opciones: {}, piezas: ['inventory', 'sales', 'clients'] },
  { clave: 'warehouse', opciones: {}, piezas: ['inventory', 'entries', 'exits'] },
  { clave: 'business', opciones: {}, piezas: ['orders', 'clients', 'inventory'] },
  {
    clave: 'shop',
    opciones: { movements: true },
    piezas: ['inventory', 'sales', 'clients', 'entries'],
  },
  {
    clave: 'business',
    opciones: { movements: true, expenses: true },
    piezas: ['orders', 'clients', 'inventory', 'entries', 'exits', 'expenses'],
  },
];

describe('plantillas', () => {
  it.each(PIEZAS_POR_PLANTILLA)(
    'la plantilla $clave con $opciones trae $piezas',
    ({ clave, opciones, piezas }) => {
      cuandoSeArmaLaPlantilla(clave, opciones);

      entoncesTraeLasPiezas(piezas);
    },
  );

  it('en el negocio los clientes y los pedidos van a media pantalla y el inventario ocupa todo', () => {
    cuandoSeArmaLaPlantilla('business');

    entoncesLosAnchosSon({ clients: 'half', orders: 'half', inventory: 'full' });
  });

  it('los pedidos del negocio apuntan a sus clientes', () => {
    cuandoSeArmaLaPlantilla('business');

    entoncesLosPedidosApuntanALosClientes();
  });

  it('las entradas y las salidas del depósito apuntan a su inventario', () => {
    cuandoSeArmaLaPlantilla('warehouse', {}, false);

    entoncesLasEntradasYSalidasApuntanAlInventario();
  });

  it('las ventas del almacén apuntan al inventario y a los clientes, y descuentan stock', () => {
    cuandoSeArmaLaPlantilla('shop');

    entoncesLasVentasApuntanAlInventarioYALosClientesYDescuentanStock();
  });

  it('las opciones que no se piden quedan apagadas', () => {
    cuandoSeArmaLaPlantilla('warehouse', { expenses: true });

    entoncesLasOpcionesSon({ movements: false, expenses: true });
  });

  it('el primer pedido de ejemplo del negocio es válido', () => {
    cuandoSeArmaLaPlantilla('business');

    entoncesElPrimerPedidoEsValido();
  });
});

describe('ventas', () => {
  it('elegir un producto sugiere su precio', () => {
    dadaLaPlantilla('shop');

    cuandoSeVendenFiadoTresDelPrimerProductoSinPrecio();

    entoncesLaVentaTieneElPrecio(3600);
  });

  it('vender descuenta lo vendido del producto', () => {
    dadaLaPlantilla('shop');

    cuandoSeVendenFiadoTresDelPrimerProductoSinPrecio();

    entoncesElPrimerProductoBajo(3);
  });

  it('venderle fiado a un cliente de ejemplo lo vuelve real', () => {
    dadaLaPlantilla('shop');

    cuandoSeVendenFiadoTresDelPrimerProductoSinPrecio();

    entoncesElClienteYaNoEsEjemplo();
  });

  it('un precio escrito a mano se conserva', () => {
    dadaLaPlantilla('shop');

    cuandoSeVendeUnoConPrecioManual(3900);

    entoncesLaVentaTieneElPrecio(3900);
  });

  it('los ingresos suman las ventas con precio sugerido y con precio manual', () => {
    dadaLaPlantilla('shop');

    cuandoSeVendenTresConPrecioSugeridoYUnoManual(3900);

    entoncesLosIngresosSon(3 * 3600 + 3900);
  });

  it('una venta descuenta del stock', () => {
    dadoUnAlmacenConAlfajoresYUnaVentaDe(10, 4);

    entoncesQuedanAlfajores(6);
  });

  it('no deja vender más de lo que hay', () => {
    dadoUnAlmacenConAlfajoresYUnaVentaDe(10, 4);

    cuandoSeIntentaVender(20);

    entoncesSeRechazaPorFaltaDeStock();
  });

  it('una venta rechazada no toca el stock', () => {
    dadoUnAlmacenConAlfajoresYUnaVentaDe(10, 4);

    cuandoSeIntentaVender(20);

    entoncesQuedanAlfajores(6);
  });

  it('corregir una venta revierte la cantidad anterior', () => {
    dadoUnAlmacenConAlfajoresYUnaVentaDe(10, 4);

    cuandoSeCorrigeLaVentaA(2);

    entoncesQuedanAlfajores(8);
  });

  it('quitar una venta devuelve el stock', () => {
    dadoUnAlmacenConAlfajoresYUnaVentaDe(10, 4);

    cuandoSeQuitaLaVenta();

    entoncesQuedanAlfajores(10);
  });

  it('quitar una venta avisa que se quitó', () => {
    dadoUnAlmacenConAlfajoresYUnaVentaDe(10, 4);

    cuandoSeQuitaLaVenta();

    entoncesSeQuito();
  });

  it('quitar una venta de ejemplo no vuelve real a su producto de ejemplo', () => {
    dadosUnProductoDeEjemploYOtroRealConUnaVentaCadaUno();

    cuandoSeQuitanAmbasVentas();

    entoncesSigueSiendoEjemplo('Alfajor');
  });

  it('quitar una venta real vuelve real a su producto', () => {
    dadosUnProductoDeEjemploYOtroRealConUnaVentaCadaUno();

    cuandoSeQuitanAmbasVentas();

    entoncesDejoDeSerEjemplo('Galletitas');
  });
});

describe('entradas y salidas', () => {
  it('las entradas suman al stock y las salidas restan', () => {
    dadoUnDepositoConAzucarConUnaEntradaYUnaSalida(10, 5, 4);

    entoncesHayAzucar(11);
  });

  it('no deja sacar más de lo que hay', () => {
    dadoUnDepositoConAzucarConUnaEntradaYUnaSalida(10, 5, 4);

    cuandoSeIntentaSacar(20);

    entoncesSeRechazaPorFaltaDeStock();
  });

  it('una salida rechazada no se guarda ni toca el stock', () => {
    dadoUnDepositoConAzucarConUnaEntradaYUnaSalida(10, 5, 4);

    cuandoSeIntentaSacar(20);

    entoncesQuedaUnaSolaSalida();
  });

  it('corregir una salida revierte la cantidad anterior', () => {
    dadoUnDepositoConAzucarConUnaEntradaYUnaSalida(10, 5, 4);

    cuandoSeCorrigeLaSalidaA(2);

    entoncesHayAzucar(13);
  });

  it('quitar la salida y la entrada deja el stock como al principio', () => {
    dadoUnDepositoConAzucarConUnaEntradaYUnaSalida(10, 5, 4);

    cuandoSeQuitanLaSalidaYLaEntrada();

    entoncesHayAzucar(10);
  });

  it('cambiar el producto de una entrada pasa el efecto al producto nuevo', () => {
    dadosDosProductosYUnaEntradaDelPrimero(5, 3, 7);

    cuandoLaEntradaPasaAlSegundoProductoCon(4);

    entoncesLosStocksSon({ A: 5, B: 7 });
  });

  it('no deja quitar una entrada que ya se consumió', () => {
    dadaUnaEntradaDelSegundoProductoYaConsumida();

    cuandoSeQuitaLaEntrada();

    entoncesSeRechazaPorFaltaDeStock();
  });

  it('una entrada que no se pudo quitar no toca el stock', () => {
    dadaUnaEntradaDelSegundoProductoYaConsumida();

    cuandoSeQuitaLaEntrada();

    entoncesLosStocksSon({ B: 1 });
  });
});

describe('quitar ejemplos y bloques de movimientos', () => {
  it('quitar los ejemplos borra las entradas de ejemplo', () => {
    dadaHarinaConUnaEntradaDeEjemplo(10, 5);

    cuandoSeQuitanLosEjemplos();

    entoncesSeQuitoLaEntradaDeEjemplo();
  });

  it('quitar los ejemplos revierte el stock que movieron', () => {
    dadaHarinaConUnaEntradaDeEjemplo(10, 5);

    cuandoSeQuitanLosEjemplos();

    entoncesHayHarina(10);
  });

  it('no deja quitar las entradas si las salidas ya las consumieron', () => {
    dadaHarinaConEntradasYaConsumidas(10, 5, 12);

    cuandoSeQuitaElBloqueDe('entries');

    entoncesSeRechazaPorFaltaDeStock();
  });

  it('un bloque que no se pudo quitar conserva sus registros y el stock', () => {
    dadaHarinaConEntradasYaConsumidas(10, 5, 12);

    cuandoSeQuitaElBloqueDe('entries');

    entoncesLasEntradasSiguenCargadas();
  });

  it('quitar el bloque de salidas devuelve su stock', () => {
    dadaHarinaConEntradasYaConsumidas(10, 5, 12);

    cuandoSeQuitaElBloqueDe('exits');

    entoncesHayHarina(15);
  });

  it('sin salidas, quitar las entradas revierte su stock', () => {
    dadaHarinaConEntradasYaConsumidas(10, 5, 12);

    cuandoSeQuitanLasSalidasYDespuesLasEntradas();

    entoncesHayHarina(10);
  });

  it('sin salidas, el bloque de entradas se puede quitar', () => {
    dadaHarinaConEntradasYaConsumidas(10, 5, 12);

    cuandoSeQuitanLasSalidasYDespuesLasEntradas();

    entoncesSeQuito();
  });
});

describe('distribución', () => {
  it('poner una pieza al lado de otra las deja a media pantalla en la misma fila', () => {
    dadaLaPlantilla('business');

    cuandoSePoneElStockAlLadoDeLosClientes();

    entoncesQuedanLadoALadoEmpezandoPorLosClientes();
  });

  it('poner una pieza al lado de otra no toca los registros', () => {
    dadaLaPlantilla('business');

    cuandoSePoneElStockAlLadoDeLosClientes();

    entoncesLosPedidosNoCambiaron();
  });

  it('volver al mosaico deshace la fila compartida', () => {
    dadaLaPlantilla('business');

    cuandoSeSeparaYSeVuelveAlMosaico();

    entoncesElStockVuelveAOcuparTodoElAncho();
  });
});
