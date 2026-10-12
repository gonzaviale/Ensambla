import { useGuardarRegistro } from '@/hooks/registros/useGuardarRegistro';
import type { Bloque } from '@/types/proyecto';
import { FormRenderer } from './FormRenderer';

export function FormularioRegistro({
  bloque,
  tablas,
  onEnviandoChange,
}: {
  bloque: Bloque;
  /** Tablas del sistema: las columnas que se conectan con otra tabla la nombran. */
  tablas?: readonly Bloque[];
  onEnviandoChange?: (enviando: boolean) => void;
}) {
  const guardar = useGuardarRegistro(bloque.id);
  return (
    <FormRenderer
      campos={bloque.campos}
      tablas={tablas}
      onSubmit={guardar}
      onEnviandoChange={onEnviandoChange}
    />
  );
}
