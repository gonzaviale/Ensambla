import { CarruselDeRentabilidad } from '@/components/inicio/CarruselDeRentabilidad';
import { PanelDelNegocio } from '@/components/inicio/PanelDelNegocio';
import { TuNegocio } from '@/components/inicio/TuNegocio';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { FichasParaEmpezar } from '@/components/sistema/FichasParaEmpezar';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { varsDeColor } from '@/components/sistema/colores';
import { sistemaDelPanel, tieneVentas } from '@/lib/sistema/panelDelNegocio';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Color, Sistema } from '@/lib/sistema/tipos';
import { usePanelDelNegocio } from '@/hooks/panel/queries';
import { useSistemaElegido } from '@/hooks/reportes/useSistemaElegido';
import { useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useAccionesDelTutorial } from '@/hooks/tutorial/useAccionesDelTutorial';
import { seleccionarSistemasDelNegocio, useSistemaStore } from '@/stores/sistemaStore';

// Inicio: siempre el mismo formato, tenga o no datos: una bienvenida corta con las maneras de empezar, «Tu
// negocio» (su nombre, sus métricas y el acceso a su cuaderno), las tarjetas de rentabilidad cuando
// hay dos semanas de ventas para comparar y, si hay ventas, el flujo de caja. Las plantillas tienen su pantalla.
// Fondo blanco; el color va lleno en cada ficha, con íconos blancos.
export function InicioPage() {
  const sistema = useSistemaDelInicio();
  const hayPanel = useHayPanel(sistema);
  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="home inicio reveal">
        {sistema ? <Bienvenida /> : <BienvenidaSinNegocio />}
        <TuNegocio />
        {!sistema && <Atajos />}
        {sistema && <CarruselDeRentabilidad sistemaId={sistema.id} />}
        {hayPanel && <PanelDelNegocio />}

        {/* <section className="inicio-bloque inicio-ejemplo">
          <Titulo
            titulo="Así se ve una sección"
            detalle="Un ejemplo para mirar. Probá cambiar cómo se reparten las piezas."
          />
          <div className="hero-system">
            <div className="hero-system-top">
              <span className="live-dot" />
              <strong>Una sección. Tu forma de organizarlo.</strong>
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

// La sección que mira el inicio: la última elegida o, si no hay, la que tiene el panel.
function useSistemaDelInicio() {
  const sistemas = useSistemaStore(seleccionarSistemasDelNegocio);
  const { sistema: elegido } = useSistemaElegido();
  return elegido ?? sistemaDelPanel(sistemas);
}

// Sin ventas en el período no hay flujo de caja para mostrar. Mientras llegan los números se supone que hay (si la
// sección tiene ventas), así el panel no parpadea.
function useHayPanel(sistema: Sistema | null) {
  const panel = usePanelDelNegocio(sistema?.id ?? null);
  if (!sistema) return false;
  return panel.data ? panel.data.hayDatos : !panel.isPending || tieneVentas(sistema);
}

// Sin ningún negocio con secciones (una cuenta nueva): la bienvenida grande, con las tres maneras de empezar explicadas
// y los atajos debajo. Cuando ya hay datos, la bienvenida es la corta de arriba.
function BienvenidaSinNegocio() {
  const nombre = useNombre();
  const tutorial = useAccionesDelTutorial();
  return (
    <section className="inicio-portada" aria-label="Empezar algo nuevo">
      <div className="inicio-saludo">
        <p className="inicio-hola">Hola, {nombre}.</p>
        <h1>¿En qué puedo ayudarte hoy?</h1>
        <p className="inicio-bajada">
          Elegí una opción para empezar. Lo que armes se puede cambiar después.
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
  );
}

function Bienvenida() {
  const nombre = useNombre();
  const tutorial = useAccionesDelTutorial();
  return (
    <section className="inicio-bienvenida" aria-label="Empezar algo nuevo">
      <div className="inicio-bienvenida-texto">
        <p className="inicio-hola">Hola, {nombre}.</p>
        <h1>¿En qué puedo ayudarte hoy?</h1>
        <button type="button" className="inicio-guia-chica" onClick={tutorial.empezar}>
          ¿Primera vez? Mostrarme cómo empezar
        </button>
      </div>
      <div className="inicio-bienvenida-acciones">
        <FichasParaEmpezar guia compacto />
        <Atajos compactos />
      </div>
    </section>
  );
}

// «Cargar desde una foto» y «Ver mi zona»: dos botones chicos en la bienvenida, debajo de las formas de empezar.
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
        texto="Compará tus precios con los de la zona y qué se vende. Los datos son anónimos."
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
