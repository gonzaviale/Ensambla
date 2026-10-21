import { totals } from '@/lib/sistema/core';
import type { Plan, Planes } from '@/types/planes';
import { estado, sistemasDe } from './estado';

const CATALOGO: Plan[] = [
  {
    codigo: 'GRATUITO',
    nombre: 'Gratuito',
    precioConfigurado: false,
    limitesConfigurados: true,
    precioMensual: 0,
    moneda: null,
    limiteProyectos: 1,
    limiteRegistros: 1000,
    limitePaginasIaMensuales: 10,
    funciones: [],
  },
  {
    codigo: 'PREMIUM',
    nombre: 'Premium',
    precioConfigurado: true,
    limitesConfigurados: true,
    precioMensual: 14900,
    moneda: 'ARS',
    limiteProyectos: null,
    limiteRegistros: null,
    limitePaginasIaMensuales: 100,
    funciones: ['RECETAS_COSTOS', 'REPORTES_FILTRADOS', 'EXPORTAR_EXCEL'],
  },
];

/** Sólo para el modo de demostración completo: la aplicación real siempre consulta al servidor. */
export function planesDe(email: string, ahora = new Date()): Planes {
  const prueba = estado.pruebasPremium.get(email) ?? null;
  const activa =
    prueba !== null &&
    Date.parse(prueba.inicioEn) <= ahora.getTime() &&
    Date.parse(prueba.finEn) > ahora.getTime();
  const inicioMes = new Date(Date.UTC(ahora.getUTCFullYear(), ahora.getUTCMonth(), 1));
  const finMes = new Date(Date.UTC(ahora.getUTCFullYear(), ahora.getUTCMonth() + 1, 1));
  const sistemas = sistemasDe(email);
  return {
    consultadoEn: ahora.toISOString(),
    planes: structuredClone(CATALOGO),
    suscripcion: {
      codigoPlanActual: activa ? 'PREMIUM' : 'GRATUITO',
      pruebaActiva: activa,
      puedeIniciarPrueba: prueba === null,
      inicioEn: prueba?.inicioEn ?? inicioMes.toISOString(),
      finEn: activa ? prueba.finEn : null,
    },
    uso: {
      proyectos: sistemas.length,
      registros: sistemas.reduce((suma, sistema) => suma + totals(sistema).records, 0),
      paginasIa: activa ? prueba.paginasIaUtilizadas : 0,
      limitePaginasIa: 10,
      periodoIa: activa ? 'PRUEBA' : 'MENSUAL',
      inicioPeriodoIa: activa ? prueba.inicioEn : inicioMes.toISOString(),
      finPeriodoIa: activa ? prueba.finEn : finMes.toISOString(),
    },
    prueba,
  };
}
