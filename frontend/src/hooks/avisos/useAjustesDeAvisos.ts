import { useGuardarPreferencias } from '@/hooks/preferencias/mutations';
import { usePreferencias } from '@/hooks/preferencias/queries';

// Qué avisos de Sami quiere recibir la persona: qué temas tiene prendidos y cuántos avisos ocultó. Viaja con su cuenta.
export function useAjustesDeAvisos() {
  const preferencias = usePreferencias();
  const guardarPreferencias = useGuardarPreferencias();

  const alternarGrupo = (grupo: string, activo: boolean) =>
    guardarPreferencias.mutate({
      ...preferencias,
      grupos: { ...preferencias.grupos, [grupo]: activo },
    });

  const restaurar = () => guardarPreferencias.mutate({ ...preferencias, descartadas: [] });

  return {
    grupos: preferencias.grupos,
    descartadas: preferencias.descartadas.length,
    alternarGrupo,
    restaurar,
  };
}
