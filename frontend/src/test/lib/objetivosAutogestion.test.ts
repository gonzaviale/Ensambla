import { beforeEach, describe, expect, it } from 'vitest';
import {
  cuantoCubren,
  decisionDeLaSemana,
  diaCorto,
  domingoDe,
  faltaParaLaMeta,
  partesDeMini,
  efectoDe,
  lunesDe,
  marcarResumenVisto,
  resumenSinVer,
  todasLasDecisiones,
  valorDeMini,
} from '@/lib/objetivosAutogestion';
import { cuandoSeCalcula, entoncesElResultadoEs } from '@/test/calculo';
import type { DecisionObjetivo, MiniObjetivo } from '@/types/objetivos';

const decision = (cambios: Partial<DecisionObjetivo> = {}): DecisionObjetivo => ({
  id: 'd1',
  tipo: 'PASO_ACEPTADO',
  miniObjetivoId: 'mini-1',
  avisoId: null,
  registroId: null,
  descripcion: 'Subile $250 a la yerba',
  tomadaEn: '2026-11-11T15:00:00Z',
  semanaInicio: '2026-11-09',
  efecto: 'SIN_MEDICION',
  deltaIngresoSemanal: null,
  ...cambios,
});

const miniDePrueba = {
  id: 'mini-1',
  modo: 'MEDIDO' as const,
  posicion: 1,
  tipo: 'MOVER_PARADO' as const,
  sentido: 'SUBE' as const,
  estado: 'EN_CURSO' as const,
  titulo: 'Mové 6 Fernet',
  justificacion: '',
  referencia: null,
  registroId: null,
  valorInicial: 1,
  valorActual: 3,
  meta: 6,
  avance: 0.4,
  aporteEstimado: 0,
  semanaInicio: '2026-11-09',
  textoConIa: false,
};

const miniQueBaja = {
  ...miniDePrueba,
  tipo: 'RECORTAR_GASTO' as const,
  sentido: 'BAJA' as const,
  valorActual: 80000,
  meta: 65000,
};

const DECISIONES_DE_LA_SEMANA = [
  decision({ id: 'a' }),
  decision({ id: 'b', tipo: 'PASO_IGNORADO' }),
  decision({ id: 'c', tipo: 'AVISO_APLICADO' }),
  decision({ id: 'd', miniObjetivoId: 'otro' }),
];

