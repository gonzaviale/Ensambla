import { useState } from 'react';
import { FichasParaEmpezar } from '@/components/sistema/FichasParaEmpezar';
import { Icon } from '@/components/sistema/Icon';
import { varsDeColor } from '@/components/sistema/colores';
import { useConfiguracionNegocio, useRubros } from '@/hooks/negocio/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { CATALOG } from '@/lib/sistema/catalogo';
import { allBlocks } from '@/lib/sistema/core';
import type { Color } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';

// Las secciones del negocio van rotando los cinco colores de la marca.
const COLORES: Color[] = ['coral', 'blue', 'orange', 'petrol', 'yellow'];

const secciones = (cantidad: number) => (cantidad === 1 ? '1 sección' : `${cantidad} secciones`);

// «Tu negocio»: el nombre y el rubro del negocio y, debajo, sus secciones como accesos directos. Desde acá se crea una
// sección nueva (con Sami, con una plantilla o desde cero). Sin secciones no aparece: la bienvenida ya invita a crear
// la primera.
export function TuNegocio() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const navegacion = useNavegacion();
  const configuracion = useConfiguracionNegocio();
  const rubros = useRubros();
  const [creando, setCreando] = useState(false);
  if (sistemas.length === 0) return null;

  const codigoDelRubro = configuracion.data?.rubro;
  const rubro = rubros.data?.rubros.find((item) => item.codigo === codigoDelRubro)?.nombre;
  const detalle = [rubro, secciones(sistemas.length)].filter(Boolean).join(' · ');

  return (
    <section className="inicio-bloque tu-negocio" aria-labelledby="tu-negocio-titulo">
      <div className="inicio-titulo">
        <span className="tu-negocio-etiqueta">
          <Icon name="home" /> Tu negocio
        </span>
        <h2 id="tu-negocio-titulo">{configuracion.data?.nombre ?? 'Tu negocio'}</h2>
        <p>{detalle}</p>
      </div>

      <ul className="sistemas-grilla" aria-label="Secciones de tu negocio">
        {sistemas.map((sistema, indice) => {
          const color = COLORES[indice % COLORES.length] ?? 'coral';
          const primera = allBlocks(sistema)[0];
          return (
            <li key={sistema.id} className="sistemas-item">
              <button
                type="button"
                className="inicio-sistema"
                style={varsDeColor(color)}
                data-color={color}
                onClick={() => void navegacion.abrirSistema(sistema.id)}
              >
                <span className="inicio-sistema-lomo" aria-hidden="true" />
                <span className="inicio-sistema-tapa">
                  <Icon name={primera ? CATALOG[primera.type].icon : 'note'} />
                  <strong>{sistema.name}</strong>
                  <span>
                    {allBlocks(sistema).length}{' '}
                    {allBlocks(sistema).length === 1 ? 'tabla' : 'tablas'}
                  </span>
                </span>
              </button>
            </li>
          );
        })}
      </ul>

      <div className="sistemas-nueva">
        <button
          type="button"
          className="btn soft small"
          aria-expanded={creando}
          aria-controls="crear-seccion"
          onClick={() => setCreando((abierto) => !abierto)}
        >
          <Icon name={creando ? 'close' : 'plus'} /> {creando ? 'Cerrar' : 'Crear una sección'}
        </button>
        {creando && (
          <div id="crear-seccion" className="sistemas-nueva-opciones">
            <p>Con Sami, con una plantilla o desde cero. Lo que armes, después lo podés cambiar.</p>
            <FichasParaEmpezar />
          </div>
        )}
      </div>
    </section>
  );
}
