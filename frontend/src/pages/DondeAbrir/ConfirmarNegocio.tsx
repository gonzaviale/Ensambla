import { useState } from 'react';
import { CheckIcon } from '@phosphor-icons/react';
import { Link } from '@tanstack/react-router';
import { Sami } from '@/components/sistema/Sami';
import { useConfiguracionNegocio } from '@/hooks/negocio/queries';
import { UN_RUBRO, type Rubro } from '@/lib/dondeAbrir';
import type { ConfiguracionNegocio } from '@/types/negocio';

const RUBROS: { valor: Rubro; etiqueta: string }[] = [
  { valor: 'almacen', etiqueta: 'Un almacén' },
  { valor: 'papeleria', etiqueta: 'Una papelería' },
  { valor: 'kiosco', etiqueta: 'Un kiosco' },
];

type Configuracion = ReturnType<typeof useConfiguracionNegocio>;

type Props = {
  rubro: Rubro;
  onConfirmar: (rubro: Rubro) => void;
};

const lugarDelNegocio = ({ ubicacion, zona }: ConfiguracionNegocio) =>
  ubicacion ? (ubicacion.localidad ?? zona ?? 'tu ubicación') : null;

function EnElLugar({ negocio }: { negocio: ConfiguracionNegocio }) {
  const lugar = lugarDelNegocio(negocio);
  if (!lugar) return null;
  return (
    <>
      {' '}
      en <strong>{lugar}</strong>
    </>
  );
}

function LoQueSabemosDelNegocio({
  configuracion,
  rubro,
}: {
  configuracion: Configuracion;
  rubro: Rubro;
}) {
  const negocio = configuracion.data;
  if (configuracion.isPending) return 'Estoy leyendo la configuración de tu negocio…';
  if (!negocio) return 'No pude leer la configuración de tu negocio.';
  return (
    <>
      Veo que tu negocio es <strong>{negocio.nombre}</strong>
      <EnElLugar negocio={negocio} />. Vamos a buscar dónde abrir <strong>{UN_RUBRO[rubro]}</strong>
      . ¿Querés continuar con estos datos?
    </>
  );
}

function FaltaLaUbicacion({ negocio }: { negocio: ConfiguracionNegocio | undefined }) {
  if (!negocio || negocio.ubicacion) return null;
  return <p className="donde-falta-ubicacion">Cargá la ubicación antes de comparar distancias.</p>;
}

function SamiDiceDelNegocio({
  configuracion,
  rubro,
}: {
  configuracion: Configuracion;
  rubro: Rubro;
}) {
  return (
    <div className="donde-sami-dice">
      <div className="donde-sami-cara grande" aria-hidden="true">
        <Sami pose="hello" />
      </div>
      <div className="donde-globo">
        <h2 id="donde-confirmar-titulo">Antes de empezar, quiero asegurarme de que te entiendo</h2>
        <p>
          <LoQueSabemosDelNegocio configuracion={configuracion} rubro={rubro} />
        </p>
        <small>
          Usamos estos datos para calcular la distancia y comparar un local nuevo con el actual.
        </small>
        <Link className="text-link donde-editar-negocio" to="/ubicacion">
          Editar ubicación de mi negocio
        </Link>
        <FaltaLaUbicacion negocio={configuracion.data} />
      </div>
    </div>
  );
}

type PropsDeEleccion = {
  elegido: Rubro;
  onElegir: (rubro: Rubro) => void;
  onUsar: (rubro: Rubro) => void;
  onCancelar: () => void;
};

function EleccionDelRubro({ elegido, onElegir, onUsar, onCancelar }: PropsDeEleccion) {
  return (
    <div className="donde-respuestas-grandes">
      <fieldset className="donde-opciones-grandes">
        <legend>¿Qué tipo de local querés abrir?</legend>
        {RUBROS.map((opcion) => (
          <label key={opcion.valor} className={elegido === opcion.valor ? 'elegida' : ''}>
            <input
              type="radio"
              name="rubro-a-abrir"
              checked={elegido === opcion.valor}
              onChange={() => onElegir(opcion.valor)}
            />
            <strong>{opcion.etiqueta}</strong>
          </label>
        ))}
      </fieldset>
      <div className="donde-botones">
        <button type="button" className="btn primary" onClick={() => onUsar(elegido)}>
          Usar este tipo de local
        </button>
        <button type="button" className="btn ghost" onClick={onCancelar}>
          Cancelar
        </button>
      </div>
    </div>
  );
}

type PropsDeContinuar = { puedeContinuar: boolean; onContinuar: () => void; onCambiar: () => void };

function ContinuarConEstosDatos({ puedeContinuar, onContinuar, onCambiar }: PropsDeContinuar) {
  return (
    <div className="donde-botones">
      <button
        type="button"
        className="btn primary"
        disabled={!puedeContinuar}
        onClick={onContinuar}
      >
        <CheckIcon aria-hidden size={18} weight="bold" /> Continuar con estos datos
      </button>
      <button type="button" className="text-link" onClick={onCambiar}>
        Cambiar el tipo de local
      </button>
    </div>
  );
}

// El primer paso: Sami dice lo que sabe del negocio y le pide a la persona que lo confirme.
export function ConfirmarNegocio({ rubro, onConfirmar }: Props) {
  const [cambiando, setCambiando] = useState(false);
  const [elegido, setElegido] = useState<Rubro>(rubro);
  const configuracion = useConfiguracionNegocio();
  const puedeContinuar = !configuracion.isPending && !configuracion.isError;
  return (
    <section className="donde-paso" aria-labelledby="donde-confirmar-titulo">
      <SamiDiceDelNegocio configuracion={configuracion} rubro={rubro} />
      {cambiando ? (
        <EleccionDelRubro
          elegido={elegido}
          onElegir={setElegido}
          onUsar={onConfirmar}
          onCancelar={() => setCambiando(false)}
        />
      ) : (
        <ContinuarConEstosDatos
          puedeContinuar={puedeContinuar}
          onContinuar={() => onConfirmar(rubro)}
          onCambiar={() => setCambiando(true)}
        />
      )}
    </section>
  );
}
