import { CheckIcon } from '@phosphor-icons/react/dist/csr/Check';

const BENEFICIOS = [
  {
    titulo: 'Encontrá esa anotación.',
    texto: 'Volvé a una venta, un gasto o un fiado sin recorrer todas las hojas.',
  },
  {
    titulo: 'Revisá lo que pasó en tu negocio.',
    texto: 'Tené tus anotaciones reunidas para consultar y hacer tus cuentas.',
  },
  { titulo: 'Seguí con tu día.', texto: 'Dejá ordenado lo que necesitás volver a mirar después.' },
] as const;

export function Pilares() {
  return (
    <section className="ens-outcome" id="por-que" aria-labelledby="beneficios-titulo">
      <div className="ens-wrap">
        <h2 id="beneficios-titulo">
          Menos buscar.
          <br />
          Más tiempo
          <br />
          para atender.
        </h2>
        <ul className="ens-outcome-list">
          {BENEFICIOS.map(({ titulo, texto }) => (
            <li key={titulo}>
              <CheckIcon aria-hidden size={24} />
              <div>
                <h3>{titulo}</h3>
                <p>{texto}</p>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
