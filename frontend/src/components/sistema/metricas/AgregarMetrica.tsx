import { useState, type FormEvent } from 'react';
import { ResultadoDeLaMetrica } from '@/components/sistema/metricas/MetricasAMedida';
import { leerErrorServidor } from '@/hooks/errores';
import { useGuardarMetrica, useVerComoQueda } from '@/hooks/metricas/mutations';
import { useMetricasSugeridas } from '@/hooks/metricas/queries';
import {
  ETIQUETA_DE_AGRUPACION,
  ETIQUETA_DE_FUENTE,
  ETIQUETA_DE_PERIODO,
  agrupacionesDe,
  ajustarALaFuente,
  calculosDe,
  definicionInicial,
  etiquetaDeCalculo,
  periodosDe,
  permiteFiado,
  resumenDeLaReceta,
} from '@/lib/metricas/metricasAMedida';
import {
  FUENTES_DE_METRICA,
  type AutoriaDeMetrica,
  type DefinicionDeMetrica,
} from '@/types/metricas';

const NO_SE_PUDO_GUARDAR = 'No pudimos guardarla. Probá de nuevo.';

// Si el servidor dijo por qué (por ejemplo, que ya está), se muestra eso; si no respondió, el aviso genérico.
const mensajeDe = (error: unknown) => {
  const delServidor = leerErrorServidor(error);
  return delServidor && delServidor.status > 0 ? delServidor.mensaje : NO_SE_PUDO_GUARDAR;
};

// Dos caminos para sumar una métrica sin escribirla a mano: elegir una de las sugeridas para el rubro, o armar la
// propia eligiendo de listas (qué mirar, cómo, repartida por qué y de cuándo). «Ver cómo queda» la calcula sin guardar.
export function AgregarMetrica({
  sistemaId,
  onCerrar,
}: {
  sistemaId: string;
  onCerrar: () => void;
}) {
  const [camino, setCamino] = useState<'sugeridas' | 'propia'>('sugeridas');
  const guardar = useGuardarMetrica(sistemaId);

  const guardarMetrica = (definicion: DefinicionDeMetrica, autoria: AutoriaDeMetrica) =>
    guardar.mutate({ ...definicion, autoria }, { onSuccess: onCerrar });

  return (
    <div className="agregar-metrica">
      <div className="agregar-metrica-cabecera">
        <div className="selector-periodo" role="group" aria-label="Cómo querés agregarla">
          <button
            type="button"
            aria-pressed={camino === 'sugeridas'}
            onClick={() => setCamino('sugeridas')}
          >
            Sugeridas para tu negocio
          </button>
          <button
            type="button"
            aria-pressed={camino === 'propia'}
            onClick={() => setCamino('propia')}
          >
            Armar la mía
          </button>
        </div>
        <button type="button" className="agregar-metrica-cerrar" onClick={onCerrar}>
          Cerrar
        </button>
      </div>
      {guardar.isError && (
        <p className="metrica-medida-error" role="alert">
          {mensajeDe(guardar.error)}
        </p>
      )}
      {camino === 'sugeridas' ? (
        <Sugeridas sistemaId={sistemaId} guardando={guardar.isPending} onAgregar={guardarMetrica} />
      ) : (
        <ArmarLaMia
          sistemaId={sistemaId}
          guardando={guardar.isPending}
          onAgregar={guardarMetrica}
        />
      )}
    </div>
  );
}

type PropsDelCamino = {
  sistemaId: string;
  guardando: boolean;
  onAgregar: (definicion: DefinicionDeMetrica, autoria: AutoriaDeMetrica) => void;
};

function Sugeridas({ sistemaId, guardando, onAgregar }: PropsDelCamino) {
  const { sugeridas, cargando, fallo, reintentar } = useMetricasSugeridas(sistemaId, true);
  if (fallo)
    return (
      <p className="metricas-medida-aviso" role="status">
        No pudimos traer las sugeridas.{' '}
        <button type="button" onClick={() => void reintentar()}>
          Probar de nuevo
        </button>
      </p>
    );
  if (cargando || !sugeridas)
    return (
      <p className="metricas-medida-aviso" role="status">
        Buscando métricas para tu negocio…
      </p>
    );
  if (sugeridas.length === 0)
    return (
      <p className="metricas-medida-aviso">
        Ya agregaste todas las sugeridas. Probá armar la tuya.
      </p>
    );
  return (
    <ul className="metricas-medida-lista">
      {sugeridas.map(({ definicion, resultado }) => (
        <li
          key={`${definicion.nombre}-${resumenDeLaReceta(definicion)}`}
          className="metrica-medida"
        >
          <h3>{definicion.nombre}</h3>
          <p className="metrica-medida-receta">{resumenDeLaReceta(definicion)}</p>
          <ResultadoDeLaMetrica resultado={resultado} />
          <button
            type="button"
            className="metricas-medida-agregar"
            disabled={guardando}
            aria-label={`Agregar ${definicion.nombre}`}
            onClick={() => onAgregar(definicion, 'SUGERIDA')}
          >
            Agregar
          </button>
        </li>
      ))}
    </ul>
  );
}

