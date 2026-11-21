import { useState, type KeyboardEvent } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { useMoverMetrica, useQuitarMetrica } from '@/hooks/metricas/mutations';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Metrica } from '@/types/metricas';

type Props = {
  metrica: Metrica;
  sistemaId: string;
  lugar: number;
  ultimoLugar: number;
  onRenombrar: () => void;
  onQuitada: () => void;
};

type Opcion = { texto: string; icono: NombreDeIcono; deshabilitada: boolean; elegir: () => void };

// Lo que se puede hacer con la métrica abierta: cambiarle el nombre, correrla de lugar entre las solapas o quitarla.
export function MenuDeLaMetrica(props: Props) {
  const [abierto, setAbierto] = useState(false);
  const { opciones, error } = useOpcionesDeLaMetrica(props, () => setAbierto(false));
  const cerrarConEscape = (evento: KeyboardEvent) => evento.key === 'Escape' && setAbierto(false);
  return (
    <div className="panel-negocio-menu" onKeyDown={cerrarConEscape}>
      <button
        type="button"
        className="panel-negocio-menu-boton"
        aria-label={`Opciones de ${props.metrica.definicion.nombre}`}
        aria-haspopup="menu"
        aria-expanded={abierto}
        onClick={() => setAbierto(!abierto)}
      >
        <Icon name="menu" />
      </button>
      {abierto && <ListaDeOpciones opciones={opciones} />}
      {error && (
        <p className="metrica-medida-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

function ListaDeOpciones({ opciones }: { opciones: Opcion[] }) {
  return (
    <ul className="panel-negocio-menu-lista" role="menu">
      {opciones.map((opcion) => (
        <li key={opcion.texto} role="none">
          <button
            type="button"
            role="menuitem"
            disabled={opcion.deshabilitada}
            onClick={opcion.elegir}
          >
            <Icon name={opcion.icono} />
            {opcion.texto}
          </button>
        </li>
      ))}
    </ul>
  );
}

function useOpcionesDeLaMetrica(props: Props, cerrar: () => void) {
  const { metrica, sistemaId, lugar, ultimoLugar, onRenombrar, onQuitada } = props;
  const mover = useMoverMetrica(sistemaId, metrica.id);
  const quitar = useQuitarMetrica(sistemaId);
  const yCerrar = (accion: () => void) => () => {
    cerrar();
    accion();
  };
  const opciones: Opcion[] = [
    {
      texto: 'Cambiar el nombre',
      icono: 'edit',
      deshabilitada: false,
      elegir: yCerrar(onRenombrar),
    },
    {
      texto: 'Mover a la izquierda',
      icono: 'back',
      deshabilitada: lugar === 0 || mover.isPending,
      elegir: yCerrar(() => mover.mutate(lugar - 1)),
    },
    {
      texto: 'Mover a la derecha',
      icono: 'arrow',
      deshabilitada: lugar === ultimoLugar || mover.isPending,
      elegir: yCerrar(() => mover.mutate(lugar + 1)),
    },
    {
      texto: 'Quitar del panel',
      icono: 'trash',
      deshabilitada: quitar.isPending,
      elegir: yCerrar(() => quitar.mutate(metrica.id, { onSuccess: onQuitada })),
    },
  ];
  return { opciones, error: errorDeLasOpciones(mover.isError, quitar.isError) };
}

function errorDeLasOpciones(noSeMovio: boolean, noSeQuito: boolean) {
  if (noSeMovio) return 'No pudimos cambiarla de lugar. Probá de nuevo.';
  if (noSeQuito) return 'No pudimos quitarla. Probá de nuevo.';
  return null;
}
