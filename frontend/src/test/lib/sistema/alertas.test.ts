import { describe, it } from 'vitest';
import {
  cuandoSeAnalizaDosVecesElBarrio,
  cuandoSeAnalizan,
  cuandoSeAnalizanCienBarrios,
  cuandoSeAnalizanConElGrupoApagado,
  cuandoSeAnalizanDosVeces,
  cuandoSeComparan,
  cuandoSeFiltraUnaZonaDeDosComercios,
  cuandoSePregunta,
  dadaUnaYerbaCon,
  dadaUnaYerbaEnUnaZonaChica,
  dadasCanastasDeAlfajorYGalletitas,
  dadoElAlmacenDeEjemplo,
  dadosFideosAgotadosConBloqueDeEntradas,
  dadoUnSistemaSinInventario,
  entoncesCadaAvisoExplicaPorQueYTraeAcciones,
  entoncesCadaProductoDeLaZonaTieneElMinimoDeComercios,
  entoncesDanLoMismo,
  entoncesElAvisoTienePrioridad,
  entoncesElDetalleDice,
  entoncesElMinimoDeComerciosEs,
  entoncesElMotivoDice,
  entoncesElMotivoEs,
  entoncesElParecidoEs,
  entoncesElPrimeroEsUrgente,
  entoncesElPrimerPrecioEsDe,
  entoncesElTituloDice,
  entoncesElTituloEs,
  entoncesEstanOrdenadosPorUrgencia,
  entoncesHayAvisosDeTipo,
  entoncesLaPrimeraAccionEs,
  entoncesLaRespuestaDice,
  entoncesLaRespuestaSoloTraeAvisosDeTipo,
  entoncesLosBarriosConPocosComerciosNoTienenDatos,
  entoncesLosBarriosConSuficientesComerciosTienenDatos,
  entoncesLosGruposSon,
  entoncesLosIdsNoSeRepiten,
  entoncesLosIdsSonLosMismos,
  entoncesLosMotivosDicen,
  entoncesLosTitulosDeTipoSon,
  entoncesNingunAvisoNombra,
  entoncesNingunProductoDeLaZonaEs,
  entoncesNingunTituloDeTipoEs,
  entoncesNoHayAvisos,
  entoncesNoHayAvisosDelGrupo,
  entoncesNoQuedaNingunProducto,
  entoncesNoTraePrecios,
  entoncesProponeCrear,
  entoncesProponeElPrecio,
  entoncesSeDisparanLasReglas,
} from '@/test/avisosDelMotor';

const SEIS_REGLAS = [
  'stock',
  'precio_zona',
  'estancado',
  'muy_vendido',
  'combo',
  'consumo_zona',
] as const;

const PRECIOS_EN_EL_UMBRAL = [
  { precio: 3600, avisos: 0 },
  { precio: 4300, avisos: 0 },
  { precio: 3587, avisos: 1 },
  { precio: 4369, avisos: 1 },
];

const PARECIDOS = [
  { tuyo: 'Yerba Playadito 1 kg', otro: 'Yerba Playadito 500 g', valor: 1 },
  { tuyo: 'Yerba Playadito', otro: 'Yerba Taragüi', valor: 0.5 },
  { tuyo: 'Playadito Yerba', otro: 'Yerba Playadito', valor: 1 },
  { tuyo: 'Fideos', otro: 'Yerba', valor: 0 },
];

