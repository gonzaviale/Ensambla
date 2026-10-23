# Mercado Pago: configurar las pruebas de ENS-98

La integración quedó preparada para **ARS 14.900 cada treinta días**, con primer cobro al terminar
la prueba vigente y cancelación desde Planes. El usuario creó las aplicaciones y ya dispone de
cuentas de prueba argentinas. No se hicieron cobros reales. La configuración privada conserva los
pagos deshabilitados. La instancia temporal los habilita sólo para el circuito activo de prueba;
producción permanece deshabilitada. Los circuitos cerrados tienen su renovación cancelada.

## Estado verificado del circuito de prueba gratuita (20/10/2026)

- Las claves quedaron en `application-ensambla-secrets.properties`, excluido de Git. El archivo
  `.example` conserva vacíos el token, el receptor y el número de aplicación.
- La consulta autenticada a `/users/me` confirmó una cuenta con la etiqueta `test_user`, país
  `MLA` y el mismo receptor configurado. La consulta de la aplicación confirmó que está activa
  y que su token coincide con el guardado. Estas comprobaciones fueron de lectura.
- La URL temporal de retorno HTTPS está guardada únicamente en la configuración privada.
  Apunta a `/planes`: sin sesión redirige a `/login?redirect=%2Fplanes`.
- El circuito actual usa la base independiente `ensambla_ens98_mp_e2e_20261020`. La base habitual
  no se modificó. El frontend compilado usa el modo parcial: autenticación, planes y pagos consultan
  el backend real; las funciones todavía pendientes del editor conservan sus simulaciones existentes.
- Se comprobó registro, inicio de sesión con CSRF y cookie `Secure`, y regreso a `/planes`.
  Agregar `status=approved` a la URL no activa Premium. La contratación desde Planes ya creó un
  contrato remoto por ARS 14.900 cada treinta días, conservando el fin original de la prueba.
- El usuario completó la tarjeta y la confirmación final con el comprador de prueba. La API confirmó
  `authorized`. El enlace «Ir al sitio del vendedor» volvió correctamente a Planes, que mostró la
  renovación autorizada sin conceder un período pagado ni aumentar las diez páginas de la prueba.
- El «pago aprobado» del checkout fue una operación `card_validation` por **ARS 0**. Ese recurso
  informó `live_mode=true`, pero no es una factura de Premium ni un cobro de ARS 14.900: la consulta
  de facturas de la suscripción devolvió cero resultados. No se usa ese recurso para activar acceso.
- Se canceló desde Planes y se comprobó `cancelled` en Mercado Pago y `CANCELADA` con
  `cancelacionSolicitada=true` en Ensambla. Una actualización posterior devolvió HTTP 204.
  La prueba sigue activa hasta `2026-10-27T18:46:52.704388Z`, con cero páginas usadas de diez.
  No se alteró su fin original. La baja quedó confirmada antes del primer cobro.

La instancia de retorno se ejecuta con el proveedor de IA simulado, los workers de IA, avisos y
vencimiento de pruebas deshabilitados, y producción deshabilitada. Al cerrar el circuito se
deshabilitaron nuevamente la contratación y la conciliación de pagos. Usa el origen HTTPS temporal y cookies seguras
mediante parámetros de arranque, sin reemplazar el origen habitual ni la base del archivo privado.
Importa explícitamente el archivo privado del código fuente; el JAR compilado previamente no contiene
las claves recién configuradas. Hay que revisar también los parámetros de arranque: cambiar sólo
el archivo no anula el valor de `habilitado` indicado al proceso.

La verificación de ese circuito aprobó **1.648 pruebas**, sin fallos, errores ni omitidas, y las
reglas de cobertura de las 486 clases (4.821 líneas y 1.229 ramas al 100 %). Se corrigieron diferencias comprobadas en la API real:
la fecha del primer cobro se redondea hacia arriba a segundos y se envía siempre con tres decimales
(`.000` cuando corresponde). La recuperación busca por la referencia UUID con el filtro `q`.
La consulta de facturas omite `limit`: el proveedor rechazó valores explícitos de 100 y 20 con
HTTP 400 y aceptó su tamaño por defecto. Se conserva la paginación completa mediante `offset` y
la cantidad real de resultados. Los contratos de diagnóstico y el contrato de este circuito están
cancelados. La autorización de la tarjeta fue completada por el usuario desde el checkout.

Mercado Pago informó el primer débito para el 27 de octubre a las 15:56, unos diez minutos después
del fin original de la prueba a las 15:46 (hora argentina). Ensambla conserva ambas fechas sin
adelantar un cobro ni extender la prueba. Con el contrato cancelado, esa fecha informada no autoriza
una renovación. No se comprobó un cobro posterior ni una renovación a treinta días en el proveedor.

