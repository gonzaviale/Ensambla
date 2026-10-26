import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';

// Lleva a la pantalla de avisos de Sami; con un aviso, la pantalla lo muestra resaltado.
export function useAbrirAvisos() {
  const navegacion = useNavegacion();
  return (foco: string | null = null) => {
    useSistemaStore.getState().abrirPanel(foco);
    return navegacion.irAAvisos();
  };
}
