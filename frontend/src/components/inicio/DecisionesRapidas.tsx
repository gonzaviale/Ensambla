import { useId, useState } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import {
  ICONO_DE_AVISO,
  NIVEL_DE_AVISO,
  NOMBRE_DE_TIPO,
  textoDeAccion,
} from '@/components/sistema/avisos/presentacion';
import { useAccionesDeAvisos } from '@/hooks/avisos/useAccionesDeAvisos';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
import { sugerenciasDeLaHome } from '@/lib/sistema/sugerenciasDeLaHome';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Desplegable } from './Desplegable';

// Las sugerencias más importantes de Sami, en una sección que arranca cerrada, en una línea cada una: qué pasa y el botón con
// lo que propone. El porqué y descartar quedan un toque más adentro, para no descartar sin querer. La lista sale
// del motor de avisos (los del servidor y los que Sami calcula en el navegador, con el límite del plan aplicado).
// Sin avisos la sección no aparece.
export function DecisionesRapidas() {
  const avisos = useSistemaStore((estado) => estado.avisos);
  const acciones = useAccionesDeAvisos();
  const navegacion = useNavegacion();
  if (avisos.length === 0) return null;

  const sugerencias = sugerenciasDeLaHome(avisos);
  const restantes = avisos.length - sugerencias.length;
  return (
    <Desplegable titulo="Para decidir hoy" resumen={resumenDe(sugerencias)}>
      <div className="panel-decisiones-cabecera">
        <p>Lo más importante que vio Sami en tu negocio.</p>
        {restantes > 0 && (
          <Btn cls="small soft" onClick={() => void navegacion.irAAvisos()}>
            Ver las otras {restantes} <Icon name="arrow" />
          </Btn>
        )}
      </div>
      <ul className="sugerencias">
        {sugerencias.map((aviso) => (
          <Sugerencia
            key={aviso.id}
            aviso={aviso}
            onAccion={acciones.ejecutar}
            onDescartar={acciones.descartar}
          />
        ))}
      </ul>
    </Desplegable>
  );
}

/** «3 sugerencias · 1 urgente»: lo que hay para decidir, sin abrir la sección. */
function resumenDe(sugerencias: Aviso[]) {
  const urgentes = sugerencias.filter((aviso) => aviso.prioridad === 3).length;
  const cuantas = `${sugerencias.length} ${sugerencias.length === 1 ? 'sugerencia' : 'sugerencias'}`;
  if (urgentes === 0) return cuantas;
  return `${cuantas} · ${urgentes} ${urgentes === 1 ? 'urgente' : 'urgentes'}`;
}

type Props = {
  aviso: Aviso;
  onAccion: (accion: AccionDeAviso) => void;
  onDescartar: (aviso: Aviso) => void;
};

function Sugerencia({ aviso, onAccion, onDescartar }: Props) {
  const [abierta, setAbierta] = useState(false);
  const idDetalle = useId();
  const [principal] = aviso.acciones;
  return (
    <li
      className="sugerencia"
      data-alerta={aviso.id}
      data-tipo={aviso.tipo}
      data-nivel={aviso.prioridad}
    >
      <span className="sugerencia-icono" aria-hidden="true">
        <Icon name={ICONO_DE_AVISO[aviso.tipo]} />
      </span>
      <div className="sugerencia-texto">
        <p className="sugerencia-meta">
          {NOMBRE_DE_TIPO[aviso.tipo]} · {NIVEL_DE_AVISO[aviso.prioridad]}
        </p>
        <h4>{aviso.titulo}</h4>
      </div>
      <div className="sugerencia-acciones">
        {principal && (
          <button type="button" className="btn petrol small" onClick={() => onAccion(principal)}>
            {textoDeAccion(principal)}
          </button>
        )}
        <button
          type="button"
          className="sugerencia-mas"
          aria-label={`${abierta ? 'Ocultar' : 'Ver por qué'}: ${aviso.titulo}`}
          aria-expanded={abierta}
          aria-controls={idDetalle}
          onClick={() => setAbierta((valor) => !valor)}
        >
          {abierta ? 'Ocultar' : 'Ver por qué'}
        </button>
      </div>
      {abierta && (
        <div className="sugerencia-detalle" id={idDetalle}>
          <p>{aviso.detalle}</p>
          {aviso.porque.length > 0 && (
            <ul>
              {aviso.porque.map((motivo) => (
                <li key={motivo}>{motivo}</li>
              ))}
            </ul>
          )}
          <button type="button" className="sugerencia-descartar" onClick={() => onDescartar(aviso)}>
            No me interesa, descartar
          </button>
        </div>
      )}
    </li>
  );
}
