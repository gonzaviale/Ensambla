import { PanelDelNegocio } from '@/components/inicio/PanelDelNegocio';
import { TuNegocio } from '@/components/inicio/TuNegocio';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { FichasParaEmpezar } from '@/components/sistema/FichasParaEmpezar';
import { MetricasDelInicio } from '@/components/sistema/MetricasDelInicio';
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
import { useNegociosDeLaCuenta } from '@/hooks/negocios/queries';

// Inicio: siempre el mismo formato, tenga o no datos: una bienvenida corta con las maneras de empezar, los negocios
// de la cuenta, «Tu negocio» (su nombre y sus secciones como accesos), sus métricas (ventas y, con dos semanas de
// ventas para comparar, las tarjetas de rentabilidad) y, si hay ventas, el flujo de caja. Las plantillas tienen su
// pantalla y su ficha.
// Fondo blanco; el color va lleno en cada ficha, con íconos blancos.
export function InicioPage() {
  const sistema = useSistemaDelInicio();
  const hayPanel = useHayPanel(sistema);
  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="home inicio reveal">
        <Bienvenida />
        <AgregarNegocio />
        <TuNegocio />
        <MetricasDelInicio />
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
        <Atajos />
      </div>
    </section>
  );
}

function AgregarNegocio() {
  const { data } = useNegociosDeLaCuenta();
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  return (
    <div className="inicio-agregar-negocio">
      <span>
        <strong>
          {data.negocios.length === 1
            ? 'Tu negocio está activo'
            : `${data.negocios.length} negocios en tu cuenta`}
        </strong>
        <small>La información de cada uno se mantiene separada.</small>
      </span>
      <button
        type="button"
        className="btn secondary"
        onClick={() => abrirModal({ tipo: 'agregarNegocio' })}
      >
        <Icon name="plus" /> Agregar un negocio
      </button>
    </div>
  );
}

// «Cargar desde una foto» y «Ver mi zona»: dos botones chicos en la bienvenida, debajo de las formas de empezar.
function Atajos() {
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const navegacion = useNavegacion();
  return (
    <section className="inicio-atajos compactos" aria-label="Más para tu negocio">
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