El túnel temporal publica únicamente el frontend compilado y su proxy `/api`, no el servidor de
desarrollo con sus fuentes. Al detener `cloudflared`, la dirección deja de funcionar; una dirección
nueva requiere actualizar el retorno, el origen permitido del backend y reiniciar esa instancia.

## Continuación: factura aprobada y rechazada (20/10/2026)

El circuito anterior de prueba gratuita se conserva en `ensambla_ens98_mp_e2e_20261020`, con
renovación cancelada. Para comprobar acceso pagado sin confundirlo con los días de prueba, se
preparó `ensambla_ens98_mp_facturas_20261020` y se registró allí el comprador de prueba existente.
La cuenta local parte de `GRATUITO`, sin historial de prueba gratuita ni Premium activo.

La instancia de ese circuito usó la nueva base y habilitó temporalmente la integración, manteniendo
`produccion=false`. El archivo privado conserva `ENSAMBLA_MP_HABILITADO=false`. Se volvió a
verificar la etiqueta `test_user` del vendedor y su correspondencia con el receptor configurado.

El usuario confirmó el checkout con el comprador `Test` y la tarjeta de prueba terminada en 0604.
La API devolvió una factura procesada y un pago `regular_payment` aprobado por ARS 14.900, con
referencia y receptor correspondientes al contrato. No es la validación previa de tarjeta por cero.

La respuesta real detectó diferencias respecto de los fixtures iniciales. El pago no expone
`application_id`: se verifica la aplicación consultando el contrato y se exige que el pago enlace
esa misma suscripción. La factura puede omitir su id público numérico; el pago incluye una referencia
interna `RECURRING_INVOICE`. Son identificadores de espacios distintos, por lo que no se exige que
sean iguales. Se prefiere siempre la referencia interna cuando existe para conservar la identidad
aunque la búsqueda agregue luego su id público. Sin referencias internas se exige el id de factura
del formato anterior. Se rechazan referencias internas múltiples o sin id.

El pago de prueba también informa `live_mode=true`. Brian aprobó ajustar el control sin habilitar
producción: ese caso sólo se admite después de consultar `/users/me`, comprobar que el token sea
del receptor configurado y que la cuenta tenga `test_user`, y verificar que el comprador sea
`@testuser.com`. Un vendedor no verificado o un comprador real siguen rechazados. En producción
se exige `live_mode=true`; en pruebas se siguen admitiendo respuestas explícitas `live_mode=false`.
No se confía en prefijos de credenciales ni se cambia el plan manualmente para superar el ensayo.

Con el adaptador corregido, «Actualizar pagos» activó Premium sin intervención manual en la base.
El período comienza en `2026-10-21T00:47:59Z` y vence en `2026-11-20T00:47:59Z`: 720 horas exactas,
con cien páginas de IA mensuales y sin prueba gratuita. En hora argentina vence el 19 de noviembre
a las 21:47. Se canceló desde Planes: Mercado Pago confirmó `cancelled`, Ensambla `CANCELADA`,
y se conservó el período pagado. Otra actualización devolvió HTTP 204 y la tabla de pagos sigue
teniendo una sola fila, sin duplicar ni extender el período.

La verificación final aprobó **1.669 pruebas**, sin fallos, errores ni omitidas, con 486 clases,
4.866 líneas y 1.267 ramas cubiertas al 100 %. Incluye 74 casos del adaptador y una consulta real
de sólo lectura con ese adaptador, antes de aplicarlo al circuito de Ensambla.

La instancia actual usa `ensambla_ens98_mp_rechazada_20261020`, con integración temporal habilitada
y producción deshabilitada. El caso aprobado se conserva en su base independiente. Se registró
otra cuenta local limpia con el mismo comprador de prueba, se verificó `GRATUITO` sin prueba y se
creó una contratación pendiente por ARS 14.900 cada treinta días. El usuario completó el checkout
con tarjeta de prueba y titular `OTHE`. La pantalla mostró «No pudimos procesar tu pago».
La consulta oficial confirmó la factura pública `7033138574` en estado `recycling`, vinculada al
pago `184244828319`, estado `rejected`, detalle `cc_rejected_other_reason`, por ARS 14.900.
El contrato remoto `9587bae0522e44cbaebe00e1e01a5711` quedó `cancelled`; en la base aislada,
la renovación está `CANCELADA` y la suscripción sigue `GRATUITO`.

