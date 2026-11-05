import type { ReactNode } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { useNegocio, type Negocio } from '@/hooks/gestion/useNegocio';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { PestanasDeNegocio, type PantallaDeNegocio } from './piezas';

const TITULOS: Record<PantallaDeNegocio, string> = {
  cierre: 'Cierre del día',
  compras: 'Compras',
  proyeccion: '¿Me conviene?',
  pedidos: 'Pedidos',
  ofertas: 'Ofertas',
  monotributo: 'Monotributo',
};

/**
 * La cáscara de las pantallas de «Mi negocio»: cabecera, título, pestañas y, si la persona todavía no tiene un
 * cuaderno con inventario y ventas, la explicación de qué le falta. El contenido recibe los números ya calculados.
 */
export function MarcoDeNegocio({
  pantalla,
  titulo,
  bajada,
  children,
}: {
  pantalla: PantallaDeNegocio;
  titulo: ReactNode;
  bajada: string;
  children: (negocio: Negocio) => ReactNode;
}) {
  useTituloPagina(TITULOS[pantalla]);
  const negocio = useNegocio();
  const navegacion = useNavegacion();
  const destinos: Record<PantallaDeNegocio, () => Promise<void>> = {
    cierre: navegacion.irANegocio,
    compras: navegacion.irACompras,
    proyeccion: navegacion.irAProyeccion,
    pedidos: navegacion.irAPedidos,
    ofertas: navegacion.irAOfertas,
    monotributo: navegacion.irAMonotributo,
  };
  const ir = (destino: PantallaDeNegocio) => void destinos[destino]();

  return (
    <>
      <CabeceraComun active="negocio" />
      <main id="main" tabIndex={-1} className="page-wrap gn-pagina reveal">
        <div className="gn-cabeza">
          <div className="page-title">
            <div className="eyebrow">
              MI NEGOCIO{negocio ? ` · ${negocio.sistema.name.toUpperCase()}` : ''}
            </div>
            <h1>{titulo}</h1>
            <p>{bajada}</p>
          </div>
          <PestanasDeNegocio activa={pantalla} onElegir={ir} />
        </div>
        {negocio ? (
          children(negocio)
        ) : (
          <div className="empty gn-vacio">
            <Icon name="box" />
            <p>
              Para ver tus números necesitás un cuaderno con <strong>inventario</strong> (con costo
              y precio) y <strong>ventas</strong>. Sacale una foto a tu cuaderno y Sami lo arma.
            </p>
            <Btn cls="primary" onClick={() => void navegacion.irATaller()}>
              Armarlo con Sami <Icon name="arrow" />
            </Btn>
          </div>
        )}
        <span className="gn-aviso-mockup">
          <Icon name="bulb" /> Mockup: lo que no sale de tu cuaderno (gastos fijos, proveedores,
          mensajes, categorías) es de ejemplo.
        </span>
        <PageFooter />
      </main>
    </>
  );
}
