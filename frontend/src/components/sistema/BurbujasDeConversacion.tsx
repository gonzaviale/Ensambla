import { burbujasDeSami } from '@/lib/burbujasDeSami';

type Props = {
  quien: 'sami' | 'you';
  texto: string;
  /** Foto que acompaña al mensaje; va en la primera burbuja. */
  foto?: string;
  /** Cuántas burbujas de Sami mostrar (para que aparezcan de a una); sin esto se ven todas. */
  visibles?: number;
};

// Lo que dice Sami se muestra en burbujas cortas; lo que escribe la persona va entero.
export function BurbujasDeConversacion({ quien, texto, foto, visibles }: Props) {
  const todas = quien === 'sami' ? burbujasDeSami(texto) : [texto];
  const partes = visibles === undefined ? todas : todas.slice(0, visibles);
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
