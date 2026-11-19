import { yaEstaALaVista } from '@/lib/metricas/recetas';
import type { Sistema } from '@/lib/sistema/tipos';
import {
  FUENTES_DE_METRICA,
  type DefinicionDeMetrica,
  type FuenteDeMetrica,
  type MetricaSugerida,
  type ResultadoDeMetrica,
} from '@/types/metricas';
import { filasDe } from './recetasDeMetricas';

// Lo mismo que hace el motor del servidor, en chico: lee las filas del cuaderno por la clave de cada campo
// (no por el nombre que ve la persona), deja afuera las de ejemplo y nunca devuelve un cero inventado.

export const MAXIMO_DE_METRICAS = 12;

const TIPO_DE_BLOQUE: Record<FuenteDeMetrica, string> = {
  VENTAS: 'sales',
  GASTOS: 'expenses',
  INVENTARIO: 'inventory',
};

const DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];

export const SUGERIDAS_DE_EJEMPLO: DefinicionDeMetrica[] = [
  {
    nombre: 'Cuánto vendí esta semana',
    fuente: 'VENTAS',
    calculo: 'SUMA',
    agrupar: 'NINGUNA',
    periodo: 'SEMANA',
    soloFiado: false,
  },
  {
    nombre: 'Qué producto vendo más',
    fuente: 'VENTAS',
    calculo: 'UNIDADES',
    agrupar: 'PRODUCTO',
    periodo: 'MES',
    soloFiado: false,
  },
  {
    nombre: 'Cuánto me deben por cliente',
    fuente: 'VENTAS',
    calculo: 'SUMA',
    agrupar: 'CLIENTE',
    periodo: 'TODO',
    soloFiado: true,
  },
  {
    nombre: 'Gastos por categoría',
    fuente: 'GASTOS',
    calculo: 'SUMA',
    agrupar: 'CATEGORIA',
    periodo: 'MES',
    soloFiado: false,
  },
];

const DIAS_DEL_PERIODO = { HOY: 1, SEMANA: 7, MES: 30, TODO: 0 } as const;

const sinValor = (
  definicion: DefinicionDeMetrica,
  motivo: string,
  filasSinDato = 0,
): ResultadoDeMetrica => ({
  formato: formatoDe(definicion),
  valor: null,
  filas: [],
  motivo,
  deDondeSale: { fuente: definicion.fuente, filasUsadas: 0, filasSinDato },
});

const formatoDe = (definicion: DefinicionDeMetrica): ResultadoDeMetrica['formato'] =>
  definicion.calculo === 'CONTEO' || definicion.calculo === 'UNIDADES' ? 'NUMERO' : 'MONEDA';

const numero = (valor: unknown) => (typeof valor === 'number' ? valor : null);

// Producto y cliente son relaciones: la fila guarda el id y el nombre está en la otra tabla.
function nombresPorId(sistema: Sistema) {
  const nombres = new Map<string, string>();
  for (const bloque of sistema.sections.flatMap((seccion) => seccion.blocks))
    for (const fila of bloque.rows)
      if (typeof fila.name === 'string') nombres.set(fila.id, fila.name);
  return nombres;
}

function etiquetaDe(
  fila: Record<string, unknown>,
  definicion: DefinicionDeMetrica,
  nombres: Map<string, string>,
) {
  if (definicion.agrupar === 'DIA_DE_LA_SEMANA')
    return DIAS[new Date(`${String(fila.date)}T12:00:00`).getDay()] ?? '';
  if (definicion.agrupar === 'CLIENTE') return nombres.get(String(fila.customer)) ?? 'Sin cliente';
  if (definicion.agrupar === 'CATEGORIA')
    return String(fila.category ?? '').trim() || 'Sin categoría';
  const producto = String(fila.product ?? fila.name ?? '');
  return nombres.get(producto) ?? (producto.trim() || 'Sin nombre');
}

const bloqueDe = (sistema: Sistema, fuente: FuenteDeMetrica) =>
  sistema.sections
    .flatMap((seccion) => seccion.blocks)
    .find((otro) => otro.type === TIPO_DE_BLOQUE[fuente]);

