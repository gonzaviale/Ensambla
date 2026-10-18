import { useEffect } from 'react';
import { Outlet } from '@tanstack/react-router';
import { MotorDeAvisos } from '@/components/sistema/avisos/MotorDeAvisos';
import { IconSprite } from '@/components/sistema/IconSprite';
import { ModalHost } from '@/components/sistema/ModalHost';
import { Toast } from '@/components/sistema/Toast';
import { Tutorial } from '@/components/sistema/tutorial/Tutorial';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import { usePreferencias } from '@/hooks/preferencias/queries';
import { useSistemasDelServidor } from '@/hooks/sistemas/queries';
import { useSincronizarSistemas } from '@/hooks/sistemas/useSincronizarSistemas';
import { useBienvenida } from '@/hooks/tutorial/useBienvenida';
import { useSistemaStore } from '@/stores/sistemaStore';
import '@/styles/sistema/index.css';

// Todo lo que está adentro de `.mk-root` usa las hojas del mockup; la landing y el acceso quedan afuera.
// Mismo armazón que el index.html del mockup: sprite de íconos, enlace de salto, #app, #overlay y #toast.
export function SistemaLayout() {
  const usuario = useUsuarioOpcional();

  // Al cerrar la sesión no queda nada de la cuenta anterior en memoria.
  useEffect(() => {
    if (!usuario) useSistemaStore.getState().reiniciar();
  }, [usuario]);

  // Sesión cerrada: el guard de la ruta destino redirige en un instante.
  if (!usuario) return null;

  return (
    <div className="mk-root">
      <IconSprite />
      <a className="skip-link" href="#main">
        Ir al contenido
      </a>
      <div id="app">
        <SistemaConDatos usuarioId={usuario.id} />
      </div>
      <ModalHost />
      <Toast />
    </div>
  );
}

// Los sistemas llegan del servidor una vez; de ahí en más la pantalla trabaja sobre su copia en el store
// y un hook aparte la va guardando.
function SistemaConDatos({ usuarioId }: { usuarioId: string }) {
  const sistemas = useSistemasDelServidor();
  usePreferencias();
  const hidratar = useSistemaStore((estado) => estado.hidratar);
  const hidratado = useSistemaStore((estado) => estado.usuarioId === usuarioId);
  useSincronizarSistemas();

  useEffect(() => {
    hidratar(usuarioId, sistemas);
  }, [hidratar, usuarioId, sistemas]);

  if (!hidratado) return null;
  return <PantallaConAvisosYGuia />;
}

// Lo que acompaña a todas las pantallas: avisos de Sami, la bienvenida y la guía interactiva.
function PantallaConAvisosYGuia() {
  useBienvenida();
  return (
    <>
      <Outlet />
      <MotorDeAvisos />
      <Tutorial />
    </>
  );
}
