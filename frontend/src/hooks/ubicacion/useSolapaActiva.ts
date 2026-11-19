import { useRef, useState, type KeyboardEvent } from 'react';
import { SECCIONES_DE_UBICACION, type SeccionUbicacion } from '@/types/ubicacion';

const SALTO_POR_TECLA: Record<string, number> = { ArrowRight: 1, ArrowLeft: -1 };

const siguienteSolapa = (actual: SeccionUbicacion, salto: number): SeccionUbicacion => {
  const cantidad = SECCIONES_DE_UBICACION.length;
  const indice = SECCIONES_DE_UBICACION.indexOf(actual);
  return SECCIONES_DE_UBICACION[(indice + salto + cantidad) % cantidad] ?? actual;
};

/** Qué solapa está abierta y cómo se pasa de una a otra, con el mouse o con las flechas. */
export function useSolapaActiva() {
  const [activa, setActiva] = useState<SeccionUbicacion>('comercio');
  const botones = useRef<Partial<Record<SeccionUbicacion, HTMLButtonElement | null>>>({});

  const registrarBoton = (solapa: SeccionUbicacion) => (boton: HTMLButtonElement | null) => {
    botones.current[solapa] = boton;
  };

  // Flechas izquierda y derecha para moverse entre solapas, como en cualquier lista de pestañas.
  const moverConTeclado = (evento: KeyboardEvent<HTMLButtonElement>) => {
    const salto = SALTO_POR_TECLA[evento.key];
    if (!salto) return;
    evento.preventDefault();
    const siguiente = siguienteSolapa(activa, salto);
    setActiva(siguiente);
    botones.current[siguiente]?.focus();
  };

  return { activa, elegir: setActiva, registrarBoton, moverConTeclado };
}

export type SolapaActiva = ReturnType<typeof useSolapaActiva>;
