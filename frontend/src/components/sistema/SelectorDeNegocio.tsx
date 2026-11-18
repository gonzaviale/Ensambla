import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react';
import { useRouter } from '@tanstack/react-router';
import { Icon } from './Icon';
import { InicialDelNegocio } from '@/components/negocios/InicialDelNegocio';
import { useDetalleDeNegocios } from '@/hooks/negocio/useDetalleDeNegocios';
import { useActivarNegocio } from '@/hooks/negocios/mutations';
import { useNegociosDeLaCuenta } from '@/hooks/negocios/queries';
import { leerErrorServidor } from '@/hooks/errores';
import { useSistemaStore } from '@/stores/sistemaStore';

export function SelectorDeNegocio() {
  const { data } = useNegociosDeLaCuenta();
  const activar = useActivarNegocio();
  const router = useRouter();
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const detalleDe = useDetalleDeNegocios();
  const [abierto, setAbierto] = useState(false);
  const raiz = useRef<HTMLDivElement>(null);
  const disparador = useRef<HTMLButtonElement>(null);
  const id = useId();
  const activo = data.negocios.find((negocio) => negocio.id === data.activoId) ?? data.negocios[0];

  useEffect(() => {
    if (!abierto) return;
    const alTocar = (evento: PointerEvent) => {
      if (!raiz.current?.contains(evento.target as Node)) setAbierto(false);
    };
    const alTeclear = (evento: globalThis.KeyboardEvent) => {
      if (evento.key !== 'Escape') return;
      setAbierto(false);
      disparador.current?.focus();
    };
    document.addEventListener('pointerdown', alTocar);
    document.addEventListener('keydown', alTeclear);
    return () => {
      document.removeEventListener('pointerdown', alTocar);
      document.removeEventListener('keydown', alTeclear);
    };
  }, [abierto]);

  if (!activo) return null;
  const variosNegocios = data.negocios.length > 1;

  const compararNegocios = () => {
    setAbierto(false);
    void router.navigate({ to: '/negocios/comparar' });
  };

  const elegir = (idNegocio: string) => {
    setAbierto(false);
    if (idNegocio !== data.activoId) activar.mutate(idNegocio);
  };

  const mover = (evento: KeyboardEvent<HTMLDivElement>) => {
    if (evento.key === 'Tab') {
      setAbierto(false);
      return;
    }
    const sentido = { ArrowDown: 1, ArrowUp: -1 }[evento.key];
    if (!sentido) return;
    evento.preventDefault();
    const opciones = [
      ...evento.currentTarget.querySelectorAll<HTMLButtonElement>('[role="menuitem"]'),
    ].filter((boton) => !boton.disabled);
    const actual = opciones.indexOf(document.activeElement as HTMLButtonElement);
    opciones.at((actual + sentido + opciones.length) % opciones.length)?.focus();
  };

  const error = leerErrorServidor(activar.error);

  return (
    <div className="negocio-selector" ref={raiz}>
      <button
        ref={disparador}
        type="button"
        className="negocio-selector-disparador"
        aria-label={`Negocio activo: ${activo.nombre}`}
        aria-haspopup="menu"
        aria-expanded={abierto}
        aria-controls={abierto ? id : undefined}
        disabled={activar.isPending}
        onClick={() => setAbierto((actual) => !actual)}
      >
        <InicialDelNegocio
          className="negocio-selector-inicial"
          nombre={activo.nombre}
          posicion={data.negocios.indexOf(activo)}
        />
        <span className="negocio-selector-texto">
          <strong>{activar.isPending ? 'Cambiando…' : activo.nombre}</strong>
        </span>
        <Icon name="down" />
      </button>
      {abierto && (
        <div id={id} className="negocio-selector-menu" role="menu" onKeyDown={mover}>
          <div className="negocio-selector-cabeza">
            <strong>Mis negocios</strong>
            <span>
              {data.limite === null
                ? data.negocios.length
                : `${data.negocios.length} de ${data.limite}`}
            </span>
          </div>
          {data.negocios.map((negocio, posicion) => (
            <button
              key={negocio.id}
              type="button"
              role="menuitem"
              className={
                negocio.activo ? 'negocio-selector-opcion activa' : 'negocio-selector-opcion'
              }
              aria-current={negocio.activo ? 'true' : undefined}
              onClick={() => elegir(negocio.id)}
            >
              <InicialDelNegocio
                className="negocio-selector-inicial"
                nombre={negocio.nombre}
                posicion={posicion}
              />
              <span>
                <strong>{negocio.nombre}</strong>
                <small>{detalleDe(negocio.id)}</small>
              </span>
              {negocio.activo && <Icon name="check" />}
            </button>
          ))}
          {variosNegocios && (
            <button
              type="button"
              role="menuitem"
              className="negocio-selector-opcion negocio-selector-todos"
              onClick={compararNegocios}
            >
              <span className="negocio-selector-inicial">
                <Icon name="grid" />
              </span>
              <span>
                <strong>Todos mis negocios</strong>
                <small>Comparar y ver los totales</small>
              </span>
            </button>
          )}
          <button
            type="button"
            role="menuitem"
            className="negocio-selector-agregar"
            onClick={() => {
              setAbierto(false);
              abrirModal({ tipo: 'agregarNegocio' });
            }}
          >
            <Icon name="plus" /> Agregar un negocio
          </button>
          {error && (
            <p className="negocio-selector-error" role="alert">
              {error.mensaje}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
