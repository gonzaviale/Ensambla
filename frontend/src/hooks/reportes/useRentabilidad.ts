import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { avisoEconomicoQuery } from '@/hooks/reportes/queries';
import { useCuadernoElegido } from '@/hooks/reportes/useCuadernoElegido';
import { diapositivasDelAviso, type Diapositiva } from '@/lib/sistema/rentabilidad';
import type { AvisoEconomico } from '@/types/avisoEconomico';

const CLAVE_DE_DESCARTADAS = 'ensambla.inicio.rentabilidadDescartada';
const MAXIMO_DE_DESCARTADAS = 60;

const leerDescartadas = (): string[] => {
  try {
    const guardado: unknown = JSON.parse(window.localStorage.getItem(CLAVE_DE_DESCARTADAS) ?? '[]');
    return Array.isArray(guardado)
      ? guardado.filter((item): item is string => typeof item === 'string')
      : [];
  } catch {
    return [];
  }
};

/**
 * Las señales de rentabilidad del negocio que se está mirando (esta semana frente a la anterior), sin las que se
 * descartaron. Mientras carga, si falla o si todavía no hay una semana anterior para comparar, no hay ninguna.
 */
export function useRentabilidad(): {
  diapositivas: Diapositiva[];
  aviso: AvisoEconomico | undefined;
  descartar: (clave: string) => void;
} {
  const { cuaderno } = useCuadernoElegido();
  const consulta = useQuery({
    ...avisoEconomicoQuery(cuaderno?.id ?? ''),
    enabled: cuaderno !== undefined,
  });
  const [descartadas, setDescartadas] = useState(leerDescartadas);
  const aviso = consulta.data?.aviso ?? undefined;

  const diapositivas =
    aviso && cuaderno
      ? diapositivasDelAviso(aviso, cuaderno.id).filter(
          (diapositiva) => !descartadas.includes(diapositiva.clave),
        )
      : [];

  const descartar = (clave: string) => {
    const siguientes = [...new Set([...descartadas, clave])].slice(-MAXIMO_DE_DESCARTADAS);
    setDescartadas(siguientes);
    try {
      window.localStorage.setItem(CLAVE_DE_DESCARTADAS, JSON.stringify(siguientes));
    } catch {
      // Sin almacenamiento funciona igual: sólo no se acuerda de lo descartado.
    }
  };

  return { diapositivas, aviso, descartar };
}
