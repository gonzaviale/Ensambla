import { useEffect, useRef } from 'react';
import { ElegirNegocio, type EstadoDeLaEleccion } from '@/components/negocios/ElegirNegocio';
import { Btn } from '@/components/sistema/Btn';
import { useElegirNegocioAlEntrar } from '@/hooks/negocios/mutations';
import { useResumenConjunto } from '@/hooks/negocios/resumenConjunto';
import { paraElegir } from '@/lib/negocios/presentacion';
import type { ListaDeNegocios } from '@/types/negocios';
// El pop-up se abre afuera de la app (en /elegir-negocio): trae las hojas del sistema para verse como sus modales.
import '@/styles/sistema/index.css';

type Props = {
  lista: ListaDeNegocios;
  /** Se llama cuando ya está activo el negocio con que se va a trabajar: recién ahí se entra a la app. */
  onListo: () => void;
};

const ENFOCABLES = 'button:not(:disabled)';

function mantenerElFocoAdentro(evento: KeyboardEvent, dialogo: HTMLElement) {
  const nodos = [...dialogo.querySelectorAll<HTMLElement>(ENFOCABLES)];
  const inicio = nodos[0];
  const fin = nodos.at(-1);
  const actual = document.activeElement;
  const saleHaciaAtras = evento.shiftKey && (actual === inicio || actual === dialogo);
  const saleHaciaAdelante = !evento.shiftKey && actual === fin;
  if (saleHaciaAtras) volverAlFoco(evento, fin);
  if (saleHaciaAdelante) volverAlFoco(evento, inicio);
}

function volverAlFoco(evento: KeyboardEvent, destino: HTMLElement | undefined) {
  evento.preventDefault();
  destino?.focus();
}

// El pop-up de T-204: al iniciar sesión con más de un negocio se elige con cuál trabajar antes de entrar, así la app
// carga de entrada los datos de ese negocio. Elegir el que ya estaba activo o «Seguir con…» entran sin cambiar nada.
// Esc no hace nada: hay que elegir.
export function ElegirNegocioAlEntrar({ lista, onListo }: Props) {
  const resumen = useResumenConjunto(lista.negocios);
  const activar = useElegirNegocioAlEntrar();
  const dialogo = useRef<HTMLElement>(null);
  const ultimo = lista.negocios.find((negocio) => negocio.id === lista.activoId);
  const abriendo = activar.isPending
    ? lista.negocios.find((negocio) => negocio.id === activar.variables)
    : undefined;

  const estado: EstadoDeLaEleccion = resumen.isPending
    ? { tipo: 'cargando' }
    : resumen.isError
      ? { tipo: 'error', reintentar: () => void resumen.refetch() }
      : {
          tipo: 'listo',
          negocios: resumen.data.negocios.map(paraElegir),
          activoId: lista.activoId,
        };

  const elegir = (negocioId: string) => {
    if (activar.isPending) return;
    if (negocioId === lista.activoId) onListo();
    else activar.mutate(negocioId, { onSuccess: onListo });
  };

  // Como los modales de la app: el foco arranca adentro, Tab no se escapa y el fondo no se desplaza.
  useEffect(() => {
    document.body.style.overflow = 'hidden';
    dialogo.current?.focus();
    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key !== 'Tab' || !dialogo.current) return;
      mantenerElFocoAdentro(evento, dialogo.current);
    };
    document.addEventListener('keydown', alTeclear);
    return () => {
      document.removeEventListener('keydown', alTeclear);
      document.body.style.overflow = '';
    };
  }, []);

  return (
    <div className="mk-root">
      <div className="overlay-backdrop elegir-negocio-al-entrar">
        <section
          ref={dialogo}
          className="modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="elegir-negocio-titulo"
          aria-describedby="elegir-negocio-ayuda"
          aria-busy={activar.isPending}
          tabIndex={-1}
        >
          <span className="eyebrow">Tenés {lista.negocios.length} negocios</span>
          <h2 id="elegir-negocio-titulo">¿Con qué negocio trabajás hoy?</h2>
          <p id="elegir-negocio-ayuda">Seleccioná uno para entrar.</p>
          <ElegirNegocio estado={estado} onElegir={elegir} />
          {abriendo && (
            <p className="elegir-negocio-abriendo" role="status">
              Abriendo {abriendo.nombre}…
            </p>
          )}
          {activar.isError && (
            <p className="tablero-aviso" role="alert">
              No pudimos abrir ese negocio. Probá de nuevo o seguí con el último que usaste.
            </p>
          )}
          <div className="modal-footer">
            <Btn cls="secondary" onClick={onListo} disabled={activar.isPending}>
              {ultimo ? `Seguir con ${ultimo.nombre}` : 'Seguir con el último que usé'}
            </Btn>
          </div>
        </section>
      </div>
    </div>
  );
}
