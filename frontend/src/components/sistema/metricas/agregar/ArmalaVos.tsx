import { useState, type FormEvent } from 'react';
import { PasosHechos } from '@/components/sistema/metricas/agregar/PasosHechos';
import { TarjetasDelPaso } from '@/components/sistema/metricas/agregar/TarjetasDelPaso';
import type { PropsDeLaPestana } from '@/components/sistema/metricas/agregar/tipos';
import { VistaPrevia } from '@/components/sistema/metricas/agregar/VistaPrevia';
import { useFuentesDelCuaderno, useVistaEnVivo } from '@/hooks/metricas/queries';
import { ajustarALaFuente, definicionInicial } from '@/lib/metricas/metricasAMedida';
import { PASO_DEL_NOMBRE, pasoConTarjetas } from '@/lib/metricas/pasosDeLaReceta';
import {
  FUENTES_DE_METRICA,
  type DefinicionDeMetrica,
  type FuenteDeMetrica,
} from '@/types/metricas';

// Sólo se ofrece armar de lo que el negocio ya cargó: lo que no tiene, después no se puede calcular.
export function ArmalaVos(props: PropsDeLaPestana) {
  const { fuentes, fallo } = useFuentesDelCuaderno(props.sistemaId);
  const disponibles = fallo ? [...FUENTES_DE_METRICA] : fuentes;
  if (!disponibles)
    return (
      <p className="metricas-medida-aviso" role="status">
        Buscando lo que cargaste…
      </p>
    );
  if (disponibles.length === 0)
    return (
      <p className="metricas-medida-aviso">
        Todavía no cargaste ventas, gastos ni stock en este negocio.
      </p>
    );
  return <PasoAPaso {...props} fuentes={disponibles} />;
}

type PropsDelPasoAPaso = PropsDeLaPestana & { fuentes: FuenteDeMetrica[] };

// Cinco pasos, uno a la vez: qué mirar, qué cuenta, cómo repartir, de cuándo y el nombre. Al tocar una tarjeta se
// pasa al siguiente y la vista previa de abajo se actualiza sola.
function PasoAPaso({ sistemaId, guardando, onAgregar, fuentes }: PropsDelPasoAPaso) {
  const receta = useRecetaPasoAPaso(fuentes);
  const guardarLaMia = (evento: FormEvent) => {
    evento.preventDefault();
    const definicion = receta.lista();
    if (definicion) onAgregar(definicion, 'PROPIA');
  };
  return (
    <form className="armala-vos" onSubmit={guardarLaMia} noValidate>
      <PasosHechos
        definicion={receta.definicion}
        fuentes={fuentes}
        paso={receta.paso}
        onIrAlPaso={receta.irAlPaso}
      />
      <PasoActual receta={receta} fuentes={fuentes} guardando={guardando} />
      <VistaEnVivo sistemaId={sistemaId} definicion={receta.definicion} />
    </form>
  );
}

const recetaInicialPara = (fuentes: FuenteDeMetrica[]) =>
  ajustarALaFuente(definicionInicial(), fuentes[0] ?? 'VENTAS');

/** La receta que se va armando, en qué paso se está y si ya tiene nombre para guardarla. */
function useRecetaPasoAPaso(fuentes: FuenteDeMetrica[]) {
  const [definicion, setDefinicion] = useState(() => recetaInicialPara(fuentes));
  const [paso, setPaso] = useState(0);
  const [sinNombre, setSinNombre] = useState(false);
  const nombre = definicion.nombre.trim();
  return {
    definicion,
    paso,
    irAlPaso: setPaso,
    faltaElNombre: sinNombre && nombre === '',
    elegirEnElPaso: (nueva: DefinicionDeMetrica) => {
      setDefinicion(nueva);
      setPaso(paso + 1);
    },
    cambiar: setDefinicion,
    nombrar: (nuevo: string) => setDefinicion({ ...definicion, nombre: nuevo }),
    lista: () => {
      setSinNombre(nombre === '');
      return nombre === '' ? null : { ...definicion, nombre };
    },
  };
}

type RecetaPasoAPaso = ReturnType<typeof useRecetaPasoAPaso>;

type PropsDelPasoActual = {
  receta: RecetaPasoAPaso;
  fuentes: FuenteDeMetrica[];
  guardando: boolean;
};

function PasoActual({ receta, fuentes, guardando }: PropsDelPasoActual) {
  if (receta.paso === PASO_DEL_NOMBRE)
    return <PasoDelNombre receta={receta} guardando={guardando} />;
  return (
    <TarjetasDelPaso
      paso={pasoConTarjetas(receta.paso)}
      numero={receta.paso + 1}
      definicion={receta.definicion}
      fuentes={fuentes}
      onElegir={receta.elegirEnElPaso}
      onCambiar={receta.cambiar}
    />
  );
}

function PasoDelNombre({ receta, guardando }: { receta: RecetaPasoAPaso; guardando: boolean }) {
  return (
    <fieldset className="armala-vos-paso" data-color="amarillo">
      <legend>Paso 5 de 5 · Ponele un nombre</legend>
      <label>
        Nombre
        <input
          type="text"
          value={receta.definicion.nombre}
          maxLength={80}
          placeholder="Por ejemplo: Ventas por día"
          aria-invalid={receta.faltaElNombre}
          onChange={(evento) => receta.nombrar(evento.target.value)}
        />
      </label>
      {receta.faltaElNombre && <span className="metrica-medida-error">Ponele un nombre.</span>}
      <button type="submit" className="metricas-medida-agregar" disabled={guardando}>
        Guardar métrica
      </button>
    </fieldset>
  );
}

function VistaEnVivo({
  sistemaId,
  definicion,
}: {
  sistemaId: string;
  definicion: DefinicionDeMetrica;
}) {
  const { resultado, fallo } = useVistaEnVivo(sistemaId, definicion);
  if (fallo)
    return (
      <p className="metrica-medida-error" role="alert">
        No pudimos calcularla. Probá de nuevo.
      </p>
    );
  if (!resultado) return null;
  return (
    <section className="panel-agregar-propuesta" aria-label="Así quedaría">
      <h3>Así quedaría</h3>
      <VistaPrevia definicion={definicion} resultado={resultado} />
    </section>
  );
}
