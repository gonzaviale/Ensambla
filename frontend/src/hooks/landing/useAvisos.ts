import { useCallback, useState } from 'react';

export type ColorAviso = 'coral' | 'celeste' | 'naranja' | 'amarillo';
export type Resolucion = 'aplicado' | 'descartado';

export interface Aviso {
  id: string;
  tipo: string;
  color: ColorAviso;
  titulo: string;
  motivo: string;
  accion: string;
  resultado: string;
}

// Avisos de muestra para la demo de la landing (datos ilustrativos, no reales).
export const AVISOS: readonly Aviso[] = [
  {
    id: 'precio-bajo',
    tipo: 'Precio bajo',
    color: 'coral',
    titulo: 'La gaseosa 1,5 L está más barata que en tu zona',
    motivo: 'La vendés a $2.300 y en comercios cercanos ronda los $2.600.',
    accion: 'Subir el precio a $2.500',
    resultado: 'Precio actualizado a $2.500.',
  },
  {
    id: 'estancado',
    tipo: 'Producto estancado',
    color: 'celeste',
    titulo: 'La yerba de 1 kg está cara y casi no sale',
    motivo: 'No tuvo ventas en 21 días y en tu zona se vende un 8 % más barata.',
    accion: 'Bajar el precio a $4.200',
    resultado: 'Precio actualizado a $4.200.',
  },
  {
    id: 'muy-vendido',
    tipo: 'Muy vendido',
    color: 'naranja',
    titulo: 'El pan lactal vuela y aguanta un aumento',
    motivo: 'Vendiste 64 esta semana, el doble que la anterior.',
    accion: 'Subir el precio un 5 %',
    resultado: 'Precio actualizado con un 5 % más.',
  },
  {
    id: 'combo',
    tipo: 'Combo recomendado',
    color: 'amarillo',
    titulo: 'Fideos y salsa salen poco: armá un combo',
    motivo: 'En 3 de cada 4 comercios de tu zona se compran juntos.',
    accion: 'Crear el combo a $3.100',
    resultado: 'Combo creado a $3.100.',
  },
];

// Estado de la demo del centro de avisos: cada aviso se aplica o se descarta, y se puede reiniciar.
export function useAvisos() {
  const [resueltos, setResueltos] = useState<Partial<Record<string, Resolucion>>>({});

  const resolver = useCallback((id: string, resolucion: Resolucion) => {
    setResueltos((prev) => ({ ...prev, [id]: resolucion }));
  }, []);

  const reiniciar = useCallback(() => setResueltos({}), []);

  const pendientes = AVISOS.filter((a) => !resueltos[a.id]).length;

  return { resueltos, pendientes, resolver, reiniciar };
}
