import { z } from 'zod';
import { proyectoSchema, type CrearProyecto } from '@/types/proyecto';
import { http } from './client';

export const listarProyectos = (signal?: AbortSignal) =>
  http.get('/proyectos', proyectoSchema.array(), signal);
export const obtenerProyecto = (id: string, signal?: AbortSignal) =>
  http.get(`/proyectos/${encodeURIComponent(id)}`, proyectoSchema, signal);
export const crearProyecto = (datos: CrearProyecto) =>
  http.post('/proyectos', datos, proyectoSchema);
export const crearProyectoDesdePlantilla = (plantillaId: string) =>
  http.post(
    `/proyectos/desde-plantilla/${encodeURIComponent(plantillaId)}`,
    undefined,
    proyectoSchema,
  );
export const renombrarProyecto = ({ id, nombre }: { id: string; nombre: string }) =>
  http.patch(`/proyectos/${encodeURIComponent(id)}`, { nombre }, proyectoSchema);
export const eliminarProyecto = (id: string) =>
  http.delete(`/proyectos/${encodeURIComponent(id)}`, z.undefined());
