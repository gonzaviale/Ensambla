import { describe, it } from 'vitest';
import {
  EL_FINDE,
  LOS_PRODUCTOS_QUE_TENGO,
  LO_QUE_ME_DEBEN_POR_CLIENTE,
  dadoElPedidoInterpretado,
  entoncesEntendio,
  entoncesEntendioTodasLasPalabras,
  entoncesNoEntendio,
} from '@/test/loQueEntendi';

describe('Esto entendí', () => {
  it('cuenta en palabras simples qué mira, qué cuenta, cómo reparte y de qué período', () => {
    dadoElPedidoInterpretado(EL_FINDE);

    entoncesEntendio([
      'Mira: tus ventas',
      'Cuenta: la plata que entró (no dijiste qué contar)',
      'Reparte: todo junto (no dijiste cómo repartirlo)',
      'Período: este mes (no dijiste cuándo)',
    ]);
  });

  it('avisa las palabras que no entendió', () => {
    dadoElPedidoInterpretado(EL_FINDE);

    entoncesNoEntendio('«finde»');
  });

  it('en el stock muestra lo que hay hoy sin avisar un período por defecto', () => {
    dadoElPedidoInterpretado(LOS_PRODUCTOS_QUE_TENGO);

    entoncesEntendio([
      'Mira: tu stock',
      'Cuenta: cuántos productos hay',
      'Reparte: todo junto (no dijiste cómo repartirlo)',
      'Período: lo que tenés hoy',
    ]);
  });

  it('si es sólo lo fiado lo dice y marca sólo lo que tomó por defecto', () => {
    dadoElPedidoInterpretado(LO_QUE_ME_DEBEN_POR_CLIENTE);

    entoncesEntendio([
      'Mira: tus ventas',
      'Cuenta: la plata que entró (no dijiste qué contar)',
      'Reparte: por cliente',
      'Período: todo lo cargado (no dijiste cuándo)',
      'Solo lo fiado: lo que te deben',
    ]);
  });

  it('si entendió todas las palabras no muestra ningún aviso', () => {
    dadoElPedidoInterpretado(LO_QUE_ME_DEBEN_POR_CLIENTE);

    entoncesEntendioTodasLasPalabras();
  });
});
