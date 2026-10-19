import { useMotorDeAvisos } from '@/hooks/avisos/useMotorDeAvisos';
import { AvisoDeSami } from './AvisoDeSami';
import { PanelDeAvisos } from './PanelDeAvisos';

// Todo lo de los avisos que no es un botón: calcula los avisos y dibuja el panel y el aviso nuevo.
export function MotorDeAvisos() {
  useMotorDeAvisos();
  return (
    <>
      <PanelDeAvisos />
      <AvisoDeSami />
    </>
  );
}
