import { describe, it } from 'vitest';
import {
  agregarPieza,
  cambiarDistribucion,
  cuandoSamiPide,
  dadoUnAlmacenDeEjemplo,
  entoncesSeAplicaron,
  entoncesSeAplico,
  entoncesSeOmitieron,
  entoncesSeOmitio,
  entoncesSoloSeOmitio,
  idDeLaPieza,
  pedido,
  quitarPieza,
  renombrarPieza,
  renombrarSistema,
  separarEnSeccion,
} from '@/test/accionesDeSami';
import {
  cuandoDejaDeExistirLaPiezaDeTipo,
  cuandoSeCopiaConOtraFecha,
  cuandoSeCopiaConOtroNombre,
  cuandoSeResume,
  cuandoSeResumeConLaPrimeraVentaYaGuardada,
  cuandoSeResumeConTodoGuardado,
  dadaUnaTablaDeTurnosConectadaALosClientes,
  dadoUnAlmacenConDosVentasCargadas,
  dadoUnAlmacenConUnaVentaAAntoGuardada,
  dadoUnAlmacenConUnaVentaDeUnProductoQueNoExiste,
  dadoUnAlmacenConOtraPiezaDeCadaTipoObligatorio,
  dadoUnAlmacenDeEjemploDeUnaSolaColumna,
  dadoUnAlmacenSinPestanas,
  entoncesElClienteDeLasVentasVuelveASerTextoSinValores,
  entoncesElProductoDeLasVentasSigueSiendoUnVinculoSinDestino,
  entoncesElResumenEsDeUnaColumnaConTodoEnLaPrimeraPestana,
  entoncesElResumenNoLlevaLasFilasDeEjemplo,
  entoncesElResumenTraeNombreDistribucionYPiezas,
  entoncesHayPiezas,
  entoncesHayUnaSolaPestanaLlamada,
  entoncesLaClientaDeLosTurnosVuelveASerTexto,
  entoncesLaCopiaEsElMismoSistema,
  entoncesLaCopiaEsOtroSistema,
  entoncesLaPestanaTieneLasPiezas,
  entoncesLaPiezaNoTieneDatos,
  entoncesLaPiezaTraeEjemplos,
  entoncesLaPiezaYElSistemaSeLlaman,
  entoncesLaPrimeraVentaViajaConSusDatosYSuClienta,
  entoncesLasPiezasVinculadasLlevanLosNombresQueTienen,
  entoncesLaVentaViajaSoloConSuCantidad,
  entoncesLoGuardadoNoViajaComoRegistro,
  entoncesLosNombresMiden,
  entoncesLosProductosSumadosAlVincularViajanSinStock,
  entoncesSoloViajaLaVentaNueva,
  entoncesTodasLasPiezasOcupanTodoElAncho,
  entoncesUnaPiezaQueNadieVinculaNoLlevaNombres,
} from '@/test/accionesSobrePiezas';

