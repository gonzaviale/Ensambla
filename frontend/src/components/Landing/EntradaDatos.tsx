import { useState } from 'react';
import { ArrowRightIcon } from '@phosphor-icons/react/dist/csr/ArrowRight';
import { NotebookIcon } from '@phosphor-icons/react/dist/csr/Notebook';
import { ScanIcon } from '@phosphor-icons/react/dist/csr/Scan';

const PASOS = [
  {
    id: 'foto',
    nombre: 'Sacás una foto',
    titulo: 'Así podrían quedar tus anotaciones',
    pie: 'De tus anotaciones a un cuaderno digital.',
    resumen: 'Tu punto de partida.',
    detalle:
      'Usás lo que ya tenés anotado. No hace falta volver a escribir cada dato para empezar.',
  },
  {
    id: 'revisar',
    nombre: 'Revisás los datos',
    titulo: 'Revisá antes de guardar',
    pie: 'Revisá la cantidad de yerba marcada en amarillo.',
    resumen: 'Vos tenés la última palabra.',
    detalle:
      'La cantidad de yerba necesita una segunda mirada. Revisás si dice 2, corregís lo que haga falta y elegís qué guardar.',
  },
  {
    id: 'guardar',
    nombre: 'Guardás en tu cuaderno',
    titulo: 'Tus anotaciones, en un solo lugar',
    pie: 'Ejemplo: anotaciones confirmadas en tu cuaderno.',
    resumen: 'Listo para volver a consultarlo.',
    detalle:
      'Después de confirmar, tus anotaciones quedan en tu cuaderno digital. Podés encontrarlas sin buscar hoja por hoja.',
  },
] as const;

type PasoId = (typeof PASOS)[number]['id'];
const PESOS = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  maximumFractionDigits: 0,
});
const ANOTACIONES = [
  { nombre: 'Yerba 500 g', cantidad: '2', importe: 5600 },
  { nombre: 'Fideos', cantidad: '1', importe: 1400 },
  { nombre: 'Fiado · Ana', cantidad: '—', importe: 3200 },
] as const;

// Demostración local del recorrido: cambiar de paso no lee fotos ni guarda anotaciones.
export function EntradaDatos() {
  const [pasoId, setPasoId] = useState<PasoId>('foto');
  const paso = PASOS.find((opcion) => opcion.id === pasoId) ?? PASOS[0];

  return (
    <section
      className="ens-section ens-demo-section"
      id="como-funciona"
      aria-labelledby="ejemplo-titulo"
    >
      <div className="ens-wrap">
        <div className="ens-demo-intro">
          <p className="ens-eyebrow">Mirá un ejemplo</p>
          <h2 id="ejemplo-titulo">
            Lo que ya anotás,
            <br />
            más fácil de encontrar.
          </h2>
          <p>
            Podés empezar con una foto de tu cuaderno. Revisás lo que se leyó y confirmás qué querés
            guardar.
          </p>
        </div>
        <ol className="ens-steps" aria-label="Pasos del ejemplo">
          {PASOS.map(({ id, nombre }, indice) => (
            <li key={id}>
              <button
                type="button"
                className="ens-step"
                aria-pressed={pasoId === id}
                aria-controls="ejemplo-contenido"
                onClick={() => setPasoId(id)}
              >
                <span className="ens-step-number" aria-hidden="true">
                  {indice + 1}
                </span>
                {nombre}
              </button>
            </li>
          ))}
        </ol>
        <div className="ens-example" id="ejemplo-contenido" aria-describedby="ejemplo-nota">
          <div className="ens-book" aria-label="Anotaciones del ejemplo en un cuaderno">
            <p className="ens-hand ens-hand-head">Ventas del día</p>
            <p className="ens-hand">
              Yerba 500 g × <span className="ens-circle">2</span>
            </p>
            <p className="ens-hand">{PESOS.format(5600)}</p>
            <p className="ens-hand">Fideos × 1 · {PESOS.format(1400)}</p>
            <p className="ens-hand">Ana — fiado {PESOS.format(3200)}</p>
          </div>
          <div className="ens-example-arrow">
            <ArrowRightIcon aria-hidden size={28} />
          </div>
          <div className="ens-digital">
            <div className="ens-digital-head">
              <span>{paso.titulo}</span>
              <NotebookIcon aria-hidden size={23} />
            </div>
            <table className="ens-table">
              <caption className="sr-only">Ejemplo ilustrativo de anotaciones ordenadas</caption>
              <thead>
                <tr>
                  <th scope="col">Anotación</th>
                  <th scope="col">Cant.</th>
                  <th scope="col">Importe</th>
                </tr>
              </thead>
              <tbody>
                {ANOTACIONES.map(({ nombre, cantidad, importe }, indice) => (
                  <tr key={nombre}>
                    <th scope="row">{nombre}</th>
                    <td>
                      <span
                        className={indice === 0 && pasoId !== 'guardar' ? 'ens-pending' : undefined}
                      >
                        {cantidad}
                      </span>
                    </td>
                    <td>{PESOS.format(importe)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="ens-digital-foot">
              <ScanIcon aria-hidden size={20} />
              {paso.pie}
            </p>
          </div>
        </div>
        <p className="ens-stage-caption" role="status">
          <strong>{paso.resumen}</strong>
          <span>{paso.detalle}</span>
        </p>
        <p className="ens-example-disclaimer" id="ejemplo-nota">
          Ejemplo ilustrativo. La lectura puede necesitar correcciones: siempre revisás antes de
          confirmar.
        </p>
      </div>
    </section>
  );
}
