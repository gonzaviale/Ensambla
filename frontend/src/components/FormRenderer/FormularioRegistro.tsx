import { useGuardarRegistro } from '@/hooks/registros/useGuardarRegistro';
import type { Bloque } from '@/types/proyecto';
import { FormRenderer } from './FormRenderer';

export function FormularioRegistro({
  bloque,
  onEnviandoChange,
}: {
  bloque: Bloque;
  onEnviandoChange?: (enviando: boolean) => void;
}) {
  const guardar = useGuardarRegistro(bloque.id);
  return (
    <FormRenderer campos={bloque.campos} onSubmit={guardar} onEnviandoChange={onEnviandoChange} />
  );
}
