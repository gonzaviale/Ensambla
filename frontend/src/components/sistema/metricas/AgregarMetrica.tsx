import { useState, type FormEvent, type ReactNode } from 'react';
import { ResultadoDeLaMetrica } from '@/components/sistema/metricas/MetricasAMedida';
import { leerErrorServidor } from '@/hooks/errores';
import { useEntenderPedido, useGuardarMetrica, useVerComoQueda } from '@/hooks/metricas/mutations';
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
  type MetricaSugerida,
} from '@/types/metricas';

const NO_SE_PUDO_GUARDAR = 'No pudimos guardarla. Probá de nuevo.';

// Si el servidor dijo por qué (por ejemplo, que ya está), se muestra eso; si no respondió, el aviso genérico.
const mensajeDe = (error: unknown) => {
  const delServidor = leerErrorServidor(error);
  return delServidor && delServidor.status > 0 ? delServidor.mensaje : NO_SE_PUDO_GUARDAR;
};

// Tres caminos para sumar una métrica sin escribir fórmulas: elegir una de las sugeridas para el rubro, armar la
// propia eligiendo de listas, o pedirla con tus palabras. En los dos últimos se ve cómo queda antes de guardarla.
export function AgregarMetrica({
  sistemaId,
  onCerrar,
}: {
  sistemaId: string;
  onCerrar: () => void;
}) {
  const [camino, setCamino] = useState<Camino>('sugeridas');
  const guardar = useGuardarMetrica(sistemaId);
  const { Componente } = CAMINOS[camino];

  const guardarMetrica = (definicion: DefinicionDeMetrica, autoria: AutoriaDeMetrica) =>
    guardar.mutate({ ...definicion, autoria }, { onSuccess: onCerrar });

  return (
    <div className="agregar-metrica">
      <div className="agregar-metrica-cabecera">
        <SelectorDeCamino camino={camino} onElegir={setCamino} />
        <button type="button" className="agregar-metrica-cerrar" onClick={onCerrar}>
          Cerrar
        </button>
      </div>
      {guardar.isError && (
        <p className="metrica-medida-error" role="alert">
          {mensajeDe(guardar.error)}
        </p>
      )}
      <Componente sistemaId={sistemaId} guardando={guardar.isPending} onAgregar={guardarMetrica} />
    </div>
  );
}

function SelectorDeCamino({
  camino,
  onElegir,
}: {
  camino: Camino;
  onElegir: (camino: Camino) => void;
}) {
  return (
    <div className="selector-periodo" role="group" aria-label="Cómo querés agregarla">
      {ORDEN_DE_LOS_CAMINOS.map((opcion) => (
        <button
          key={opcion}
          type="button"
          aria-pressed={camino === opcion}
          onClick={() => onElegir(opcion)}
        >
          {CAMINOS[opcion].etiqueta}
        </button>
      ))}
    </div>
  );
}

type PropsDelCamino = {
  sistemaId: string;
  guardando: boolean;
  onAgregar: (definicion: DefinicionDeMetrica, autoria: AutoriaDeMetrica) => void;
};

type Camino = 'sugeridas' | 'propia' | 'palabras';

const ORDEN_DE_LOS_CAMINOS: Camino[] = ['sugeridas', 'propia', 'palabras'];

const CAMINOS: Record<
  Camino,
  { etiqueta: string; Componente: (props: PropsDelCamino) => ReactNode }