describe('el almacén de ejemplo', () => {
  it('dispara las seis reglas', () => {
    dadoElAlmacenDeEjemplo();

    cuandoSeAnalizan();

    entoncesSeDisparanLasReglas([...SEIS_REGLAS]);
  });

  it('cada aviso explica por qué y trae acciones', () => {
    dadoElAlmacenDeEjemplo();

    cuandoSeAnalizan();

    entoncesCadaAvisoExplicaPorQueYTraeAcciones();
  });

  it('primero va lo agotado', () => {
    dadoElAlmacenDeEjemplo();

    cuandoSeAnalizan();

    entoncesElPrimeroEsUrgente('Te quedaste sin Aceite Natura 900 ml');
  });

  it('ordena por urgencia', () => {
    dadoElAlmacenDeEjemplo();

    cuandoSeAnalizan();

    entoncesEstanOrdenadosPorUrgencia();
  });

  it('los ids no se repiten', () => {
    dadoElAlmacenDeEjemplo();

    cuandoSeAnalizan();

    entoncesLosIdsNoSeRepiten();
  });

  it('los ids son iguales si los datos no cambian', () => {
    dadoElAlmacenDeEjemplo();

    cuandoSeAnalizanDosVeces();

    entoncesLosIdsSonLosMismos();
  });

  it('agrupa en «Tu stock», «Precios y ventas» y «Tu zona»', () => {
    dadoElAlmacenDeEjemplo();

    cuandoSeAnalizan();

    entoncesLosGruposSon(['stock', 'ventas', 'zona']);
  });

  it('un grupo apagado no avisa', () => {
    dadoElAlmacenDeEjemplo();

    cuandoSeAnalizanConElGrupoApagado('stock');

    entoncesNoHayAvisosDelGrupo('stock');
  });

  it('con la zona apagada no sugiere lo que se vende cerca', () => {
    dadoElAlmacenDeEjemplo();

    cuandoSeAnalizanConElGrupoApagado('zona');

    entoncesHayAvisosDeTipo('consumo_zona', 0);
  });

  it('sólo ofrece sumar lo que se vende cerca', () => {
    dadoElAlmacenDeEjemplo();

    cuandoSeAnalizan();

    entoncesLosTitulosDeTipoSon('consumo_zona', [
      'En tu zona se vende Leche La Serenísima 1 L y no lo tenés',
      'En tu zona se vende Agua Villavicencio 2 L y no lo tenés',
    ]);
  });

  it('no nombra productos que vende un solo comercio', () => {
    dadoElAlmacenDeEjemplo();

    cuandoSeAnalizan();

    entoncesNingunAvisoNombra('Fernet');
  });
});

describe('stock', () => {
  it('por debajo del mínimo avisa para hoy', () => {
    dadaUnaYerbaCon({ quantity: 8 });

    cuandoSeAnalizan();

    entoncesElAvisoTienePrioridad('stock', 2);
  });

  it('agotado avisa como urgente', () => {
    dadaUnaYerbaCon({ quantity: 0 });

    cuandoSeAnalizan();

    entoncesElAvisoTienePrioridad('stock', 3);
  });

  it('por encima del mínimo no avisa', () => {
    dadaUnaYerbaCon({ quantity: 11 });

    cuandoSeAnalizan();

    entoncesHayAvisosDeTipo('stock', 0);
  });

  it('con ventas, calcula para cuántos días alcanza', () => {
    dadaUnaYerbaCon({ quantity: 6 }, [
      [1, 30],
      [10, 30],
    ]);

    cuandoSeAnalizan();

    entoncesElMotivoEs('stock', 0, 'Te alcanza para 3 días.');
  });

  it('con ventas, calcula cuánto pedir para dos semanas', () => {
    dadaUnaYerbaCon({ quantity: 6 }, [
      [1, 30],
      [10, 30],
    ]);

    cuandoSeAnalizan();

    entoncesElMotivoDice('stock', 1, 'pedir 22');
  });

  it('sugiere registrar una entrada si hay bloque de entradas', () => {
    dadosFideosAgotadosConBloqueDeEntradas();

    cuandoSeAnalizan();

    entoncesLaPrimeraAccionEs('stock', 'registrar');
  });
});

describe('precio frente a la zona', () => {
  it('más barato que la zona avisa que puede perder margen', () => {
    dadaUnaYerbaCon({ price: 3500 });

    cuandoSeAnalizan();

    entoncesElTituloEs('precio_zona', 'Yerba Playadito 1 kg está más barato que en tu zona');
  });

  it('más barato propone subir sin pasar el máximo de la zona', () => {
    dadaUnaYerbaCon({ price: 3500 });

    cuandoSeAnalizan();

    entoncesProponeElPrecio('precio_zona', 3780);
  });

  it('más barato cuenta cuánto ganaría', () => {
    dadaUnaYerbaCon({ price: 3500 });

    cuandoSeAnalizan();

    entoncesElMotivoDice('precio_zona', 1, 'ganarías');
  });

  it('casi al costo sube la prioridad', () => {
    dadaUnaYerbaCon({ price: 3000, cost: 2900 });

    cuandoSeAnalizan();

    entoncesElAvisoTienePrioridad('precio_zona', 2);
  });

  it('casi al costo lo dice en el título', () => {
    dadaUnaYerbaCon({ price: 3000, cost: 2900 });

    cuandoSeAnalizan();

    entoncesElTituloDice('precio_zona', 'casi al costo');
  });

  it('más caro que la zona lo dice en el título', () => {
    dadaUnaYerbaCon({ price: 4600 }, [[3, 10]]);

    cuandoSeAnalizan();

    entoncesElTituloDice('precio_zona', 'más caro que en tu zona');
  });

  it('más caro explica que bajarlo ayuda a vender más', () => {
    dadaUnaYerbaCon({ price: 4600 }, [[3, 10]]);

    cuandoSeAnalizan();

    entoncesElDetalleDice('precio_zona', 'vender más');
  });

  it('más caro propone bajar apenas por encima del promedio', () => {
    dadaUnaYerbaCon({ price: 4600 }, [[3, 10]]);

    cuandoSeAnalizan();

    entoncesProponeElPrecio('precio_zona', 4020);
  });

  // 3900 es el promedio: dentro del 8 % más barato (3588) o del 12 % más caro (4368) no se avisa.
  it.each(PRECIOS_EN_EL_UMBRAL)('a $precio hay $avisos avisos de precio', ({ precio, avisos }) => {
    dadaUnaYerbaCon({ price: precio });

    cuandoSeAnalizan();

    entoncesHayAvisosDeTipo('precio_zona', avisos);
  });

  it('no compara con una zona sin datos suficientes', () => {
    dadaUnaYerbaEnUnaZonaChica({ price: 3000 });

    cuandoSeAnalizan();

    entoncesNoHayAvisos();
  });
});

