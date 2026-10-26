import { useState, type FormEvent } from 'react';
import { Link } from '@tanstack/react-router';
import { useUsuario } from '@/hooks/auth/queries';
import { useConfiguracionNegocio } from '@/hooks/negocio/queries';
import { useGuardarPreferencias } from '@/hooks/preferencias/mutations';
import { usePreferencias } from '@/hooks/preferencias/queries';
import { useNombre } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';

// El rubro llega como código («KIOSCO», «OTRO»): se muestra como «Kiosco», «Otro».
const rubroLegible = (rubro: string) =>
  `${rubro.charAt(0)}${rubro.slice(1).toLowerCase().replaceAll('_', ' ')}`;

// Quién es la persona y cuál es su comercio: el nombre con el que la llama Sami, el email de la cuenta y el negocio.
export function SeccionPerfil() {
  const usuario = useUsuario();
  const nombreActual = useNombre();
  const preferencias = usePreferencias();
  const guardarPreferencias = useGuardarPreferencias();
  const negocio = useConfiguracionNegocio();
  const avisar = useSistemaStore((estado) => estado.avisar);
  const [nombre, setNombre] = useState(nombreActual);
  const limpio = nombre.trim();

  const guardar = (evento: FormEvent) => {
    evento.preventDefault();
    if (!limpio || limpio === nombreActual) return;
    guardarPreferencias.mutate(
      { ...preferencias, nombre: limpio },
      { onSuccess: () => avisar('Guardamos tu nombre.') },
    );
  };

  const comercio = negocio.data;

  return (
    <>
      <form className="cuenta-tarjeta" onSubmit={guardar} aria-labelledby="cuenta-perfil-titulo">
        <h2 id="cuenta-perfil-titulo">Tus datos</h2>
        <div className="field">
          <label htmlFor="cuenta-nombre">¿Cómo querés que te llame Sami?</label>
          <input
            className="input"
            id="cuenta-nombre"
            maxLength={40}
            required
            value={nombre}
            onChange={(evento) => setNombre(evento.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="cuenta-email">Email de la cuenta</label>
          <input className="input" id="cuenta-email" value={usuario.email} readOnly />
          <span className="input-hint">Es con el que entrás. Por ahora no se puede cambiar.</span>
        </div>
        <div className="cuenta-acciones">
          <button
            className="btn primary"
            type="submit"
            disabled={!limpio || limpio === nombreActual || guardarPreferencias.isPending}
          >
            Guardar nombre
          </button>
        </div>
      </form>

      <section className="cuenta-tarjeta" aria-labelledby="cuenta-comercio-titulo">
        <h2 id="cuenta-comercio-titulo">Tu comercio</h2>
        {negocio.isPending ? (
          <p role="status">Cargando los datos de tu comercio…</p>
        ) : comercio ? (
          <dl className="cuenta-datos">
            <div>
              <dt>Nombre</dt>
              <dd>{comercio.nombre}</dd>
            </div>
            <div>
              <dt>Rubro</dt>
              <dd>{rubroLegible(comercio.rubro)}</dd>
            </div>
            <div>
              <dt>Ubicación</dt>
              <dd>
                {comercio.ubicacion?.localidad ?? 'Todavía no la cargaste'}
                {comercio.zona ? ` · Zona ${comercio.zona}` : ''}
              </dd>
            </div>
            <div>
              <dt>Datos de tu zona</dt>
              <dd>
                {comercio.comparteDatosZona
                  ? 'Los compartís, sin que se te identifique'
                  : 'No los compartís'}
              </dd>
            </div>
          </dl>
        ) : (
          <p>No pudimos traer los datos de tu comercio.</p>
        )}
        <Link className="text-link" to="/ubicacion">
          Ubicación y rubro de mi comercio
        </Link>
      </section>
    </>
  );
}
