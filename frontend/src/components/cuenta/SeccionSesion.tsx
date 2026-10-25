import { useCerrarSesion } from '@/hooks/auth/mutations';
import { Btn } from '../sistema/Btn';

// Salir de la cuenta en este dispositivo.
export function SeccionSesion() {
  const { cerrarSesion, cerrando } = useCerrarSesion();

  return (
    <section className="cuenta-tarjeta" aria-labelledby="cuenta-sesion-titulo">
      <h2 id="cuenta-sesion-titulo">Cerrar sesión</h2>
      <p className="cuenta-ayuda">
        Al cerrar la sesión salís de tu cuenta en este dispositivo. Tus sistemas y tus datos quedan
        guardados para cuando vuelvas a entrar.
      </p>
      <div className="cuenta-acciones">
        <Btn cls="secondary" disabled={cerrando} onClick={() => void cerrarSesion()}>
          Cerrar sesión
        </Btn>
      </div>
    </section>
  );
}
