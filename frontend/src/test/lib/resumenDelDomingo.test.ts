import { describe, expect, it } from 'vitest';
import { quePasoPosible } from '@/lib/respuestaManual';
import { armarResumenDelDomingo, estaVacio } from '@/lib/resumenDelDomingo';
import type { DecisionObjetivo, MiniObjetivo, SemanaObjetivo } from '@/types/objetivos';

const SEMANA = '2026-11-09';

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

describe('resumen del domingo', () => {
  it('separa lo que se cumplió, lo que no y lo nuevo, sólo con lo de la semana que cerró', () => {
    const resumen = armarResumenDelDomingo({
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

    expect(resumen.cumplido).toEqual(['Hablar con 3 clientes']);
    expect(resumen.noCumplido).toEqual([
      { titulo: 'Armar el cartel', motivo: 'EN_PARTE' },
      { titulo: 'Saludar por el nombre', motivo: 'NO_HECHO' },
      { titulo: 'Anotar qué piden', motivo: 'SIN_RESPUESTA' },
    ]);
    // Lo que empezó antes del domingo no es nuevo.
    expect(resumen.nuevo).toEqual(['Hablar con 5 clientes']);
  });

  it('suma los reajustes de los medidos: subir la meta es cumplido, cambiar o reemplazar es lo que no se dio', () => {
    const resumen = armarResumenDelDomingo({
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

    expect(resumen.cumplido).toEqual(['Subile $150 a la yerba']);
    expect(resumen.noCumplido).toEqual([
      { titulo: 'Recortá gastos', motivo: 'CAMBIADO' },
      { titulo: 'Vendé más', motivo: 'CAMBIADO' },
    ]);
    expect(resumen.nuevo).toEqual(['Subile $250 a la yerba', 'Mové 6 Fernet']);
  });

  it('una semana sin nada que contar queda vacía', () => {
    const resumen = armarResumenDelDomingo({
      semana: cierre(),
      decisiones: [],
      minis: [],
      sinResponder: [],
    });

    expect(estaVacio(resumen)).toBe(true);
  });

  it('«no tuve tiempo» no combina con «lo hice»', () => {
    expect(quePasoPosible('SI', 'NO_TUVE_TIEMPO')).toBe(false);
    expect(quePasoPosible('SI', 'FUNCIONO')).toBe(true);
    expect(quePasoPosible('EN_PARTE', 'NO_TUVE_TIEMPO')).toBe(true);
    expect(quePasoPosible('NO', 'NO_TUVE_TIEMPO')).toBe(true);
  });
});
