import { describe, it } from 'vitest';
import {
  agregarPieza,
  cargarEjemplos,
  cargarRegistros,
  cuandoSamiPide,
  dadoUnSistemaVacio,
  entoncesNoCambioNada,
  entoncesSeAplico,
  entoncesSeOmitio,
  entoncesSoloSeHizo,
  entoncesSoloSeOmitio,
} from '@/test/accionesDeSami';
import {
  cuandoSamiCargaSieteEjemplosDeClientes,
  cuandoSeArmanClientasYTurnosConEjemplosQueLasNombran,
  cuandoSeArmanInventarioVentasYClientesConEjemplosQueSeNombran,
  cuandoSeBuscanLosPendientesVigentes,
  cuandoSeCarganGastosConFechasComoEnUnCuaderno,
  cuandoSeCarganMasDeTreintaProductos,
  cuandoSeCarganTresClientes,
  cuandoSeCarganVentasDeEjemploPorNombreYPorUnIdQueYaNoExiste,
  cuandoSeConectanPedidosConLosClientesQueYaTraeDeFabrica,
  cuandoSeConectanPedidosConVelasDeVariasFormas,
  cuandoSeConectanTurnosConClientesPorUnDatoEscrito,
  cuandoSeSumanEsosPendientes,
  cuandoSeSumanTablasYClientesRepetidos,
  cuandoSeSumanTurnosConEjemplosDeMas,
  cuandoSeSumanVelasConAromaYEjemplos,
  dadaUnaVentaPendienteDeMartaCargadaAManoYOtraDeNadiaBorrada,
  dadasVelasClientasYVentasSinFilas,
  dadasVelasConUnSoloEjemploValido,
  dadasVentasDeEjemploDeYerba,
  dadosClientesConUnaClientaReal,
  dadosClientesYUnaNota,
  dadosGastosSinEjemplos,
  dadosPendientesDeDosTurnosConUnaVentaYaVinculada,
  dadoUnInventarioVacio,
  entoncesElClienteEscritoDeLosTurnosPasaASerElVinculo,
  entoncesElPrimerProductoQuedaConCadaValorEnSuTipo,
  entoncesElPrimerTurnoQuedaConCadaValorEnSuTipo,
  entoncesElProductoDeLaVentaQuedaVacio,
  entoncesLaClientaRealSigueAntesQueLosEjemplos,
  entoncesLaPiezaTieneFilas,
  entoncesLaPiezaTieneVinculos,
  entoncesLasFechasSeEntiendenYLasQueNoExistenSeDescartan,
  entoncesLasVelasSonUnInventarioConElAromaAlFinal,
  entoncesLasVelasTienenSoloElEjemploDeSami,
  entoncesLasVentasApuntanALoSumadoYLaDelIdViejoQuedaVacia,
  entoncesLaVelaYLaClientaNombradasSeSumanComoEjemplo,
  entoncesLaVentaApuntaAlProductoYAlClienteYSigueFiada,
  entoncesLaVentaQuedaConLaClientaQueSeCargoAMano,
  entoncesLosEncargosQuedanSolosEnLaPestanaAgenda,
  entoncesLosNombresDe,
  entoncesLosPedidosTienenUnSoloVinculoConLasVelas,
  entoncesLosPendientesVigentesSon,
  entoncesLosRegistrosNuevosSonRealesConSusDatos,
  entoncesLosTurnosApuntanASusClientas,
  entoncesNoSeCreaLaClientaDeUnaVentaBorrada,
  entoncesQuienNoEstabaSeSumaComoClientaDeEjemplo,
  entoncesSeAplicoEntreOtras,
  entoncesSeCargaronLosPrimerosTreinta,
  entoncesTodasLasFilasSonEjemplos,
} from '@/test/accionesDesdeCero';
import {
  cuandoSeCarganTresVentasPorNombre,
  cuandoSeSumanLosPendientes,
  dadoUnCuadernoConVelasDeEjemploYUnaClientaReal,
  entoncesLasVentasQuedanVinculadasALosRegistrosNuevos,
  entoncesLosFaltantesQuedanPendientesUnaSolaVez,
  entoncesNoSeCreoNingunRegistroNuevo,
  entoncesSoloRosaQuedaVinculada,
} from '@/test/vinculosPorNombre';

const CINCO_CLIENTES = ['Cliente 0', 'Cliente 1', 'Cliente 2', 'Cliente 3', 'Cliente 4'];

