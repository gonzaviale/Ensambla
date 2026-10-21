import { FIELD_TYPES } from '@/lib/sistema/catalogo';
import {
  TIPOS_DE_CAMPO_NUEVO,
  camposSinDecidir,
  errorDeNombre,
  type AccionDeRevision,
  type CampoNuevo,
  type Revision,
} from '@/lib/sistema/revision';
import type { Campo, TipoCampo } from '@/lib/sistema/tipos';
import { Btn } from '../Btn';
import { Icon } from '../Icon';

type Props = {
  revision: Revision;
  /** Los campos que el bloque ya tiene: sirven para no repetir un nombre. */
  existentes: Campo[];
  onAccion: (accion: AccionDeRevision) => void;
};

const MAXIMO_DE_EJEMPLOS = 3;

const ejemplosDe = (revision: Revision, clave: string) =>
  [
    ...new Set(
      revision.filas
        .map((fila) => fila.valores[clave])
        .filter((valor) => valor != null && String(valor).trim() !== '')
        .map(String),
    ),
  ].slice(0, MAXIMO_DE_EJEMPLOS);

const ETIQUETA_DEL_ESTADO = { aceptado: 'Aceptado', descartado: 'Descartado' } as const;

function ColumnaNueva({ campo, revision, existentes, onAccion }: Props & { campo: CampoNuevo }) {
  const idNombre = `campo-nuevo-${campo.clave}-nombre`;
  const idTipo = `campo-nuevo-${campo.clave}-tipo`;
  const descartado = campo.estado === 'descartado';
  const error = descartado ? undefined : errorDeNombre(campo, revision, existentes);
  const ejemplos = ejemplosDe(revision, campo.clave);

  return (
    <li className={`campo-nuevo ${campo.estado}`} data-campo-nuevo={campo.clave}>
      <div className="campo-nuevo-edicion">
        <div className={`field ${error ? 'invalid' : ''}`.trim()}>
          <label htmlFor={idNombre}>Nombre del campo nuevo</label>
          <input
            className="input"
            id={idNombre}
            maxLength={60}
            value={campo.label}
            disabled={descartado}
            aria-describedby={`${idNombre}-error`}
            onChange={(evento) =>
              onAccion({ tipo: 'renombrarCampo', clave: campo.clave, label: evento.target.value })
            }
          />
          <span className="field-error" id={`${idNombre}-error`} role={error ? 'alert' : undefined}>
            {error}
          </span>
        </div>
        <div className="field">
          <label htmlFor={idTipo}>Tipo de dato sugerido</label>
          <select
            id={idTipo}
            value={campo.tipo}
            disabled={descartado}
            onChange={(evento) =>
              onAccion({
                tipo: 'cambiarTipoDeCampo',
                clave: campo.clave,
                tipoDeCampo: evento.target.value as TipoCampo,
              })
            }
          >
            {TIPOS_DE_CAMPO_NUEVO.map((tipo) => (
              <option key={tipo} value={tipo}>
                {FIELD_TYPES[tipo]}
              </option>
            ))}
          </select>
        </div>
      </div>
      {ejemplos.length > 0 && (
        <p className="campo-nuevo-ejemplos">Se leyó, por ejemplo: {ejemplos.join(' · ')}</p>
      )}
      <div className="campo-nuevo-acciones">
        {campo.estado !== 'pendiente' && (
          <span className={`tag ${campo.estado}`}>{ETIQUETA_DEL_ESTADO[campo.estado]}</span>
        )}
        {campo.estado === 'pendiente' && (
          <Btn
            cls="primary small"
            disabled={Boolean(error)}
            onClick={() => onAccion({ tipo: 'aceptarCampo', clave: campo.clave })}
          >
            <Icon name="check" /> Aceptar<span className="sr-only"> {campo.label}</span>
          </Btn>
        )}
        {descartado ? (
          <Btn
            cls="secondary small"
            onClick={() => onAccion({ tipo: 'reabrirCampo', clave: campo.clave })}
          >
            <Icon name="undo" /> Volver a sumarlo<span className="sr-only"> {campo.label}</span>
          </Btn>
        ) : (
          <Btn
            cls="secondary small"
            onClick={() => onAccion({ tipo: 'descartarCampo', clave: campo.clave })}
          >
            <Icon name="close" /> Descartar<span className="sr-only"> {campo.label}</span>
          </Btn>
        )}
      </div>
    </li>
  );
}

// «Campos nuevos detectados»: las columnas de la foto que el bloque todavía no tiene. Cada una se acepta
// (se crea al incorporar), se renombra, cambia de tipo o se descarta (no se crea y sus datos se ignoran).
export function CamposNuevosDetectados({ revision, existentes, onAccion }: Props) {
  if (!revision.camposNuevos.length) return null;
  const sinDecidir = camposSinDecidir(revision).length;
  return (
    <section className="campos-nuevos" aria-labelledby="campos-nuevos-titulo">
      <h3 id="campos-nuevos-titulo">Campos nuevos detectados</h3>
      <p>
        La foto trae datos que esta tabla todavía no tiene. Si los aceptás, se suman como campos
        nuevos y los registros que ya tenías quedan como están.
        {sinDecidir > 0 && ` Falta decidir ${sinDecidir === 1 ? 'uno' : sinDecidir}.`}
      </p>
      <ul className="lista-simple">
        {revision.camposNuevos.map((campo) => (
          <ColumnaNueva
            key={campo.clave}
            campo={campo}
            revision={revision}
            existentes={existentes}
            onAccion={onAccion}
          />
        ))}
      </ul>
    </section>
  );
}