describe('estancado', () => {
  it('caro y sin ventas en 30 días avisa para hoy', () => {
    dadaUnaYerbaCon({ price: 4400 }, [[45, 3]]);

    cuandoSeAnalizan();

    entoncesElAvisoTienePrioridad('estancado', 2);
  });

  it('cuenta cuándo fue la última venta', () => {
    dadaUnaYerbaCon({ price: 4400 }, [[45, 3]]);

    cuandoSeAnalizan();

    entoncesLosMotivosDicen('estancado', 'La última venta que anotaste fue el');
  });

  it('propone la rebaja al promedio de la zona', () => {
    dadaUnaYerbaCon({ price: 4400 }, [[45, 3]]);

    cuandoSeAnalizan();

    entoncesProponeElPrecio('estancado', 3900);
  });

  it('un producto estancado no avisa además por precio', () => {
    dadaUnaYerbaCon({ price: 4400 }, [[45, 3]]);

    cuandoSeAnalizan();

    entoncesHayAvisosDeTipo('precio_zona', 0);
  });

  it('con una venta reciente ya no está estancado', () => {
    dadaUnaYerbaCon({ price: 4400 }, [
      [45, 3],
      [5, 1],
    ]);

    cuandoSeAnalizan();

    entoncesHayAvisosDeTipo('estancado', 0);
  });

  it('sin historial de ventas no se afirma que no se mueve', () => {
    dadaUnaYerbaCon({ price: 4400 });

    cuandoSeAnalizan();

    entoncesHayAvisosDeTipo('estancado', 0);
  });
});

describe('muy vendido', () => {
  it('mucha rotación y precio bajo propone un aumento', () => {
    dadaUnaYerbaCon({ price: 3600 }, [[2, 20]]);

    cuandoSeAnalizan();

    entoncesProponeElPrecio('muy_vendido', 3820);
  });

  it('cuenta lo que se gana por mes con el aumento', () => {
    dadaUnaYerbaCon({ price: 3600 }, [[2, 20]]);

    cuandoSeAnalizan();

    entoncesElMotivoDice('muy_vendido', 1, '$ 4.400 más por mes');
  });

  it('un producto muy vendido no avisa además por precio', () => {
    dadaUnaYerbaCon({ price: 3600 }, [[2, 20]]);

    cuandoSeAnalizan();

    entoncesHayAvisosDeTipo('precio_zona', 0);
  });

  it('poca rotación no alcanza', () => {
    dadaUnaYerbaCon({ price: 3600 }, [[2, 14]]);

    cuandoSeAnalizan();

    entoncesHayAvisosDeTipo('muy_vendido', 0);
  });
});

describe('combos', () => {
  it('dos productos de poca salida vendidos juntos tres veces se avisan como combo', () => {
    dadasCanastasDeAlfajorYGalletitas(3);

    cuandoSeAnalizan();

    entoncesElTituloEs('combo', 'Alfajor Jorgito y Galletitas Criollitas 3 U se venden juntos');
  });

  it('el combo propuesto cuesta un 10 % menos que por separado', () => {
    dadasCanastasDeAlfajorYGalletitas(3);

    cuandoSeAnalizan();

    entoncesProponeCrear('combo', {
      name: 'Combo Alfajor Jorgito + Galletitas Criollitas 3 U',
      price: 1850,
      cost: 1380,
    });
  });

  it('con dos veces todavía no', () => {
    dadasCanastasDeAlfajorYGalletitas(2);

    cuandoSeAnalizan();

    entoncesHayAvisosDeTipo('combo', 0);
  });
});

