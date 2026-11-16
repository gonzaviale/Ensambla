# ENS-327: entrega y protocolo de escaneo remoto

## Base y entregas

Actualización de alcance: por indicación de Nach0, el emparejamiento celular–PC (ENS-328 a ENS-331)
queda postergado. El protocolo de abajo es sólo una propuesta; no hay endpoints ni WebSockets
implementados ni se habilita esa opción en la aplicación. El trabajo activo es presentación mobile,
accesibilidad y accesos rápidos. El enlace para probar la aplicación desde el celular es un tema
distinto del emparejamiento y no autoriza cambios de Caddy, pipelines, deploy o runtimes.

Rama: feat/ENS-327-caja-mobile. Base dev actualizada, con la dependencia local del PR #93.
No publicar un PR que vuelva a incluir Caja completa: integrar primero #93 a dev y actualizar esta rama.
Todos los PR futuros tienen destino dev. No se modifican workflows, deploy, Caddy, runtimes ni políticas globales de migración.

1. Acceso compartido a Caja y al lector del inventario del negocio activo (ENS-334 y componente reutilizable ENS-315).
2. Responsive y accesibilidad de Caja, cierre y pantallas de presentación (ENS-332/333).
3. Contrato, backend y pantallas de emparejamiento (ENS-328 a ENS-331).
4. Prueba física Android, Safari y redes distintas, con guía y hallazgos (ENS-335).

## Accesos rápidos implementados

Flotante inferior izquierdo para convivir con Sami a la derecha. Abre Nueva venta y los inventarios
del negocio activo. No mezcla inventarios de otros negocios. No aparece durante la venta, la carga,
un modal ni fuera de la aplicación autenticada. Acciones de 48 px, texto de 14 px, margen de área segura
y espacio al pie del contenido. Escape y cierre devuelven el foco; abrir enfoca la primera acción.
Cambiar de ruta cierra el panel. Sin inventarios, explica qué falta.

Verificar en un teléfono que no tape contenido ni controles al aparecer el teclado; la prueba
automática de navegación no sustituye esta aceptación física.

## Protocolo propuesto para ENS-328 (pendiente de implementación y revisión)

La sesión de escaneo es un recurso efímero en memoria. No cambia la sesión completa del celular.
Queda ligada al usuario, NegocioId de la sesión autenticada, proyecto e inventario validados
contra los recursos del negocio, y al destino INVENTARIO o VENTA.

### HTTP

POST /api/escaneo/sesiones, con sesión y CSRF del escritorio:
request: {proyectoId, inventarioId, destino}
response 201: {sesionId, tokenEmparejamiento, venceTokenEn, venceSesionEn}
Token aleatorio de un solo uso: 60 segundos. Sesión: 15 minutos. Conservar sólo huellas de los tokens.

El QR abre /escaner-remoto#token=... . El fragmento evita enviar el token al servidor HTTP en la URL.
El celular retira inmediatamente el fragmento del historial y canjea el token en el cuerpo de
POST /api/escaneo/sesiones/canjear:
request: {token}
response: {sesionId, credencial, venceSesionEn}.
La credencial sólo sirve para enviar códigos a esa sesión. No permite leer productos, ventas,
clientes ni otros datos del negocio. No se escribe en localStorage ni se registra en logs.

DELETE /api/escaneo/sesiones/{sesionId}, con sesión y CSRF del propietario, revoca la sesión.
Recurso ajeno: 404. Token inválido/usado/vencido: respuesta genérica sin enumerar sesiones.
Antes de canjear debe comprobarse Origin permitido. No usar CORS abierto ni comodines.

### WebSocket

Ruta prevista /api/escaneo/ws. Autenticación como primer mensaje, nunca credenciales en la URL:
escritorio: {tipo:"escritorio", sesionId}; cookie autenticada, Origin permitido y propietario verificado.
celular: {tipo:"celular", sesionId, credencial}; no depende de cookies completas.
Cerrar conexiones sin autenticación válida en un plazo corto. Una sola pareja por sesión.

Lectura: {tipo:"lectura", secuencia:1, codigo:"7790895000997", formato:"EAN_13"}.
Respuesta: {tipo:"confirmacion", secuencia:1}.
Estado: {tipo:"estado", estado:"conectado"|"desconectado"|"cerrado"|"vencido"}.
Latido: {tipo:"ping"} / {tipo:"pong"}.

Validar GTIN con CodigoBarras en el servidor. Límite propuesto: 10 lecturas por segundo.
Deduplicar por sesión y secuencia, no por código: dos lecturas intencionales del mismo producto
son válidas. Confirmar la recepción y reenviar tras reconectar sólo las secuencias sin confirmar.
No confirmar como agregada a la compra una lectura que el flujo de venta todavía no aceptó.

Reconectar con espera creciente y límite de intentos, sin superar la caducidad. Mostrar el estado
y ofrecer emparejar de nuevo al vencer. Cerrar desde cualquiera de los dispositivos revoca ambas
conexiones, libera la cámara y borra la credencial del celular.
Al cambiar de negocio, cerrar la sesión anterior. Un mensaje nunca se publica a otro negocio.

## Implementación e infraestructura

Dominio sin Spring, ids tipados, Clock en casos de uso y tiempos pasados a reglas/servicios.
Registro concurrente en memoria, limpieza de sesiones caducadas, límite de sesiones por propietario
y pruebas de expiración, reuso de tokens, revocación, reconexión y aislamiento.
Manejador WebSocket y configuración del módulo en infrastructure.

Verificar primero el proxy existente para WebSocket. No cambiar Caddy, deploy o pipelines como
parte implícita de la historia. Si el entorno requiere un ajuste de proxy para probarlo, presentar
el archivo y el impacto por separado antes de modificarlo. No agregar etapas ni disparadores de CI.

## Aceptación pendiente

### Revisión de presentación realizada

- Navegador a 375 px: Inicio, Avisos, Objetivos, Zona y Planes sin desborde horizontal de página.
  Se corrigieron las evidencias del aviso económico del inicio, que excedían el ancho disponible.
- Cabecera compacta a 375 px: símbolo de marca, controles y selector de negocio sin quitar navegación.
  Botones del menú medidos en 46 px de alto.
- Compra de dos artículos a 375×812 y 768×1024: página y lista sin scroll adicional.
- Componente real de cobro en un escenario visual aislado: 375×667 y 768×1024, sin desborde
  horizontal ni scroll del cuerpo, incluso con pago parcial y mixto. Confirmación visible al pie.
- Los diálogos incluyen los selectores en el ciclo de Tab, omiten controles deshabilitados,
  conservan el foco al cambiar el estado de guardado y restauran el foco al cerrar.
  Su altura se ajusta al viewport visual disponible; falta comprobar el teclado físico de Safari y Android.
- El flotante se oculta ante un diálogo o al editar texto en mobile y cierra sus acciones al cambiar de negocio.

La demo completa quedó en «Guardando inventario» al preparar Caja: no se validó el cobro de punta a punta
en ese recorrido. La revisión visual aislada no demuestra persistencia de una venta ni sustituye
la prueba con backend. No se modificó la sincronización ni se atribuyó una causa sin confirmarla.

Android Chrome e iPhone Safari reales: cámara, orientación, teclado, enfoque, desconexión, caducidad,
lecturas repetidas, varias tablas y cambio de negocio. Emparejar en redes distintas y comprobar
que revocar impide enviar nuevos códigos. Nunca marcar ENS-335 como terminada con mocks.
