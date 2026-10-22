import { TUTORIAL_INICIAL } from '@/lib/sistema/tutorial';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';

// Empezar y salir del tutorial: lo comparten el inicio («Mostrarme cómo empezar»), la bienvenida y el cartel.
export function useAccionesDelTutorial() {
  const navegacion = useNavegacion();

  const empezar = () => {
    const store = useSistemaStore.getState();
    store.cerrarModal();
    store.ponerTutorial({
      ...TUTORIAL_INICIAL,
      activo: true,
      paso: 'choose',
      sistemasAlElegir: store.sistemas.length,
    });
    void navegacion.irAInicio();
  };

  return { empezar };
}

/** Termina el tutorial y avisa cómo retomarlo. */
export function salirDelTutorial() {
  const store = useSistemaStore.getState();
  store.ponerTutorial(TUTORIAL_INICIAL);
  store.avisar('Podés retomar la guía desde Sami en el inicio.');
}
