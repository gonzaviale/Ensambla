import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import type { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import type { useSamiProyecto } from '@/hooks/sistemas/useSamiProyecto';
import { detalleDeColumna, tipoEnPalabras } from './columnas';

type Sami = ReturnType<typeof useSamiProyecto>;
type Navegacion = ReturnType<typeof useNavegacion>;

function ProyectoCreado({ sami, navegacion }: { sami: Sami; navegacion: Navegacion }) {
  const { creado } = sami;
  if (!creado) return null;
  return (
    <div className="sp-hecho">
      <span className="eyebrow">LISTO</span>
      <h2>Tu proyecto «{creado.nombre}» ya está creado.</h2>
      <ul className="sp-columnas">
        {creado.bloques.map((bloque) => (
          <li key={bloque.nombre} className="sp-columna">
            <span>{bloque.nombre}</span>
            <small>{bloque.campos.length} columnas</small>
          </li>
        ))}
      </ul>
      <div className="sp-acciones">
        <Btn cls="primary" onClick={() => void navegacion.irAInicio()}>
          Volver al inicio <Icon name="arrow" />
        </Btn>
        <Btn cls="ghost" onClick={sami.reiniciar}>
          Armar otro proyecto
        </Btn>
      </div>
    </div>
  );
}

function PropuestaDelProyecto({ sami }: { sami: Sami }) {
  const { propuesta, plantilla } = sami;
  if (!propuesta) return null;
  return (
    <>
      <div className="draft-heading">
        <div>
          <span className="eyebrow">TU PROPUESTA, A LA VISTA</span>
          <input
            className="draft-name-input"
            aria-label="Nombre de tu proyecto"
            maxLength={100}
            value={sami.nombre}
            onChange={(evento) => sami.setNombre(evento.target.value)}
          />
        </div>
        <span className="tag">
          {propuesta.origen === 'IA' ? 'Armada con ayuda de IA' : 'Armada por Sami'}
        </span>
      </div>
      <p className="draft-intro">
        Partimos de la plantilla «{propuesta.plantilla}». Sacá las columnas que no necesites.
      </p>
      <div className="sp-bloques">
        {(plantilla?.estructura.bloques ?? []).map((bloque, indice) => (
          <section key={bloque.nombre} className="sp-bloque">
            <h3>{bloque.nombre}</h3>
            <ul className="sp-columnas">
              {bloque.campos.map((campo) => (
                <li key={campo.nombre} className="sp-columna">
                  <span>{campo.nombre}</span>
                  <small>{tipoEnPalabras(campo.tipo)}</small>
                </li>
              ))}
              {indice === 0 &&
                propuesta.columnas.map((columna) => {
                  const quitada = sami.quitadas.includes(columna.nombre);
                  return (
                    <li
                      key={columna.nombre}
                      className={`sp-columna extra${quitada ? 'quitada' : ''}`}
                    >
                      <span>{columna.nombre}</span>
                      <small>{quitada ? 'sin esta' : detalleDeColumna(columna)}</small>
                      <button type="button" onClick={() => sami.alternarColumna(columna.nombre)}>
                        {quitada ? 'Volver a sumar' : 'Quitar'}
                        <span className="sr-only"> la columna {columna.nombre}</span>
                      </button>
                    </li>
                  );
                })}
            </ul>
          </section>
        ))}
        {!plantilla && (
          <section className="sp-bloque">
            <h3>{propuesta.plantilla}</h3>
            <ul className="sp-columnas">
              {propuesta.columnas.map((columna) => (
                <li key={columna.nombre} className="sp-columna extra">
                  <span>{columna.nombre}</span>
                  <small>{detalleDeColumna(columna)}</small>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>
      <div className="draft-footer">
        <div>
          <Icon name="check-circle" />
          <span>
            <strong>¿Se parece a tu manera de trabajar?</strong>
            <small>Podés seguir ajustándolo después de crearlo.</small>
          </span>
        </div>
        <Btn
          cls="primary"
          disabled={sami.creando || !sami.nombre.trim()}
          onClick={() => void sami.crearProyecto()}
        >
          Sí, crear mi proyecto <Icon name="arrow" />
        </Btn>
      </div>
    </>
  );
}

function LienzoVacio() {
  return (
    <div className="studio-empty">
      <div className="empty-studio-label">TU PROPUESTA VA A APARECER ACÁ</div>
      <h2>Primero te escucho.</h2>
      <p>
        Si hace falta te hago alguna pregunta. Después ves la estructura armada y decidís si la
        creamos.
      </p>
      <span className="empty-studio-note">
        <Icon name="spark" />
        Podés escribir o dictar lo que hacés.
      </span>
    </div>
  );
}

// El lienzo de la derecha según en qué va la conversación: proyecto creado, propuesta a revisar o todavía nada.
export function LienzoDelProyecto({ sami, navegacion }: { sami: Sami; navegacion: Navegacion }) {
  if (sami.creado) return <ProyectoCreado sami={sami} navegacion={navegacion} />;
  if (sami.propuesta) return <PropuestaDelProyecto sami={sami} />;
  return <LienzoVacio />;
}
