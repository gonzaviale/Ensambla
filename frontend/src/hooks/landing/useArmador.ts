import { useCallback, useEffect, useRef, useState } from 'react';

export interface BloqueArmado {
  id: number;
  nombre: string;
  saliendo: boolean;
}

const DURACION_SALIDA_MS = 400;
const DEMO = ['Inventario', 'Pedidos', 'Clientes'];

// Estado del armador de la landing: bloques sobre el tablero, con su animación de salida.
export function useArmador() {
  const [bloques, setBloques] = useState<BloqueArmado[]>([]);
  const siguienteId = useRef(0);
  const temporizadores = useRef<number[]>([]);
  const hayBloques = useRef(false);

  useEffect(() => {
    hayBloques.current = bloques.length > 0;
  }, [bloques]);

  useEffect(() => {
    const pendientes = temporizadores.current;
    return () => pendientes.forEach((id) => window.clearTimeout(id));
  }, []);

  const programar = useCallback((accion: () => void, ms: number) => {
    temporizadores.current.push(window.setTimeout(accion, ms));
  }, []);

  const agregar = useCallback((nombre: string) => {
    setBloques((prev) =>
      prev.some((b) => b.nombre === nombre && !b.saliendo)
        ? prev
        : [...prev, { id: siguienteId.current++, nombre, saliendo: false }],
    );
  }, []);

  const quitar = useCallback(
    (ids: number[]) => {
      setBloques((prev) => prev.map((b) => (ids.includes(b.id) ? { ...b, saliendo: true } : b)));
      programar(
        () => setBloques((prev) => prev.filter((b) => !ids.includes(b.id))),
        DURACION_SALIDA_MS,
      );
    },
    [programar],
  );

  const alternar = useCallback(
    (nombre: string) => {
      const activo = bloques.find((b) => b.nombre === nombre && !b.saliendo);
      if (activo) quitar([activo.id]);
      else agregar(nombre);
    },
    [agregar, bloques, quitar],
  );

  const reiniciar = useCallback(
    () => quitar(bloques.filter((b) => !b.saliendo).map((b) => b.id)),
    [bloques, quitar],
  );

  // Muestra la idea con tres bloques, una sola vez y sólo si el tablero sigue vacío.
  const mostrarDemo = useCallback(() => {
    if (hayBloques.current) return;
    DEMO.forEach((nombre, i) => programar(() => agregar(nombre), 350 + i * 280));
  }, [agregar, programar]);

  const activos = bloques.filter((b) => !b.saliendo);
  const estaActivo = (nombre: string) => activos.some((b) => b.nombre === nombre);

  return { bloques, cantidad: activos.length, estaActivo, alternar, reiniciar, mostrarDemo };
}
