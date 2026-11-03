import { useEffect, useState } from 'react';
import { Sami, type PoseDeSami } from '@/components/sistema/Sami';

// Una sola imagen: primero se desvanece la pose anterior y luego entra la nueva.
export function SamiPoses({ pose }: { pose: PoseDeSami }) {
  const [visible, setVisible] = useState(pose);
  useEffect(() => {
    if (visible === pose) return;
    const reducir = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? true;
    const timer = window.setTimeout(() => setVisible(pose), reducir ? 0 : 180);
    return () => window.clearTimeout(timer);
  }, [pose, visible]);
  return (
    <div className="ens-sami-character" data-cambiando={visible !== pose}>
      <Sami pose={visible} />
    </div>
  );
}
