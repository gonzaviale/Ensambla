import { useMemo, useState } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { IconTile } from '@/components/sistema/IconTile';
import { PageFooter } from '@/components/sistema/PageFooter';
import { PreviewBoard } from '@/components/sistema/PreviewBoard';
import { Sami } from '@/components/sistema/Sami';
import { TemplateCard } from '@/components/sistema/TemplateCard';
import { COLORS, TEMPLATES } from '@/lib/sistema/catalogo';
import { allBlocks, applyLayout, makeTemplate, totals } from '@/lib/sistema/core';
import type { ClavePlantilla, Distribucion } from '@/lib/sistema/tipos';
import { useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';

const CLAVES = Object.keys(TEMPLATES) as ClavePlantilla[];

// Inicio: el héroe con un sistema de ejemplo, los sistemas guardados, las tres puertas y las plantillas.
export function InicioPage() {
  const nombre = useNombre();
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const avisar = useSistemaStore((estado) => estado.avisar);
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
      <main id="main" tabIndex={-1} className="home home-v2 reveal">
        <section className="home-hero">
          <div className="hero-copy">
            <span className="eyebrow">TU MANERA DE TRABAJAR TIENE LUGAR ACÁ</span>
            <p className="hero-hello">Hola, {nombre}.</p>
            <h1>
              ¿En qué puedo
              <br /> ayudarte hoy?
            </h1>
            <p className="hero-description">
              Vos traés la idea. <br />
              <strong>Juntos le damos forma.</strong>
            </p>
            <p className="hero-detail">
              Contanos cómo trabajás y empezá con un sistema que podés ver, mover y hacer tuyo.
            </p>
            <div className="hero-sami">
              <Sami />
              <div>
                <strong>Soy Sami. Lo armamos juntos.</strong>
                <span>Te acompaño desde tu primer bloque.</span>
                <button
                  type="button"
                  className="text-link"
                  onClick={() => avisar('El recorrido guiado llega más adelante.')}
                >
                  Mostrarme cómo empezar <Icon name="arrow" />
                </button>
              </div>
            </div>
            <div className="hero-actions">
              <Btn cls="primary" onClick={() => void navegacion.irATaller()}>
                Armémoslo con Sami <Icon name="arrow" />
              </Btn>
              <button
                type="button"
                className="text-link"
                onClick={() => void navegacion.irAPlantillas()}
              >
                Quiero ver plantillas
              </button>
            </div>
            <div className="brand-pieces" aria-label="Los cinco colores de Ensambla">
              <i />
              <i />
              <i />
              <i />
              <i />
              <span>Cada pieza cuenta.</span>
            </div>
          </div>
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
        </section>

        {sistemas.length > 0 && (
          <section id="my-systems" className="my-systems">
            <div className="section-lead">
              <h2>Seguí donde lo dejaste</h2>
              <span />
            </div>
            <div className="saved-grid">
              {sistemas.map((sistema) => (
                <button
                  key={sistema.id}
                  type="button"
                  className="saved-card"
                  onClick={() => void navegacion.abrirSistema(sistema.id)}
                >
                  <div className="row">
                    <IconTile tipo={allBlocks(sistema)[0]?.type ?? 'form'} />
                    <Icon name="arrow" />
                  </div>
                  <h3>{sistema.name}</h3>
                  <p>
                    {allBlocks(sistema).length} bloques · {sistema.sections.length} secciones ·{' '}
                    {totals(sistema).records} registros
                  </p>
                </button>
              ))}
            </div>
          </section>
        )}

        <div className="section-lead">
          <h2>Tres puertas. El mismo lugar para crear.</h2>
          <span />
        </div>
        <section className="start-grid start-grid-v2">
          <article className="route-card sami-route">
            <span className="card-tag">TU PRIMER SISTEMA, ACOMPAÑADO</span>
            <Sami />
            <h3>
              Contalo.
              <br />
              Miralo tomar forma.
            </h3>
            <p>
              Sami te propone una base. Vos le decís qué falta, cambiás las piezas y decidís cuándo
              está lista.
            </p>
            <div className="card-foot">
              <Btn cls="orange" onClick={() => void navegacion.irATaller()}>
                Crear con Sami <Icon name="arrow" />
              </Btn>
            </div>
          </article>
          <article className="route-card template-route">
            <div className="route-blocks" aria-hidden="true">
              <i>
                <Icon name="check-circle" />
              </i>
              <i>
                <Icon name="users" />
              </i>
              <i>
                <Icon name="box" />
              </i>
            </div>
            <h3>
              Un conjunto
              <br />
              que ya funciona.
            </h3>
            <p>
              Almacén o kiosco, Depósito o Pedidos y clientes: bloques conectados para empezar con
              ventaja.
            </p>
            <div className="card-foot">
              <Btn cls="secondary" onClick={() => void navegacion.irAPlantillas()}>
                Elegir una plantilla <Icon name="arrow" />
              </Btn>
            </div>
          </article>
          <article className="route-card free-route">
            <div className="free-arrangement" aria-hidden="true">
              <i />
              <i />
              <i>
                <Icon name="plus" />
              </i>
            </div>
            <h3>
              Tu lienzo.
              <br />
              Tus reglas.
            </h3>
            <p>
              Armá columnas, dejá el stock bien ancho y separá el trabajo en las secciones que
              quieras.
            </p>
            <div className="card-foot">
              <Btn cls="secondary" onClick={() => abrirModal({ tipo: 'blanco' })}>
                Empezar de cero <Icon name="arrow" />
              </Btn>
            </div>
          </article>
        </section>

        <section className="import-banner">
          <Icon name="camera" />
          <div>
            <h3>Tu cuaderno también puede ser un punto de partida.</h3>
            <p>Probá la carga desde una foto o captura. Siempre revisás antes de incorporar.</p>
          </div>
          <button
            type="button"
            className="text-link"
            onClick={() =>
              abrirModal({
                tipo: 'importar',
                sistemaId: null,
                bloqueId: null,
                contexto: 'registros',
              })
            }
          >
            Mostrarme cómo <Icon name="arrow" />
          </button>
        </section>

        <section className="zone-banner">
          <span className="zone-banner-bars" aria-hidden="true">
            <i style={{ background: COLORS.blue, height: 14 }} />
            <i style={{ background: COLORS.coral, height: 26 }} />
            <i style={{ background: COLORS.yellow, height: 20 }} />
          </span>
          <div>
            <h3>¿Tus precios están a tono con tu zona?</h3>
            <p>
              Mirá cuánto se cobra cerca y qué se vende. Son datos de varios comercios juntos: nunca
              se sabe quién es quién.
            </p>
          </div>
          <Btn cls="primary" onClick={() => void navegacion.irAZona()}>
            Ver mi zona <Icon name="arrow" />
          </Btn>
        </section>

        <section className="home-templates">
          <div className="sample-heading">
            <h2>Una plantilla es un pequeño sistema.</h2>
            <p>Varias piezas que se entienden entre sí. Después sumás las tuyas.</p>
          </div>
          <div className="template-grid">
            {CLAVES.map((clave) => (
              <TemplateCard
                key={clave}
                clave={clave}
                onVer={(elegida) => abrirModal({ tipo: 'plantilla', clave: elegida })}
              />
            ))}
          </div>
        </section>
        <PageFooter />
      </main>
    </>
  );
}
