import { useMemo } from 'react';
import { Sami } from '@/components/sistema/Sami';
import { NOMBRE_DE_JURISDICCION, type DatosDeZona, type Rubro } from '@/lib/dondeAbrir';
import { ETIQUETA_DE_MODALIDAD, guiaDeTramites, type Modalidad } from '@/lib/tramitesParaAbrir';

type Props = {
  zona: DatosDeZona;
  rubro: Rubro;
  superficie: number;
  onSuperficie: (metros: number) => void;
};

// Lo que hay que tramitar para abrir en la zona elegida: qué aprobaciones, qué documentos y si se hace online o
// en la oficina. Sami lo cuenta primero en pocas palabras; el detalle está abajo.
export function GuiaDeTramites({ zona, rubro, superficie, onSuperficie }: Props) {
  const guia = useMemo(
    () => guiaDeTramites(zona.jurisdiccion, rubro, superficie),
    [zona.jurisdiccion, rubro, superficie],
  );

  return (
    <div className="donde-guia">
      <div className="donde-narra">
        <div className="donde-sami-cara" aria-hidden="true">
          <Sami pose="hello" />
        </div>
        <div>
          <h4>{guia.titulo}</h4>
          <p>{guia.resumenSami}</p>
        </div>
      </div>

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

      <ul className="donde-sellos" aria-label="Resumen del trámite">
        <li>
          <span>Cómo se hace</span>
          <b className={`modalidad ${guia.modalidad}`}>{ETIQUETA_DE_MODALIDAD[guia.modalidad]}</b>
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

      <section aria-label="Aprobaciones que necesitás">
        <h4 className="donde-subtitulo">Aprobaciones que necesitás</h4>
        <ul className="donde-aprobaciones">
          {guia.aprobaciones.map((aprobacion) => (
            <li key={aprobacion.id}>
              <div>
                <strong>{aprobacion.nombre}</strong>
                <span>{aprobacion.organismo}</span>
                <p>{aprobacion.paraQue}</p>
                {aprobacion.cuando && <em>{aprobacion.cuando}</em>}
              </div>
              <ChipDeModalidad modalidad={aprobacion.modalidad} />
            </li>
          ))}
        </ul>
      </section>

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

      <details className="donde-plegable">
        <summary>Documentos para tener a mano</summary>
        <ul>
          {guia.documentos.map((documento) => (
            <li key={documento}>{documento}</li>
          ))}
        </ul>
      </details>

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

      <details className="donde-plegable">
        <summary>Cosas a tener en cuenta</summary>
        <ul>
          {guia.avisos.map((aviso) => (
            <li key={aviso}>{aviso}</li>
          ))}
        </ul>
      </details>

      <footer className="donde-fuentes">
        <p>
          Sale de las páginas oficiales de {NOMBRE_DE_JURISDICCION[zona.jurisdiccion]}, consultadas
          en noviembre de 2026. Confirmá los requisitos antes de empezar: pueden cambiar. En esta
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
    </div>
  );
}

function ChipDeModalidad({ modalidad }: { modalidad: Modalidad }) {
  return <b className={`modalidad ${modalidad}`}>{ETIQUETA_DE_MODALIDAD[modalidad]}</b>;
}
