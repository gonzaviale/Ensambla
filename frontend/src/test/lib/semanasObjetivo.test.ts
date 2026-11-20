import { describe, expect, it } from 'vitest';
import { semanasObjetivoSchema } from '@/types/objetivos';

const REAJUSTE_SIN_META = {
  tipo: 'SUBIR_META',
  motivo: 'Hiciste «Hablá con 3 clientes» y te funcionó.',
  de: { titulo: 'Hablá con 3 clientes', meta: null },
  a: { titulo: 'Un paso más: Hablá con 3 clientes', meta: null },
};

const MANUAL_CONTADO = { miniObjetivoId: 'a', titulo: 'Contado', hizo: 'SI', quePaso: 'FUNCIONO' };
const MANUAL_SIN_CONTAR = { miniObjetivoId: 'b', titulo: 'Sin contar', hizo: null, quePaso: null };

const dadoElCierreConManuales = (manuales: unknown[]) => ({
  ultimoCierre: '2026-11-16',
  semanas: [
    {
      semana: '2026-11-16',
      resultado: 511860,
      avance: 0,
      decisiones: { aplicadas: 0, descartadas: 0, aceptadas: 0, ignoradas: 0 },
      queFunciono: 'De tus acciones respondiste: 2 hechas, 1 en parte y 0 sin hacer.',
      textoConIa: false,
      manuales,
      reajustes: [REAJUSTE_SIN_META],
    },
  ],
});

const cuandoSeLeeLaRespuesta = (respuesta: unknown) => semanasObjetivoSchema.safeParse(respuesta);

const entoncesSeAcepta = (resultado: ReturnType<typeof cuandoSeLeeLaRespuesta>) => {
  expect(resultado.success).toBe(true);
};

const entoncesSoloQuedanLosManualesContados = (
  resultado: ReturnType<typeof cuandoSeLeeLaRespuesta>,
) => {
  const manuales = resultado.data?.semanas.flatMap((semana) => semana.manuales ?? []);
  expect(manuales?.map((manual) => manual.titulo)).toEqual(['Contado']);
};

describe('las semanas de un objetivo', () => {
  it('acepta el cierre de un objetivo manual, cuyos mini-objetivos no tienen meta', () => {
    const resultado = cuandoSeLeeLaRespuesta(dadoElCierreConManuales([MANUAL_CONTADO]));

    entoncesSeAcepta(resultado);
  });

  it('deja afuera los manuales que la persona no contó', () => {
    const cierre = dadoElCierreConManuales([MANUAL_CONTADO, MANUAL_SIN_CONTAR]);

    const resultado = cuandoSeLeeLaRespuesta(cierre);

    entoncesSeAcepta(resultado);
    entoncesSoloQuedanLosManualesContados(resultado);
  });
});
