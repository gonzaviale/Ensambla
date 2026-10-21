import { DownloadSimpleIcon } from '@phosphor-icons/react/dist/csr/DownloadSimple';
import { FileCsvIcon } from '@phosphor-icons/react/dist/csr/FileCsv';

export function TusDatos() {
  return (
    <section className="ens-data" id="tus-datos" aria-labelledby="tus-datos-titulo">
      <div className="ens-wrap">
        <span className="ens-data-icon">
          <DownloadSimpleIcon aria-hidden size={30} />
        </span>
        <div>
          <p className="ens-review-badge">Función prevista</p>
          <h2 id="tus-datos-titulo">Tus anotaciones son tuyas.</h2>
          <p>
            Queremos que puedas descargar una copia de tus ventas, gastos y fiados para guardarla o
            seguir trabajando por tu cuenta.
          </p>
          <p className="ens-data-pending">La descarga todavía no está disponible.</p>
        </div>
        <div className="ens-file" aria-hidden="true">
          <FileCsvIcon size={32} />
          <strong>Mis anotaciones.csv</strong>
          <span>Una copia para llevarte.</span>
          <span>Descarga prevista</span>
        </div>
      </div>
    </section>
  );
}