describe('objetivos que se autogestionan', () => {
  beforeEach(() => localStorage.clear());

  it.each([
    ['SUBIR_PRECIO', 14200, '$14.200'],
    ['VENTAS_SEMANALES', 90000, '$90.000 por semana'],
    ['RECORTAR_GASTO', 65000, '$65.000 en el mes'],
    ['MEJORAR_MARGEN', 32.5, '32,5 %'],
    ['MOVER_PARADO', 6, '6 por semana'],
    ['PONER_A_LA_VISTA', 12, '12 por semana'],
  ] as const)('muestra el valor de %s en su unidad', (tipo, valor, texto) => {
    cuandoSeCalcula(() => valorDeMini(tipo, valor));

    entoncesElResultadoEs(texto);
  });

  it.each([
    ['2026-11-12', '2026-11-09'],
    ['2026-11-15', '2026-11-09'],
    ['2026-11-09', '2026-11-09'],
  ])('cuenta la semana del %s por su lunes: %s', (dia, lunes) => {
    cuandoSeCalcula(() => lunesDe(dia));

    entoncesElResultadoEs(lunes);
  });

  it.each([
    ['2026-11-09', '9 nov'],
    ['2026-11-11T15:00:00Z', '11 nov'],
  ])('muestra el día corto de %s como %s', (dia, corto) => {
    cuandoSeCalcula(() => diaCorto(dia));

    entoncesElResultadoEs(corto);
  });

  it('el domingo de una semana es seis días después del lunes', () => {
    cuandoSeCalcula(() => domingoDe('2026-11-09'));

    entoncesElResultadoEs('2026-11-15');
  });

  it.each([
    [{ efecto: 'MIDIENDO' }, 'Sami todavía lo está midiendo.'],
    [{ efecto: 'MEDIDO', deltaIngresoSemanal: 4500 }, 'Después vendiste $4.500 más por semana.'],
    [{ efecto: 'MEDIDO', deltaIngresoSemanal: -800 }, 'Después vendiste $800 menos por semana.'],
    [{ efecto: 'MEDIDO', deltaIngresoSemanal: 0 }, 'Las ventas no cambiaron.'],
    [{}, null],
  ] as [Partial<DecisionObjetivo>, string | null][])(
    'cuenta el efecto de una decisión %j solo cuando ya se midió',
    (cambios, texto) => {
      cuandoSeCalcula(() => efectoDe(decision(cambios)));

      entoncesElResultadoEs(texto);
    },
  );

  it('junta las decisiones de todas las semanas de la más vieja a la más nueva', () => {
    cuandoSeCalcula(() =>
      todasLasDecisiones({
        semanas: [
          { semanaInicio: '2026-11-09', decisiones: [decision({ id: 'nueva' })] },
          {
            semanaInicio: '2026-11-02',
            decisiones: [decision({ id: 'vieja', tomadaEn: '2026-11-03T10:00:00Z' })],
          },
        ],
      }).map((item) => item.id),
    );

    entoncesElResultadoEs(['vieja', 'nueva']);
  });

  it('sin decisiones guardadas no hay ninguna', () => {
    cuandoSeCalcula(() => todasLasDecisiones(undefined));

    entoncesElResultadoEs([]);
  });

  it('toma la última decisión de «lo hago» o «ahora no» de la semana sobre ese mini', () => {
    cuandoSeCalcula(() => decisionDeLaSemana(DECISIONES_DE_LA_SEMANA, 'mini-1', '2026-11-09'));

    entoncesElResultadoEs('IGNORADO');
  });

  it('si sólo dijo «lo hago», la decisión de la semana es aceptado', () => {
    cuandoSeCalcula(() =>
      decisionDeLaSemana(DECISIONES_DE_LA_SEMANA.slice(0, 1), 'mini-1', '2026-11-09'),
    );

    entoncesElResultadoEs('ACEPTADO');
  });

  it('en otra semana no hay decisión', () => {
    cuandoSeCalcula(() => decisionDeLaSemana(DECISIONES_DE_LA_SEMANA, 'mini-1', '2026-11-02'));

    entoncesElResultadoEs(undefined);
  });

  it('un objetivo sin cierre no tiene resumen sin ver', () => {
    cuandoSeCalcula(() => resumenSinVer('o1', null));

    entoncesElResultadoEs(false);
  });

  it('un cierre que la persona todavía no vio está sin ver', () => {
    cuandoSeCalcula(() => resumenSinVer('o1', '2026-11-02'));

    entoncesElResultadoEs(true);
  });

  it('recuerda qué cierre ya vio la persona', () => {
    marcarResumenVisto('o1', '2026-11-02');

    cuandoSeCalcula(() => resumenSinVer('o1', '2026-11-02'));

    entoncesElResultadoEs(false);
  });

  it('un cierre nuevo del mismo objetivo vuelve a estar sin ver', () => {
    marcarResumenVisto('o1', '2026-11-02');

    cuandoSeCalcula(() => resumenSinVer('o1', '2026-11-09'));

    entoncesElResultadoEs(true);
  });

  it('lo que vio un objetivo no cuenta para otro', () => {
    marcarResumenVisto('o1', '2026-11-02');

    cuandoSeCalcula(() => resumenSinVer('o2', '2026-11-02'));

    entoncesElResultadoEs(true);
  });

  it('si lo guardado está roto, trata los cierres como no vistos', () => {
    localStorage.setItem('ensambla.objetivos.resumenVisto', '{roto');

    cuandoSeCalcula(() => resumenSinVer('o1', '2026-11-02'));

    entoncesElResultadoEs(true);
  });

  it.each([
    ['VENTAS_SEMANALES', 84000, { numero: '$84.000', unidad: 'por semana' }],
    ['SUBIR_PRECIO', 15100, { numero: '$15.100', unidad: '' }],
    ['MEJORAR_MARGEN', 28, { numero: '28 %', unidad: 'de ganancia por venta' }],
  ] as const)('separa el número de su unidad en %s', (tipo, valor, partes) => {
    cuandoSeCalcula(() => partesDeMini(tipo, valor));

    entoncesElResultadoEs(partes);
  });

  it('dice cuánto falta cuando la meta es bajar', () => {
    cuandoSeCalcula(() => faltaParaLaMeta(miniQueBaja));

    entoncesElResultadoEs('$15.000 en el mes');
  });

  it('no dice nada si ya bajó de la meta', () => {
    cuandoSeCalcula(() => faltaParaLaMeta({ ...miniQueBaja, valorActual: 60000 }));

    entoncesElResultadoEs(null);
  });

  it('dice cuánto falta cuando la meta es subir', () => {
    cuandoSeCalcula(() => faltaParaLaMeta({ ...miniDePrueba, valorActual: 3, meta: 6 }));

    entoncesElResultadoEs('3 por semana');
  });

  it('lo que falta de ganancia por venta se cuenta en puntos, no en %', () => {
    cuandoSeCalcula(() =>
      faltaParaLaMeta({ ...miniDePrueba, tipo: 'MEJORAR_MARGEN', valorActual: 25.9, meta: 27.9 }),
    );

    entoncesElResultadoEs('2 puntos');
  });

  it('un solo punto se dice en singular', () => {
    cuandoSeCalcula(() =>
      faltaParaLaMeta({ ...miniDePrueba, tipo: 'MEJORAR_MARGEN', valorActual: 26, meta: 27 }),
    );

    entoncesElResultadoEs('1 punto');
  });
});

describe('cuantoCubren', () => {
  const mini = (
    aporteEstimado: number,
    estado: MiniObjetivo['estado'] = 'EN_CURSO',
  ): MiniObjetivo => ({
    ...miniDePrueba,
    id: `m-${aporteEstimado}-${estado}`,
    tipo: 'VENTAS_SEMANALES',
    estado,
    titulo: 'Vender más',
    valorInicial: 0,
    valorActual: 0,
    meta: 1,
    avance: 0,
    aporteEstimado,
  });

  it('cuenta lo que suman los mini en curso frente a lo que falta', () => {
    cuandoSeCalcula(() =>
      cuantoCubren([mini(25_714), mini(11_999), mini(1_660), mini(9_000, 'LOGRADO')], 972_000),
    );

    entoncesElResultadoEs(
      expect.stringMatching(/ganás cerca de \$39\.373 más por mes de los \$972\.000 que te faltan/),
    );
  });

  it('avisa cuando alcanzan para llegar a la meta', () => {
    cuandoSeCalcula(() => cuantoCubren([mini(30_000), mini(20_000)], 40_000));

    entoncesElResultadoEs(expect.stringMatching(/alcanza para llegar/));
  });

  it('sin brecha no dice nada', () => {
    cuandoSeCalcula(() => cuantoCubren([mini(30_000)], 0));

    entoncesElResultadoEs(null);
  });

  it('sin aporte no dice nada', () => {
    cuandoSeCalcula(() => cuantoCubren([mini(0)], 50_000));

    entoncesElResultadoEs(null);
  });
});
