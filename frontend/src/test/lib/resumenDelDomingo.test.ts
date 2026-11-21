import { describe, expect, it } from 'vitest';
import { quePasoPosible } from '@/lib/respuestaManual';
import { armarResumenDelDomingo, estaVacio } from '@/lib/resumenDelDomingo';
import { cuandoSeCalcula, entoncesElResultadoEs, entoncesElResultadoDe } from '@/test/calculo';
import type { DecisionObjetivo, MiniObjetivo, SemanaObjetivo } from '@/types/objetivos';

const SEMANA = '2026-11-09';

type Resumen = ReturnType<typeof armarResumenDelDomingo>;
type Entrada = Parameters<typeof armarResumenDelDomingo>[0];

const cierre = (reajustes: SemanaObjetivo['reajustes'] = []): SemanaObjetivo => ({
  semana: SEMANA,
  resultado: 0,
  avance: 0,
  decisiones: { aplicadas: 0, descartadas: 0, aceptadas: 0, ignoradas: 0 },
  queFunciono: '',
  textoConIa: false,
  reajustes,
});

const decision = (tipo: DecisionObjetivo['tipo'], descripcion: string, semana = SEMANA) => ({
  id: `${tipo}-${descripcion}`,
  tipo,
  miniObjetivoId: null,
  avisoId: null,
  registroId: null,
  descripcion,
  tomadaEn: `${semana}T15:00:00Z`,
  semanaInicio: semana,
  efecto: 'SIN_MEDICION' as const,
});

const manual = (titulo: string, semanaInicio: string): MiniObjetivo => ({
  id: titulo,
  modo: 'MANUAL',
  posicion: 1,
  estado: 'EN_CURSO',
  titulo,
  justificacion: '',
  semanaInicio,
  textoConIa: true,
});

const entradaDeUnaSemanaConDecisiones = (): Entrada => ({
  semana: cierre(),
  decisiones: [
    decision('MINI_HECHO', 'Hablar con 3 clientes'),
    decision('MINI_EN_PARTE', 'Armar el cartel'),
    decision('MINI_NO_HECHO', 'Saludar por el nombre'),
    decision('MINI_HECHO', 'Algo de otra semana', '2026-11-02'),
  ],
  minis: [manual('Hablar con 5 clientes', '2026-11-16'), manual('Viejo', SEMANA)],
  sinResponder: ['Anotar qué piden'],
});

const entradaConReajustes = (): Entrada => ({
  semana: cierre([
    {
      tipo: 'SUBIR_META',
      motivo: '',
      de: { titulo: 'Subile $150 a la yerba', meta: 1 },
      a: { titulo: 'Subile $250 a la yerba', meta: 2 },
    },
    {
      tipo: 'REEMPLAZAR',
      motivo: '',
      de: { titulo: 'Recortá gastos', meta: 1 },
      a: { titulo: 'Mové 6 Fernet', meta: 2 },
    },
    { tipo: 'PROBAR_OTRO_CAMINO', motivo: '', de: { titulo: 'Vendé más', meta: 1 }, a: null },
  ]),
  decisiones: [],
  minis: [],
  sinResponder: [],
});

const entradaSinNadaQueContar = (): Entrada => ({
  semana: cierre(),
  decisiones: [],
  minis: [],
  sinResponder: [],
});

const de = (leer: (resumen: Resumen) => unknown, esperado: unknown) =>
  entoncesElResultadoDe<Resumen>(leer, esperado);

describe('resumen del domingo', () => {
  it('lo cumplido es sólo lo que se hizo en la semana que cerró', () => {
    cuandoSeArmaElResumen(entradaDeUnaSemanaConDecisiones());

    de((resumen) => resumen.cumplido, ['Hablar con 3 clientes']);
  });

  it('lo no cumplido suma lo hecho en parte, lo no hecho y lo que quedó sin responder', () => {
    cuandoSeArmaElResumen(entradaDeUnaSemanaConDecisiones());

    de(
      (resumen) => resumen.noCumplido,
      [
        { titulo: 'Armar el cartel', motivo: 'EN_PARTE' },
        { titulo: 'Saludar por el nombre', motivo: 'NO_HECHO' },
        { titulo: 'Anotar qué piden', motivo: 'SIN_RESPUESTA' },
      ],
    );
  });

  it('lo nuevo es lo que empezó después del domingo, no lo que empezó antes', () => {
    cuandoSeArmaElResumen(entradaDeUnaSemanaConDecisiones());

    de((resumen) => resumen.nuevo, ['Hablar con 5 clientes']);
  });

  it('subir la meta de un medido cuenta como cumplido', () => {
    cuandoSeArmaElResumen(entradaConReajustes());

    de((resumen) => resumen.cumplido, ['Subile $150 a la yerba']);
  });

  it('cambiar o reemplazar un medido cuenta como lo que no se dio', () => {
    cuandoSeArmaElResumen(entradaConReajustes());

    de(
      (resumen) => resumen.noCumplido,
      [
        { titulo: 'Recortá gastos', motivo: 'CAMBIADO' },
        { titulo: 'Vendé más', motivo: 'CAMBIADO' },
      ],
    );
  });

  it('los reajustes suman a lo nuevo el paso que viene', () => {
    cuandoSeArmaElResumen(entradaConReajustes());

    de((resumen) => resumen.nuevo, ['Subile $250 a la yerba', 'Mové 6 Fernet']);
  });

  it('una semana sin nada que contar queda vacía', () => {
    cuandoSeArmaElResumen(entradaSinNadaQueContar());

    entoncesElResumenEstaVacio();
  });

  it.each([
    ['SI', 'NO_TUVE_TIEMPO', false],
    ['SI', 'FUNCIONO', true],
    ['EN_PARTE', 'NO_TUVE_TIEMPO', true],
    ['NO', 'NO_TUVE_TIEMPO', true],
  ] as const)('si lo hizo «%s», «%s» es posible: %s', (hizo, quePaso, posible) => {
    cuandoSeCalcula(() => quePasoPosible(hizo, quePaso));

    entoncesElResultadoEs(posible);
  });
});

let resumenArmado: Resumen;

function cuandoSeArmaElResumen(entrada: Entrada) {
  cuandoSeCalcula(() => (resumenArmado = armarResumenDelDomingo(entrada)));
}

function entoncesElResumenEstaVacio() {
  expect(estaVacio(resumenArmado)).toBe(true);
}
