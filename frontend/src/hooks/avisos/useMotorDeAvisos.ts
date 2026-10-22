import { useEffect, useMemo, useRef } from 'react';
import { analizar } from '@/lib/sistema/alertas';
import { aAvisoDeSami } from '@/lib/sistema/avisosServidor';
import { hoy } from '@/lib/sistema/formato';
import { useAvisosDelServidor } from '@/hooks/avisos/queries';
import { usePreferencias } from '@/hooks/preferencias/queries';
import { useZona } from '@/hooks/zona/queries';
import { useSistemaStore } from '@/stores/sistemaStore';

// El motor de avisos de Sami: reúne los que calcula en el navegador (a partir de los sistemas y la zona) con los
// que el motor del servidor dejó pendientes, los deja en el store y, si apareció alguno nuevo, lo muestra un rato.
// Cada aviso del servidor lleva el id de su cuenta, así que no se pisa con los del navegador; si el servidor trae
// avisos de un tipo, los del navegador de ese tipo se dejan de mostrar.
export function useMotorDeAvisos() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const zona = useZona();
  const { grupos, descartadas } = usePreferencias();
  const delServidor = useAvisosDelServidor();
  const vistos = useRef<Set<string> | null>(null);

  const avisos = useMemo(() => {
    const nombres = new Map(sistemas.map((sistema) => [sistema.id, sistema.name]));
    const remotos = delServidor
      .map((aviso) => aAvisoDeSami(aviso, nombres))
      .filter((aviso) => grupos[aviso.grupo] !== false);
    // Si el servidor ya trae avisos de un tipo (producto estancado, combos…), los que Sami calcula en el navegador
    // para ese mismo tipo se omiten: serían los mismos avisos duplicados.
    const tiposDelServidor = new Set(remotos.map((aviso) => aviso.tipo));
    const locales = analizar({ sistemas, zona, hoy: hoy(), grupos }).filter(
      (aviso) => !descartadas.includes(aviso.id) && !tiposDelServidor.has(aviso.tipo),
    );
    return [...remotos, ...locales].sort((a, b) => b.prioridad - a.prioridad);
  }, [sistemas, zona, grupos, descartadas, delServidor]);

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
