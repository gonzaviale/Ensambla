import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { leerErrorServidor } from '@/hooks/errores';
import { useDesvincularWhatsapp } from '@/hooks/whatsapp/mutations';
import { useSistemaStore } from '@/stores/sistemaStore';
import { type VinculoWhatsapp } from '@/types/whatsapp';
import { enlaceDeSami, fecha } from './formatos';
import { Btn } from '../../sistema/Btn';

export function NumeroVinculado({ vinculo }: { vinculo: VinculoWhatsapp }) {
  const desvincular = useDesvincularWhatsapp();
  const avisar = useSistemaStore((estado) => estado.avisar);
  const [confirmando, setConfirmando] = useState(false);

  return (
    <>
      <p role="status" className="cuenta-plan-actual">
        <strong>+{vinculo.telefono}</strong>
        <span>Vinculado el {fecha(vinculo.vinculadoEn)}</span>
      </p>
      <p>
        Enviá a Sami una foto de tu cuaderno o un texto como «gasté 5000 en harina»: arma las filas,
        te las muestra y las guarda cuando le respondés que sí. Las fotos descuentan de las páginas
        con IA de <Link to="/planes">tu plan</Link>.
      </p>
      {desvincular.isError ? (
        <p role="alert">{leerErrorServidor(desvincular.error)?.mensaje}</p>
      ) : null}
      <div className="cuenta-acciones">
        {vinculo.numeroBot ? (
          <a
            className="btn primary"
            href={enlaceDeSami(vinculo.numeroBot)}
            target="_blank"
            rel="noreferrer"
          >
            Escribirle a Sami
          </a>
        ) : null}
        {confirmando ? (
          <>
            <Btn
              cls="danger"
              disabled={desvincular.isPending}
              onClick={() =>
                desvincular.mutate(undefined, {
                  onSuccess: () => avisar('Desvinculamos tu número de WhatsApp.'),
                })
              }
            >
              Sí, desvincular
            </Btn>
            <Btn cls="ghost" onClick={() => setConfirmando(false)}>
              Cancelar
            </Btn>
          </>
        ) : (
          <Btn cls="secondary" onClick={() => setConfirmando(true)}>
            Desvincular
          </Btn>
        )}
      </div>
    </>
  );
}

// Paso 2: el código llegó por WhatsApp; se escribe acá. Se puede pedir otro pasada la espera o cambiar el número.
