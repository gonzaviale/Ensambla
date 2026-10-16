import { useMutation, useQueryClient } from '@tanstack/react-query';
import { crearBloque } from '@/api/bloques';
import { crearCampo } from '@/api/campos';
import { crearProyecto } from '@/api/proyectos';
import { pieza as piezaDelCatalogo, nombreDePieza } from '@/lib/catalogo/piezas';
import type { PiezaId, RolCampo, RubroId } from '@/types/negocio';
import type { Proyecto } from '@/types/proyecto';
import { proyectosKeys } from '../proyectos/queries';

// Arma las tablas de una pieza con la API de siempre (bloque + columnas). La fórmula de «Ganancia»
// cita las columnas por id, así que se crean en orden y se reemplazan los roles por los ids.

async function crearPieza(proyectoId: string, id: PiezaId, rubro: RubroId) {
  const definicion = piezaDelCatalogo(id);
  const bloque = await crearBloque({ proyectoId, nombre: nombreDePieza(id, rubro) });
  const idsPorRol = new Map<RolCampo, string>();
  for (const campo of definicion.campos) {
    let formula: string | null = null;
    if (campo.tipo === 'FORMULA' && campo.formula) {
      formula = campo.formula.replace(
        /\{(\w+)\}/g,
        (_, rol: RolCampo) => `{${idsPorRol.get(rol) ?? ''}}`,
      );
    }
    const creado = await crearCampo({
      bloqueId: bloque.id,
      datos: {
        nombre: campo.nombre,
        tipo: campo.tipo,
        obligatorio: campo.obligatorio ?? false,
        valorInicial: campo.valorInicial ?? null,
        opciones: campo.opciones ?? [],
        bloqueRelacionadoId: null,
        formula,
      },
    });
    if (campo.tipo !== 'FORMULA') idsPorRol.set(campo.rol, creado.id);
  }
  return bloque;
}

/** Crea el sistema de la persona con las piezas ensambladas (onboarding). */
export function useArmarSistema() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      nombre,
      piezas,
      rubro,
    }: {
      nombre: string;
      piezas: PiezaId[];
      rubro: RubroId;
    }): Promise<Proyecto> => {
      const proyecto = await crearProyecto({ nombre });
      for (const id of piezas) await crearPieza(proyecto.id, id, rubro);
      return proyecto;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: proyectosKeys.all }),
  });
}

/** Suma una pieza a un sistema que ya existe («Sumar bloque»). */
export function useSumarPieza(proyectoId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, rubro = 'comercio' }: { id: PiezaId; rubro?: RubroId }) =>
      crearPieza(proyectoId, id, rubro),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: proyectosKeys.detalle(proyectoId), exact: true }),
        queryClient.invalidateQueries({ queryKey: proyectosKeys.all }),
      ]),
  });
}