/** Igual que el servidor: sólo se arma una métrica de lo que el negocio ya tiene. */
export const fuentesDe = (sistema: Sistema) =>
  FUENTES_DE_METRICA.filter((fuente) => bloqueDe(sistema, fuente) !== undefined);

/** Lo que da la receta con las filas de verdad del cuaderno, o por qué no se puede calcular. */
export function calcularMetricaDeEjemplo(
  definicion: DefinicionDeMetrica,
  sistema: Sistema,
  hoy: string,
): ResultadoDeMetrica {
  const bloque = bloqueDe(sistema, definicion.fuente);
  if (!bloque)
    return sinValor(
      definicion,
      `Todavía no cargaste ${filasDe(definicion.fuente)} en este negocio.`,
    );
  const filas = bloque.rows.filter((fila) => !fila.__example);
  if (filas.length === 0)
    return sinValor(definicion, `Todavía no cargaste ${filasDe(definicion.fuente)}.`);

  const dias = DIAS_DEL_PERIODO[definicion.periodo];
  const desde = dias === 0 ? null : restarDias(hoy, dias - 1);
  const delPeriodo = filas.filter(
    (fila) =>
      (!desde || (typeof fila.date === 'string' && fila.date >= desde && fila.date <= hoy)) &&
      (!definicion.soloFiado || fila.credit === true),
  );
  if (delPeriodo.length === 0)
    return sinValor(definicion, `No hay ${filasDe(definicion.fuente)} en este período.`);

  const importe = (fila: Record<string, unknown>) => {
    if (definicion.fuente === 'GASTOS') return numero(fila.total);
    if (definicion.fuente === 'INVENTARIO') {
      const costo = numero(fila.cost);
      const cantidad = numero(fila.quantity);
      return costo === null || cantidad === null ? null : costo * cantidad;
    }
    const precio = numero(fila.price);
    const cantidad = numero(fila.quantity);
    return precio === null || cantidad === null ? null : precio * cantidad;
  };
  const valorDe = (fila: Record<string, unknown>) =>
    definicion.calculo === 'UNIDADES'
      ? numero(fila.quantity)
      : definicion.calculo === 'CONTEO'
        ? 1
        : importe(fila);

  const usables = delPeriodo.filter((fila) => valorDe(fila) !== null);
  const filasSinDato = delPeriodo.length - usables.length;
  if (usables.length === 0)
    return sinValor(definicion, 'Faltan datos en las filas para poder calcularlo.', filasSinDato);

  const agrupar = (grupo: Record<string, unknown>[]) => {
    const total = grupo.reduce((suma, fila) => suma + (valorDe(fila) ?? 0), 0);
    return definicion.calculo === 'PROMEDIO' ? total / grupo.length : total;
  };
  const nombres = nombresPorId(sistema);
  const grupos = new Map<string, Record<string, unknown>[]>();
  if (definicion.agrupar !== 'NINGUNA')
    for (const fila of usables) {
      const etiqueta = etiquetaDe(fila, definicion, nombres);
      grupos.set(etiqueta, [...(grupos.get(etiqueta) ?? []), fila]);
    }
  const reparto = [...grupos.entries()]
    .map(([etiqueta, grupo]) => ({ etiqueta, valor: agrupar(grupo) }))
    .sort((a, b) => b.valor - a.valor)
    .slice(0, 10);

  return {
    formato: formatoDe(definicion),
    valor: agrupar(usables),
    filas: reparto,
    motivo: null,
    deDondeSale: {
      fuente: definicion.fuente,
      filasUsadas: usables.length,
      filasSinDato,
      desde,
      hasta: desde ? hoy : null,
    },
  };
}

function restarDias(iso: string, dias: number) {
  const fecha = new Date(`${iso}T12:00:00`);
  fecha.setDate(fecha.getDate() - dias);
  return fecha.toISOString().slice(0, 10);
}

export function sugeridasDe(
  sistema: Sistema,
  guardadas: DefinicionDeMetrica[],
  hoy: string,
): MetricaSugerida[] {
  return SUGERIDAS_DE_EJEMPLO.map((definicion) => ({
    definicion,
    resultado: calcularMetricaDeEjemplo(definicion, sistema, hoy),
    yaLaTenes: yaEstaALaVista(definicion, guardadas),
  }));
}
