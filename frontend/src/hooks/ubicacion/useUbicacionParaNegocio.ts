import { useCallback } from 'react';
import { buscarDirecciones, resolverLocalidad } from '@/api/ubicacion';
import type { Direccion } from '@/types/ubicacion';

export function useUbicacionParaNegocio() {
  const buscar = useCallback(async (consulta: string) => {
    const respuesta = await buscarDirecciones(consulta);
    return respuesta.resultados;
  }, []);

  const completarLocalidad = useCallback(async (direccion: Direccion) => {
    return direccion.localidad?.trim() || (await resolverLocalidad(direccion)).localidad;
  }, []);

  return { buscar, completarLocalidad };
}
