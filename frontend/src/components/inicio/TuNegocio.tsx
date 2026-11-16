import { Icon } from '@/components/sistema/Icon';
import { MetricasDelInicio } from '@/components/sistema/MetricasDelInicio';
import { varsDeColor } from '@/components/sistema/colores';
import { useRubroDelPerfil } from '@/hooks/negocio/useDetalleDeNegocios';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { detalleDelNegocio, textoDeTablas } from '@/lib/negocios/presentacion';
import { CATALOG } from '@/lib/sistema/catalogo';
import { allBlocks } from '@/lib/sistema/core';
import { seleccionarNegocioActivo, useSistemaStore } from '@/stores/sistemaStore';

// «Tu negocio»: el negocio con que se está trabajando, que es un cuaderno. Muestra su nombre, el rubro del perfil de la
// cuenta y, en dos columnas, sus métricas y el acceso para abrirlo (en el celular, primero el acceso). Los demás
// negocios se eligen en la barra y se agregan desde «Agregar un negocio».
// Sin ningún negocio no aparece: la bienvenida ya invita a crear el primero.
export function TuNegocio() {
  const negocio = useSistemaStore(seleccionarNegocioActivo);
  const navegacion = useNavegacion();
  const rubro = useRubroDelPerfil();
  if (!negocio) return null;

  const bloques = allBlocks(negocio);
  const primera = bloques[0];
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
        <div className="sistemas-grilla">
          <button
            type="button"
            className="inicio-sistema"
            style={varsDeColor('coral')}
            data-color="coral"
            onClick={() => void navegacion.abrirSistema(negocio.id)}
          >
            <span className="inicio-sistema-lomo" aria-hidden="true" />
            <span className="inicio-sistema-tapa">
              <Icon name={primera ? CATALOG[primera.type].icon : 'note'} />
              <strong>Abrir {negocio.name}</strong>
              <span>{textoDeTablas(bloques.length)}</span>
            </span>
          </button>
        </div>
      </div>
    </section>
  );
}
