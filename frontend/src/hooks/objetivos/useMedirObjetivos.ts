import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { medirObjetivos } from '@/api/objetivos';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import { objetivosKey } from './queries';

/**
 * Al abrir los objetivos, y cada vez que la persona vuelve a esta pestaña, le pide al servidor que mida el
 * negocio en ese momento y después recarga todo lo de los objetivos. Así una venta recién cargada en la sección
 * se ve enseguida en los mini-objetivos y en el gráfico, sin esperar al repaso de cada hora.
 */
export function useMedirObjetivos() {
  const usuario = useUsuarioOpcional();
  const queryClient = useQueryClient();
  const midiendo = useRef(false);

  useEffect(() => {
    if (!usuario) return;
    const medir = () => {
      if (midiendo.current) return;
      midiendo.current = true;
      medirObjetivos()
        .catch(() => undefined) // Si falla, se muestra lo último medido: no hay nada que avisar.
        .finally(() => {
          midiendo.current = false;
          void queryClient.invalidateQueries({ queryKey: objetivosKey });
        });
    };
    const alVolver = () => {
      if (document.visibilityState === 'visible') medir();
    };
    medir();
    document.addEventListener('visibilitychange', alVolver);
    window.addEventListener('focus', alVolver);
    return () => {
      document.removeEventListener('visibilitychange', alVolver);
      window.removeEventListener('focus', alVolver);
    };
  }, [usuario, queryClient]);
}
