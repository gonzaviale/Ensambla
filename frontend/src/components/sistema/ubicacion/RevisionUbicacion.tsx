import type { ConfiguracionUbicacion } from '@/types/ubicacion';

export function RevisionUbicacion({ configuracion }: { configuracion: ConfiguracionUbicacion }) {
  return (
    <section className="ubicacion-revision" role="status" aria-labelledby="titulo-revision">
      <h2 id="titulo-revision">Configuración revisada</h2>
      <dl>
        <div>
          <dt>Dirección de referencia</dt>
          <dd>{configuracion.direccion}</dd>
        </div>
        <div>
          <dt>Punto {configuracion.ajustado ? 'ajustado manualmente' : 'encontrado'}</dt>
          <dd>
            {configuracion.punto.latitud}, {configuracion.punto.longitud}
          </dd>
        </div>
        <div>
          <dt>Rubro</dt>
          <dd>{configuracion.rubro.nombre}</dd>
        </div>
      </dl>
      <p>
        Estos datos todavía no se guardaron. Confirmá con Guardar configuración para actualizar tu
        comercio.
      </p>
    </section>
  );
}