describe('sumar una pieza con nombre, datos y ejemplos', () => {
  it('suma el componente con su nombre', () => {
    cuandoSeSumanVelasConAromaYEjemplos();

    entoncesSeAplico('Sumé velas.');
  });

  it('omite los datos repetidos o de un tipo que no existe', () => {
    cuandoSeSumanVelasConAromaYEjemplos();

    entoncesSeOmitio('«producto» ya está.', 'No pude sumar ese dato.');
  });

  it('la pieza es del tipo pedido y los datos nuevos van al final', () => {
    cuandoSeSumanVelasConAromaYEjemplos();

    entoncesLasVelasSonUnInventarioConElAromaAlFinal();
  });

  it('lleva las filas de ejemplo de Sami en lugar de las del catálogo', () => {
    cuandoSeSumanVelasConAromaYEjemplos();

    entoncesLasVelasTienenSoloElEjemploDeSami();
  });

  it('carga como mucho cinco filas de ejemplo', () => {
    cuandoSeSumanTurnosConEjemplosDeMas();

    entoncesLaPiezaTieneFilas('Turnos', 5);
  });

  it('convierte cada valor al tipo del dato y descarta lo que no sirve', () => {
    cuandoSeSumanTurnosConEjemplosDeMas();

    entoncesElPrimerTurnoQuedaConCadaValorEnSuTipo();
  });

  it('sin ejemplos usa las filas del catálogo', () => {
    dadoUnSistemaVacio();

    cuandoSamiPide(agregarPieza('expenses', { nombre: 'Insumos' }));

    entoncesTodasLasFilasSonEjemplos('Insumos');
  });

  it('la tabla a medida se puede repetir con otro nombre', () => {
    cuandoSeSumanTablasYClientesRepetidos();

    entoncesSeAplico('Sumé turnos.', 'Sumé encargos en la pestaña Agenda.', 'Sumé clientes.');
  });

  it('las demás piezas y los nombres no se repiten', () => {
    cuandoSeSumanTablasYClientesRepetidos();

    entoncesSeOmitio(
      'Ya hay una pieza «turnos».',
      'Ya hay una pieza «Encargos».',
      'Esa pieza ya está.',
    );
  });

  it('una pieza pedida en otra pestaña la crea y va ahí', () => {
    cuandoSeSumanTablasYClientesRepetidos();

    entoncesLosEncargosQuedanSolosEnLaPestanaAgenda();
  });

  it('descarta los ejemplos que no traen el dato principal', () => {
    dadasVelasConUnSoloEjemploValido();

    entoncesLosNombresDe('Velas', ['Vela lavanda']);
  });
});

describe('cargar ejemplos', () => {
  it('si ninguna fila trae el dato principal, se omite', () => {
    dadasVelasConUnSoloEjemploValido();

    cuandoSamiPide(cargarEjemplos('Velas', [{ Cantidad: 5 }, { Precio: 900 }]));

    entoncesSeOmitio('No pude cargar esos ejemplos en velas.');
  });

  it('si no se pudo cargar, conserva los ejemplos que había', () => {
    dadasVelasConUnSoloEjemploValido();

    cuandoSamiPide(cargarEjemplos('Velas', [{ Cantidad: 5 }, { Precio: 900 }]));

    entoncesLosNombresDe('Velas', ['Vela lavanda']);
  });

  it('carga hasta cinco ejemplos', () => {
    dadosClientesConUnaClientaReal();

    cuandoSamiCargaSieteEjemplosDeClientes();

    entoncesSeAplico('Cargué 5 ejemplos en clientes.');
  });

  it('reemplaza los ejemplos de la pieza y deja las filas reales', () => {
    dadosClientesConUnaClientaReal();

    cuandoSamiCargaSieteEjemplosDeClientes();

    entoncesLaClientaRealSigueAntesQueLosEjemplos(CINCO_CLIENTES);
  });

  it('omite una pieza que no existe, filas vacías o una pieza que no lleva filas', () => {
    dadosClientesYUnaNota();

    cuandoSamiPide(
      cargarEjemplos('nada', [{ Nombre: 'X' }]),
      cargarEjemplos('clients', []),
      cargarEjemplos('clients', [{ Color: 'Rojo' }]),
      cargarEjemplos('note', [{ Nombre: 'X' }]),
    );

    entoncesSoloSeOmitio(
      'No encontré esa pieza.',
      'No pude cargar esos ejemplos en clientes.',
      'No pude cargar esos ejemplos en clientes.',
      '«Nota» no lleva filas.',
    );
  });
});

