import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { FichasParaEmpezar } from '@/components/sistema/FichasParaEmpezar';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { varsDeColor } from '@/components/sistema/colores';
import { CATALOG, TEMPLATES } from '@/lib/sistema/catalogo';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { ClavePlantilla, Color } from '@/lib/sistema/tipos';
import { useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useAccionesDelTutorial } from '@/hooks/tutorial/useAccionesDelTutorial';
import { useSistemaStore } from '@/stores/sistemaStore';

const CLAVES = Object.keys(TEMPLATES) as ClavePlantilla[];

// Inicio: un saludo, las tres maneras de empezar como fichas de color, las otras herramientas y las
// plantillas (los cuadernos guardados tienen su propia pantalla, «Mis cuadernos»). Fondo blanco; el color va lleno en cada ficha, con íconos blancos.
export function InicioPage() {
  const nombre = useNombre();
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const tutorial = useAccionesDelTutorial();
  const navegacion = useNavegacion();
  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="home inicio reveal">
        <section className="inicio-portada">
          <div className="inicio-saludo">
            <p className="inicio-hola">Hola, {nombre}.</p>
            <h1>¿En qué puedo ayudarte hoy?</h1>
            <p className="inicio-bajada">
              Tocá una ficha para arrancar. Lo que armes, después lo podés cambiar.
            </p>
            <button type="button" className="inicio-guia" onClick={tutorial.empezar}>
              <span className="inicio-guia-icono">
                <Icon name="help" />
              </span>
              ¿Primera vez? Mostrarme cómo empezar
            </button>
          </div>

          <FichasParaEmpezar guia />
        </section>

        <section className="inicio-atajos" aria-label="Más para tu negocio">
          <Atajo
            color="coral"
            icono="camera"
            titulo="Cargar desde una foto"
            texto="Sacale una foto a tu cuaderno o subí una captura. Siempre revisás antes de guardar."
            onClick={() =>
              abrirModal({
                tipo: 'importar',
                sistemaId: null,
                bloqueId: null,
                contexto: 'registros',
              })
            }
          />
          <Atajo
            color="petrol"
            icono="chart"
            titulo="Ver mi zona"
            texto="Mirá cuánto se cobra cerca y qué se vende. Nunca se sabe quién es quién."
            onClick={() => void navegacion.irAZona()}
          />
        </section>

        <section className="inicio-bloque">
          <Titulo
            titulo="Plantillas para arrancar"
            detalle="Cada una trae varias piezas que se entienden entre sí. Después sumás las tuyas."
          />
          <div className="inicio-plantillas">
            {CLAVES.map((clave) => {
              const plantilla = TEMPLATES[clave];
              return (
                <article
                  key={clave}
                  className="inicio-plantilla"
                  data-template={clave}
                  data-color={plantilla.color}
                  style={varsDeColor(plantilla.color)}
                >
                  <span className="inicio-plantilla-icono">
                    <Icon name={plantilla.icon} />
                  </span>
                  <h3>{plantilla.name}</h3>
                  <p>{plantilla.subtitle}</p>
                  <ul className="inicio-plantilla-piezas">
                    {plantilla.types.map((tipo) => (
                      <li key={tipo}>
                        <Icon name={CATALOG[tipo].icon} />
                        {CATALOG[tipo].name}
                      </li>
                    ))}
                  </ul>
                  <button
                    type="button"
                    className="inicio-plantilla-ver"
                    data-accion="preview-template"
                    onClick={() => abrirModal({ tipo: 'plantilla', clave })}
                  >
                    Ver el cuaderno <Icon name="arrow" />
                  </button>
                </article>
              );
            })}
          </div>
        </section>

        {/* <section className="inicio-bloque inicio-ejemplo">
          <Titulo
            titulo="Así se ve un cuaderno"
            detalle="Un ejemplo para mirar. Probá cambiar cómo se reparten las piezas."
          />
          <div className="hero-system">
            <div className="hero-system-top">
              <span className="live-dot" />
              <strong>Un cuaderno. Tu forma de organizarlo.</strong>
              <span className="tag outline">Ejemplo</span>
            </div>
            <PreviewBoard sistema={ejemplo} compacto />
            <div className="hero-layout">
              <span>Probá otra distribución</span>
              <Btn
                cls={`small ${distribucion === 'mosaic' ? 'soft' : 'ghost'}`}
                onClick={() => setDistribucion('mosaic')}
              >
                <Icon name="grid" /> Mosaico
              </Btn>
              <Btn
                cls={`small ${distribucion === 'single' ? 'soft' : 'ghost'}`}
                onClick={() => setDistribucion('single')}
              >
                <Icon name="form" /> Una columna
              </Btn>
            </div>
            <div className="hero-sticker">
              <Icon name="spark" />
              Todo se puede mover.
            </div>
          </div>
        </section> */}
        <PageFooter />
      </main>
    </>
  );
}

// Título de sección.
function Titulo({ titulo, detalle }: { titulo: string; detalle?: string }) {
  return (
    <div className="inicio-titulo">
      <h2>{titulo}</h2>
      {detalle && <p>{detalle}</p>}
    </div>
  );
}

type PropsDeAccion = {
  color: Color;
  icono: NombreDeIcono;
  titulo: string;
  texto: string;
  onClick: () => void;
};

// Otra herramienta: un botón redondo de color con el ícono en blanco y su explicación al lado.
function Atajo({ color, icono, titulo, texto, onClick }: PropsDeAccion) {
  return (
    <button
      type="button"
      className="inicio-atajo"
      style={varsDeColor(color)}
      data-color={color}
      onClick={onClick}
    >
      <span className="inicio-atajo-icono">
        <Icon name={icono} />
      </span>
      <span className="inicio-atajo-texto">
        <strong>{titulo}</strong>
        <span>{texto}</span>
      </span>
    </button>
  );
}
