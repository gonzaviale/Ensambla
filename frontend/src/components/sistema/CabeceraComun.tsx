import { useCerrarSesion } from '@/hooks/auth/mutations';
import { useAbrirAvisos } from '@/hooks/avisos/useAbrirAvisos';
import { useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Campana } from './avisos/Campana';
import { TopBar, type SeccionActiva } from './TopBar';

// El `commonHeader()` del mockup: la barra de arriba conectada al router, al store y a la sesión.
export function CabeceraComun({ active = 'home' }: { active?: SeccionActiva }) {
  const nombre = useNombre();
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const navegacion = useNavegacion();
  const abrirAvisos = useAbrirAvisos();
  const { cerrarSesion, cerrando } = useCerrarSesion();

  return (
    <TopBar
      active={active}
      nombre={nombre}
      campana={<Campana />}
      onInicio={() => void navegacion.irAInicio()}
      onSistemas={() => void navegacion.irAMisSistemas()}
      onPlantillas={() => void navegacion.irAPlantillas()}
      onZona={() => void navegacion.irAZona()}
      onAvisos={() => void abrirAvisos()}
      onPlanes={() => void navegacion.irAPlanes()}
      onAyuda={() => abrirModal({ tipo: 'ayuda' })}
      onCuenta={() => void navegacion.irACuenta()}
      onCerrarSesion={() =>
        abrirModal({
          tipo: 'confirmar',
          titulo: '¿Cerrar la sesión?',
          mensaje:
            'Salís de tu cuenta en este dispositivo. Tus cuadernos y tus datos quedan guardados para cuando vuelvas a entrar.',
          etiqueta: 'Cerrar sesión',
          onConfirmar: () => void cerrarSesion(),
        })
      }
      cerrandoSesion={cerrando}
    />
  );
}
