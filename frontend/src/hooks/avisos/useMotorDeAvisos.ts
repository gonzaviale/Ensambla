import { useEffect, useMemo, useRef } from 'react';
import { analizar } from '@/lib/sistema/alertas';
import { hoy } from '@/lib/sistema/formato';
import { usePreferencias } from '@/hooks/preferencias/queries';
import { useZona } from '@/hooks/zona/queries';
import { useSistemaStore } from '@/stores/sistemaStore';

// El motor de avisos de Sami: cada vez que cambian los sistemas, la zona o lo que la persona decidió
// ocultar, recalcula los avisos, los deja en el store y, si apareció alguno nuevo, lo muestra un rato.
export function useMotorDeAvisos() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const zona = useZona();
  const { grupos, descartadas } = usePreferencias();
  const vistos = useRef<Set<string> | null>(null);

  const avisos = useMemo(
    () =>
      analizar({ sistemas, zona, hoy: hoy(), grupos }).filter(
        (aviso) => !descartadas.includes(aviso.id),
      ),
    [sistemas, zona, grupos, descartadas],
  );

  useEffect(() => {
    const store = useSistemaStore.getState();
    store.ponerAvisos(avisos);
    const anteriores = vistos.current;
    if (anteriores) {
      const nuevos = avisos.filter((aviso) => !anteriores.has(aviso.id));
      const primero = nuevos[0];
      if (primero && !store.panel.abierto) store.mostrarAvisoNuevo(primero, nuevos.length);
    }
    vistos.current = new Set(avisos.map((aviso) => aviso.id));
  }, [avisos]);
}