function ArmarLaMia({ sistemaId, guardando, onAgregar }: PropsDelCamino) {
  const [definicion, setDefinicion] = useState(definicionInicial);
  const [sinNombre, setSinNombre] = useState(false);
  const vista = useVerComoQueda(sistemaId);

  // Cualquier cambio deja la vista previa vieja: se vuelve a pedir con «Ver cómo queda».
  const cambiar = (nueva: DefinicionDeMetrica) => {
    setDefinicion(nueva);
    vista.reset();
  };
  const conNombre = definicion.nombre.trim().length > 0;
  const faltaElNombre = sinNombre && !conNombre;

  const guardarLaMia = (evento: FormEvent) => {
    evento.preventDefault();
    if (!conNombre) return setSinNombre(true);
    onAgregar({ ...definicion, nombre: definicion.nombre.trim() }, 'PROPIA');
  };

  return (
    <form className="agregar-metrica-form" onSubmit={guardarLaMia} noValidate>
      <label>
        Nombre
        <input
          type="text"
          value={definicion.nombre}
          maxLength={80}
          aria-invalid={faltaElNombre}
          aria-describedby={faltaElNombre ? 'agregar-metrica-sin-nombre' : undefined}
          onChange={(e) => cambiar({ ...definicion, nombre: e.target.value })}
        />
      </label>
      {faltaElNombre && (
        <span id="agregar-metrica-sin-nombre" className="metrica-medida-error">
          Ponele un nombre.
        </span>
      )}
      <label>
        ¿Qué querés mirar?
        <select
          value={definicion.fuente}
          onChange={(e) =>
            cambiar(ajustarALaFuente(definicion, e.target.value as DefinicionDeMetrica['fuente']))
          }
        >
          {FUENTES_DE_METRICA.map((fuente) => (
            <option key={fuente} value={fuente}>
              {ETIQUETA_DE_FUENTE[fuente]}
            </option>
          ))}
        </select>
      </label>
      <label>
        ¿Qué cuenta querés hacer?
        <select
          value={definicion.calculo}
          onChange={(e) =>
            cambiar({ ...definicion, calculo: e.target.value as DefinicionDeMetrica['calculo'] })
          }
        >
          {calculosDe(definicion.fuente).map((calculo) => (
            <option key={calculo} value={calculo}>
              {etiquetaDeCalculo(definicion.fuente, calculo)}
            </option>
          ))}
        </select>
      </label>
      <label>
        ¿Cómo la querés repartir?
        <select
          value={definicion.agrupar}
          onChange={(e) =>
            cambiar({ ...definicion, agrupar: e.target.value as DefinicionDeMetrica['agrupar'] })
          }
        >
          {agrupacionesDe(definicion.fuente).map((agrupar) => (
            <option key={agrupar} value={agrupar}>
              {ETIQUETA_DE_AGRUPACION[agrupar]}
            </option>
          ))}
        </select>
      </label>
      <label>
        ¿De cuándo?
        <select
          value={definicion.periodo}
          onChange={(e) =>
            cambiar({ ...definicion, periodo: e.target.value as DefinicionDeMetrica['periodo'] })
          }
        >
          {periodosDe(definicion.fuente).map((periodo) => (
            <option key={periodo} value={periodo}>
              {ETIQUETA_DE_PERIODO[periodo]}
            </option>
          ))}
        </select>
      </label>
      {permiteFiado(definicion.fuente) && (
        <label className="agregar-metrica-check">
          <input
            type="checkbox"
            checked={definicion.soloFiado}
            onChange={(e) => cambiar({ ...definicion, soloFiado: e.target.checked })}
          />
          Sólo lo fiado
        </label>
      )}

      <div className="agregar-metrica-acciones">
        <button
          type="button"
          className="agregar-metrica-ver"
          disabled={vista.isPending}
          onClick={() =>
            vista.mutate({ ...definicion, nombre: definicion.nombre.trim() || 'Vista previa' })
          }
        >
          Ver cómo queda
        </button>
        <button type="submit" className="metricas-medida-agregar" disabled={guardando}>
          Guardar métrica
        </button>
      </div>
      {vista.isError && (
        <p className="metrica-medida-error" role="alert">
          No pudimos calcularla. Probá de nuevo.
        </p>
      )}
      {vista.data && (
        <div className="agregar-metrica-vista" aria-label="Así quedaría">
          <ResultadoDeLaMetrica resultado={vista.data} />
        </div>
      )}
    </form>
  );
}
