import { useEffect, useMemo, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { z } from 'zod';
import { http } from '@/api/client';
import { desbloquearAvisos } from '@/api/contenidoPlan';
import { analizar } from '@/lib/sistema/alertas';
import { aAvisoDeSami } from '@/lib/sistema/avisosServidor';
import { hoy } from '@/lib/sistema/formato';
import { useAvisosDelServidor } from '@/hooks/avisos/queries';
import { usePreferencias } from '@/hooks/preferencias/queries';
import { useZona } from '@/hooks/zona/queries';
import { useSistemaStore } from '@/stores/sistemaStore';

// El motor de avisos de Sami: reúne los que calcula en el navegador (a partir de los sistemas y la zona) con los
// que el motor del servidor dejó pendientes, los deja en el store y, si apareció alguno nuevo, lo muestra un rato.
// Cada aviso del servidor lleva el id de su cuenta, así que no se pisa con los del navegador.
export function useMotorDeAvisos() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const zona = useZona();
  const { grupos, descartadas } = usePreferencias();
  const { avisos: delServidor, cargando: avisosCargando } = useAvisosDelServidor();
  const vistos = useRef<Set<string> | null>(null);

  const candidatos = useMemo(() => {
    const nombres = new Map(sistemas.map((sistema) => [sistema.id, sistema.name]));
    const locales = analizar({ sistemas, zona, hoy: hoy(), grupos }).filter(
      (aviso) => !descartadas.includes(aviso.id),
    );
    const remotos = delServidor
      .map((aviso) => aAvisoDeSami(aviso, nombres))
      .filter((aviso) => grupos[aviso.grupo] !== false);
    return { locales, remotos };
  }, [sistemas, zona, grupos, descartadas, delServidor]);

  const claves = candidatos.locales.map((aviso) => `local:${aviso.id}`);
  const acceso = useQuery({
    queryKey: ['contenido-plan', 'AVISOS', claves, delServidor.map((aviso) => aviso.id)],
    queryFn: ({ signal }) => desbloquearAvisos(claves.slice(0, 500), signal),
    staleTime: 30_000,
    refetchInterval: 30_000,
    retry: false,
  }).data;
  const contador = useQuery({
    queryKey: ['avisos', 'contador'],
    queryFn: ({ signal }) =>
      http.get(
        '/avisos/contador',
        z.object({ pendientes: z.number(), urgentes: z.number() }),
        signal,
      ),
    refetchInterval: 60_000,
    retry: false,
  }).data;
  const avisos = useMemo(
    () =>
      [
        ...candidatos.remotos,
        ...candidatos.locales.filter((aviso) => acceso?.autorizadas.includes(`local:${aviso.id}`)),
      ].sort((a, b) => b.prioridad - a.prioridad),
    [candidatos, acceso],
  );

  useEffect(() => {
    useSistemaStore.setState({
      accesoAvisos: acceso
        ? {
            ...acceso,
            bloqueadas:
              candidatos.locales.length -
              (avisos.length - candidatos.remotos.length) +
              Math.max(0, (contador?.pendientes ?? delServidor.length) - delServidor.length),
          }
        : null,
    });
  }, [acceso, candidatos, avisos, contador, delServidor]);

  useEffect(() => {
    const store = useSistemaStore.getState();
    store.ponerAvisos(avisos);
    if (!acceso || avisosCargando) return;
    const anteriores = vistos.current;
    if (anteriores) {
      const nuevos = avisos.filter((aviso) => !anteriores.has(aviso.id));
      const primero = nuevos[0];
      if (primero && !store.panel.abierto) store.mostrarAvisoNuevo(primero, nuevos.length);
    }
    vistos.current = new Set(avisos.map((aviso) => aviso.id));
  }, [avisos, acceso, avisosCargando]);
}