Ese pago rechazado omite `payer.email`. Se reprodujo el bloqueo de `validarAmbiente` y se corrigió
con autorización del usuario: sólo para un rechazo sin correo en modo de pruebas, tras verificar
el vendedor configurado con etiqueta `test_user`, se exige que el comprador del pago coincida
con el del contrato validado. La excepción no admite pagos aprobados, pendientes, reembolsados,
correos reales ni compradores ausentes o distintos. Los controles financieros y de referencia
permanecen vigentes; producción conserva su validación de ambiente.

Con el jar corregido se reutilizó el recurso existente, sin otra tarjeta ni otro intento de pago.
Dos llamadas autenticadas con CSRF a `POST /api/planes/renovacion/actualizar` devolvieron HTTP 204.
La consulta del plan devolvió `GRATUITO` sin prueba activa ambas veces. PostgreSQL conserva una
sola fila `rejected` de la factura interna `3e88ae5db3fd4f869fba04b31c446ba4`, cero pagos aprobados
y la renovación `CANCELADA`. No se activó Premium ni se modificó el plan por SQL.
La suite completa pasó **1.689 pruebas**, con 486 clases, 4.873 líneas y 1.273 ramas al 100 %;
incluye 94 casos del adaptador. La configuración privada sigue con pagos y producción deshabilitados;
la instancia temporal habilita únicamente el circuito de pruebas ya descrito.
Usar exclusivamente los datos oficiales de tarjeta de prueba: `APRO` para aprobado y `OTHE` para
rechazo general. Si el checkout rechaza sólo la autorización de la tarjeta, eso no prueba por sí
solo la existencia de una factura de suscripción rechazada; hay que consultar ambos recursos.

Fuentes: [compra de prueba de Suscripciones](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/integration-test/payment-approval)
y [tarjetas y resultados de prueba](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/additional-content/your-integrations/test/cards).
Se conservan las reglas comerciales de acceso por pago aprobado, referencia, receptor, precio y
moneda; se ajustó el adaptador a los campos observados. No se hicieron commits ni cobros reales.

## 1. Crear la aplicación y las cuentas de prueba

