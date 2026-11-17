import { Icon } from '@/components/sistema/Icon';
import { MetricasDelInicio } from '@/components/sistema/MetricasDelInicio';
import { useRubroDelPerfil } from '@/hooks/negocio/useDetalleDeNegocios';
import { detalleDelNegocio } from '@/lib/negocios/presentacion';
import { allBlocks } from '@/lib/sistema/core';
import { seleccionarNegocioActivo, useSistemaStore } from '@/stores/sistemaStore';

// «Tu negocio»: el negocio con que se está trabajando, que es un cuaderno. Muestra su nombre, el rubro del perfil de la
// cuenta y sus métricas. Sus tablas se abren desde «Mi negocio», en la barra. Los demás
// negocios se eligen en la barra y se agregan desde «Agregar un negocio».
// Sin ningún negocio no aparece: la bienvenida ya invita a crear el primero.
export function TuNegocio() {
  const negocio = useSistemaStore(seleccionarNegocioActivo);
  const rubro = useRubroDelPerfil();
  if (!negocio) return null;

  const bloques = allBlocks(negocio);
  const detalle = detalleDelNegocio(rubro, bloques.length);

  return (
    <section className="inicio-bloque tu-negocio" aria-labelledby="tu-negocio-titulo">
      <div className="inicio-titulo">
        <span className="tu-negocio-etiqueta">
          <Icon name="home" /> Tu negocio
        </span>
        <div className="tu-negocio-nombre">
          <h2 id="tu-negocio-titulo">{negocio.name}</h2>
          <p>{detalle}</p>
        </div>
      </div>

      <div className="tu-negocio-cuerpo">
        <MetricasDelInicio sistema={negocio} />
      </div>
    </section>
  );
}