describe('lo que se vende en la zona', () => {
  it('sin inventario no hay nada que sugerir', () => {
    dadoUnSistemaSinInventario();

    cuandoSeAnalizan();

    entoncesNoHayAvisos();
  });

  it('un producto que ya tenés, aunque se llame distinto, no se sugiere', () => {
    dadaUnaYerbaCon({ name: 'Leche La Serenísima entera' });

    cuandoSeAnalizan();

    entoncesNingunTituloDeTipoEs(
      'consumo_zona',
      'En tu zona se vende Leche La Serenísima 1 L y no lo tenés',
    );
  });

  it('un producto de pocos comercios no existe para el motor', () => {
    entoncesNingunProductoDeLaZonaEs('Fernet');
  });
});

describe('zona anónima', () => {
  it('el mínimo de comercios es tres', () => {
    entoncesElMinimoDeComerciosEs(3);
  });

  it('cada producto de la zona cumple el mínimo de comercios', () => {
    entoncesCadaProductoDeLaZonaTieneElMinimoDeComercios();
  });

  it('un barrio con pocos comercios no devuelve ningún dato', () => {
    cuandoSeAnalizanCienBarrios();

    entoncesLosBarriosConPocosComerciosNoTienenDatos();
  });

  it('un barrio con suficientes comercios sí devuelve datos', () => {
    cuandoSeAnalizanCienBarrios();

    entoncesLosBarriosConSuficientesComerciosTienenDatos();
  });

  it('una zona de dos comercios se queda sin productos', () => {
    cuandoSeFiltraUnaZonaDeDosComercios();

    entoncesNoQuedaNingunProducto();
  });

  it('el mismo texto da el mismo análisis', () => {
    cuandoSeAnalizaDosVecesElBarrio('Palermo');

    entoncesDanLoMismo();
  });
});

describe('preguntas a Sami', () => {
  it('qué reponer trae sólo los avisos de stock', () => {
    cuandoSePregunta('¿Qué tengo que reponer?');

    entoncesLaRespuestaSoloTraeAvisosDeTipo('stock');
  });

  it('qué reponer cuenta cuántos productos hay que reponer', () => {
    cuandoSePregunta('¿Qué tengo que reponer?');

    entoncesLaRespuestaDice('3 productos para reponer');
  });

  it('el precio de un producto en la zona se da agregado', () => {
    cuandoSePregunta('¿cuánto cobran la yerba en mi zona?');

    entoncesLaRespuestaDice('va de $ 3.500 a $ 4.300');
  });

  it('el precio de un producto trae el producto de la zona que lo vende más', () => {
    cuandoSePregunta('¿cuánto cobran la yerba en mi zona?');

    entoncesElPrimerPrecioEsDe('Yerba Playadito 1 kg');
  });

  it('un producto que casi nadie vende no se informa', () => {
    cuandoSePregunta('¿cuánto cobran el fernet?');

    entoncesLaRespuestaDice('al menos 3 comercios');
  });

  it('de un producto que casi nadie vende no muestra precios', () => {
    cuandoSePregunta('¿cuánto cobran el fernet?');

    entoncesNoTraePrecios();
  });

  it('qué se vende cerca trae lo que se vende en la zona', () => {
    cuandoSePregunta('¿qué se vende cerca?');

    entoncesLaRespuestaSoloTraeAvisosDeTipo('consumo_zona');
  });

  it('qué no se vende trae lo estancado', () => {
    cuandoSePregunta('¿qué no se vende?');

    entoncesLaRespuestaSoloTraeAvisosDeTipo('estancado');
  });

  it('si no entiende, lo dice y da ejemplos', () => {
    cuandoSePregunta('blablá');

    entoncesLaRespuestaDice('Todavía no sé responder eso');
  });

  it('una pregunta vacía pide una pregunta corta', () => {
    cuandoSePregunta('   ');

    entoncesLaRespuestaDice('pregunta corta');
  });
});

describe('parecido entre productos', () => {
  it.each(PARECIDOS)('«$tuyo» y «$otro» se parecen $valor', ({ tuyo, otro, valor }) => {
    cuandoSeComparan(tuyo, otro);

    entoncesElParecidoEs(valor);
  });
});
