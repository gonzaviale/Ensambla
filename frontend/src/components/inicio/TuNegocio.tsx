import { Icon } from '@/components/sistema/Icon';
import { PanelDeTuNegocio } from '@/components/sistema/metricas/PanelDeTuNegocio';
import { seleccionarNegocioActivo, useSistemaStore } from '@/stores/sistemaStore';

// «Tu negocio»: el negocio con que se está trabajando, que es un cuaderno. Muestra sus ventas y las métricas que la
// persona eligió, en solapas; sus tablas se abren desde «Mi negocio», en la barra. Los demás negocios se eligen en la barra y se agregan desde «Agregar un negocio».
// Sin ningún negocio no aparece: la bienvenida ya invita a crear el primero.
export function TuNegocio() {
  const negocio = useSistemaStore(seleccionarNegocioActivo);
  if (!negocio) return null;

  return (
    <section className="inicio-bloque tu-negocio" aria-labelledby="tu-negocio-titulo">
      <div className="inicio-titulo">
        <span id="tu-negocio-titulo" className="tu-negocio-etiqueta">
          <Icon name="home" /> Tu negocio
        </span>
      </div>

      <div className="tu-negocio-cuerpo">
        <PanelDeTuNegocio sistema={negocio} />
      </div>
    </section>
  );
}
