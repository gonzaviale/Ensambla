import { keepPreviousData, queryOptions, useQueries, useQuery } from '@tanstack/react-query';
import { listarReferencias, listarRegistros } from '@/api/registros';
import type { EtiquetaRelacion } from '@/lib/valores';
import type { Campo } from '@/types/proyecto';
import type { FiltroRegistros, Registro, Valor } from '@/types/registro';
import { useDebouncedValue } from '../useDebouncedValue';

// Las referencias de una tabla cuelgan de sus registros: lo que invalida las filas de una tabla
// (cargar, borrar, cambiar sus columnas) también refresca las etiquetas que la nombran.
export const registrosKeys = {
  bloque: (bloqueId: string) => ['registros', bloqueId] as const,
  lista: (bloqueId: string, filtro: FiltroRegistros) => ['registros', bloqueId, filtro] as const,
  referencias: (bloqueId: string) => ['registros', bloqueId, 'referencias'] as const,
  busquedaReferencias: (bloqueId: string, q: string) =>
    ['registros', bloqueId, 'referencias', { q }] as const,
  etiquetas: (bloqueId: string, ids: readonly string[]) =>
    ['registros', bloqueId, 'referencias', { ids }] as const,
};

// Al buscar, ordenar o paginar se conserva la página anterior mientras llega la nueva.
export const registrosQuery = (bloqueId: string, filtro: FiltroRegistros) =>
  queryOptions({
    queryKey: registrosKeys.lista(bloqueId, filtro),
    queryFn: ({ signal }) => listarRegistros(bloqueId, filtro, signal),
    placeholderData: keepPreviousData,
  });

export function useRegistros(bloqueId: string, filtro: FiltroRegistros) {
  return useQuery(registrosQuery(bloqueId, filtro));
}

export const referenciasQuery = (bloqueId: string, q: string) =>
  queryOptions({
    queryKey: registrosKeys.busquedaReferencias(bloqueId, q),
    queryFn: ({ signal }) => listarReferencias(bloqueId, { q }, signal),
    placeholderData: keepPreviousData,
  });

export const etiquetasQuery = (bloqueId: string, ids: readonly string[]) =>
  queryOptions({
    queryKey: registrosKeys.etiquetas(bloqueId, ids),
    queryFn: ({ signal }) => listarReferencias(bloqueId, { ids }, signal),
    placeholderData: keepPreviousData,
  });

const ESPERA_BUSQUEDA_MS = 300;

/** Filas de otra tabla para elegir en una columna que se conecta con ella; busca mientras se escribe. */
export function useReferencias(bloqueId: string, texto: string) {
  const busqueda = useDebouncedValue(texto.trim(), ESPERA_BUSQUEDA_MS);
  return { busqueda, ...useQuery(referenciasQuery(bloqueId, busqueda)) };
}

/** Etiqueta de una sola fila de otra tabla (null si ya no existe); con `id` vacío no pide nada. */
export function useEtiquetaDeFila(bloqueId: string, id: string) {
  return useQuery({
    ...etiquetasQuery(bloqueId, [id]),
    enabled: Boolean(id),
    select: (referencias) =>
      referencias.find((referencia) => referencia.id === id)?.etiqueta ?? null,
  });
}

/**
 * Etiquetas de los valores de las columnas que se conectan con otra tabla. Junta los ids que hay en
 * `registros` por tabla de destino y los pide una sola vez por tabla (`?ids=`). Devuelve cómo
 * mostrar cada valor: su etiqueta, que todavía está cargando o que esa fila ya no existe.
 */
export function useEtiquetas(campos: readonly Campo[], registros: readonly Registro[]) {
  const idsPorTabla = new Map<string, Set<string>>();
  for (const campo of campos) {
    if (campo.tipo !== 'RELACION' || !campo.bloqueRelacionadoId) continue;
    const ids = idsPorTabla.get(campo.bloqueRelacionadoId) ?? new Set<string>();
    for (const registro of registros) {
      const valor = registro.valores[campo.id];
      if (typeof valor === 'string' && valor) ids.add(valor);
    }
    idsPorTabla.set(campo.bloqueRelacionadoId, ids);
  }
  const tablas = [...idsPorTabla]
    .filter(([, ids]) => ids.size > 0)
    .map(([bloqueId, ids]) => ({ bloqueId, ids: [...ids].sort() }));

  const resultados = useQueries({
    queries: tablas.map(({ bloqueId, ids }) => etiquetasQuery(bloqueId, ids)),
  });

  return (campo: Campo, valor: Valor | undefined): EtiquetaRelacion | undefined => {
    if (campo.tipo !== 'RELACION' || typeof valor !== 'string' || !valor) return undefined;
    const indice = tablas.findIndex((tabla) => tabla.bloqueId === campo.bloqueRelacionadoId);
    const resultado = resultados[indice];
    if (!resultado || resultado.isPending) return { estado: 'cargando' };
    if (resultado.isError) return { estado: 'error' };
    const encontrada = resultado.data.find((referencia) => referencia.id === valor);
    if (encontrada) return { estado: 'lista', texto: encontrada.etiqueta };
    // Mientras llegan las de otra página se ven las anteriores: la que falta todavía no se pidió.
    return resultado.isPlaceholderData ? { estado: 'cargando' } : { estado: 'eliminada' };
  };
}
