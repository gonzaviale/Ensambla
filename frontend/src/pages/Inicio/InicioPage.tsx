import { useMemo, useState } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { PreviewBoard } from '@/components/sistema/PreviewBoard';
import { varsDeColor } from '@/components/sistema/colores';
import { CATALOG, TEMPLATES } from '@/lib/sistema/catalogo';
import { allBlocks, applyLayout, makeTemplate, totals } from '@/lib/sistema/core';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { ClavePlantilla, Color, Distribucion } from '@/lib/sistema/tipos';
import { useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useAccionesDelTutorial } from '@/hooks/tutorial/useAccionesDelTutorial';
import { useSistemaStore } from '@/stores/sistemaStore';

const CLAVES = Object.keys(TEMPLATES) as ClavePlantilla[];
// Los cuadernos guardados van rotando los cinco colores de la marca.
const COLORES_DE_CUADERNOS: Color[] = ['coral', 'blue', 'orange', 'petrol', 'yellow'];

// Inicio: un saludo, las tres maneras de empezar como fichas de color, las otras herramientas, los
// cuadernos guardados y las plantillas. Fondo blanco; el color va lleno en cada ficha, con íconos blancos.
export function InicioPage() {
  const nombre = useNombre();
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const tutorial = useAccionesDelTutorial();
  const navegacion = useNavegacion();
  const [distribucion, setDistribucion] =
    useState<Extract<Distribucion, 'mosaic' | 'single'>>('mosaic');

  const ejemplo = useMemo(() => {
    const sistema = makeTemplate('shop', {}, true);
    applyLayout(sistema, distribucion);
    return sistema;
  }, [distribucion]);

  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="home inicio reveal">
        <section className="inicio-portada">
          <div className="inicio-saludo">
            <p className="inicio-hola">Hola, {nombre}.</p>
            <h1>
              ¿En qué puedo ayudarte hoy?
            </h1>
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

          <div className="inicio-fichas" data-tour="choose">
            <Ficha
              color="orange"
              icono="spark"
              titulo="Crear con Sami"
              texto="Contale cómo trabajás y te propone una base."
              accion="guide"
              imagen="/mockup/sami-constructora.webp"
              onClick={() => void navegacion.irATaller()}
            />
            <Ficha
              color="blue"
              icono="grid"
              titulo="Elegir una plantilla"
              texto="Almacén, depósito o pedidos, listos para usar."
              accion="templates"
              onClick={() => void navegacion.irAPlantillas()}
            />
            <Ficha
              color="yellow"
              icono="plus"
              titulo="Empezar de cero"
              texto="Armá tus columnas y tus secciones a tu manera."
              accion="blank"
              onClick={() => abrirModal({ tipo: 'blanco' })}
            />
          </div>
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

        {sistemas.length > 0 && (
          <section id="my-systems" className="inicio-bloque">
            <Titulo titulo="Seguí donde lo dejaste" />
            <div className="inicio-cuadernos">
              {sistemas.map((sistema, indice) => {
                const color = COLORES_DE_CUADERNOS[indice % COLORES_DE_CUADERNOS.length] ?? 'coral';
                const primero = allBlocks(sistema)[0];
                return (
                  <button
                    key={sistema.id}
                    type="button"
                    className="inicio-cuaderno"
                    style={varsDeColor(color)}
                    data-color={color}
                    onClick={() => void navegacion.abrirSistema(sistema.id)}
                  >
                    <span className="inicio-cuaderno-lomo" aria-hidden="true" />
                    <span className="inicio-cuaderno-tapa">
                      <Icon name={primero ? CATALOG[primero.type].icon : 'note'} />
                      <strong>{sistema.name}</strong>
                      <span>
                        {allBlocks(sistema).length} bloques · {totals(sistema).records} registros
                      </span>
                    </span>
                  </button>
                );
              })}
            </div>
          </section>
        )}

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
                    Ver el sistema <Icon name="arrow" />
                  </button>
                </article>
              );
            })}
          </div>
        </section>

        {/* <section className="inicio-bloque inicio-ejemplo">
          <Titulo
            titulo="Así se ve un sistema"
            detalle="Un ejemplo para mirar. Probá cambiar cómo se reparten las piezas."
          />
          <div className="hero-system">
            <div className="hero-system-top">
              <span className="live-dot" />
              <strong>Un sistema. Tu forma de organizarlo.</strong>
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

// Una de las tres maneras de empezar: ficha de color lleno con el ícono en blanco (o una imagen de Sami).
function Ficha({
  color,
  icono,
  titulo,
  texto,
  accion,
  imagen,
  onClick,
}: PropsDeAccion & {
  /** Marca que usa la guía para saber qué camino eligió la persona. */
  accion: string;
  /** Imagen que reemplaza al ícono. */
  imagen?: string;
}) {
  return (
    <button
      type="button"
      className="inicio-ficha"
      style={varsDeColor(color)}
      data-color={color}
      data-accion={accion}
      onClick={onClick}
    >
      {imagen ? (
        <img className="inicio-ficha-imagen" src={imagen} alt="" />
      ) : (
        <span className="inicio-ficha-icono">
          <Icon name={icono} />
        </span>
      )}
      <span className="inicio-ficha-texto">
        <strong>{titulo}</strong>
        <span>{texto}</span>
      </span>
      <span className="inicio-ficha-flecha">
        <Icon name="arrow" />
      </span>
    </button>
  );
}

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
