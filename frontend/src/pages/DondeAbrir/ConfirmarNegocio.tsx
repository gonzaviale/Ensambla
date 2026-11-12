import { useState } from 'react';
import { CheckIcon } from '@phosphor-icons/react';
import { Link } from '@tanstack/react-router';
import { Sami } from '@/components/sistema/Sami';
import { useConfiguracionNegocio } from '@/hooks/negocio/queries';
import { NOMBRE_DE_RUBRO, UN_RUBRO, type Rubro } from '@/lib/dondeAbrir';

const RUBROS: { valor: Rubro; etiqueta: string }[] = [
  { valor: 'almacen', etiqueta: 'Un almacén' },
  { valor: 'papeleria', etiqueta: 'Una papelería' },
  { valor: 'kiosco', etiqueta: 'Un kiosco' },
];

type Props = {
  rubro: Rubro;
  onConfirmar: (rubro: Rubro) => void;
};

// El primer paso: Sami dice lo que sabe del negocio y le pide a la persona que lo confirme.
export function ConfirmarNegocio({ rubro, onConfirmar }: Props) {
  const [cambiando, setCambiando] = useState(false);
  const [elegido, setElegido] = useState<Rubro>(rubro);
  const configuracion = useConfiguracionNegocio();
  const negocio = configuracion.data;
  const lugar = negocio?.ubicacion
    ? `${negocio.ubicacion.localidad ?? negocio.zona ?? 'tu ubicación'}`
    : null;

  return (
    <section className="donde-paso" aria-labelledby="donde-confirmar-titulo">
      <div className="donde-sami-dice">
        <div className="donde-sami-cara grande" aria-hidden="true">
          <Sami pose="hello" />
        </div>
        <div className="donde-globo">
          <h2 id="donde-confirmar-titulo">
            Antes de empezar, quiero asegurarme de que te entiendo
          </h2>
          <p>
            {configuracion.isPending ? (
              'Estoy leyendo la configuración de tu negocio…'
            ) : negocio ? (
              <>
                Veo que tu negocio es <strong>{negocio.nombre}</strong>
                {lugar ? (
                  <>
                    {' '}
                    en <strong>{lugar}</strong>
                  </>
                ) : (
                  ''
                )}
                . Vamos a buscar dónde abrir <strong>{UN_RUBRO[rubro]}</strong>. ¿Es correcto?
              </>
            ) : (
              'No pude leer la configuración de tu negocio.'
            )}
          </p>
          <small>
            Usamos estos datos para calcular la distancia y comparar un local nuevo con el actual.
          </small>
          <Link className="text-link donde-editar-negocio" to="/ubicacion">
            Editar datos de mi negocio
          </Link>
          {negocio && !negocio.ubicacion && (
            <p className="donde-falta-ubicacion">
              Cargá la ubicación antes de comparar distancias.
            </p>
          )}
        </div>
      </div>

      {cambiando ? (
        <div className="donde-respuestas-grandes">
          <fieldset className="donde-opciones-grandes">
            <legend>¿Qué tipo de local querés abrir?</legend>
            {RUBROS.map((opcion) => (
              <label key={opcion.valor} className={elegido === opcion.valor ? 'elegida' : ''}>
                <input
                  type="radio"
                  name="rubro-a-abrir"
                  checked={elegido === opcion.valor}
                  onChange={() => setElegido(opcion.valor)}
                />
                <strong>{opcion.etiqueta}</strong>
              </label>
            ))}
          </fieldset>
          <div className="donde-botones">
            <button type="button" className="btn primary" onClick={() => onConfirmar(elegido)}>
              Seguir con {UN_RUBRO[elegido]}
            </button>
            <button type="button" className="btn ghost" onClick={() => setCambiando(false)}>
              Volver
            </button>
          </div>
        </div>
      ) : (
        <div className="donde-botones">
          <button
            type="button"
            className="btn primary"
            disabled={configuracion.isPending || configuracion.isError}
            onClick={() => onConfirmar(rubro)}
          >
            <CheckIcon aria-hidden size={18} weight="bold" /> Sí, es correcto
          </button>
          <button type="button" className="btn secondary" onClick={() => setCambiando(true)}>
            No, quiero cambiar algo
          </button>
        </div>
      )}
      <p className="donde-nota-chica">
        Voy a mirar zonas para {UN_RUBRO[rubro]}. Si cambiás de {NOMBRE_DE_RUBRO[rubro]} a otro
        rubro, recalculo todo.
      </p>
    </section>
  );
}
