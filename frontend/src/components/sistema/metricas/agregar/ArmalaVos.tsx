import { useState, type FormEvent } from 'react';
import { PasosHechos } from '@/components/sistema/metricas/agregar/PasosHechos';
import { TarjetasDelPaso } from '@/components/sistema/metricas/agregar/TarjetasDelPaso';
import type { PropsDeLaPestana } from '@/components/sistema/metricas/agregar/tipos';
import { VistaPrevia } from '@/components/sistema/metricas/agregar/VistaPrevia';
import { NombreDeLaMetrica } from '@/components/sistema/metricas/NombreDeLaMetrica';
import { useFuentesDelCuaderno, useVistaEnVivo } from '@/hooks/metricas/queries';
import {
  ajustarALaFuente,
  conPrimeraEnMayuscula,
  definicionInicial,
} from '@/lib/metricas/metricasAMedida';
import {
  PASO_DEL_NOMBRE,
  hechosDespuesDeElegir,
  pasoConTarjetas,
  pasosSinContestar,
} from '@/lib/metricas/pasosDeLaReceta';
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

// Cinco solapas, una por pregunta. Nada viene elegido de antemano: la métrica sólo se puede guardar con todas
// contestadas y con nombre, así nunca se arma una con datos que la persona no decidió.
function PasoAPaso({ sistemaId, guardando, onAgregar, fuentes }: PropsDelPasoAPaso) {
  const receta = useRecetaPasoAPaso(fuentes);
  const vista = useVistaEnVivo(sistemaId, receta.definicion, receta.contestadasLasPreguntas);
  // Sin datos para calcularla no se guarda: sería una métrica vacía.
  const sinDatos = vista.resultado?.valor == null;
  const guardarLaMia = (evento: FormEvent) => {
    evento.preventDefault();
    const definicion = receta.lista();
    if (definicion && !sinDatos) onAgregar(definicion, 'PROPIA');
  };
  return (
    <form className="armala-vos" onSubmit={guardarLaMia} noValidate>
      <PasosHechos
        definicion={receta.definicion}
        fuentes={fuentes}
        hechos={receta.hechos}
        paso={receta.paso}
        onIrAlPaso={receta.irAlPaso}
      />
      <PasoActual receta={receta} fuentes={fuentes} guardando={guardando} sinDatos={sinDatos} />
      {receta.contestadasLasPreguntas && (
        <VistaEnVivo vista={vista} definicion={receta.definicion} />
      )}
    </form>
  );
}

const recetaInicialPara = (fuentes: FuenteDeMetrica[]) =>
  ajustarALaFuente(definicionInicial(), fuentes[0] ?? 'VENTAS');

/** La receta que se va armando, qué solapas ya se contestaron y si está lista para guardar. */
function useRecetaPasoAPaso(fuentes: FuenteDeMetrica[]) {
  const [definicion, setDefinicion] = useState(() => recetaInicialPara(fuentes));
  const [hechos, setHechos] = useState<number[]>([]);
  const [paso, setPaso] = useState(0);
  const nombre = definicion.nombre.trim();
  const faltan = pasosSinContestar(hechos);
  const sePuedeGuardar = faltan.length === 0 && nombre !== '';
  return {
    definicion,
    hechos,
    paso,
    faltan: nombre === '' ? [...faltan, 'Nombre'] : faltan,
    sePuedeGuardar,
    contestadasLasPreguntas: faltan.length === 0,
    irAlPaso: setPaso,
    elegirEnElPaso: (nueva: DefinicionDeMetrica) => {
      setHechos(hechosDespuesDeElegir(hechos, paso, nueva.fuente !== definicion.fuente));
      setDefinicion(nueva);
      setPaso(paso + 1);
    },
    cambiar: setDefinicion,
    nombrar: (nuevo: string) =>
      setDefinicion({ ...definicion, nombre: conPrimeraEnMayuscula(nuevo) }),
    lista: () => (sePuedeGuardar ? { ...definicion, nombre } : null),
  };
}

type RecetaPasoAPaso = ReturnType<typeof useRecetaPasoAPaso>;

type PropsDelPasoActual = {
  receta: RecetaPasoAPaso;
  fuentes: FuenteDeMetrica[];
  guardando: boolean;
  sinDatos: boolean;
};

function PasoActual({ receta, fuentes, guardando, sinDatos }: PropsDelPasoActual) {
  if (receta.paso === PASO_DEL_NOMBRE)
    return <PasoDelNombre receta={receta} guardando={guardando} sinDatos={sinDatos} />;
  return (
    <TarjetasDelPaso
      paso={pasoConTarjetas(receta.paso)}
      numero={receta.paso + 1}
      hecho={receta.hechos.includes(receta.paso)}
      definicion={receta.definicion}
      fuentes={fuentes}
      onElegir={receta.elegirEnElPaso}
      onCambiar={receta.cambiar}
    />
  );
}

type PropsDelNombre = { receta: RecetaPasoAPaso; guardando: boolean; sinDatos: boolean };

function PasoDelNombre({ receta, guardando, sinDatos }: PropsDelNombre) {
  return (
    <fieldset className="armala-vos-paso" data-color="petroleo">
      <legend>Paso 5 de 5 · Ponele un nombre</legend>
      <label>
        Nombre
        <input
          type="text"
          value={receta.definicion.nombre}
          maxLength={80}
          placeholder="Por ejemplo: Ventas por día"
          onChange={(evento) => receta.nombrar(evento.target.value)}
        />
      </label>
      <LoQueFalta faltan={receta.faltan} />
      {receta.contestadasLasPreguntas && sinDatos && <SinDatosParaGuardar />}
      <button
        type="submit"
        className="metricas-medida-agregar"
        disabled={guardando || !receta.sePuedeGuardar || sinDatos}
      >
        Guardar métrica
      </button>
    </fieldset>
  );
}

function SinDatosParaGuardar() {
  return (
    <p className="armala-vos-falta" role="status">
      Todavía no hay datos para armar esta métrica. Cargalos y volvé a intentar.
    </p>
  );
}

function LoQueFalta({ faltan }: { faltan: string[] }) {
  if (faltan.length === 0) return null;
  return (
    <p className="armala-vos-falta" role="status">
      Para guardarla te falta: {faltan.join(', ')}.
    </p>
  );
}

function VistaEnVivo({
  vista,
  definicion,
}: {
  vista: ReturnType<typeof useVistaEnVivo>;
  definicion: DefinicionDeMetrica;
}) {
  const { resultado, fallo } = vista;
  if (fallo)
    return (
      <p className="metrica-medida-error" role="alert">
        No pudimos calcularla. Probá de nuevo.
      </p>
    );
  if (!resultado) return null;
  return (
    <section className="panel-agregar-propuesta" aria-label="Así quedaría">
      <NombreDeLaMetrica nombre="Así quedaría" resultado={resultado} />
      <VistaPrevia definicion={definicion} resultado={resultado} />
    </section>
  );
}
