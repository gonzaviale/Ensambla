import { MapPinIcon } from '@phosphor-icons/react';
import { Encabezado, Explicacion, Metrica } from './piezas';

// El cruce de una decisión de precio con las estadísticas agrupadas de Mi zona. Es una pestaña en prueba:
// todavía se está analizando si queda acá, en Mi zona o si se borra.
export function CruceConZona({ onPlan }: { onPlan: () => void }) {
  return (
    <section>
      <Encabezado
        etiqueta="Objetivos + Mi zona"
        titulo="Compará una decisión con tu contexto"
        texto="El precio de la zona no decide por vos: funciona como una referencia agregada para evaluar si una alternativa es razonable."
      />
      <div className="objetivos-zona-layout">
        <article className="objetivos-producto-zona">
          <div className="objetivos-producto-titulo">
            <div>
              <p className="objetivos-etiqueta">Producto involucrado</p>
              <h2>Aceite de girasol 1,5 L</h2>
            </div>
            <MapPinIcon aria-hidden size={28} />
          </div>
          <div className="objetivos-precios-zona">
            <Metrica etiqueta="Tu precio actual" valor="$3.500" />
            <Metrica etiqueta="Promedio de la zona" valor="$3.820" />
            <Metrica etiqueta="Precio para margen de 25%" valor="$3.873" />
          </div>
          <div className="objetivos-rango">
            <span style={{ left: '16%' }}>$3.420 mínimo</span>
            <i />
            <b style={{ left: '42%' }}>Tu precio</b>
            <em style={{ left: '67%' }}>Promedio</em>
            <span style={{ right: '0' }}>$4.120 máximo</span>
          </div>
          <p className="objetivos-privacidad">
            Datos ficticios y agrupados de 8 comercios para esta demostración. No se muestran
            nombres ni direcciones.
          </p>
        </article>
        <aside className="objetivos-alternativas-zona">
          <h2>Tres alternativas</h2>
          <AlternativaZona nombre="Mantener" precio="$3.500" margen="17%" aporte="$47.600" />
          <AlternativaZona
            nombre="Acercarse al promedio"
            precio="$3.820"
            margen="24%"
            aporte="$66.240"
            recomendada
          />
          <AlternativaZona
            nombre="Alcanzar margen objetivo"
            precio="$3.873"
            margen="25%"
            aporte="$69.328"
          />
          <button type="button" className="btn primary" onClick={onPlan}>
            Usar en mi plan
          </button>
        </aside>
      </div>
      <Explicacion>
        El promedio de la zona es una referencia, no una orden. Si llevás el precio a $3.820 y las
        unidades bajan menos de 8 %, el aporte seguiría mejorando. Podés cambiar esa caída supuesta.
      </Explicacion>
    </section>
  );
}

function AlternativaZona({
  nombre,
  precio,
  margen,
  aporte,
  recomendada,
}: {
  nombre: string;
  precio: string;
  margen: string;
  aporte: string;
  recomendada?: boolean;
}) {
  return (
    <div className={`objetivos-alternativa-zona ${recomendada ? 'recomendada' : ''}`.trim()}>
      <div>
        <strong>{nombre}</strong>
        {recomendada && <span>Equilibrada</span>}
      </div>
      <dl>
        <div>
          <dt>Precio</dt>
          <dd>{precio}</dd>
        </div>
        <div>
          <dt>Margen</dt>
          <dd>{margen}</dd>
        </div>
        <div>
          <dt>Aporte estimado</dt>
          <dd>{aporte}</dd>
        </div>
      </dl>
    </div>
  );
}
