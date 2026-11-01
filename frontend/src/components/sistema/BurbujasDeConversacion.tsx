import { burbujasDeSami } from '@/lib/burbujasDeSami';

type Props = {
  quien: 'sami' | 'you';
  texto: string;
  /** Foto que acompaña al mensaje; va en la primera burbuja. */
  foto?: string;
};

// Lo que dice Sami se muestra en burbujas cortas; lo que escribe la persona va entero.
export function BurbujasDeConversacion({ quien, texto, foto }: Props) {
  const partes = quien === 'sami' ? burbujasDeSami(texto) : [texto];
  return partes.map((parte, indice) => (
    <div key={indice} className={`conversation-bubble ${quien}`}>
      {indice === 0 && foto && (
        <img
          className="conversation-photo"
          src={foto}
          alt={quien === 'you' ? 'La foto que le mandaste a Sami' : 'Una foto'}
        />
      )}
      {parte}
    </div>
  ));
}
