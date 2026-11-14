import { useSonidosEscaneo } from '@/hooks/escaneo/useSonidosEscaneo';

export function ControlSonidosEscaneo() {
  const sonido = useSonidosEscaneo();
  return (
    <div className="escaneo-sonidos" role="group" aria-label="Sonidos del lector">
      <div className="escaneo-sonidos-botones">
        <button
          type="button"
          className="btn secondary small"
          aria-pressed={sonido.habilitado}
          aria-label={
            sonido.habilitado ? 'Silenciar sonidos del lector' : 'Activar sonidos del lector'
          }
          onClick={sonido.alternar}
        >
          <span aria-hidden="true">♪</span>Sonido {sonido.habilitado ? 'activado' : 'desactivado'}
        </button>
        <button
          type="button"
          className="btn secondary small"
          disabled={!sonido.habilitado}
          onClick={sonido.probar}
        >
          Probar sonidos
        </button>
      </div>
      <p className="escaneo-ayuda">
        Tono ascendente: artículo registrado. Aviso grave: fuera del inventario.
      </p>
    </div>
  );
}
