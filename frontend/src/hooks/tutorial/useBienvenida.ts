import { useEffect, useRef } from 'react';
import { useGuardarPreferencias } from '@/hooks/preferencias/mutations';
import { usePreferencias } from '@/hooks/preferencias/queries';
import { useSistemaStore } from '@/stores/sistemaStore';

// La bienvenida se muestra la primera vez. Cuando se la cierra, sea como sea (con la guía, explorando o
// con Esc), queda registrado en las preferencias de la cuenta y no vuelve a aparecer.
export function useBienvenida() {
  const preferencias = usePreferencias();
  const guardarPreferencias = useGuardarPreferencias();
  const abierta = useSistemaStore((estado) => estado.modal?.tipo === 'bienvenida');
  const yaSeMostro = useRef(false);
  const estabaAbierta = useRef(false);

  useEffect(() => {
    if (preferencias.bienvenidaVista || yaSeMostro.current) return;
    yaSeMostro.current = true;
    useSistemaStore.getState().abrirModal({ tipo: 'bienvenida' });
  }, [preferencias.bienvenidaVista]);

  useEffect(() => {
    if (estabaAbierta.current && !abierta && !preferencias.bienvenidaVista) {
      guardarPreferencias.mutate({ ...preferencias, bienvenidaVista: true });
    }
    estabaAbierta.current = abierta;
  }, [abierta, preferencias, guardarPreferencias]);
}
