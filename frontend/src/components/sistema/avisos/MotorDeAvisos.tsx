import { useMotorDeAvisos } from '@/hooks/avisos/useMotorDeAvisos';
import { AvisoDeSami } from './AvisoDeSami';

// Lo de los avisos que acompaña a todas las pantallas: calcula los avisos y dibuja el aviso nuevo.
export function MotorDeAvisos() {
  useMotorDeAvisos();
  return <AvisoDeSami />;
}
