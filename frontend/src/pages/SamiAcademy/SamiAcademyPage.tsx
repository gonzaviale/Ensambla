import { useNavigate } from '@tanstack/react-router';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { DATOS_DE_LECCION, LECCIONES_DE_ACADEMY, type LeccionDeAcademy } from '@/lib/academy';
import { useObjetivosStore } from '@/stores/objetivosStore';
import { Encabezado } from '@/pages/Objetivos/piezas';
import { LeccionEscenario } from './LeccionEscenario';
import {
  LeccionConsejos,
  LeccionMetricas,
  LeccionRecomendaciones,
  LeccionResultado,
} from './Lecciones';

type Props = { leccion: LeccionDeAcademy; objetivoId?: string };

// Sami Academy: donde Sami enseña contabilidad aplicada al negocio y explica cómo hace sus recomendaciones.
export function SamiAcademyPage({ leccion, objetivoId }: Props) {
  useTituloPagina('Sami Academy');
  const navigate = useNavigate();
  const navegacion = useNavegacion();
  const objetivos = useObjetivosStore((estado) => estado.objetivos);

  // Cambiar de lección no vuelve al principio de la página ni suma entradas al historial.
  const abrir = (siguiente: LeccionDeAcademy, objetivo = objetivoId) =>
    void navigate({
      to: '/sami-academy',
      search: { leccion: siguiente, objetivo },
      replace: true,
      resetScroll: false,
    });
  const irAObjetivos = () => void navegacion.irAObjetivos();
  const datos = DATOS_DE_LECCION[leccion];

  return (
    <>
      <CabeceraComun active="academy" />
      <main id="main" tabIndex={-1} className="page-wrap objetivos-page academy-page reveal">
        <Encabezado
          etiqueta="Sami Academy"
          titulo="Entendé tus números con Sami"
          texto="Lecciones cortas de contabilidad aplicadas a tu negocio: de dónde sale cada métrica, cómo hace Sami sus recomendaciones y cómo probar escenarios con tus objetivos."
          sami="Te enseña sin tecnicismos"
        />
        <div className="academy-layout">
          <nav className="academy-indice" aria-label="Lecciones">
            <ol>
              {LECCIONES_DE_ACADEMY.map((clave, indice) => (
                <li key={clave}>
                  <button
                    type="button"
                    className={clave === leccion ? 'active' : ''}
                    aria-current={clave === leccion ? 'page' : undefined}
                    onClick={() => abrir(clave)}
                  >
                    <span>{indice + 1}</span>
                    <div>
                      <strong>{DATOS_DE_LECCION[clave].titulo}</strong>
                      <small>{DATOS_DE_LECCION[clave].minutos} min</small>
                    </div>
                  </button>
                </li>
              ))}
            </ol>
          </nav>
          <section className="academy-contenido" aria-labelledby="leccion-titulo">
            <header>
              <p className="objetivos-etiqueta">
                Lección {LECCIONES_DE_ACADEMY.indexOf(leccion) + 1} de {LECCIONES_DE_ACADEMY.length}
              </p>
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
