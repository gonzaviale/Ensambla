import { ArrowLeftIcon } from '@phosphor-icons/react';
import { useNavigate } from '@tanstack/react-router';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { varsDeColor } from '@/components/sistema/colores';
import { useObjetivos } from '@/hooks/objetivos/queries';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { DATOS_DE_LECCION, LECCIONES_DE_SAMI, type LeccionDeSami } from '@/lib/ensenanza';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Color } from '@/lib/sistema/tipos';
import { Encabezado } from '@/pages/Objetivos/piezas';
import { LeccionEscenario } from './LeccionEscenario';
import {
  LeccionConsejos,
  LeccionMetricas,
  LeccionRecomendaciones,
  LeccionResultado,
} from './Lecciones';

type Props = { leccion: LeccionDeSami; objetivoId?: string };

// Cada lección es un tema con su color y su ícono, como las fichas del inicio: se elige cualquiera, sin orden.
const TEMA_DE_LECCION: Record<LeccionDeSami, { color: Color; icono: NombreDeIcono }> = {
  resultado: { color: 'petrol', icono: 'chart' },
  metricas: { color: 'blue', icono: 'grid' },
  recomendaciones: { color: 'orange', icono: 'spark' },
  escenario: { color: 'coral', icono: 'settings' },
  consejos: { color: 'yellow', icono: 'bulb' },
};

// Sami te enseña: donde Sami enseña contabilidad aplicada al negocio y explica cómo hace sus recomendaciones.
export function SamiEnsenaPage({ leccion, objetivoId }: Props) {
  useTituloPagina('Sami te enseña');
  const navigate = useNavigate();
  const navegacion = useNavegacion();
  const { objetivos } = useObjetivos();

  // Cambiar de lección no vuelve al principio de la página ni suma entradas al historial.
  const abrir = (siguiente: LeccionDeSami, objetivo = objetivoId) =>
    void navigate({
      to: '/sami-te-ensena',
      search: { leccion: siguiente, objetivo },
      replace: true,
      resetScroll: false,
    });
  const irAObjetivos = () => void navegacion.irAObjetivos();
  const datos = DATOS_DE_LECCION[leccion];
  // Si llegó desde el plan de un objetivo, tiene que poder volver a ese plan sin buscarlo.
  const objetivoDeOrigen = objetivos.find((objetivo) => objetivo.id === objetivoId);

  return (
    <>
      <CabeceraComun active="learn" />
      <main id="main" tabIndex={-1} className="page-wrap objetivos-page ensenanza-page reveal">
        {objetivoDeOrigen && (
          <button
            type="button"
            className="objetivos-volver"
            onClick={() => void navegacion.irAObjetivo(objetivoDeOrigen.id)}
          >
            <ArrowLeftIcon aria-hidden size={20} /> Volver al plan de acción
          </button>
        )}
        <Encabezado
          etiqueta="Lecciones"
          titulo="Sami te enseña"
          texto="Lecciones cortas de contabilidad aplicadas a tu negocio: de dónde sale cada métrica, cómo hace Sami sus recomendaciones y cómo probar escenarios con tus objetivos."
          sami="Te enseña sin tecnicismos"
        />
        <div className="ensenanza-layout">
          <nav className="ensenanza-temas" aria-label="Lecciones">
            <ul>
              {LECCIONES_DE_SAMI.map((clave) => (
                <li key={clave}>
                  <button
                    type="button"
                    className={clave === leccion ? 'active' : ''}
                    style={varsDeColor(TEMA_DE_LECCION[clave].color)}
                    aria-current={clave === leccion ? 'page' : undefined}
                    onClick={() => abrir(clave)}
                  >
                    <Icon name={TEMA_DE_LECCION[clave].icono} />
                    <strong>{DATOS_DE_LECCION[clave].titulo}</strong>
                    <small>{DATOS_DE_LECCION[clave].minutos} min</small>
                  </button>
                </li>
              ))}
            </ul>
          </nav>
          <section className="ensenanza-contenido" aria-labelledby="leccion-titulo">
            <header>
              <h2 id="leccion-titulo">{datos.titulo}</h2>
              <p>{datos.resumen}</p>
            </header>
            {leccion === 'resultado' && <LeccionResultado onObjetivo={irAObjetivos} />}
            {leccion === 'metricas' && <LeccionMetricas />}
            {leccion === 'recomendaciones' && <LeccionRecomendaciones />}
            {leccion === 'escenario' && (
              <LeccionEscenario
                objetivos={objetivos}
                objetivoId={objetivoId}
                onElegir={(id) => abrir('escenario', id)}
                onDefinir={irAObjetivos}
                onPlan={(id) => void navegacion.irAObjetivo(id)}
              />
            )}
            {leccion === 'consejos' && <LeccionConsejos onObjetivo={irAObjetivos} />}
          </section>
        </div>
      </main>
    </>
  );
}