describe('el resumen del sistema que se manda a Sami', () => {
  it('trae el nombre, la distribución y las piezas con sus datos', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSeResume();

    entoncesElResumenTraeNombreDistribucionYPiezas();
  });

  it('no lleva las filas de ejemplo', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSeResume();

    entoncesElResumenNoLlevaLasFilasDeEjemplo();
  });

  it('lleva la distribución elegida y la pestaña de cada pieza', () => {
    dadoUnAlmacenDeEjemploDeUnaSolaColumna();

    cuandoSeResume();

    entoncesElResumenEsDeUnaColumnaConTodoEnLaPrimeraPestana();
  });

  it('lleva los registros del armado pero no los que ya estaban guardados', () => {
    dadoUnAlmacenConDosVentasCargadas();

    cuandoSeResumeConLaPrimeraVentaYaGuardada();

    entoncesSoloViajaLaVentaNueva();
  });

  it('cada registro viaja con su id y sus datos por etiqueta, también el nombre de su vínculo', () => {
    dadoUnAlmacenConDosVentasCargadas();

    cuandoSeResume();

    entoncesLaPrimeraVentaViajaConSusDatosYSuClienta();
  });

  it('los productos que se sumaron al vincular las ventas viajan como registros sin stock', () => {
    dadoUnAlmacenConDosVentasCargadas();

    cuandoSeResumeConLaPrimeraVentaYaGuardada();

    entoncesLosProductosSumadosAlVincularViajanSinStock();
  });

  it('las piezas con las que otra se vincula llevan los nombres que ya tienen, también los guardados', () => {
    dadoUnAlmacenConUnaVentaAAntoGuardada();

    cuandoSeResumeConTodoGuardado();

    entoncesLasPiezasVinculadasLlevanLosNombresQueTienen();
  });

  it('lo que ya estaba guardado no viaja como registro', () => {
    dadoUnAlmacenConUnaVentaAAntoGuardada();

    cuandoSeResumeConTodoGuardado();

    entoncesLoGuardadoNoViajaComoRegistro();
  });

  it('una pieza con la que nadie se vincula no lleva nombres', () => {
    dadoUnAlmacenConUnaVentaAAntoGuardada();

    cuandoSeResumeConTodoGuardado();

    entoncesUnaPiezaQueNadieVinculaNoLlevaNombres();
  });

  it('un vínculo que no apunta a ninguna fila no viaja', () => {
    dadoUnAlmacenConUnaVentaDeUnProductoQueNoExiste();

    cuandoSeResume();

    entoncesLaVentaViajaSoloConSuCantidad();
  });
});

describe('agregar y quitar piezas', () => {
  it('suma una pieza del catálogo', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(agregarPieza('expenses'));

    entoncesSeAplico('Sumé gastos.');
  });

  it('la pieza sumada trae los ejemplos del catálogo', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(agregarPieza('expenses'));

    entoncesLaPiezaTraeEjemplos('expenses');
  });

  it('no suma una pieza repetida ni de un tipo que no es del catálogo', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(agregarPieza('inventory'), agregarPieza('turnos'));

    entoncesSoloSeOmitio('Esa pieza ya está.', 'No existe una pieza «turnos».');
  });

  it('lo que no se pudo sumar no agrega piezas', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(agregarPieza('inventory'), agregarPieza('turnos'));

    entoncesHayPiezas(3);
  });

  it('suma también la nota y el resumen', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      agregarPieza('note', { nombre: 'Para recordar' }),
      agregarPieza('summary', { campos: [{ etiqueta: 'X', tipo: 'text' }] }),
    );

    entoncesSeAplico('Sumé para recordar.', 'Sumé resumen.');
  });

  it('el resumen no lleva datos propios aunque Sami se los pida', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(agregarPieza('summary', { campos: [{ etiqueta: 'X', tipo: 'text' }] }));

    entoncesLaPiezaNoTieneDatos('summary');
  });

  it('no puede sumar nada si no hay pestañas', () => {
    dadoUnAlmacenSinPestanas();

    cuandoSamiPide(agregarPieza('expenses'));

    entoncesSeOmitio('No hay dónde sumar la pieza.');
  });

  it('quita una pieza por id, por tipo o por nombre', () => {
    dadoUnAlmacenConOtraPiezaDeCadaTipoObligatorio();

    cuandoSamiPide(
      quitarPieza(idDeLaPieza('clients')),
      quitarPieza('sales'),
      quitarPieza('inventario'),
    );

    entoncesSeAplicaron(3);
  });

  it('no quita la última pieza de un tipo que el negocio necesita', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(
      quitarPieza(idDeLaPieza('clients')),
      quitarPieza('sales'),
      quitarPieza('inventario'),
    );

    entoncesHayPiezas(3);
  });

  it('no quita una pieza que no existe', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(quitarPieza('fantasma'));

    entoncesSeOmitio('No encontré esa pieza.');
  });

  it('si faltan los clientes, el cliente de las ventas vuelve a ser un texto sin valores', () => {
    dadaUnaTablaDeTurnosConectadaALosClientes();

    cuandoDejaDeExistirLaPiezaDeTipo('clients');

    entoncesElClienteDeLasVentasVuelveASerTextoSinValores();
  });

  it('si faltan los clientes, un vínculo que sumó Sami también vuelve a ser texto', () => {
    dadaUnaTablaDeTurnosConectadaALosClientes();

    cuandoDejaDeExistirLaPiezaDeTipo('clients');

    entoncesLaClientaDeLosTurnosVuelveASerTexto();
  });

  it('si falta el inventario, el producto de las ventas sigue siendo un vínculo, sin destino', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoDejaDeExistirLaPiezaDeTipo('inventory');

    entoncesElProductoDeLasVentasSigueSiendoUnVinculoSinDestino();
  });
});

