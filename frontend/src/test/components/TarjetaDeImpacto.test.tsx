import { describe, it } from 'vitest';
import {
  cuandoSeMuestraLaTarjetaDeImpacto,
  dadoUnAvisoMedidoQueCambioElIngresoSemanalEn,
  dadoUnImpactoSinAvisosMedidos,
  entoncesDiceCuantoMasSeVendePorSemana,
  entoncesDiceCuantoMenosSeVendePorSemana,
  entoncesDiceQueLasVentasEstanIgualQueAntes,
  entoncesMuestraElDetalleDelAvisoMedido,
  entoncesNoMuestraNada,
} from '@/test/impactoDeAvisos';

describe('la tarjeta de impacto de los avisos', () => {
  it('muestra cuánto cambió lo que se vende por semana y el detalle de cada aviso medido', async () => {
    dadoUnAvisoMedidoQueCambioElIngresoSemanalEn(770);

    cuandoSeMuestraLaTarjetaDeImpacto();

    await entoncesDiceCuantoMasSeVendePorSemana();
    entoncesMuestraElDetalleDelAvisoMedido();
  });

  it('si se vende menos lo dice sin presentarlo como una ganancia', async () => {
    dadoUnAvisoMedidoQueCambioElIngresoSemanalEn(-300);

    cuandoSeMuestraLaTarjetaDeImpacto();

    await entoncesDiceCuantoMenosSeVendePorSemana();
  });

  it('si no cambió, lo dice', async () => {
    dadoUnAvisoMedidoQueCambioElIngresoSemanalEn(0);

    cuandoSeMuestraLaTarjetaDeImpacto();

    await entoncesDiceQueLasVentasEstanIgualQueAntes();
  });

  it('sin avisos medidos no muestra nada', async () => {
    dadoUnImpactoSinAvisosMedidos();

    cuandoSeMuestraLaTarjetaDeImpacto();

    await entoncesNoMuestraNada();
  });
});
