import { describe, it } from 'vitest';
import {
  borrarRegistros,
  cargarRegistros,
  cuandoSamiPide,
  entoncesSeAplico,
  entoncesSeOmitieron,
  entoncesSeOmitio,
  entoncesSoloSeHizo,
  entoncesSoloSeOmitio,
} from '@/test/accionesDeSami';
import {
  cuandoSeBorraPorIdLaPrimeraVentaYLaSegundaJuntoConLasDeVero,
  cuandoSeBorraPorIdUnClienteDeEjemplo,
  cuandoSeCargaUnaVentaReal,
  cuandoSeCorrigeElClienteDeLaPrimeraVentaA,
  cuandoSeCorrigenElPrecioDeUnaYLaCantidadDeOtra,
  cuandoSeCreaElSistema,
  cuandoSeIntentanCorregirRegistrosQueNoSePuedenCorregir,
  dadasDosVentasCargadasAAntoYAVero,
  dadasTresVentasCargadasDeAntoYVero,
  dadasVentasYClientesDeEjemplo,
  entoncesAntoYaNoEstaNiEnLasVentas,
  entoncesElClienteDeEjemploSigue,
  entoncesLaPrimeraVentaEsDe,
  entoncesLaPrimeraVentaQuedaSinCliente,
  entoncesLasVentasQuedanCorregidasSinSumarFilas,
  entoncesLasVentasSoloTienenElRegistroReal,
  entoncesLasVentasYElEjemploQuedanComoEstaban,
  entoncesLoCreadoNoTieneEjemplosYConservaLaVenta,
  entoncesLosClientesSiguenSiendoEjemplos,
  entoncesQuedanVentas,
  entoncesQuedanVentasRealesSinEjemplos,
  entoncesQuedaPendienteSinCrearse,
  entoncesQuedaUnaSolaVentaConSuProducto,
} from '@/test/accionesSobreRegistros';

describe('borrar registros', () => {
  it('borra las filas cuyos datos coinciden, sin importar mayúsculas ni tildes', () => {
    dadasTresVentasCargadasDeAntoYVero();

    cuandoSamiPide(borrarRegistros('sales', { donde: { cliente: 'anto sdani' } }));

    entoncesSoloSeHizo('Borré 2 registros de ventas.');
  });

  it('borra sólo las filas que coinciden', () => {
    dadasTresVentasCargadasDeAntoYVero();

    cuandoSamiPide(borrarRegistros('sales', { donde: { cliente: 'anto sdani' } }));

    entoncesQuedaUnaSolaVentaConSuProducto();
  });

  it('exige que coincidan todos los datos de un criterio', () => {
    dadasTresVentasCargadasDeAntoYVero();

    cuandoSamiPide(borrarRegistros('sales', { donde: { Cliente: 'Anto Sdani', Cantidad: '0,5' } }));

    entoncesQuedanVentas(2);
  });

  it('acepta una lista de criterios y basta con que coincida uno', () => {
    dadasTresVentasCargadasDeAntoYVero();

    cuandoSamiPide(
      borrarRegistros('Ventas', { donde: [{ Cliente: 'Vero Susy' }, { Cliente: 'Anto Sdani' }] }),
    );

    entoncesSeAplico('Borré 3 registros de ventas.');
  });

  it('un solo registro se cuenta en singular', () => {
    dadasTresVentasCargadasDeAntoYVero();

    cuandoSamiPide(borrarRegistros('clients', { donde: { Nombre: 'Anto Sdani' } }));

    entoncesSeAplico('Borré un registro de clientes.');
  });

  it('los vínculos que apuntaban a lo borrado quedan vacíos', () => {
    dadasTresVentasCargadasDeAntoYVero();

    cuandoSamiPide(borrarRegistros('clients', { donde: { Nombre: 'Anto Sdani' } }));

    entoncesAntoYaNoEstaNiEnLasVentas();
  });

  it('nunca borra de más: sin criterio, con un dato que no existe o sin coincidencias se omite', () => {
    dadasTresVentasCargadasDeAntoYVero();

    cuandoSamiPide(
      borrarRegistros('sales'),
      borrarRegistros('sales', { donde: {} }),
      borrarRegistros('sales', { donde: 'todo' }),
      borrarRegistros('sales', { donde: { Color: 'Rojo' } }),
      borrarRegistros('sales', { donde: { Cliente: 'Anto Sdani', Color: 'Rojo' } }),
      borrarRegistros('sales', { donde: { Cantidad: 'mucho' } }),
      borrarRegistros('sales', { donde: { Cliente: 'Nadie' } }),
      borrarRegistros('nada', { donde: { Cliente: 'Anto Sdani' } }),
    );

    entoncesSeOmitieron(8);
  });

  it('lo que se omitió no borra ninguna fila', () => {
    dadasTresVentasCargadasDeAntoYVero();

    cuandoSamiPide(borrarRegistros('sales', { donde: { Cliente: 'Anto Sdani', Color: 'Rojo' } }));

    entoncesQuedanVentas(3);
  });

  it('borrar y cargar en el mismo turno corrige sin duplicar', () => {
    dadasTresVentasCargadasDeAntoYVero();

    cuandoSamiPide(
      borrarRegistros('sales', { donde: { Cliente: 'Anto Sdani' } }),
      cargarRegistros('sales', [
        { Producto: 'Almohaditas', Cantidad: 0.5, Precio: 3200, Cliente: 'Anto Solari' },
      ]),
    );

    entoncesQuedanVentasRealesSinEjemplos(2);
  });

  it('borra por id sólo los registros reales que nombra, y junto con un criterio', () => {
    dadasTresVentasCargadasDeAntoYVero();

    cuandoSeBorraPorIdLaPrimeraVentaYLaSegundaJuntoConLasDeVero();

    entoncesSeAplico('Borré un registro de ventas.', 'Borré 2 registros de ventas.');
  });

  it('borrar por id y por criterio deja la pieza vacía si cubren todo', () => {
    dadasTresVentasCargadasDeAntoYVero();

    cuandoSeBorraPorIdLaPrimeraVentaYLaSegundaJuntoConLasDeVero();

    entoncesQuedanVentas(0);
  });

  it('nunca borra por id una fila de ejemplo', () => {
    dadasVentasYClientesDeEjemplo();

    cuandoSeBorraPorIdUnClienteDeEjemplo();

    entoncesSeOmitio('No encontré registros de clientes que coincidan.');
  });

  it('el ejemplo nombrado por id sigue en su pieza', () => {
    dadasVentasYClientesDeEjemplo();

    cuandoSeBorraPorIdUnClienteDeEjemplo();

    entoncesElClienteDeEjemploSigue();
  });
});

