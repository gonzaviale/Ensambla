import type { TooltipContentProps } from 'recharts';
import type { DatoDelGrafico } from '@/lib/metricas/graficos';

/** Lo que aparece al pasar el mouse (o llegar con el teclado) por una parte: de quién es y cuánto vale. */
export function TarjetaDelDato({ active, payload }: TooltipContentProps) {
  const dato = payload?.[0]?.payload as DatoDelGrafico | undefined;
  if (!active || !dato) return null;
  return (
    <div className="grafico-tarjeta" style={{ borderColor: dato.color }}>
      <strong>{dato.etiqueta}</strong>
      <span>{dato.detalle}</span>
      <span className="grafico-tarjeta-porcentaje">{dato.porcentaje}% del total</span>
    </div>
  );
}
