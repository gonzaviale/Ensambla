import { useState } from 'react';
import { CheckIcon } from '@phosphor-icons/react';
import { Sami } from '@/components/sistema/Sami';
import {
  LOCAL_ACTUAL,
  NOMBRE_DE_RUBRO,
  SIGLA_DE_JURISDICCION,
  UN_RUBRO,
  type Rubro,
} from '@/lib/dondeAbrir';

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
            Veo que tu negocio es <strong>{LOCAL_ACTUAL.nombre}</strong>, {UN_RUBRO[rubro]} en{' '}
            <strong>
              {LOCAL_ACTUAL.barrio}, {SIGLA_DE_JURISDICCION.caba}
            </strong>
            . ¿Es correcto?
          </p>
          <small>
            Lo tomo de la configuración de tu negocio. En esta demostración es un ejemplo.
          </small>
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
          <button type="button" className="btn primary" onClick={() => onConfirmar(rubro)}>
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
