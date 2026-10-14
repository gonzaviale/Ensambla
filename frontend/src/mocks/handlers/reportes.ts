import { delay, http, HttpResponse } from 'msw';
import type { Bloque } from '@/types/proyecto';
import type { Registro } from '@/types/registro';
import { conFormulas } from '../db/formula';
import { estado } from '../db/estado';
import { errorResponse } from '../errores';
import { api, exigirCsrf, exigirSesion, LATENCIA_BACKEND_MS } from './comun';
import { bloqueDeLaSesion, coincide } from './registros';

// Mismo contrato que GET /api/bloques/{id}/reporte y /reporte/descarga del backend: las primeras filas y los
// totales de todas las que cumplen el filtro; el archivo trae todas. Las fechas son las del día en Argentina.
const MAXIMO_VISTA = 50;
const MAXIMO_EXPORTACION = 10_000;
const ZONA = '-03:00';
const TIPO_XLSX = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

const pathDe = (request: Request) => new URL(request.url).pathname;

function privado(request: Request, escritura = false) {
  return (escritura && exigirCsrf(request)) || exigirSesion(request);
}

const comienzoDelDia = (fecha: string, sumarDias = 0) =>
  new Date(new Date(`${fecha}T00:00:00${ZONA}`).getTime() + sumarDias * 86_400_000);

type Consulta = {
  texto: string;
  desde: string | null;
  hasta: string | null;
  campoId: string | null;
  valor: string | null;
};

function leerConsulta(request: Request): Consulta {
  const parametros = new URL(request.url).searchParams;
  return {
    texto: parametros.get('q')?.trim().toLowerCase() ?? '',
    desde: parametros.get('desde'),
    hasta: parametros.get('hasta'),
    campoId: parametros.get('campoId'),
    valor: parametros.get('valor'),
  };
}

/** Las filas del bloque que cumplen el filtro (las más nuevas primero) o la respuesta de error. */
function filtrar(bloque: Bloque, consulta: Consulta, path: string): Registro[] | Response {
  const { texto, desde, hasta, campoId, valor } = consulta;
  if (desde && hasta && desde > hasta) {
    return errorResponse(400, 'La fecha desde no puede ser posterior a la fecha hasta', path);
  }
  if (campoId) {
    const campo = bloque.campos.find((item) => item.id === campoId);
    if (!campo || (campo.tipo !== 'OPCION' && campo.tipo !== 'SI_NO')) {
      return errorResponse(400, 'No se puede filtrar por ese campo', path);
    }
    const validos = campo.tipo === 'SI_NO' ? ['true', 'false'] : campo.opciones;
    if (!valor || !validos.includes(valor)) {
      return errorResponse(400, 'Elegí un valor de la columna «' + campo.nombre + '»', path);
    }
  }
  const inicio = desde ? comienzoDelDia(desde) : null;
  const fin = hasta ? comienzoDelDia(hasta, 1) : null;
  return [...estado.registros.values()]
    .filter((registro) => registro.bloqueId === bloque.id)
    .filter((registro) => !texto || coincide(registro, texto))
    .filter((registro) => !inicio || new Date(registro.creadoEn) >= inicio)
    .filter((registro) => !fin || new Date(registro.creadoEn) < fin)
    .filter((registro) => !campoId || String(registro.valores[campoId]) === valor)
    .sort((a, b) => b.creadoEn.localeCompare(a.creadoEn) || b.id.localeCompare(a.id));
}

function totalesDe(bloque: Bloque, registros: Registro[]) {
  const sumas = Object.fromEntries(
    bloque.campos
      .filter((campo) => campo.tipo === 'NUMERO')
      .map((campo) => [
        campo.id,
        Number(
          registros
            .reduce((total, registro) => {
              const valor = registro.valores[campo.id];
              return typeof valor === 'number' ? total + valor : total;
            }, 0)
            .toFixed(6),
        ),
      ]),
  );
  return { cantidad: registros.length, sumas };
}

const sinTildes = (texto: string) =>
  texto
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');

/**
 * El «archivo» que baja el modo simulado: texto separado por tabuladores (no una planilla real) con los
 * encabezados, todas las filas y la fila de totales, para poder comprobar que coincide con la pantalla.
 */
function archivoDe(bloque: Bloque, registros: Registro[]) {
  const campos = [...bloque.campos].sort((a, b) => a.posicion - b.posicion);
  const totales = totalesDe(bloque, registros);
  const lineas = [
    [...campos.map((campo) => campo.nombre), 'Cargado'].join('\t'),
    ...registros.map((registro) => {
      const valores = conFormulas(bloque.campos, registro.valores);
      return [...campos.map((campo) => String(valores[campo.id] ?? '')), registro.creadoEn].join(
        '\t',
      );
    }),
    [
      ...campos.map((campo, indice) =>
        campo.tipo === 'NUMERO'
          ? String(totales.sumas[campo.id])
          : indice === 0
            ? `Total (${totales.cantidad} filas)`
            : '',
      ),
      '',
    ].join('\t'),
  ];
  return lineas.join('\n');
}

export const handlersReportes = [
  http.get(api('/bloques/:id/reporte'), async ({ request, params }) => {
    const error = privado(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const path = pathDe(request);
    const bloque = bloqueDeLaSesion(String(params.id));
    if (!bloque) return errorResponse(404, 'El bloque no existe', path);
    const filas = filtrar(bloque, leerConsulta(request), path);
    if (filas instanceof Response) return filas;
    return HttpResponse.json({
      filas: filas.slice(0, MAXIMO_VISTA).map((fila) => ({
        ...fila,
        valores: conFormulas(bloque.campos, fila.valores),
      })),
      totales: totalesDe(bloque, filas),
      truncado: filas.length > MAXIMO_VISTA,
      maximoFilas: MAXIMO_VISTA,
    });
  }),

  http.get(api('/bloques/:id/reporte/descarga'), async ({ request, params }) => {
    const error = privado(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const path = pathDe(request);
    const bloque = bloqueDeLaSesion(String(params.id));
    if (!bloque) return errorResponse(404, 'El bloque no existe', path);
    const filas = filtrar(bloque, leerConsulta(request), path);
    if (filas instanceof Response) return filas;
    if (filas.length > MAXIMO_EXPORTACION) {
      return errorResponse(
        400,
        `El reporte tiene más de ${MAXIMO_EXPORTACION} filas: acotá los filtros para poder descargarlo`,
        path,
      );
    }
    const hoy = new Date().toISOString().slice(0, 10).replaceAll('-', '');
    return new HttpResponse(archivoDe(bloque, filas), {
      headers: {
        'Content-Type': TIPO_XLSX,
        'Content-Disposition': `attachment; filename="reporte-${sinTildes(bloque.nombre) || 'tabla'}-${hoy}.xlsx"`,
      },
    });
  }),
];
