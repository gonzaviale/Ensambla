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
    />
  );
}
