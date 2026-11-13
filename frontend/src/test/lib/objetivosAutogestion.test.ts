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

describe('objetivos que se autogestionan', () => {
  beforeEach(() => localStorage.clear());

  it('muestra cada valor en su unidad', () => {
    expect(valorDeMini('SUBIR_PRECIO', 14200)).toBe('$14.200');
    expect(valorDeMini('VENTAS_SEMANALES', 90000)).toBe('$90.000 por semana');
    expect(valorDeMini('RECORTAR_GASTO', 65000)).toBe('$65.000 en el mes');
    expect(valorDeMini('MEJORAR_MARGEN', 32.5)).toBe('32,5 %');
    expect(valorDeMini('MOVER_PARADO', 6)).toBe('6 por semana');
    expect(valorDeMini('PONER_A_LA_VISTA', 12)).toBe('12 por semana');
  });

  it('cuenta la semana por su lunes, también si el día es domingo', () => {
    expect(lunesDe('2026-11-12')).toBe('2026-11-09');
    expect(lunesDe('2026-11-15')).toBe('2026-11-09');
    expect(lunesDe('2026-11-09')).toBe('2026-11-09');
    expect(diaCorto('2026-11-09')).toBe('9 nov');
    expect(diaCorto('2026-11-11T15:00:00Z')).toBe('11 nov');
  });

  it('cuenta el efecto de una decisión solo cuando ya se midió', () => {
    expect(efectoDe(decision({ efecto: 'MIDIENDO' }))).toBe('Sami todavía lo está midiendo.');
    expect(efectoDe(decision({ efecto: 'MEDIDO', deltaIngresoSemanal: 4500 }))).toBe(
      'Después vendiste $4.500 más por semana.',
    );
    expect(efectoDe(decision({ efecto: 'MEDIDO', deltaIngresoSemanal: -800 }))).toBe(
      'Después vendiste $800 menos por semana.',
    );
    expect(efectoDe(decision({ efecto: 'MEDIDO', deltaIngresoSemanal: 0 }))).toBe(
      'Las ventas no cambiaron.',
    );
    expect(efectoDe(decision())).toBeNull();
  });

  it('junta las decisiones de todas las semanas de la más vieja a la más nueva', () => {
    const vieja = decision({ id: 'vieja', tomadaEn: '2026-11-03T10:00:00Z' });
    const nueva = decision({ id: 'nueva' });

    expect(
      todasLasDecisiones({
        semanas: [
          { semanaInicio: '2026-11-09', decisiones: [nueva] },
          { semanaInicio: '2026-11-02', decisiones: [vieja] },
        ],
      }).map((item) => item.id),
    ).toEqual(['vieja', 'nueva']);
    expect(todasLasDecisiones(undefined)).toEqual([]);
  });

  it('toma la última decisión de «lo hago» o «ahora no» de la semana sobre ese mini', () => {
    const lista = [
      decision({ id: 'a' }),
      decision({ id: 'b', tipo: 'PASO_IGNORADO' }),
      decision({ id: 'c', tipo: 'AVISO_APLICADO' }),
      decision({ id: 'd', miniObjetivoId: 'otro' }),
    ];

    expect(decisionDeLaSemana(lista, 'mini-1', '2026-11-09')).toBe('IGNORADO');
    expect(decisionDeLaSemana(lista.slice(0, 1), 'mini-1', '2026-11-09')).toBe('ACEPTADO');
    expect(decisionDeLaSemana(lista, 'mini-1', '2026-11-02')).toBeUndefined();
  });

  it('recuerda por objetivo qué cierre ya vio la persona', () => {
    expect(resumenSinVer('o1', null)).toBe(false);
    expect(resumenSinVer('o1', '2026-11-02')).toBe(true);

    marcarResumenVisto('o1', '2026-11-02');

    expect(resumenSinVer('o1', '2026-11-02')).toBe(false);
    expect(resumenSinVer('o1', '2026-11-09')).toBe(true);
    expect(resumenSinVer('o2', '2026-11-02')).toBe(true);
  });

  it('si lo guardado está roto, trata los cierres como no vistos', () => {
    localStorage.setItem('ensambla.objetivos.resumenVisto', '{roto');

    expect(resumenSinVer('o1', '2026-11-02')).toBe(true);
  });

  it('separa el número de su unidad y dice cuánto falta, también cuando la meta es bajar', () => {
    expect(partesDeMini('VENTAS_SEMANALES', 84000)).toEqual({
      numero: '$84.000',
      unidad: 'por semana',
    });
    expect(partesDeMini('SUBIR_PRECIO', 15100)).toEqual({ numero: '$15.100', unidad: '' });
    expect(partesDeMini('MEJORAR_MARGEN', 28)).toEqual({ numero: '28 %', unidad: 'de margen' });

    const base = {
      tipo: 'RECORTAR_GASTO' as const,
      sentido: 'BAJA' as const,
      valorActual: 80000,
      meta: 65000,
    };
    expect(faltaParaLaMeta({ ...miniDePrueba, ...base })).toBe('$15.000 en el mes');
    expect(faltaParaLaMeta({ ...miniDePrueba, ...base, valorActual: 60000 })).toBeNull();
    expect(faltaParaLaMeta({ ...miniDePrueba, valorActual: 3, meta: 6 })).toBe('3 por semana');
    expect(domingoDe('2026-11-09')).toBe('2026-11-15');
  });
});

const miniDePrueba = {
  id: 'mini-1',
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

describe('cuantoCubren', () => {
  const mini = (
    aporteEstimado: number,
    estado: MiniObjetivo['estado'] = 'EN_CURSO',
  ): MiniObjetivo => ({
    id: `m-${aporteEstimado}-${estado}`,
    posicion: 1,
    tipo: 'VENTAS_SEMANALES',
    sentido: 'SUBE',
    estado,
    titulo: 'Vender más',
    justificacion: '',
    referencia: null,
    registroId: null,
    valorInicial: 0,
    valorActual: 0,
    meta: 1,
    avance: 0,
    aporteEstimado,
    semanaInicio: '2026-11-09',
    textoConIa: false,
  });

  it('cuenta lo que suman los mini en curso frente a lo que falta', () => {
    expect(
      cuantoCubren([mini(25_714), mini(11_999), mini(1_660), mini(9_000, 'LOGRADO')], 972_000),
    ).toMatch(/sumás cerca de \$39\.373 por mes de los \$972\.000 que te faltan/);
  });

  it('avisa cuando alcanzan para llegar a la meta', () => {
    expect(cuantoCubren([mini(30_000), mini(20_000)], 40_000)).toMatch(/alcanza para llegar/);
  });

  it('sin brecha o sin aporte no dice nada', () => {
    expect(cuantoCubren([mini(30_000)], 0)).toBeNull();
    expect(cuantoCubren([mini(0)], 50_000)).toBeNull();
  });
});
