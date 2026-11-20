import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { useSistemaStore } from '@/stores/sistemaStore';
import { useRentabilidad } from '@/hooks/reportes/useRentabilidad';
import { conSigno, type Diapositiva } from '@/lib/sistema/rentabilidad';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import { tonoDe, type Tono } from '@/lib/sistema/tonos';
import type { AvisoEconomico } from '@/types/avisoEconomico';

// Las señales de rentabilidad del negocio, arriba de todo en el Inicio y en poco espacio: una tarjeta corta para lo que
// cambió en las ventas, otra para la ganancia y otra para los productos que lo explican. Se desliza con las flechas
// o, en el celular, con el dedo. Sin señales no ocupa lugar. Sólo la de productos se toca (abre el detalle) y se ve
// distinta: llena de color y con flecha; las otras son datos y quedan planas.
export function CarruselDeRentabilidad({ sistemaId }: { sistemaId: string }) {
  const { diapositivas, aviso } = useRentabilidad(sistemaId);
  const pista = useRef<HTMLUListElement>(null);
  const [desborda, setDesborda] = useState(false);

  // Las flechas sólo aparecen cuando las tarjetas no entran todas en el ancho.
  useEffect(() => {
    const medir = () => {
      const nodo = pista.current;
      setDesborda(Boolean(nodo && nodo.scrollWidth > nodo.clientWidth + 2));
    };
    medir();
    window.addEventListener('resize', medir);
    return () => window.removeEventListener('resize', medir);
  }, [diapositivas.length]);

  if (!aviso || diapositivas.length === 0) return null;

  const mover = (sentido: number) =>
    pista.current?.scrollBy?.({
      left: sentido * pista.current.clientWidth * 0.8,
      behavior: 'smooth',
    });

  return (
    <section className="inicio-carrusel" aria-label="Avisos de rentabilidad">
      {desborda && (
        <button
          type="button"
          className="inicio-carrusel-flecha anterior"
          aria-label="Aviso anterior"
          onClick={() => mover(-1)}
        >
          <Icon name="back" />
        </button>
      )}
      <ul ref={pista} className="inicio-carrusel-pista">
        {diapositivas.map((diapositiva) => (
          <li key={diapositiva.clave}>
            <TarjetaCorta diapositiva={diapositiva} aviso={aviso} />
          </li>
        ))}
      </ul>
      {desborda && (
        <button
          type="button"
          className="inicio-carrusel-flecha siguiente"
          aria-label="Aviso siguiente"
          onClick={() => mover(1)}
        >
          <Icon name="arrow" />
        </button>
      )}
    </section>
  );
}

type DiapositivaDe<T extends Diapositiva['tipo']> = Extract<Diapositiva, { tipo: T }>;

const ICONO_POR_SIGNO: Record<string, NombreDeIcono> = { '1': 'sube', '0': 'igual', '-1': 'baja' };

const iconoDelCambio = (cambio: number) => ICONO_POR_SIGNO[String(Math.sign(cambio))] ?? 'igual';

const tonoDeLosProductos = (aviso: AvisoEconomico): Tono =>
  aviso.tendencia === 'MEJORA' ? tonoDe('ganancia') : 'coral';

function TarjetaCorta({ diapositiva, aviso }: { diapositiva: Diapositiva; aviso: AvisoEconomico }) {
  if (diapositiva.tipo === 'productos')
    return <TarjetaDeProductos diapositiva={diapositiva} tono={tonoDeLosProductos(aviso)} />;
  if (diapositiva.tipo === 'costos') return <TarjetaSinCostos />;
  return <TarjetaDeCambio diapositiva={diapositiva} />;
}

type PropsDeMarco = { tono: Tono; icono: NombreDeIcono; clickeable?: boolean; children: ReactNode };

function MarcoDeTarjeta({ tono, icono, clickeable, children }: PropsDeMarco) {
  return (
    <article
      className="rentabilidad-tarjeta"
      data-tono={tono}
      data-clickeable={clickeable || undefined}
      aria-label="Aviso económico"
    >
      <span className="rentabilidad-icono">
        <Icon name={icono} />
      </span>
      <div className="rentabilidad-cuerpo">{children}</div>
    </article>
  );
}

function TarjetaDeCambio({ diapositiva }: { diapositiva: DiapositivaDe<'ventas' | 'ganancia'> }) {
  return (
    <MarcoDeTarjeta tono={tonoDe(diapositiva.tipo)} icono={iconoDelCambio(diapositiva.cambio)}>
      <p className="rentabilidad-etiqueta">{ETIQUETA_DEL_CAMBIO[diapositiva.tipo]}</p>
      <p className="rentabilidad-valor">
        <strong>{conSigno(diapositiva.cambio)}</strong>
        <span>frente a la semana pasada</span>
      </p>
    </MarcoDeTarjeta>
  );
}

const ETIQUETA_DEL_CAMBIO = { ventas: 'Ventas', ganancia: 'Ganancia' } as const;

function TarjetaSinCostos() {
  return (
    <MarcoDeTarjeta tono={tonoDe('ganancia')} icono="wallet">
      <p className="rentabilidad-etiqueta">Ganancia</p>
      <p className="rentabilidad-valor">
        <strong>Sin costos</strong>
      </p>
      <p className="rentabilidad-detalle">Cargá los costos para verla.</p>
    </MarcoDeTarjeta>
  );
}

type PropsDeProductos = { diapositiva: DiapositivaDe<'productos'>; tono: Tono };

function TarjetaDeProductos({ diapositiva, tono }: PropsDeProductos) {
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const abrirDetalle = () =>
    abrirModal({
      tipo: 'productosDelAviso',
      cambios: diapositiva.cambios,
      categoria: diapositiva.categoria,
      titulo: diapositiva.titulo,
    });
  return (
    <MarcoDeTarjeta tono={tono} icono="box" clickeable>
      <button type="button" className="rentabilidad-abrir" onClick={abrirDetalle}>
        <span className="rentabilidad-titulo">{diapositiva.titulo}</span>
        <span className="rentabilidad-detalle">Tocá para ver cuáles</span>
        <Icon name="arrow" />
      </button>
    </MarcoDeTarjeta>
  );
}
