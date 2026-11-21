# Caja mobile: alcance activo de ENS-327

Los ajustes se integran al PR #93 de Caja, con destino dev e incluyendo el merge del lector de inventario del PR #88.
Se conservan clientes, deudas, pagos parciales, pagos mixtos y cierres de ENS-317.

## Alcance

- ENS-332: presentación responsive y accesibilidad de Caja, compra, cobro y cierre.
- ENS-333: revisión mobile de las pantallas de la presentación.
- ENS-334: un solo acceso flotante para Nueva venta y Escanear inventario.
- ENS-328, ENS-329, ENS-330, ENS-331 y ENS-335: postergadas por indicación de Nach0.

El emparejamiento celular-PC no se implementa ni se ofrece en la aplicación. Abrir la aplicación directamente
con el teléfono y usar su propia cámara es independiente del emparejamiento.
No se modifican Caddy, pipelines, deploy, runtimes ni políticas globales de migración.

## Comportamiento integrado

El flotante inferior izquierdo convive con Sami a la derecha. Ofrece Nueva venta y los inventarios del
negocio activo. No aparece en login, durante la venta, durante la carga de inventario ni con un modal abierto.
En mobile también se oculta al editar texto. Abrir enfoca la primera acción; Escape restaura el foco;
salir con el teclado o cambiar de negocio cierra el panel.

El lector del inventario conserva la presentación mergeada en #88: visor, código manual y ajustes.
Caja conserva lectura continua, sonidos, borradores y confirmaciones antes de descartar. El guardado del
sistema conserva la protección de ventas, cierres y cobros frente a escrituras de una copia desactualizada.

Los diálogos de Caja incluyen los selectores en el recorrido de Tab, omiten controles deshabilitados,
restauran el foco al cerrar y ajustan su altura al viewport visual disponible.

## Revisión realizada y límites

- A 375 px se revisaron Inicio, Avisos, Objetivos, Zona y Planes sin desborde horizontal de página.
  Se corrigió el desborde de las evidencias del aviso económico del inicio.
- Cabecera compacta a 375 px, manteniendo navegación y selector del negocio. Menú con botones de 46 px.
- Compra de dos artículos a 375x812 y 768x1024: página y lista sin scroll adicional.
- Cobro en un escenario visual aislado a 375x667 y 768x1024: cuerpo sin scroll ni desborde horizontal,
  incluso con pago parcial y mixto, y confirmación visible al pie.
- La demo completa quedó en «Guardando inventario» al preparar Caja. La revisión visual aislada no
  valida la persistencia de una venta ni sustituye el recorrido con backend.

Queda completar la revisión de textos y controles de Caja, arqueo/cierre, estados del escáner y pruebas
con cámara y teclado reales en Android y Safari. No se consideran cerradas ENS-332/333/334 por estas
comprobaciones parciales. Las capturas y los informes temporales de validación no se incorporan al repositorio.
