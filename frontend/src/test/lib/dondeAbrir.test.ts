import { describe, expect, it } from 'vitest';
import {
  CAPAS,
  ETIQUETA_DE_COINCIDENCIA,
  FACTORES,
  FILTROS_INICIALES,
  LOCAL_ACTUAL,
  PLURAL_DE_RUBRO,
  PRESETS,
  SIN_LIMITE_ALQUILER,
  SIN_LIMITE_COMPETENCIA,
  SIN_LIMITE_KM,
  UMBRAL_BUENA,
  UMBRAL_INTERMEDIA,
  ZONAS,
  conPreset,
  distanciaEnKm,
  explorar,
  filtrosActivos,
  pesosPara,
  presetActivo,
  puntosDeLaZona,
  puntuar,
  queVerificar,
  resumenParaCompartir,
  sinFiltro,
  type FactorClave,
  type Filtros,
  type Importancia,
  type Publico,
  type Rubro,
} from '@/lib/dondeAbrir';

const RUBROS: Rubro[] = ['almacen', 'papeleria', 'kiosco'];
const PUBLICOS: Publico[] = ['vecinos', 'estudiantes', 'oficinas'];

const sinLimites = (filtros: Filtros): Filtros => ({
  ...filtros,
  limites: {
    distanciaMaxKm: SIN_LIMITE_KM,
    competenciaMax: SIN_LIMITE_COMPETENCIA,
    escuelasMin: 0,
    parquesMin: 0,
    transporteMin: 0,
    alquilerMax: SIN_LIMITE_ALQUILER,
  },
});
const conImportancia = (
  filtros: Filtros,
  cambios: Partial<Record<FactorClave, Importancia>>,
): Filtros => ({
  ...filtros,
  importancia: { ...filtros.importancia, ...cambios },
});

const todosLosFiltros = (): Filtros[] =>
  RUBROS.flatMap((rubro) =>
    PUBLICOS.flatMap((publico) =>
      PRESETS.map((preset) => conPreset({ ...FILTROS_INICIALES, rubro, publico }, preset)),
    ),
  );

