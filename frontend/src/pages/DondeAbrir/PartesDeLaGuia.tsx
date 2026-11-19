import { Sami } from '@/components/sistema/Sami';
import { NOMBRE_DE_JURISDICCION, type Jurisdiccion } from '@/lib/dondeAbrir';
import {
  ETIQUETA_DE_MODALIDAD,
  type Aprobacion,
  type GuiaDeTramites as Guia,
  type Modalidad,
} from '@/lib/tramitesParaAbrir';

function ChipDeModalidad({ modalidad }: { modalidad: Modalidad }) {
  return <b className={`modalidad ${modalidad}`}>{ETIQUETA_DE_MODALIDAD[modalidad]}</b>;
}

export function NarracionDeSami({ guia }: { guia: Guia }) {
  return (
    <div className="donde-narra">
      <div className="donde-sami-cara" aria-hidden="true">
        <Sami pose="hello" />
      </div>
      <div>
        <h4>{guia.titulo}</h4>
        <p>{guia.resumenSami}</p>
      </div>
    </div>
  );
}

type PropsDeTamano = { superficie: number; onSuperficie: (metros: number) => void };

export function TamanoDelLocal({ superficie, onSuperficie }: PropsDeTamano) {
  return (
    <div className="donde-superficie">
      <label htmlFor="donde-superficie">
        Tamaño del local <output htmlFor="donde-superficie">{superficie} m²</output>
      </label>
      <input
        id="donde-superficie"
        type="range"
        min={20}
        max={600}
        step={10}
        value={superficie}
        aria-valuetext={`${superficie} metros cuadrados`}
        onChange={(evento) => onSuperficie(Number(evento.target.value))}
      />
    </div>
  );
}

export function ResumenDelTramite({ guia }: { guia: Guia }) {
  return (
    <>
      <ul className="donde-sellos" aria-label="Resumen del trámite">
        <li>
          <span>Cómo se hace</span>
          <ChipDeModalidad modalidad={guia.modalidad} />
        </li>
        <li>
          <span>Costo</span>
          <b>{guia.costoCorto}</b>
        </li>
        <li>
          <span>Profesional</span>
          <b>{guia.profesionalCorto}</b>
        </li>
      </ul>
      <p className="donde-nota-chica">
        <strong>Costo:</strong> {guia.costo} <strong>Profesional:</strong> {guia.profesional}{' '}
        <strong>Plazo:</strong> {guia.plazo}
      </p>
    </>
  );
}

function MasInformacion({ aprobacion }: { aprobacion: Aprobacion }) {
  if (!aprobacion.fuente) return null;
  return (
    <a className="donde-mas-info" href={aprobacion.fuente.url} target="_blank" rel="noreferrer">
      Más información sobre {aprobacion.nombre}
    </a>
  );
}

function AprobacionDeLaGuia({ aprobacion }: { aprobacion: Aprobacion }) {
  return (
    <li>
      <div>
        <strong>{aprobacion.nombre}</strong>
        <span>{aprobacion.organismo}</span>
        <p>{aprobacion.paraQue}</p>
        {aprobacion.cuando && <em>{aprobacion.cuando}</em>}
        <MasInformacion aprobacion={aprobacion} />
      </div>
      <ChipDeModalidad modalidad={aprobacion.modalidad} />
    </li>
  );
}

export function AprobacionesNecesarias({ guia }: { guia: Guia }) {
  return (
    <section aria-label="Aprobaciones que necesitás">
      <h4 className="donde-subtitulo">Aprobaciones que necesitás</h4>
      <ul className="donde-aprobaciones">
        {guia.aprobaciones.map((aprobacion) => (
          <AprobacionDeLaGuia key={aprobacion.id} aprobacion={aprobacion} />
        ))}
      </ul>
    </section>
  );
}

export function PasosDelTramite({ guia }: { guia: Guia }) {
  return (
    <section aria-label={guia.tituloDePasos}>
      <h4 className="donde-subtitulo">{guia.tituloDePasos}</h4>
      <ol className="donde-pasos">
        {guia.pasos.map((paso) => (
          <li key={paso.texto}>
            <span>{paso.texto}</span>
            {paso.modalidad && <ChipDeModalidad modalidad={paso.modalidad} />}
          </li>
        ))}
      </ol>
    </section>
  );
}

function ListaPlegable({ titulo, textos }: { titulo: string; textos: string[] }) {
  return (
    <details className="donde-plegable">
      <summary>{titulo}</summary>
      <ul>
        {textos.map((texto) => (
          <li key={texto}>{texto}</li>
        ))}
      </ul>
    </details>
  );
}

export const DocumentosAMano = ({ guia }: { guia: Guia }) => (
  <ListaPlegable titulo="Documentos para tener a mano" textos={guia.documentos} />
);

export const CosasATenerEnCuenta = ({ guia }: { guia: Guia }) => (
  <ListaPlegable titulo="Cosas a tener en cuenta" textos={guia.avisos} />
);

export function DondeIrYConQuienHablar({ guia }: { guia: Guia }) {
  return (
    <details className="donde-plegable">
      <summary>Dónde ir y con quién hablar</summary>
      <ul className="donde-oficinas">
        {guia.oficinas.map((oficina) => (
          <li key={oficina.nombre}>
            <strong>{oficina.nombre}</strong>
            <span>{oficina.donde}</span>
            {oficina.horario && <span>{oficina.horario}</span>}
            {oficina.contacto && <span>{oficina.contacto}</span>}
          </li>
        ))}
      </ul>
    </details>
  );
}

export function FuentesDeLaGuia({
  guia,
  jurisdiccion,
}: {
  guia: Guia;
  jurisdiccion: Jurisdiccion;
}) {
  return (
    <footer className="donde-fuentes">
      <p>
        Sale de las páginas oficiales de {NOMBRE_DE_JURISDICCION[jurisdiccion]}, consultadas en
        noviembre de 2026. Confirmá los requisitos antes de empezar: pueden cambiar. En esta
        demostración no se inicia ningún trámite.
      </p>
      <ul>
        {guia.fuentes.map((fuente) => (
          <li key={fuente.url}>
            <a href={fuente.url} target="_blank" rel="noreferrer">
              {fuente.texto}
            </a>
          </li>
        ))}
      </ul>
    </footer>
  );
}