describe('cargar registros reales', () => {
  it('entiende las fechas como se anotan en un cuaderno y descarta las que no existen', () => {
    dadosGastosSinEjemplos();

    cuandoSeCarganGastosConFechasComoEnUnCuaderno();

    entoncesLasFechasSeEntiendenYLasQueNoExistenSeDescartan();
  });

  it('avisa cuántos registros cargó', () => {
    dadosClientesConUnaClientaReal([{ Nombre: 'Ejemplo' }]);

    cuandoSeCarganTresClientes();

    entoncesSoloSeHizo('Cargué 3 registros en clientes.');
  });

  it('suma los registros a lo que ya había y reemplaza los ejemplos de esa pieza', () => {
    dadosClientesConUnaClientaReal([{ Nombre: 'Ejemplo' }]);

    cuandoSeCarganTresClientes();

    entoncesLosNombresDe('Clientes', ['Rosa', 'Lucía', 'Marta', 'Pedro']);
  });

  it('los registros nuevos son reales y guardan sus datos', () => {
    dadosClientesConUnaClientaReal([{ Nombre: 'Ejemplo' }]);

    cuandoSeCarganTresClientes();

    entoncesLosRegistrosNuevosSonRealesConSusDatos();
  });

  it('carga como mucho treinta registros', () => {
    dadoUnInventarioVacio();

    cuandoSeCarganMasDeTreintaProductos();

    entoncesSeAplico('Cargué 30 registros en inventario.');
  });

  it('descarta las filas sin el dato principal y se queda con las primeras treinta', () => {
    dadoUnInventarioVacio();

    cuandoSeCarganMasDeTreintaProductos();

    entoncesSeCargaronLosPrimerosTreinta();
  });

  it('convierte los valores como los ejemplos', () => {
    dadoUnInventarioVacio();

    cuandoSeCarganMasDeTreintaProductos();

    entoncesElPrimerProductoQuedaConCadaValorEnSuTipo();
  });

  it('omite una pieza que no existe, una que no lleva filas o filas sin el dato principal', () => {
    dadosClientesYUnaNota();

    cuandoSamiPide(
      cargarRegistros('nada', [{ Nombre: 'X' }]),
      cargarRegistros('clients', []),
      cargarRegistros('clients', [{ Teléfono: '11 5555-0000' }]),
      cargarRegistros('note', [{ Nombre: 'X' }]),
    );

    entoncesSoloSeOmitio(
      'No encontré esa pieza.',
      'No pude cargar esos registros en clientes.',
      'No pude cargar esos registros en clientes.',
      '«Nota» no lleva filas.',
    );
  });

  it('lo que no se pudo cargar deja el sistema igual', () => {
    dadosClientesYUnaNota();

    cuandoSamiPide(cargarRegistros('clients', [{ Teléfono: '11 5555-0000' }]));

    entoncesNoCambioNada();
  });

  it('una venta se vincula con la clienta real que ya está cargada', () => {
    dadoUnCuadernoConVelasDeEjemploYUnaClientaReal();

    cuandoSeCarganTresVentasPorNombre();

    entoncesSoloRosaQuedaVinculada();
  });

  it('un nombre que no existe no crea ningún registro nuevo', () => {
    dadoUnCuadernoConVelasDeEjemploYUnaClientaReal();

    cuandoSeCarganTresVentasPorNombre();

    entoncesNoSeCreoNingunRegistroNuevo();
  });

  it('los nombres que faltan quedan pendientes una sola vez cada uno', () => {
    dadoUnCuadernoConVelasDeEjemploYUnaClientaReal();

    cuandoSeCarganTresVentasPorNombre();

    entoncesLosFaltantesQuedanPendientesUnaSolaVez();
  });

  it('al sumar los pendientes las ventas quedan vinculadas a los registros nuevos', () => {
    dadoUnCuadernoConVelasDeEjemploYUnaClientaReal();
    cuandoSeCarganTresVentasPorNombre();

    cuandoSeSumanLosPendientes();

    entoncesLasVentasQuedanVinculadasALosRegistrosNuevos();
  });
});