describe('dónde abrir: cálculo', () => {
  it('los pesos suman 1 con cualquier combinación', () => {
    for (const filtros of todosLosFiltros()) {
      const suma = Object.values(pesosPara(filtros)).reduce((a, b) => a + b, 0);
      expect(suma).toBeCloseTo(1, 10);
    }
  });

  it('un factor que no importa pesa cero, y uno que importa mucho pesa más que uno normal', () => {
    const base = pesosPara(FILTROS_INICIALES);
    const sinParques = pesosPara(conImportancia(FILTROS_INICIALES, { parques: 0 }));
    const muchoParques = pesosPara(conImportancia(FILTROS_INICIALES, { parques: 2 }));
    expect(sinParques.parques).toBe(0);
    expect(muchoParques.parques).toBeGreaterThan(base.parques);
  });

  it('si nada importa, mira todo por igual en vez de fallar', () => {
    const nada = Object.fromEntries(FACTORES.map((factor) => [factor.clave, 0])) as Record<
      FactorClave,
      Importancia
    >;
    const pesos = pesosPara({ ...FILTROS_INICIALES, importancia: nada });
    for (const peso of Object.values(pesos)) expect(peso).toBeCloseTo(1 / FACTORES.length, 10);
  });

  it('ordena las zonas de mayor a menor puntaje, entre 0 y 10', () => {
    for (const filtros of todosLosFiltros()) {
      const { ranking } = explorar(sinLimites(filtros));
      expect(ranking).toHaveLength(ZONAS.length);
      ranking.forEach((item, indice) => {
        expect(item.puntaje).toBeGreaterThanOrEqual(0);
        expect(item.puntaje).toBeLessThanOrEqual(10);
        if (indice > 0)
          expect(item.puntaje).toBeLessThanOrEqual(ranking[indice - 1]?.puntaje ?? 10);
      });
    }
  });

  it('el rubro y el público cambian cuál es la mejor zona', () => {
    const papeleria = explorar(
      sinLimites({ ...FILTROS_INICIALES, rubro: 'papeleria', publico: 'estudiantes' }),
    );
    const kiosco = explorar(
      sinLimites({ ...FILTROS_INICIALES, rubro: 'kiosco', publico: 'oficinas' }),
    );
    expect(papeleria.ranking[0]?.zona.id).not.toBe(kiosco.ranking[0]?.zona.id);
  });

  it('dar más importancia a un factor mejora a las zonas que lo tienen', () => {
    const base = sinLimites(FILTROS_INICIALES);
    const parquesAntes = explorar(base).ranking.find((item) => item.zona.id === 'villa-del-parque');
    const parquesDespues = explorar(conImportancia(base, { parques: 2 })).ranking.find(
      (item) => item.zona.id === 'villa-del-parque',
    );
    // Villa del Parque es la zona con más parques de las comparadas.
    expect(parquesDespues?.puntaje).toBeGreaterThan(parquesAntes?.puntaje ?? 0);
  });

  it('una zona pegada al local actual avisa que se pueden quitar clientes y pierde puntos', () => {
    const cercana = ZONAS.find((zona) => zona.id === 'villa-crespo-sur');
    if (!cercana) throw new Error('falta Villa Crespo Sur');
    const item = puntuar(cercana, FILTROS_INICIALES);
    expect(item.distanciaKm).toBeLessThan(1);
    expect(item.riesgos[0]).toMatch(/Queda a .* de tu local actual/);
    const bruto = item.aportes.reduce((suma, aporte) => suma + aporte.aporte, 0);
    expect(item.puntaje).toBeCloseTo(bruto - 1.2, 1);
  });

  it('solo habla de lo que a la persona le importa', () => {
    const sinParques = conImportancia(sinLimites(FILTROS_INICIALES), { parques: 0 });
    for (const item of explorar(sinParques).ranking) {
      expect([...item.porQue, ...item.riesgos].join(' ')).not.toMatch(/parque/i);
    }
  });

  it('cada zona trae razones y riesgos, con el género del rubro bien escrito', () => {
    for (const filtros of todosLosFiltros()) {
      for (const item of explorar(sinLimites(filtros)).ranking) {
        expect(item.porQue.length).toBeGreaterThan(0);
        expect(item.riesgos.length).toBeGreaterThan(0);
        expect([...item.porQue, ...item.riesgos].join(' ')).not.toMatch(/parecidos/);
      }
    }
    const frases = explorar(sinLimites({ ...FILTROS_INICIALES, rubro: 'papeleria' }))
      .ranking.flatMap((item) => [...item.porQue, ...item.riesgos])
      .join(' ');
    expect(frases).toContain(PLURAL_DE_RUBRO.papeleria);
    expect(queVerificar('papeleria').join(' ')).toContain('una papelería');
  });
});

describe('dónde abrir: límites y filtros', () => {
  it('descarta las zonas que no cumplen los límites y dice por qué', () => {
    const filtros: Filtros = {
      ...FILTROS_INICIALES,
      limites: { ...FILTROS_INICIALES.limites, distanciaMaxKm: 2, parquesMin: 2 },
    };
    const { ranking, descartadas, evaluadas } = explorar(filtros);
    expect(ranking.length + descartadas.length).toBe(evaluadas);
    for (const item of ranking) {
      expect(item.distanciaKm).toBeLessThanOrEqual(2);
      expect(item.zona.parques).toBeGreaterThanOrEqual(2);
    }
    for (const item of descartadas) expect(item.motivos.length).toBeGreaterThan(0);
    expect(
      descartadas.some((item) => item.motivos.some((motivo) => /pediste hasta/.test(motivo))),
    ).toBe(true);
    expect(
      descartadas.some((item) =>
        item.motivos.some((motivo) => /parques: pediste al menos/.test(motivo)),
      ),
    ).toBe(true);
  });

  it('sin límites compara todas las zonas y ninguna queda afuera', () => {
    const { ranking, descartadas } = explorar(sinLimites(FILTROS_INICIALES));
    expect(ranking).toHaveLength(ZONAS.length);
    expect(descartadas).toHaveLength(0);
  });

  it('límites muy exigentes pueden dejar el ranking vacío', () => {
    const filtros: Filtros = {
      ...FILTROS_INICIALES,
      limites: { ...FILTROS_INICIALES.limites, escuelasMin: 9, parquesMin: 4 },
    };
    const exploracion = explorar(filtros);
    expect(exploracion.ranking).toHaveLength(0);
  });

  it('muestra como etiquetas lo que se apartó de lo de siempre, y cada una se puede quitar', () => {
    expect(filtrosActivos(FILTROS_INICIALES)).toHaveLength(0);
    expect(filtrosActivos(sinLimites(FILTROS_INICIALES)).map((activo) => activo.texto)).toEqual([
      'Sin límite de distancia',
    ]);
    const filtros = conImportancia(
      { ...FILTROS_INICIALES, limites: { ...FILTROS_INICIALES.limites, parquesMin: 2 } },
      { escuelas: 2, oficinas: 0 },
    );
    const activos = filtrosActivos(filtros);
    const textos = activos.map((activo) => activo.texto);
    expect(textos).toContain('Al menos 2 parques');
    expect(textos).toContain('Mucho: escuelas cerca');
    expect(textos).toContain('Sin mirar: oficinas cerca');

    let restante = filtros;
    for (const activo of activos) restante = sinFiltro(restante, activo.id);
    expect(restante.limites.parquesMin).toBe(0);
    expect(restante.importancia.escuelas).toBe(1);
    expect(restante.importancia.oficinas).toBe(1);
    expect(filtrosActivos(restante)).toHaveLength(0);
  });

  it('reconoce cuándo la importancia coincide con una combinación rápida', () => {
    expect(presetActivo(FILTROS_INICIALES)).toBe('equilibrado');
    for (const preset of PRESETS)
      expect(presetActivo(conPreset(FILTROS_INICIALES, preset))).toBe(preset.id);
    expect(presetActivo(conImportancia(FILTROS_INICIALES, { salud: 2 }))).toBeNull();
  });
});

