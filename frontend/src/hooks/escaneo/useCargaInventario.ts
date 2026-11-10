import { useCallback, useEffect, useRef, useState } from 'react';
import { useBlocker } from '@tanstack/react-router';
import { consultarCatalogos } from '@/api/catalogosProductos';
import { findBlock } from '@/lib/sistema/core';
import {
  buscarProductoPorCodigo,
  guardarProductoEscaneado,
  prepararProducto,
} from '@/lib/escaneo/inventario';
import { codigoGtin, mismoGtin } from '@/lib/escaneo/codigo';
import { sugerirProducto } from '@/lib/escaneo/catalogos';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { ErroresFila, Fila, Valor } from '@/lib/sistema/tipos';
import type { BorradorProductoInventario, ResultadoCatalogos } from '@/types/escaneo';

export function useCargaInventario(sistemaId: string, inventarioId: string) {
  const sistema = useSistemaStore((estado) =>
    estado.sistemas.find((item) => item.id === sistemaId),
  );
  const pendiente = useSistemaStore((estado) => estado.pendientes.includes(sistemaId));
  const inventario = findBlock(sistema, inventarioId);
  const [codigo, setCodigo] = useState('');
  const [borrador, setBorrador] = useState<BorradorProductoInventario | null>(null);
  const [resultado, setResultado] = useState<ResultadoCatalogos | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [errores, setErrores] = useState<ErroresFila>({});
  const [mensaje, setMensaje] = useState('');
  const [ultimo, setUltimo] = useState<Fila | null>(null);
  const formulario = useRef<HTMLFormElement>(null);
  const entradaCodigo = useRef<HTMLInputElement>(null);
  const origenLectura = useRef<'codigo' | 'camara'>('codigo');
  const [retomarLectura, setRetomarLectura] = useState(0);
  useEffect(() => {
    if (retomarLectura > 0 && origenLectura.current === 'codigo') entradaCodigo.current?.focus();
  }, [retomarLectura]);
  const [revisionErrores, setRevisionErrores] = useState(0);
  useEffect(() => {
    formulario.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
  }, [revisionErrores]);
  const consulta = useRef<AbortController | null>(null);
  const secuencia = useRef(0);
  useEffect(
    () => () => {
      secuencia.current++;
      consulta.current?.abort();
    },
    [],
  );
  useBlocker({
    shouldBlockFn: () =>
      !!borrador &&
      !window.confirm('Hay un producto sin guardar. ¿Querés salir y descartar esta carga?'),
    enableBeforeUnload: !!borrador,
  });

  const detener = () => {
    secuencia.current++;
    consulta.current?.abort();
    setBuscando(false);
  };
  const limpiar = () => {
    setBorrador(null);
    setResultado(null);
    setCodigo('');
    setErrores({});
  };
  const buscar = useCallback(
    async (entrada: string, origen: 'codigo' | 'camara' = 'codigo') => {
      if (!inventario || inventario.type !== 'inventory') return;
      let valido: string;
      try {
        valido = codigoGtin(entrada);
      } catch (e) {
        setErrores({ _form: e instanceof Error ? e.message : 'Revisá el código.' });
        return;
      }
      if (
        borrador &&
        !window.confirm('Hay un producto sin guardar. ¿Querés descartarlo y buscar otro código?')
      )
        return;
      consulta.current?.abort();
      origenLectura.current = origen;
      const numero = ++secuencia.current;
      setErrores({});
      setMensaje('');
      setResultado(null);
      setBuscando(false);
      setCodigo(valido);
      const propio = buscarProductoPorCodigo(inventario, valido);
      setBorrador(prepararProducto(inventario, valido, propio));
      if (propio) {
        setMensaje(
          'Este código ya está en tu inventario. No se agrega otro producto ni se modifica la cantidad al escanear.',
        );
        return;
      }
      const controlador = new AbortController();
      consulta.current = controlador;
      const plazo = setTimeout(() => controlador.abort(), 12_000);
      setBuscando(true);
      try {
        const encontrados = await consultarCatalogos(valido, controlador.signal);
        if (numero !== secuencia.current) return;
        setResultado(encontrados);
        const sugerencia = sugerirProducto(encontrados);
        if (sugerencia.nombre)
          setBorrador((actual) =>
            actual
              ? { ...actual, valores: { ...actual.valores, name: sugerencia.nombre } }
              : actual,
          );
      } catch (e) {
        if (numero === secuencia.current)
          setErrores({
            _form:
              e instanceof Error && e.name === 'AbortError'
                ? 'La consulta tardó demasiado. Podés completar los datos a mano.'
                : 'No pudimos consultar los catálogos. Podés cargar el producto a mano.',
          });
      } finally {
        clearTimeout(plazo);
        if (numero === secuencia.current) setBuscando(false);
      }
    },
    [inventario, borrador],
  );

  const guardar = () => {
    if (!borrador) return;
    const estado = useSistemaStore.getState();
    const actual = estado.sistemas.find((item) => item.id === sistemaId);
    if (!actual || estado.activeId !== sistemaId) {
      setErrores({ _form: 'El negocio ya no existe.' });
      return;
    }
    const guardado = guardarProductoEscaneado(actual, inventarioId, borrador);
    if (guardado.errores) {
      setErrores(guardado.errores);
      setRevisionErrores((revision) => revision + 1);
      return;
    }
    estado.commit((item) => {
      item.sections = guardado.sistema.sections;
    });
    detener();
    limpiar();
    setUltimo(guardado.producto);
    setRetomarLectura((revision) => revision + 1);
    setMensaje(
      borrador.productoId
        ? 'Cambios del producto aplicados al inventario.'
        : 'Producto agregado al inventario. Ya podés escanear el siguiente.',
    );
  };
  return {
    sistema,
    inventario,
    pendiente,
    codigo,
    borrador,
    resultado,
    buscando,
    errores,
    mensaje,
    ultimo,
    sugerencia: resultado ? sugerirProducto(resultado) : null,
    formulario,
    entradaCodigo,
    productosVinculables:
      inventario?.rows
        .filter(
          (producto) => !producto.barcode || mismoGtin(producto.barcode, borrador?.codigo ?? ''),
        )
        .map((producto) => ({ id: producto.id, nombre: String(producto.name || producto.id) })) ??
      [],
    grupos: [
      {
        titulo: 'Identificación del producto',
        ayuda: 'Revisá la descripción que se usará en tu inventario.',
        claves: ['name'],
      },
      {
        titulo: 'Datos de tu negocio',
        ayuda: 'Completá tu conteo real, costos y precios. Estos datos los definís vos.',
        claves: ['quantity', 'cost', 'price', 'minimum'],
      },
      {
        titulo: 'Otros datos de tu inventario',
        ayuda: 'Son los campos que configuraste en esta tabla.',
        claves: null,
      },
    ]
      .map((grupo) => ({
        ...grupo,
        campos:
          inventario?.fields.filter(
            (campo) =>
              campo.key !== 'barcode' &&
              (grupo.claves
                ? grupo.claves.includes(campo.key)
                : !['name', 'quantity', 'cost', 'price', 'minimum'].includes(campo.key)),
          ) ?? [],
      }))
      .filter((grupo) => grupo.campos.length > 0),
    setCodigo,
    buscar,
    guardar,
    cambiar: (clave: string, valor: Valor) => {
      setBorrador((actual) =>
        actual ? { ...actual, valores: { ...actual.valores, [clave]: valor } } : actual,
      );
      setErrores((actual) => ({ ...actual, [clave]: '' }));
    },
    elegirExistente: (id: string) => {
      if (!inventario || !borrador) return;
      const propio = inventario.rows.find((fila) => fila.id === id);
      const nuevo = prepararProducto(inventario, borrador.codigo, propio);
      if (!propio && resultado) nuevo.valores.name = sugerirProducto(resultado).nombre;
      setBorrador(nuevo);
      setErrores({});
    },
    cancelar: () => {
      detener();
      limpiar();
      setRetomarLectura((revision) => revision + 1);
      setMensaje('Carga descartada. El inventario conserva sus datos.');
    },
  };
}
