import { useState } from 'react';
import { ArrowLeftIcon } from '@phosphor-icons/react';
import { Sami } from '@/components/sistema/Sami';
import { useConfiguracionNegocio } from '@/hooks/negocio/queries';
import type { RespuestasDeSami } from '@/lib/respuestasDeSami';

type Opcion = { valor: string; titulo: string; ayuda: string };
type Pregunta = {
  clave: keyof RespuestasDeSami;
  texto: string;
  opciones: Opcion[];
};

const PREGUNTAS: Pregunta[] = [
  {
    clave: 'alcance',
    texto: '¿Dónde querés buscar?',
    opciones: [
      { valor: 'cerca', titulo: 'Cerca de mi local actual', ayuda: 'Hasta 4 km de tu local actual' },
      {
        valor: 'toda',
        titulo: 'En cualquier zona',
        ayuda: 'Incluye zonas de la Ciudad y de La Matanza',
      },
    ],
  },
  {
    clave: 'prioridad',
    texto: '¿Qué pesa más para vos?',
    opciones: [
      { valor: 'alquiler', titulo: 'Alquiler bajo', ayuda: 'Prefiero gastar menos por mes' },
      { valor: 'movimiento', titulo: 'Mucho movimiento', ayuda: 'Prefiero que pase mucha gente' },
      { valor: 'equilibrado', titulo: 'Un equilibrio', ayuda: 'Un poco de cada cosa' },
    ],
  },
  {
    clave: 'publico',
    texto: '¿A quién le vendés más?',
    opciones: [
      { valor: 'vecinos', titulo: 'A los vecinos', ayuda: 'Gente que vive en la zona' },
      {
        valor: 'estudiantes',
        titulo: 'A estudiantes y familias',
        ayuda: 'Cerca de escuelas y parques',
      },
      { valor: 'oficinas', titulo: 'A gente que trabaja cerca', ayuda: 'Oficinas y bancos' },
    ],
  },
  {
    clave: 'tamano',
    texto: '¿Qué tamaño tendría el local?',
    opciones: [
      { valor: 'chico', titulo: 'Chico', ayuda: 'Hasta 70 m²' },
      { valor: 'mediano', titulo: 'Mediano', ayuda: 'De 70 a 200 m²' },
      { valor: 'grande', titulo: 'Grande', ayuda: 'De 200 a 500 m²' },
      { valor: 'muy-grande', titulo: 'Muy grande', ayuda: 'Más de 500 m²' },
    ],
  },
];

const tituloDeLaRespuesta = (pregunta: Pregunta, valor: string) =>
  pregunta.opciones.find((opcion) => opcion.valor === valor)?.titulo ?? valor;

type Props = {
  respuestas: RespuestasDeSami;
  onResponder: (respuestas: RespuestasDeSami) => void;
  onTerminar: () => void;
};

// Sami pregunta de a una. Lo ya respondido queda arriba como una charla; al contestar la última, muestra las zonas.
export function PreguntasDeSami({ respuestas, onResponder, onTerminar }: Props) {
  const [indice, setIndice] = useState(0);
  const negocio = useConfiguracionNegocio().data;
  const localidad = negocio?.ubicacion?.localidad ?? negocio?.zona ?? 'tu local actual';
  const actual = PREGUNTAS[indice];

  const responder = (pregunta: Pregunta, valor: string) => {
    onResponder({ ...respuestas, [pregunta.clave]: valor });
    if (indice === PREGUNTAS.length - 1) onTerminar();
    else setIndice(indice + 1);
  };

  return (
    <section className="donde-paso" aria-labelledby="donde-pregunta-titulo">
      <ol className="donde-charla" aria-label="Lo que ya me contaste">
        {PREGUNTAS.slice(0, indice).map((pregunta) => (
          <li key={pregunta.clave}>
            <span className="sami-dice">{pregunta.texto}</span>
            <span className="vos-dijiste">
              {tituloDeLaRespuesta(pregunta, respuestas[pregunta.clave])}
            </span>
          </li>
        ))}
      </ol>

      {actual && (
        <div className="donde-pregunta-actual">
          <div className="donde-sami-dice">
            <div className="donde-sami-cara" aria-hidden="true">
              <Sami pose="think" />
            </div>
            <div className="donde-globo">
              <small>
                Pregunta {indice + 1} de {PREGUNTAS.length}
              </small>
              <h2 id="donde-pregunta-titulo">{actual.texto}</h2>
            </div>
          </div>
          <div
            className="donde-opciones-grandes"
            role="group"
            aria-labelledby="donde-pregunta-titulo"
          >
            {actual.opciones.map((opcion) => (
              <button
                key={opcion.valor}
                type="button"
                className="donde-opcion"
                onClick={() => responder(actual, opcion.valor)}
              >
                <strong>{opcion.titulo}</strong>
                <span>
                  {opcion.valor === 'cerca' ? `Hasta 4 km de ${localidad}` : opcion.ayuda}
                </span>
              </button>
            ))}
          </div>
          {indice > 0 && (
            <button type="button" className="donde-volver" onClick={() => setIndice(indice - 1)}>
              <ArrowLeftIcon aria-hidden size={14} /> Volver a la pregunta anterior
            </button>
          )}
        </div>
      )}
    </section>
  );
}
