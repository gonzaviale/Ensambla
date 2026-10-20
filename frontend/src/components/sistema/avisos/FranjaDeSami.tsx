import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { Sami } from '../Sami';
import { ICONO_DE_AVISO } from './presentacion';

// La franja de Sami arriba de los bloques cuando se usa un sistema: lo más importante, a un toque.
export function FranjaDeSami({ sistemaId }: { sistemaId: string }) {
  const avisos = useSistemaStore((estado) => estado.avisos).filter(
    (aviso) => aviso.sistemaId === sistemaId,
  );
  const abrirPanel = useSistemaStore((estado) => estado.abrirPanel);

  if (!avisos.length) {
    return (
      <section className="sami-franja ok" aria-label="Avisos de Sami">
        <img className="sami-franja-img" src="/mockup/sami-guino.webp" alt="" />
        <p>
          <strong>Todo en orden.</strong> Sigo mirando tu stock, tus ventas y los precios de tu
          zona.
        </p>
      </section>
    );
  }
  return (
    <section className="sami-franja" aria-label="Avisos de Sami">
      <Sami pose="think" />
      <div className="sami-franja-txt">
        <strong>
          {avisos.length === 1
            ? 'Encontré algo para mirar.'
            : `Encontré ${avisos.length} cosas para mirar.`}
        </strong>
        <div className="sami-franja-items">
          {avisos.slice(0, 3).map((aviso) => (
            <button
              key={aviso.id}
              type="button"
              className={`sami-chip p${aviso.prioridad}`}
              onClick={() => abrirPanel(aviso.id)}
            >
              <Icon name={ICONO_DE_AVISO[aviso.tipo]} />
              <span>{aviso.titulo}</span>
            </button>
          ))}
        </div>
      </div>
      <Btn cls="soft small" onClick={() => abrirPanel()}>
        Ver {avisos.length > 3 ? `los ${avisos.length}` : 'todo'} <Icon name="arrow" />
      </Btn>
    </section>
  );
}
