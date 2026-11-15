import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { BrandMark } from './BrandMark';

// El pie de todas las pantallas de la app (T-216): la marca, los atajos que más se usan, agrupados, y Sami saludando. Mismos
// nombres que la barra de arriba para que nada se llame de dos maneras. Son enlaces de verdad (se pueden abrir en otra
// pestaña), pero al tocarlos navegan como la barra, limpiando lo que había abierto.
export function PageFooter() {
  const navegacion = useNavegacion();
  const grupos = [
    {
      titulo: 'Tu negocio',
      enlaces: [
        { texto: 'Inicio', ruta: '/inicio', ir: navegacion.irAInicio },
        { texto: 'Mis cuadernos', ruta: '/cuadernos', ir: navegacion.irAMisSistemas },
        { texto: 'Avisos', ruta: '/avisos', ir: navegacion.irAAvisos },
        { texto: 'Objetivos', ruta: '/objetivos', ir: navegacion.irAObjetivos },
      ],
    },
    {
      titulo: 'Para crecer',
      enlaces: [
        { texto: 'Mi zona', ruta: '/zona', ir: navegacion.irAZona },
        { texto: 'Plantillas', ruta: '/plantillas', ir: navegacion.irAPlantillas },
        {
          texto: 'Sami te enseña',
          ruta: '/sami-te-ensena',
          ir: () => navegacion.irASamiTeEnsena(),
        },
      ],
    },
    {
      titulo: 'Tu cuenta',
      enlaces: [
        { texto: 'Configuración', ruta: '/cuenta', ir: () => navegacion.irACuenta() },
        { texto: 'Planes', ruta: '/planes', ir: navegacion.irAPlanes },
      ],
    },
  ];

  return (
    <footer className="page-footer pie">
      {/* Cinco bloques de la marca apilados en escalera: marcan que acá termina la pantalla. */}
      <div className="pie-bloques" aria-hidden="true">
        <span />
        <span />
        <span />
        <span />
        <span />
      </div>
      <div className="pie-cuerpo">
        <div className="pie-marca">
          <p className="pie-logo">
            <BrandMark />
            <strong>Ensambla</strong>
          </p>
          <p className="pie-frase">Tu negocio, un bloque a la vez.</p>
        </div>
        <nav className="pie-enlaces" aria-label="Pie de página">
          {grupos.map((grupo) => (
            <div key={grupo.titulo} className="pie-grupo">
              <p className="pie-grupo-titulo">{grupo.titulo}</p>
              <ul>
                {grupo.enlaces.map((enlace) => (
                  <li key={enlace.texto}>
                    <a
                      href={enlace.ruta}
                      onClick={(evento) => {
                        evento.preventDefault();
                        void enlace.ir();
                      }}
                    >
                      {enlace.texto}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>
        <div className="pie-sami">
          <p className="pie-sami-globo">Hola, soy Sami.</p>
          <img src="/mockup/sami-saludando.webp" alt="" />
        </div>
      </div>
    </footer>
  );
}