describe('los pendientes', () => {
  it('sumarlos usa un registro que se cargó mientras tanto', () => {
    dadaUnaVentaPendienteDeMartaCargadaAManoYOtraDeNadiaBorrada();

    cuandoSeSumanEsosPendientes();

    entoncesLaVentaQuedaConLaClientaQueSeCargoAMano();
  });

  it('sumarlos ignora las filas que ya no están', () => {
    dadaUnaVentaPendienteDeMartaCargadaAManoYOtraDeNadiaBorrada();

    cuandoSeSumanEsosPendientes();

    entoncesNoSeCreaLaClientaDeUnaVentaBorrada();
  });

  it('los vigentes descartan las filas ya vinculadas y las piezas que no están, y juntan los repetidos', () => {
    dadosPendientesDeDosTurnosConUnaVentaYaVinculada();

    cuandoSeBuscanLosPendientesVigentes();

    entoncesLosPendientesVigentesSon([
      ['Yerba', 1],
      ['Marta', 1],
      ['Mate', 2],
      ['Nadia', 1],
    ]);
  });
});

describe('conectar piezas', () => {
  it('suma un vínculo con el nombre de la otra pieza', () => {
    cuandoSeConectanPedidosConVelasDeVariasFormas();

    entoncesSeAplicoEntreOtras('Conecté pedidos con velas.');
  });

  it('no conecta dos veces, consigo misma ni con una pieza que no está', () => {
    cuandoSeConectanPedidosConVelasDeVariasFormas();

    entoncesSeOmitio(
      'Pedidos ya está conectada con velas.',
      'Una pieza no se conecta consigo misma.',
      'No encontré las piezas para conectar.',
    );
  });

  it('el vínculo nuevo apunta a la otra pieza y no se duplica', () => {
    cuandoSeConectanPedidosConVelasDeVariasFormas();

    entoncesLosPedidosTienenUnSoloVinculoConLasVelas();
  });

  it('un vínculo que la pieza ya trae de fábrica no se suma otra vez', () => {
    cuandoSeConectanPedidosConLosClientesQueYaTraeDeFabrica();

    entoncesSeOmitio('Pedidos ya está conectada con clientes.');
  });

  it('la pieza queda con su único vínculo de fábrica', () => {
    cuandoSeConectanPedidosConLosClientesQueYaTraeDeFabrica();

    entoncesLaPiezaTieneVinculos('Pedidos', 1);
  });

  it('el dato principal no se convierte en vínculo', () => {
    cuandoSeConectanTurnosConClientesPorUnDatoEscrito();

    entoncesSeOmitio('«Nombre» ya está en turnos.');
  });

  it('un dato escrito con la etiqueta del vínculo pasa a ser el vínculo', () => {
    cuandoSeConectanTurnosConClientesPorUnDatoEscrito();

    entoncesElClienteEscritoDeLosTurnosPasaASerElVinculo();
  });
});

describe('vínculos por nombre en el mismo turno', () => {
  it('lo que se suma en el turno se puede nombrar en las acciones siguientes', () => {
    cuandoSeArmanClientasYTurnosConEjemplosQueLasNombran();

    entoncesSeOmitio();
  });

  it('un ejemplo que nombra a alguien que no está lo suma como ejemplo para que el vínculo se vea', () => {
    cuandoSeArmanClientasYTurnosConEjemplosQueLasNombran();

    entoncesQuienNoEstabaSeSumaComoClientaDeEjemplo();
  });

  it('los ejemplos se vinculan por nombre sin importar mayúsculas ni tildes', () => {
    cuandoSeArmanClientasYTurnosConEjemplosQueLasNombran();

    entoncesLosTurnosApuntanASusClientas();
  });

  it('si la pieza vinculada está vacía, el ejemplo suma ahí lo que nombra', () => {
    dadasVelasClientasYVentasSinFilas();

    cuandoSeCarganVentasDeEjemploPorNombreYPorUnIdQueYaNoExiste();

    entoncesLaVelaYLaClientaNombradasSeSumanComoEjemplo();
  });

  it('un id que ya no existe deja el vínculo vacío', () => {
    dadasVelasClientasYVentasSinFilas();

    cuandoSeCarganVentasDeEjemploPorNombreYPorUnIdQueYaNoExiste();

    entoncesLasVentasApuntanALoSumadoYLaDelIdViejoQuedaVacia();
  });

  it('los vínculos del catálogo (producto y cliente de una venta) también se resuelven por nombre', () => {
    cuandoSeArmanInventarioVentasYClientesConEjemplosQueSeNombran();

    entoncesLaVentaApuntaAlProductoYAlClienteYSigueFiada();
  });

  it('si cambian los ejemplos de la pieza vinculada, el vínculo que quedó sin registro se vacía', () => {
    dadasVentasDeEjemploDeYerba();

    cuandoSamiPide(cargarEjemplos('inventory', [{ Producto: 'Fideos' }]));

    entoncesElProductoDeLaVentaQuedaVacio();
  });
});