1. Ingresá a [Mercado Pago Developers](https://www.mercadopago.com.ar/developers) con la cuenta
   que administrará la integración. Abrí **Tus integraciones** y creá una aplicación para Ensambla.
2. Consultá la sección de **Suscripciones** de la aplicación y sus credenciales. Los nombres y
   opciones del panel pueden variar; la integración usa la API de Suscripciones (`preapproval`),
   con autorización en el checkout de Mercado Pago. No usa una preferencia de Checkout Pro.
3. Generá las cuentas de prueba necesarias para vendedor y comprador, ambas de **Argentina**,
   siguiendo el circuito de pruebas de Suscripciones indicado por Mercado Pago. Vendedor y
   comprador deben ser distintos. Usá exclusivamente los medios de pago de prueba del proveedor.
4. Identificá el **Access Token** correspondiente al vendedor de pruebas, su **User ID / Collector ID**
   y el **Application ID** de esa misma aplicación. El receptor se puede corroborar con
   `GET https://api.mercadopago.com/users/me` usando ese token, sin compartirlo en el chat.

En Suscripciones, el panel indica usar las credenciales productivas de una **cuenta de prueba**.
Se obtienen desde la aplicación creada dentro de la sesión del vendedor de prueba, nunca desde
la cuenta personal. Un enlace `mpago.la` que abre `/subscriptions/checkout?preapproval_plan_id=...`
identifica un checkout de un plan, no el panel de la aplicación ni sus credenciales. Ensambla usa
suscripciones sin plan asociado para conservar la fecha original de fin de la prueba.

No deducimos el ambiente por el prefijo `TEST-` o `APP_USR-`. Con `ENSAMBLA_MP_PRODUCCION=false`
se acepta `live_mode=false`, o `live_mode=true` exclusivamente con vendedor de prueba verificado
mediante `/users/me` y comprador `@testuser.com`. El circuito concreto de cuentas y credenciales
debe corresponder a **Suscripciones**, no al de otro producto de Mercado Pago.

Fuentes: [Suscripciones](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/overview),
[cuentas de prueba](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/additional-content/your-integrations/test/accounts),
[pruebas por producto](https://www.mercadopago.com.ar/developers/es/news/2023/11/16/Questions-on-how-to-test-your-integration--).

## 2. Guardar la configuración solamente en el backend local

Copiá `backend/src/main/resources/application-ensambla-secrets.properties.example` como
`application-ensambla-secrets.properties`, si todavía no existe. Si existe, agregá las claves de
Mercado Pago conservando el contenido anterior. Ese archivo ya está excluido de Git.

```properties
ENSAMBLA_MP_HABILITADO=false
ENSAMBLA_MP_PRODUCCION=false
ENSAMBLA_MP_ACCESS_TOKEN=
ENSAMBLA_MP_COLLECTOR_ID=
ENSAMBLA_MP_APPLICATION_ID=
ENSAMBLA_MP_RETURN_URL=
ENSAMBLA_MP_NOTIFICATION_URL=
ENSAMBLA_MP_WEBHOOK_SECRET=
```

Completá token y ambos identificadores con los valores de la misma aplicación de pruebas.
No los pongas en archivos del frontend, capturas, commits ni mensajes. No necesitamos una clave
pública en el navegador: la autorización del medio de pago ocurre en el checkout alojado.

Durante esta etapa usá una **base PostgreSQL independiente para las pruebas**, nunca la base
habitual por accidente. La validación local del desarrollo usa `ensambla_ens98_local_20261020`;
sus operaciones con proveedor ficticio no sirven como evidencia de pagos en Mercado Pago.
Para el circuito externo conviene otra base de pruebas limpia.

## 3. Preparar el retorno HTTPS

Para probar el circuito completo desde la PC se necesita una dirección HTTPS accesible que llegue
a la aplicación local. Puede ser un túnel temporal, sin desplegar ni publicar el repositorio.
Ya se abrió un túnel temporal para verificar el retorno; no se desplegó el proyecto.

`ENSAMBLA_MP_RETURN_URL` debe llevar a la pantalla **`/planes`** de ese entorno.
El backend exige HTTPS al habilitar los pagos. El proxy `/api` del frontend debe seguir apuntando al
backend y el origen de `FRONTEND_URL` debe coincidir con el que se use para iniciar sesión.
Si se prueba por HTTPS, configurar también la cookie segura según el entorno.

Deshabilitar la integración en configuración no cancela contratos que ya existan en Mercado Pago:
para detener sus renovaciones debe confirmarse la baja en el proveedor.

## 4. Activar conciliación y, cuando estén disponibles, notificaciones firmadas

Al completar token, receptor, aplicación y retorno, cambiá `ENSAMBLA_MP_HABILITADO=true` y reiniciá
el backend. La conciliación consulta contratos y facturas cada minuto. Desde Planes también se
puede usar **Actualizar pagos**; ni volver del checkout ni autorizar la suscripción habilitan pago.

Las notificaciones son opcionales para la conciliación. La documentación de Mercado Pago presenta
una particularidad para Suscripciones: indica configurar la URL durante la creación del recurso.
Cuando la aplicación ofrezca la firma secreta y comprobemos su envío en este producto, completar:

```properties
ENSAMBLA_MP_NOTIFICATION_URL=https://TU_ENTORNO_DE_PRUEBA/api/pagos/mercadopago
ENSAMBLA_MP_WEBHOOK_SECRET=VALOR_PRIVADO_DE_LA_APLICACION
```

Si se indica una URL de notificación, la configuración exige HTTPS y secreto. Si se dejan ambos
campos vacíos, se trabaja mediante conciliación autenticada a la API. No se aceptan notificaciones
sin firma ni se habilita Premium desde el contenido de un webhook. Nunca inventar una clave
esperando que Mercado Pago la utilice automáticamente.

Eventos implementados: `subscription_preapproval`, `subscription_authorized_payment` y `payment`.
La firma valida `data.id`, `x-request-id` y `ts` mediante HMAC-SHA256. Después se consulta el recurso
en una URL fija de la API y se comprueba receptor, aplicación, ambiente, importe, moneda y referencia.
[Documentación oficial](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/additional-content/your-integrations/notifications/webhooks).

## 5. Recorrer estas pruebas antes de considerar validado el proveedor

1. Registrar un negocio en la base de pruebas y comenzar su prueba Premium. Para el primer circuito,
   usá el email del comprador de pruebas de Mercado Pago, evitando mezclarlo con el vendedor o con
   una cuenta real: ese email es el que enviamos como `payer_email`.
2. Contratar durante la prueba: verificar ARS 14.900, frecuencia **30 / days** y primer cobro en
   su fecha original de fin, sin otros siete días ni páginas nuevas. Verificar también el caso
   sin prueba vigente y las fechas cercanas al vencimiento.
3. Autorizar desde Mercado Pago usando el comprador y medio de pago de prueba. Debe seguir
   figurando como prueba hasta su fin; una autorización todavía no es un pago aprobado.
4. Comprobar en Mercado Pago la fecha del primer cobro y las siguientes. El primer pago aprobado
   habilita su período de treinta días; pendientes/rechazados no lo habilitan. La cuota de IA
   sigue siendo mensual UTC, independiente del ciclo de cobro.
5. Repetir una actualización o notificación: no debe extender el mismo período otra vez.
6. Cancelar desde Planes antes del primer cobro y comprobar la baja en Mercado Pago. Probar también
   cancelar con un período pagado: conserva acceso hasta vencer. Actualizar luego de la baja.
7. Probar rechazo, pago pendiente, pérdida de respuesta, cobro posterior y reembolso completo con
   las herramientas del ambiente de pruebas. Si el proveedor no permite acelerar los treinta días,
   no marcar una renovación real como comprobada sólo por verla en el simulador local.

Ya se comprobó la creación externa con frecuencia de treinta días y formato/precisión de `start_date`.
También se comprobaron autorización, retorno y cancelación desde Planes antes del primer débito.
También se comprobó una factura de prueba aprobada, su período de acceso y la cancelación posterior
sin perderlo. Se comprobó también un pago de factura rechazado y que el negocio limpio conserva
Gratuito, con renovación cancelada y sincronización del rechazo sin correo completada.
El 21/10/2026 se completó la devolución externa desde el panel del vendedor de prueba, después
de que la API de devolución hubiera respondido HTTP 401. La API confirmó el reembolso total y
la conciliación devolvió el negocio a Gratuito. No se sustituyeron las claves por las de una
cuenta real ni se modificó el plan manualmente.

También se verificó la entrega firmada del simulador del panel de Mercado Pago para el pago
ya devuelto: dos envíos respondieron HTTP 200 y conservaron una única factura y el plan Gratuito.
Entre ambos hubo un HTTP 502 de causa no determinada; se recuperó con un único reintento.
Los avisos sin firma y con firma inválida respondieron HTTP 401. La firma permanece en el archivo
privado local; la URL del túnel es temporal y debe renovarse si cambia o termina el proceso.

Luego se comprobó un evento automático `subscription_preapproval` al cancelar desde Planes una
nueva solicitud pendiente sin tarjeta ni autorización de pago. Se configuró la misma dirección
temporal en ambas pestañas del panel de la aplicación del vendedor de prueba; «Modo productivo»
es el nombre de una pestaña de esa cuenta de prueba y no implica cambiar a un vendedor real.
El receptor validó la firma y respondió HTTP 200 en 453 ms. El nuevo contrato quedó cancelado,
sin facturas, y el negocio permaneció en Gratuito. No se usó el simulador para ese evento.

Pendiente: la segunda renovación real a los treinta días y su aviso de pago. La entrega automática
comprobada corresponde a una cancelación de suscripción, sin cobro.
El servidor local y las pruebas automatizadas validan nuestra implementación del contrato; no
demuestran que una cuenta concreta de Mercado Pago ya esté habilitada para cobrar.

Para continuar, alcanza con avisar que creaste la aplicación y en qué paso estás. Las claves quedan
en tu configuración local. No hace falta pegarlas en el chat. Producción y cobros reales son otra etapa.

## 6. Probar la segunda renovación sin esperar treinta días

`RenovacionPremiumIntegrationTest` permite recorrer el ciclo con un `Clock` controlado y PostgreSQL
aislado de Testcontainers. No modifica el reloj de Windows ni los contratos del vendedor de prueba.
Las consultas al proveedor usan respuestas simuladas; la firma HMAC se genera y valida con una
clave exclusiva del test, sin leer las claves privadas de la aplicación.

El caso aprobado parte de una primera factura que cubre los días 0 a 30. Simula una segunda
factura con débito en el día 30 y entrega su aviso en el día 32 mediante el receptor HTTP de
pruebas, tanto para `subscription_authorized_payment` como para `payment`. Dos entregas del
mismo aviso deben dejar dos facturas en total y acceso hasta el día 60, sin agregar días por
la demora o por repetir el evento.

Los otros dos casos envían el aviso firmado de la segunda factura en estado `pending` o
`rejected` al llegar al día 30. Debe conservarse el historial del primer pago y quedar Gratuito,
sin conceder un nuevo período. Los casos existentes cubren también los días anteriores al
vencimiento, durante los cuales se conserva el acceso ya pagado.

Para repetir estos casos, con Docker activo y desde `backend/`:

```powershell
.\mvnw.cmd "-Dtest=RenovacionPremiumIntegrationTest" test
```

Este circuito comprueba el comportamiento de Ensambla ante una segunda factura. No genera ni
demuestra una segunda factura automática de Mercado Pago; esa comprobación externa sigue pendiente.
