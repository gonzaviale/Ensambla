import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { ContenidoPremium } from '@/components/planes/ContenidoPremium';
import { ResumenFrenteALaZona } from '@/components/sistema/zona/ResumenFrenteALaZona';
import { useInventarioPropio, useSumarProducto } from '@/hooks/zona/useInventarioPropio';
import { useZonaDeLoQueSeVende } from '@/hooks/zona/queries';
import {
  masVendidosPrimero,
  sinSuficientesComercios,
  yaLoTiene,
  type DestinoDeProducto,
  type Zona,
} from '@/lib/sistema/zona';
import { SinDatosSuficientes } from './SinDatosSuficientes';
import { TarjetaDeLoQueSeVende } from './TarjetaDeLoQueSeVende';

type Consulta = ReturnType<typeof useZonaDeLoQueSeVende>;

function AvisoSinInventario({ destino }: { destino: DestinoDeProducto | undefined }) {
  if (destino) return null;
  return (
    <div className="info-box">Para sumar productos a tu stock necesitás una sección de Stock.</div>
  );
}

function EstadoDeLaConsulta({ cerca }: { cerca: Consulta }) {
  if (cerca.isPending) {
    return (
      <div className="empty" role="status">
        <Icon name="orders" />
        <p>Buscando lo que se vende cerca…</p>
      </div>
    );
  }
  if (!cerca.isError) return null;
  return (
    <div className="empty" role="alert">
      <Icon name="orders" />
      <p>No pudimos traer lo que se vende cerca.</p>
      <Btn cls="soft small" onClick={() => void cerca.refetch()}>
        Probar de nuevo
      </Btn>
    </div>
  );
}

// `zona` es la de «Precios» (primero lo que ya tenés): sirve para el resumen lateral, que compara tus precios. La lista
// sale de la zona de esta pestaña, que prioriza lo que todavía no tenés y gasta su propio cupo del plan gratuito.
export function VistaDeLoQueSeVende({ zona }: { zona: Zona }) {
  const { propios, destino, nombres } = useInventarioPropio();
  const sumar = useSumarProducto();
  const cerca = useZonaDeLoQueSeVende(!sinSuficientesComercios(zona));
  if (sinSuficientesComercios(zona)) return <SinDatosSuficientes zona={zona} />;
  const productos = masVendidosPrimero(cerca.data?.productos ?? []);

  return (
    <>
      <div className="zona-grid">
        <section className="zona-panel">
          <AvisoSinInventario destino={destino} />
          <div className="row between">
            <h2>Lo más vendido en {zona.nombre}</h2>
            <span className="tag outline">{productos.length}</span>
          </div>
          <div className="zona-lista">
            <EstadoDeLaConsulta cerca={cerca} />
            {productos.map((producto) => (
              <TarjetaDeLoQueSeVende
                key={producto.nombre}
                producto={producto}
                zona={zona}
                yaLoTiene={yaLoTiene(producto, nombres)}
                puedeSumar={Boolean(destino)}
                onSumar={() => destino && sumar(destino, producto)}
              />
            ))}
          </div>
        </section>
        <aside className="zona-lateral">
          <ResumenFrenteALaZona productos={zona.productos} propios={propios} zona={zona.nombre} />
        </aside>
      </div>
      <ContenidoPremium acceso={cerca.data?.acceso} recurso="recomendaciones" compacto />
    </>
  );
}
