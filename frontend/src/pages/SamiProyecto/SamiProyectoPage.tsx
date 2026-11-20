import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';
import { Icon } from '@/components/sistema/Icon';
import { useDictado } from '@/hooks/sistemas/useDictado';
import { saludoDeSami, useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useSamiProyecto } from '@/hooks/sistemas/useSamiProyecto';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { useSistemaStore } from '@/stores/sistemaStore';
import { ConversacionConSami } from './ConversacionConSami';
import { LienzoDelProyecto } from './LienzoDelProyecto';

type Sami = ReturnType<typeof useSamiProyecto>;

function etiquetaDelEstado({ creado, propuesta }: Sami): string {
  if (creado) return 'Proyecto creado';
  if (propuesta) return 'Propuesta sin guardar';
  return 'Contame tu negocio';
}

// «Armalo con Sami» (T-118): la conversación que arma un proyecto real a partir de cómo la persona cuenta su negocio.
export function SamiProyectoPage() {
  useTituloPagina('Armalo con Sami');
  const nombrePersona = useNombre();
  const navegacion = useNavegacion();
  const avisar = useSistemaStore((estado) => estado.avisar);
  const sami = useSamiProyecto(saludoDeSami(nombrePersona));
  const [entrada, setEntrada] = useState('');
  const mensajes = useRef<HTMLDivElement>(null);
  const dictado = useDictado({ obtenerBase: () => entrada, alTexto: setEntrada, alAvisar: avisar });

  const cantidadDeMensajes = sami.mensajes.length;
  useEffect(() => {
    if (mensajes.current) mensajes.current.scrollTop = mensajes.current.scrollHeight;
  }, [cantidadDeMensajes, sami.pensando]);

  const alEnviar = (evento: FormEvent) => {
    evento.preventDefault();
    dictado.detener();
    void sami.describir(entrada).then((enviado) => {
      if (enviado) setEntrada('');
    });
  };

  return (
    <>
      <CabeceraComun />
      <main id="main" tabIndex={-1} className="creation-studio reveal">
        <div className="studio-top">
          <Btn cls="ghost small" onClick={() => void navegacion.irAInicio()}>
            <Icon name="back" /> Inicio
          </Btn>
          <span>ARMALO CON SAMI</span>
          <span className="tag outline">{etiquetaDelEstado(sami)}</span>
        </div>
        <div className="studio-layout">
          <ConversacionConSami
            sami={sami}
            entrada={entrada}
            onEntrada={setEntrada}
            dictado={dictado}
            onEnviar={alEnviar}
            referencia={mensajes}
          />
          <section className="studio-canvas" aria-label="Tu proyecto">
            <LienzoDelProyecto sami={sami} navegacion={navegacion} />
          </section>
        </div>
      </main>
    </>
  );
}
