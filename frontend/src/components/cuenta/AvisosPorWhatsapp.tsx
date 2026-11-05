import { leerErrorServidor } from '@/hooks/errores';
import { usePreferencias } from '@/hooks/preferencias/queries';
import { useConfigurarAvisosPorWhatsapp } from '@/hooks/whatsapp/mutations';

// El resumen de la mañana: Sami le manda a la persona, una vez por día, lo que encontró en su negocio.
export function AvisosPorWhatsapp() {
  const preferencias = usePreferencias();
  const configurar = useConfigurarAvisosPorWhatsapp();
  const activo = preferencias.avisosPorWhatsapp === true;

  return (
    <div className="cuenta-avisos-whatsapp">
      <label className="cuenta-opcion" htmlFor="cuenta-avisos-whatsapp">
        <input
          id="cuenta-avisos-whatsapp"
          type="checkbox"
          checked={activo}
          disabled={configurar.isPending}
          aria-describedby="cuenta-avisos-whatsapp-ayuda"
          onChange={(evento) => configurar.mutate(evento.target.checked)}
        />
        <span>Mandame el resumen de Sami cada mañana</span>
      </label>
      <p id="cuenta-avisos-whatsapp-ayuda" className="input-hint">
        Una vez por día, desde las 9, te cuenta por WhatsApp lo que encontró: productos agotados, precios para revisar
        y lo que pide la zona. Si no hay nada nuevo, no te escribe.
      </p>
      {configurar.isError ? <p role="alert">{leerErrorServidor(configurar.error)?.mensaje}</p> : null}
    </div>
  );
}
