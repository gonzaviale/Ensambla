import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { PageFooter } from '@/components/sistema/PageFooter';
import { Sami } from '@/components/sistema/Sami';
import { TopBar } from '@/components/sistema/TopBar';
import { useUsuario } from '@/hooks/auth/queries';
import { useSistemaStore } from '@/stores/sistemaStore';

// Provisorio: sirve para comprobar el CSS del mockup. Se reemplaza por el inicio real.
export function InicioPage() {
  const usuario = useUsuario();
  const sistemas = useSistemaStore((estado) => estado.sistemas.length);
  const nada = () => undefined;

  return (
    <>
      <TopBar
        nombre={usuario.nombre}
        sistemas={sistemas}
        onInicio={nada}
        onSistemas={nada}
        onPlantillas={nada}
        onZona={nada}
        onAyuda={nada}
        onPerfil={nada}
      />
      <main id="main" tabIndex={-1} className="home home-v2 reveal">
        <section className="home-hero">
          <div className="hero-copy">
            <span className="eyebrow">TU MANERA DE TRABAJAR TIENE LUGAR ACÁ</span>
            <p className="hero-hello">Hola, {usuario.nombre}.</p>
            <h1>
              ¿En qué puedo
              <br /> ayudarte hoy?
            </h1>
            <div className="hero-sami">
              <Sami />
              <div>
                <strong>Soy Sami. Lo armamos juntos.</strong>
                <span>Te acompaño desde tu primer bloque.</span>
              </div>
            </div>
            <div className="hero-actions">
              <Btn cls="primary">
                Armémoslo con Sami <Icon name="arrow" />
              </Btn>
            </div>
          </div>
        </section>
        <PageFooter />
      </main>
    </>
  );
}
