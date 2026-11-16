import { useState } from 'react';
import { useLocation } from '@tanstack/react-router';
import { seleccionarNegocioActivo, useSistemaStore } from '@/stores/sistemaStore';
import { allBlocks } from '@/lib/sistema/core';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';

export function useAccesosRapidos() {
  const ubicacion = useLocation();
  const negocio = useSistemaStore(seleccionarNegocioActivo);
  const modal = useSistemaStore((estado) => estado.modal);
  const navegacion = useNavegacion();
  const contexto = `${ubicacion.href}:${negocio?.id ?? ''}`;
  const [menu, cambiarMenu] = useState({ contexto, abierto: false });
  const abierto = menu.contexto === contexto && menu.abierto;
  const cambiarAbierto = (valor: boolean) => cambiarMenu({ contexto, abierto: valor });
  const oculto = ubicacion.pathname === '/caja' || !!ubicacion.search.escanear || !!modal;
  const inventarios = allBlocks(negocio).filter((bloque) => bloque.type === 'inventory');
  const cerrar = () => cambiarAbierto(false);
  const vender = () => {
    cerrar();
    void navegacion.irACaja();
  };
  const escanear = (id: string) => {
    if (!negocio) return;
    cerrar();
    void navegacion.irAEscaneoInventario(negocio.id, id);
  };
  return { oculto, abierto, cambiarAbierto, cerrar, vender, escanear, inventarios };
}