describe('corregir registros', () => {
  it('corrige los registros nombrados por id', () => {
    dadasDosVentasCargadasAAntoYAVero();

    cuandoSeCorrigenElPrecioDeUnaYLaCantidadDeOtra();

    entoncesSoloSeHizo('Corregí 2 registros en ventas.');
  });

  it('pisa sólo los datos que llegan, pasados a su tipo, sin sumar filas', () => {
    dadasDosVentasCargadasAAntoYAVero();

    cuandoSeCorrigenElPrecioDeUnaYLaCantidadDeOtra();

    entoncesLasVentasQuedanCorregidasSinSumarFilas();
  });

  it('un cliente corregido por nombre se vincula con el registro de ese nombre', () => {
    dadasDosVentasCargadasAAntoYAVero();

    cuandoSeCorrigeElClienteDeLaPrimeraVentaA('Vero');

    entoncesLaPrimeraVentaEsDe('Vero');
  });

  it('un cliente corregido a un nombre que no está deja el vínculo vacío', () => {
    dadasDosVentasCargadasAAntoYAVero();

    cuandoSeCorrigeElClienteDeLaPrimeraVentaA('Nadia');

    entoncesLaPrimeraVentaQuedaSinCliente();
  });

  it('un cliente corregido a un nombre que no está queda pendiente, sin crearlo', () => {
    dadasDosVentasCargadasAAntoYAVero();

    cuandoSeCorrigeElClienteDeLaPrimeraVentaA('Nadia');

    entoncesQuedaPendienteSinCrearse('Nadia');
  });

  it('omite un id que no está, una fila de ejemplo o datos que no sirven', () => {
    dadasDosVentasCargadasAAntoYAVero();

    cuandoSeIntentanCorregirRegistrosQueNoSePuedenCorregir();

    entoncesSoloSeOmitio(
      'No encontré esos registros en ventas.',
      'No encontré esos registros en gastos.',
      'No encontré esa pieza.',
    );
  });

  it('lo que no se pudo corregir queda como estaba', () => {
    dadasDosVentasCargadasAAntoYAVero();

    cuandoSeIntentanCorregirRegistrosQueNoSePuedenCorregir();

    entoncesLasVentasYElEjemploQuedanComoEstaban();
  });
});

describe('los ejemplos no se mezclan con lo real', () => {
  it('al cargar registros reales en una pieza desaparecen sus ejemplos', () => {
    dadasVentasYClientesDeEjemplo();

    cuandoSeCargaUnaVentaReal();

    entoncesLasVentasSoloTienenElRegistroReal();
  });

  it('los ejemplos de las otras piezas siguen marcados', () => {
    dadasVentasYClientesDeEjemplo();

    cuandoSeCargaUnaVentaReal();

    entoncesLosClientesSiguenSiendoEjemplos();
  });

  it('lo que se guarda al crear nunca incluye una fila de ejemplo', () => {
    dadasVentasYClientesDeEjemplo();
    cuandoSeCargaUnaVentaReal();

    cuandoSeCreaElSistema();

    entoncesLoCreadoNoTieneEjemplosYConservaLaVenta();
  });
});
