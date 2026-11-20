import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
import { EstadoVacioDeAvisos } from './EstadoVacioDeAvisos';
import { SeccionDeAvisos } from './SeccionDeAvisos';
import { agruparAvisos } from './presentacion';

type Props = {
  avisos: Aviso[];
  foco?: string | null;
  filtro: string;
  sinDatos: boolean;
  hayDemo: boolean;
  onAccion: (accion: AccionDeAviso) => void;
  onDescartar: (aviso: Aviso) => void;
  onCrearDemo: () => void;
};

// Los avisos de a secciones, del más urgente al menos; si no hay ninguno, por qué no hay.
export function ListaDeAvisos({ avisos, foco, filtro, sinDatos, hayDemo, ...acciones }: Props) {
  return (
    <div className="alertas-lista">
      {avisos.length ? (
        <Secciones avisos={avisos} foco={foco} {...acciones} />
      ) : (
        <EstadoVacioDeAvisos
          sinDatos={sinDatos}
          hayDemo={hayDemo}
          filtro={filtro}
          onCrearDemo={acciones.onCrearDemo}
        />
      )}
    </div>
  );
}

function Secciones({
  avisos,
  foco,
  onAccion,
  onDescartar,
}: Pick<Props, 'avisos' | 'foco' | 'onAccion' | 'onDescartar'>) {
  return agruparAvisos(avisos).map(({ clave, avisos: juntos }) => (
    <SeccionDeAvisos
      key={clave}
      avisos={juntos}
      foco={foco}
      onAccion={onAccion}
      onDescartar={onDescartar}
    />
  ));
}
