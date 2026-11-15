import { DecisionesRapidas } from '@/components/inicio/DecisionesRapidas';
import { PanelDelNegocio } from '@/components/inicio/PanelDelNegocio';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { FichasParaEmpezar } from '@/components/sistema/FichasParaEmpezar';
import { Icon } from '@/components/sistema/Icon';
import { MetricasDelInicio } from '@/components/sistema/MetricasDelInicio';
import { SelectorDeCuaderno } from '@/components/sistema/metricas/Selectores';
import { PageFooter } from '@/components/sistema/PageFooter';
import { varsDeColor } from '@/components/sistema/colores';
import { sistemaDelPanel } from '@/lib/sistema/panelDelNegocio';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Color } from '@/lib/sistema/tipos';
import { useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useCuadernoElegido } from '@/hooks/reportes/useCuadernoElegido';
import { useAccionesDelTutorial } from '@/hooks/tutorial/useAccionesDelTutorial';
import { useSistemaStore } from '@/stores/sistemaStore';

// Inicio: quien ya tiene un cuaderno ve un encabezado corto con el negocio que mira, las maneras de empezar (Sami
// primero) y, abajo, lo que conviene decidir hoy, cómo viene vendiendo y el flujo de caja; quien recién llega ve la
// bienvenida grande. Las
// plantillas tienen su pantalla y su ficha de arriba (los cuadernos guardados, la suya: «Mis cuadernos»).
// Fondo blanco; el color va lleno en cada ficha, con íconos blancos.
export function InicioPage() {
  const nombre = useNombre();
  const tutorial = useAccionesDelTutorial();
  // Quien ya tiene un cuaderno ve el panel de su negocio: la bienvenida pasa a una franja corta (el h1 es el mismo)
  // y las formas de empezar siguen a mano. Quien recién empieza ve la bienvenida grande de siempre.
  const hayPanel = useSistemaStore((estado) => sistemaDelPanel(estado.sistemas) !== null);
  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="home inicio reveal">
        {hayPanel && (
          <section className="inicio-bienvenida" aria-label="Empezar algo nuevo">
            <div className="inicio-encabezado">
              <div className="inicio-bienvenida-texto">
                <p className="inicio-hola">Hola, {nombre}.</p>
                <h1>¿En qué puedo ayudarte hoy?</h1>
                <button type="button" className="inicio-guia-chica" onClick={tutorial.empezar}>
                  ¿Primera vez? Mostrarme cómo empezar
                </button>
              </div>
              <NegocioElegido />
            </div>
            <div className="inicio-bienvenida-acciones">
              <FichasParaEmpezar guia compacto />
              <Atajos compactos />
            </div>
          </section>
        )}

        {/* El orden de la home (T-213): lo que hay que decidir hoy, después cómo viene el negocio y, al final, la
            consulta de la caja. */}
        {hayPanel && (
          <>
            <DecisionesRapidas />
            <MetricasDelInicio />
            <PanelDelNegocio />
          </>
        )}

        {!hayPanel && (
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
        )}

        {!hayPanel && <Atajos />}

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

// De qué negocio son las ventas y la caja de abajo: el mismo selector de antes, ahora en el encabezado.
function NegocioElegido() {
  const { cuadernos, cuaderno, elegir } = useCuadernoElegido();
  if (!cuaderno) return null;
  return (
    <p className="inicio-negocio">
      <span>Estás mirando</span>
      <SelectorDeCuaderno cuadernos={cuadernos} cuaderno={cuaderno} onCambio={elegir} />
    </p>
  );
}

// «Cargar desde una foto» y «Ver mi zona». Con la bienvenida grande son dos atajos con su explicación; en la franja
// compacta de quien ya tiene datos son dos botones chicos, debajo de las formas de empezar.
function Atajos({ compactos = false }: { compactos?: boolean }) {
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const navegacion = useNavegacion();
  return (
    <section
      className={compactos ? 'inicio-atajos compactos' : 'inicio-atajos'}
      aria-label="Más para tu negocio"
    >
      <Atajo
        color="petrol"
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
        color="blue"
        icono="chart"
        titulo="Ver mi zona"
        texto="Mirá cuánto se cobra cerca y qué se vende. Nunca se sabe quién es quién."
        onClick={() => void navegacion.irAZona()}
      />
    </section>
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