describe('dónde abrir: mapa y textos', () => {
  it('dibuja siempre los mismos puntos para una zona, tantos como dicen sus datos', () => {
    const zona = ZONAS.find((item) => item.id === 'caballito-norte');
    if (!zona) throw new Error('falta Caballito Norte');
    const primera = puntosDeLaZona(zona, 'almacen');
    expect(puntosDeLaZona(zona, 'almacen')).toEqual(primera);
    const esperados =
      zona.competencia.almacen +
      zona.escuelas +
      zona.parques +
      zona.transporte +
      zona.oficinas +
      zona.salud +
      zona.complementarios;
    expect(primera).toHaveLength(esperados);
    for (const punto of primera) expect(distanciaEnKm(punto, zona) * 1000).toBeLessThan(500);
    expect(new Set(primera.map((punto) => punto.tipo))).toEqual(
      new Set(CAPAS.map((capa) => capa.tipo)),
    );
  });

  it('las zonas de ejemplo están a una distancia razonable del local y son de la Ciudad o de La Matanza', () => {
    for (const zona of ZONAS) {
      const distancia = distanciaEnKm(LOCAL_ACTUAL, zona);
      expect(distancia).toBeGreaterThan(0);
      expect(distancia).toBeLessThan(20);
      expect(['caba', 'la-matanza']).toContain(zona.jurisdiccion);
    }
    expect(ZONAS.some((zona) => zona.jurisdiccion === 'la-matanza')).toBe(true);
  });

  it('las zonas de La Matanza solo aparecen si no se limita la distancia', () => {
    const cerca = explorar(FILTROS_INICIALES).ranking;
    const toda = explorar(sinLimites(FILTROS_INICIALES)).ranking;
    expect(cerca.some((item) => item.zona.jurisdiccion === 'la-matanza')).toBe(false);
    expect(toda.some((item) => item.zona.jurisdiccion === 'la-matanza')).toBe(true);
  });

  it('cada zona es de buena, intermedia o mala coincidencia según su puntaje', () => {
    const { ranking } = explorar(sinLimites(FILTROS_INICIALES));
    for (const item of ranking) {
      const esperada =
        item.puntaje >= UMBRAL_BUENA
          ? 'buena'
          : item.puntaje >= UMBRAL_INTERMEDIA
            ? 'intermedia'
            : 'mala';
      expect(item.coincidencia).toBe(esperada);
    }
    const etiquetas = Object.values(ETIQUETA_DE_COINCIDENCIA);
    expect(etiquetas).toEqual([
      'Buena coincidencia',
      'Coincidencia intermedia',
      'Mala coincidencia',
    ]);
    expect(new Set(ranking.map((item) => item.coincidencia)).size).toBeGreaterThan(1);
  });

  it('el resumen para compartir nombra la mejor zona y aclara que es una estimación', () => {
    const exploracion = explorar(FILTROS_INICIALES);
    const texto = resumenParaCompartir(FILTROS_INICIALES, exploracion);
    expect(texto).toContain(exploracion.ranking[0]?.zona.nombre);
    expect(texto).toMatch(/estimación/);
    expect(texto).toMatch(/no una predicción de ventas/);
  });
});