describe('renombrar', () => {
  it('renombra una pieza y el sistema, recortando los espacios', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(renombrarPieza('sales', '  Caja  '), renombrarSistema('  Velas de Ana '));

    entoncesLaPiezaYElSistemaSeLlaman('Caja', 'Velas de Ana');
  });

  it('recorta los nombres demasiado largos', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(renombrarPieza('sales', 'x'.repeat(80)), renombrarSistema('y'.repeat(150)));

    entoncesLosNombresMiden(60, 100);
  });

  it('omite un nombre vacío o una pieza que no existe', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(renombrarPieza('sales', ' '), renombrarPieza('nada', 'X'), renombrarSistema(''));

    entoncesSeOmitieron(3);
  });
});

describe('pestañas y distribución', () => {
  it('crea la pestaña y pasa la pieza', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(separarEnSeccion('clients', 'Mis clientes'));

    entoncesLaPestanaTieneLasPiezas('Mis clientes', ['clients']);
  });

  it('reutiliza una pestaña que ya existe sin importar mayúsculas', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(separarEnSeccion('clients', 'Aparte'), separarEnSeccion('sales', 'aparte'));

    entoncesHayUnaSolaPestanaLlamada('Aparte');
  });

  it('las piezas que van a la misma pestaña quedan juntas', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(separarEnSeccion('clients', 'Aparte'), separarEnSeccion('sales', 'aparte'));

    entoncesLaPestanaTieneLasPiezas('Aparte', ['clients', 'sales']);
  });

  it('omite una pieza inexistente o una pestaña sin nombre', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(separarEnSeccion('nada', 'X'), separarEnSeccion('clients', ' '));

    entoncesSeOmitieron(2);
  });

  it('cambia la distribución', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(cambiarDistribucion('single'));

    entoncesSeAplico('Cambié la distribución.');
  });

  it('con una sola columna todas las piezas ocupan el ancho completo', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(cambiarDistribucion('single'));

    entoncesTodasLasPiezasOcupanTodoElAncho();
  });

  it('omite una distribución que no existe', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(cambiarDistribucion('diagonal'));

    entoncesSeOmitio('No conozco esa distribución.');
  });
});

describe('herramientas que Sami no conoce', () => {
  it('una herramienta desconocida se omite', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(pedido('borrar_todo'), agregarPieza('expenses'));

    entoncesSeOmitio('No sé hacer «borrar_todo».');
  });

  it('una herramienta desconocida no impide aplicar las demás', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSamiPide(pedido('borrar_todo'), agregarPieza('expenses'));

    entoncesSeAplico('Sumé gastos.');
  });
});

describe('comparar sistemas', () => {
  it('ignora la fecha de actualización', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSeCopiaConOtraFecha();

    entoncesLaCopiaEsElMismoSistema();
  });

  it('detecta cualquier otro cambio', () => {
    dadoUnAlmacenDeEjemplo();

    cuandoSeCopiaConOtroNombre();

    entoncesLaCopiaEsOtroSistema();
  });
});
