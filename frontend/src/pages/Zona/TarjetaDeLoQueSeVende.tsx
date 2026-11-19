import { Btn } from '@/components/sistema/Btn';
import { varsDeColor } from '@/components/sistema/colores';
import { Icon } from '@/components/sistema/Icon';
import { pesos } from '@/lib/sistema/alertas';
import { porcentajeDeComercios, type ProductoDeZona, type Zona } from '@/lib/sistema/zona';

type Props = {
  producto: ProductoDeZona;
  zona: Zona;
  yaLoTiene: boolean;
  puedeSumar: boolean;
  onSumar: () => void;
};

function CabezaDeLaTarjeta({ producto, zona }: Pick<Props, 'producto' | 'zona'>) {
  return (
    <div className="zona-tarjeta-cabeza">
      <div>
        <strong>{producto.nombre}</strong>
        <div className="zona-meta">
          Lo venden {producto.ventas.comercios} de {zona.comercios} comercios · unas{' '}
          {producto.ventas.unidadesPorSemana} unidades por semana cada uno
        </div>
      </div>
      <div className="zona-precio">
        {pesos(producto.precio.promedio)}
        <small>promedio</small>
      </div>
    </div>
  );
}

function BarraDeComercios({ porcentaje }: { porcentaje: number }) {
  return (
    <div
      className="zona-barra"
      role="img"
      aria-label={`${porcentaje}% de los comercios de la zona lo venden`}
    >
      <i style={{ width: `${porcentaje}%` }} />
    </div>
  );
}

function AccionDeLaTarjeta({ yaLoTiene, puedeSumar, onSumar }: Omit<Props, 'producto' | 'zona'>) {
  if (yaLoTiene) {
    return (
      <span className="zona-etiqueta justo">
        <Icon name="check-circle" /> Ya lo tenés
      </span>
    );
  }
  return (
    <Btn cls="soft small" disabled={!puedeSumar} onClick={onSumar}>
      <Icon name="plus" /> Sumar a mis productos
    </Btn>
  );
}

/** Un producto que se vende cerca: cuántos comercios lo venden y si ya lo tenés o podés sumarlo. */
export function TarjetaDeLoQueSeVende({ producto, zona, yaLoTiene, puedeSumar, onSumar }: Props) {
  return (
    <article className="zona-tarjeta" style={varsDeColor(yaLoTiene ? 'blue' : 'orange')}>
      <CabezaDeLaTarjeta producto={producto} zona={zona} />
      <BarraDeComercios porcentaje={porcentajeDeComercios(producto, zona)} />
      <div className="zona-acciones">
        <AccionDeLaTarjeta yaLoTiene={yaLoTiene} puedeSumar={puedeSumar} onSumar={onSumar} />
      </div>
    </article>
  );
}
