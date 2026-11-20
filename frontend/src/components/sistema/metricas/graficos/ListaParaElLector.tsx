import type { DatoDelGrafico } from '@/lib/metricas/graficos';

/** El mismo reparto escrito, para quien no ve el dibujo (lector de pantalla). */
export function ListaParaElLector({ datos }: { datos: DatoDelGrafico[] }) {
  return (
    <ul className="sr-only">
      {datos.map((dato) => (
        <li key={dato.etiqueta}>
          {dato.etiqueta}: {dato.detalle}
        </li>
      ))}
    </ul>
  );
}
