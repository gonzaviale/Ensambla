import { useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Campana } from './avisos/Campana';
import { TopBar, type SeccionActiva } from './TopBar';

// El `commonHeader()` del mockup: la barra de arriba conectada al router, al store y a la sesión.
export function CabeceraComun({ active = 'home' }: { active?: SeccionActiva }) {
  const nombre = useNombre();
  const sistemas = useSistemaStore((estado) => estado.sistemas.length);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const navegacion = useNavegacion();

  return (
    <TopBar
      active={active}
      nombre={nombre}
      sistemas={sistemas}
      campana={<Campana />}
      onInicio={() => void navegacion.irAInicio()}
      onSistemas={() => void navegacion.irAMisSistemas()}
      onPlantillas={() => void navegacion.irAPlantillas()}
      onZona={() => void navegacion.irAZona()}
      onPlanes={() => void navegacion.irAPlanes()}
      onAyuda={() => abrirModal({ tipo: 'ayuda' })}
      onPerfil={() => abrirModal({ tipo: 'perfil' })}
    />
  );
}
