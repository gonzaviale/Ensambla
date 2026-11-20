import { describe, expect, it } from 'vitest';
import {
  datosUsados,
  editablesDe,
  paraGuardar,
  problemaDelManual,
  textoDeCuota,
  tipoQueQueda,
} from '@/lib/objetivosSami';
import { propuestaObjetivoSchema } from '@/types/objetivos';
import { cuandoSeCalcula, entoncesElResultadoEs } from '@/test/calculo';

// El ejemplo del contrato de docs/objetivos-con-sami.md (sección 7).
const PROPUESTA = propuestaObjetivoSchema.parse({
  respuesta: 'Entendí que querés vender más y darte a conocer.',
  entendido: 'Vender más y que más vecinos conozcan tu local',
  tipo: 'HIBRIDO',
  esMonetario: false,
  minis: [
    {
      modo: 'MEDIDO',
      tipo: 'VENTAS_SEMANALES',
      registroId: null,
      referencia: null,
      titulo: 'Vender un 10 % más por semana',
      justificacion: 'En los últimos 7 días vendiste $182.000.',
      valorInicial: 182000,
      meta: 200200,
      aporteEstimado: 31200,
      textoConIa: false,
    },
    {
      modo: 'MANUAL',
      tipo: null,
      registroId: null,
      referencia: null,
      titulo: 'Pedile a 3 clientes que te recomienden con un vecino',
      justificacion: 'Es la forma más barata de que te conozcan.',
      valorInicial: null,
      meta: null,
      aporteEstimado: null,
      textoConIa: true,
    },
  ],
  datos: [
    { fuente: 'VENTAS', hay: true },
    { fuente: 'MARGEN', hay: true },
    { fuente: 'GASTOS', hay: false },
    { fuente: 'ZONA', hay: false },
  ],
  origen: 'IA',
  mensajesRestantes: 5,
  puedeGuardar: true,
});

const CUOTA = { usadas: 2, limite: 5, restantes: 3 };

type Editable = ReturnType<typeof editablesDe>[number];

/** El mini medido y el manual de la propuesta de ejemplo. */
function lasDos() {
  const [medido, manual] = editablesDe(PROPUESTA);
  if (!medido || !manual) throw new Error('La propuesta de ejemplo trae un medido y un manual');
  return { medido, manual };
}

const medido = (): Editable => lasDos().medido;
const manual = (): Editable => lasDos().manual;

const PROPUESTA_ACLARANDO = {
  respuesta: 'Los budines no están en tus datos. ¿Querés sumarlos al catálogo?',
  entendido: null,
  tipo: null,
  esMonetario: false,
  minis: [],
  datos: [{ fuente: 'VENTAS', hay: true }],
  origen: 'IA',
  mensajesRestantes: 5,
  puedeGuardar: false,
  cuota: CUOTA,
};

describe('objetivos con Sami', () => {
  it('si Sami todavía necesita aclarar, el entendido llega vacío', () => {
    cuandoSeCalcula(() => propuestaObjetivoSchema.parse(PROPUESTA_ACLARANDO).entendido);

    entoncesElResultadoEs('');
  });

  it('si Sami todavía necesita aclarar, la propuesta se lee igual sin tipo', () => {
    cuandoSeCalcula(() => {
      const aclarando = propuestaObjetivoSchema.parse(PROPUESTA_ACLARANDO);
      return [aclarando.tipo, aclarando.origen];
    });

    entoncesElResultadoEs([null, 'IA']);
  });

  it('la propuesta del contrato se lee con sus dos mini-objetivos', () => {
    cuandoSeCalcula(() => PROPUESTA.minis.length);

    entoncesElResultadoEs(2);
  });

  it('la cuota todavía puede no venir', () => {
    cuandoSeCalcula(() => PROPUESTA.cuota);

    entoncesElResultadoEs(undefined);
  });

  it('la cuota que viene se conserva', () => {
    cuandoSeCalcula(() => propuestaObjetivoSchema.parse({ ...PROPUESTA, cuota: CUOTA }).cuota);

    entoncesElResultadoEs(CUOTA);
  });

  it('dice la cuota que queda', () => {
    cuandoSeCalcula(() => textoDeCuota(CUOTA));

    entoncesElResultadoEs('Objetivos con IA disponibles este mes: 3 de 5');
  });

  it('dice cuándo la cuota se agotó', () => {
    cuandoSeCalcula(() => textoDeCuota({ usadas: 20, limite: 20, restantes: 0 }));

    entoncesElResultadoEs('Usaste los 20 objetivos con IA de este mes');
  });

  it('cuenta en qué datos se apoyó Sami y de cuáles no hay', () => {
    cuandoSeCalcula(() => datosUsados(PROPUESTA.datos));

    entoncesElResultadoEs('Se apoyó en: ventas, ganancia por venta. Sin datos de: gastos, zona.');
  });

  it('sin datos no cuenta nada', () => {
    cuandoSeCalcula(() => datosUsados([]));

    entoncesElResultadoEs('');
  });

  it('sin mini-objetivos no queda ningún tipo', () => {
    cuandoSeCalcula(() => tipoQueQueda([]));

    entoncesElResultadoEs(null);
  });

  it('con sólo un medido el tipo es con datos', () => {
    cuandoSeCalcula(() => tipoQueQueda([medido()]));

    entoncesElResultadoEs('CON_DATOS');
  });

  it('con sólo un manual el tipo es sin datos', () => {
    cuandoSeCalcula(() => tipoQueQueda([manual()]));

    entoncesElResultadoEs('SIN_DATOS');
  });

  it('con un medido y un manual el tipo es híbrido', () => {
    cuandoSeCalcula(() => tipoQueQueda([medido(), manual()]));

    entoncesElResultadoEs('HIBRIDO');
  });

  it('de un medido sólo se manda cuál es', () => {
    cuandoSeCalcula(() => paraGuardar(medido()));

    entoncesElResultadoEs({
      modo: 'MEDIDO',
      tipo: 'VENTAS_SEMANALES',
      registroId: null,
      referencia: null,
    });
  });

  it('del manual se manda su texto ya retocado', () => {
    cuandoSeCalcula(() => paraGuardar({ ...manual(), titulo: '  Hablá con 5 vecinos ' }));

    entoncesElResultadoEs({
      modo: 'MANUAL',
      titulo: 'Hablá con 5 vecinos',
      justificacion: 'Es la forma más barata de que te conozcan.',
    });
  });

  it('una acción manual bien escrita no tiene problema', () => {
    cuandoSeCalcula(() => problemaDelManual(manual()));

    entoncesElResultadoEs(null);
  });

  it('una acción manual no puede estar vacía', () => {
    cuandoSeCalcula(() => problemaDelManual({ ...manual(), titulo: ' ' }));

    entoncesElResultadoEs(expect.stringMatching(/Escribí qué vas a hacer/));
  });

  it('una acción manual no puede traer importes en la justificación', () => {
    cuandoSeCalcula(() => problemaDelManual({ ...manual(), justificacion: 'Subir $5.000' }));

    entoncesElResultadoEs(expect.stringMatching(/Sin importes/));
  });

  it('una acción manual no puede traer porcentajes en el título', () => {
    cuandoSeCalcula(() => problemaDelManual({ ...manual(), titulo: 'Vender 20 %' }));

    entoncesElResultadoEs(expect.stringMatching(/Sin importes/));
  });

  it('los números de un medido los calcula el servidor: no se validan acá', () => {
    cuandoSeCalcula(() => problemaDelManual({ ...medido(), titulo: 'Vender un 10 %' }));

    entoncesElResultadoEs(null);
  });
});