> = {
  sugeridas: { etiqueta: 'Sugeridas para tu negocio', Componente: Sugeridas },
  propia: { etiqueta: 'Armar la mía', Componente: ArmarLaMia },
  palabras: { etiqueta: 'Pedila con tus palabras', Componente: PedilaConTusPalabras },
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

const EJEMPLOS_DE_PEDIDOS = [
  'Cuánto vendí esta semana',
  'Qué producto vendo más este mes',
  'Cuánto me deben por cliente',
  'En qué gasté más este mes',
];

// Se escribe lo que se quiere ver y se entiende con un diccionario fijo (sin IA): la receta y su número se muestran
// antes de guardar, y si no se entendió o no se puede calcular, se dice por qué.
function PedilaConTusPalabras({ sistemaId, guardando, onAgregar }: PropsDelCamino) {
  const { pedido, setPedido, entender, propuesta, aviso, entendiendo, descartar } =
    usePedidoEnPalabras(sistemaId);
  return (
    <form className="agregar-metrica-form" onSubmit={entender} noValidate>
      <label>
        ¿Qué querés ver?
        <input
          type="text"
          value={pedido}
          maxLength={200}
          placeholder="Por ejemplo: cuánto vendí esta semana"
          onChange={(evento) => setPedido(evento.target.value)}
        />
      </label>
      <div className="agregar-metrica-acciones">
        <button type="submit" className="agregar-metrica-ver" disabled={entendiendo}>
          Ver qué entendí
        </button>
      </div>
      {aviso && <AvisoDelPedido texto={aviso} />}
      {propuesta && (
        <PropuestaDelPedido
          propuesta={propuesta}
          guardando={guardando}
          onGuardar={() => onAgregar(propuesta.definicion, 'PROPIA')}
          onDescartar={descartar}
        />
      )}
    </form>
  );
}

/** Lo que la persona escribe y lo que respondió el servidor: la propuesta o por qué no se pudo. */
function usePedidoEnPalabras(sistemaId: string) {
  const entendido = useEntenderPedido(sistemaId);
  const [pedido, setPedido] = useState('');
  const [faltaElPedido, setFaltaElPedido] = useState(false);
  const entender = (evento: FormEvent) => {
    evento.preventDefault();
    const limpio = pedido.trim();
    setFaltaElPedido(limpio === '');
    if (limpio !== '') entendido.mutate(limpio);
  };
  return {
    pedido,
    setPedido,
    entender,
    propuesta: entendido.data,
    aviso: avisoDelPedido(faltaElPedido, entendido.error),
    entendiendo: entendido.isPending,
    descartar: entendido.reset,
  };
}

function avisoDelPedido(faltaElPedido: boolean, error: unknown) {
  if (faltaElPedido) return 'Contanos qué querés ver.';
  if (!error) return null;
  const delServidor = leerErrorServidor(error);
  return delServidor && delServidor.status > 0
    ? delServidor.mensaje
    : 'No pudimos entenderlo ahora. Probá de nuevo.';
}

function AvisoDelPedido({ texto }: { texto: string }) {
  return (
    <div className="agregar-metrica-no-entendi" role="alert">
      <p>{texto}</p>
      <p>Podés probar con:</p>
      <ul>
        {EJEMPLOS_DE_PEDIDOS.map((ejemplo) => (
          <li key={ejemplo}>«{ejemplo}»</li>
        ))}
      </ul>
    </div>
  );
}

type PropsDeLaPropuesta = {
  propuesta: MetricaSugerida;
  guardando: boolean;
  onGuardar: () => void;
  onDescartar: () => void;
};

function PropuestaDelPedido({ propuesta, guardando, onGuardar, onDescartar }: PropsDeLaPropuesta) {
  const { definicion, resultado } = propuesta;
  return (
    <div className="agregar-metrica-vista" aria-label="Así quedaría">
      <h3>{definicion.nombre}</h3>
      <p className="metrica-medida-receta">{resumenDeLaReceta(definicion)}</p>
      <ResultadoDeLaMetrica resultado={resultado} />
      <div className="agregar-metrica-acciones">
        <button
          type="button"
          className="metricas-medida-agregar"
          disabled={guardando}
          onClick={onGuardar}
        >
          Guardar métrica
        </button>
        <button type="button" className="agregar-metrica-ver" onClick={onDescartar}>
          Descartar
        </button>
      </div>
    </div>
  );
}
