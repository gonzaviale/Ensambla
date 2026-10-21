# ENS-98 · Renovación automática de Premium

Diseño aprobado por Brian el 20 de octubre de 2026: ARS 14.900 finales cada **30 días**,
primer cobro al terminar la prueba vigente y cancelación desde Planes. Todo en local, sin commits.

## Alcance y decisiones

Se integra Suscripciones de Mercado Pago sin plan asociado y con checkout alojado (`preapproval`,
estado inicial `pending`). El backend conserva el precio y la referencia por negocio; no recibe
importe, moneda, negocio ni fechas elegidas por el navegador. La prueba sigue teniendo diez páginas
totales; autorizar la renovación no inicia un período pago ni reinicia la prueba. Si la prueba ya
terminó o no existe, el primer cobro se solicita desde la contratación. Cada factura corresponde
a treinta días desde su fecha de débito; un webhook tardío no concede treinta días nuevos.
La cuota de cien páginas de IA sigue siendo mensual UTC, independiente del ciclo de cobro.

Se persiste el contrato antes de llamar a Mercado Pago. Una creación con respuesta perdida queda
en preparación y se busca por su referencia; no se vuelve a crear a ciegas. Un único contrato abierto
por negocio impide duplicados. Las llamadas externas quedan fuera del bloqueo transaccional.

Mientras exista una contratación abierta no se permite iniciar después una prueba gratuita:
el cobro ya preparado podría anticiparse a su fin. Ambos caminos comparten el bloqueo de negocio;
si la prueba empieza primero, la contratación respeta su vencimiento. Si se cancela una contratación
sin pagos y todavía no se utilizó la prueba, vuelve a estar disponible. Al consultar el contrato se
prioriza el abierto sobre el historial, incluso si comparten la misma fecha de creación.

Sólo un pago aprobado, consultado desde la API, con referencia, receptor, aplicación, ambiente,
importe, moneda, factura y contrato coincidentes habilita el período pago. Autorización y redirección
no conceden acceso. Facturas y pagos únicos, períodos persistidos y el bloqueo de negocio evitan
extensiones duplicadas. Una renovación rechazada no elimina un período pagado vigente.
Reembolsos/contracargos completos retiran exclusivamente el período afectado; se conservan datos.

Cancelar registra primero la intención, luego pide la baja a Mercado Pago. Mientras el proveedor
no confirma, se muestra cancelación pendiente y se reintenta sincronizando. La baja no elimina
prueba ni tiempo pagado restante. Una respuesta antigua no deshace una cancelación solicitada.

Notificaciones: ruta exclusiva fuera de sesión/CSRF, verificación HMAC y consulta posterior del
recurso en la API. Un proceso periódico reconcilia contratos y facturas aunque falte el webhook.
No se aceptan URLs externas suministradas por una notificación. Fallas externas se informan sin
exponer credenciales y sin inventar aprobación. Se mantiene historial de contratos cancelados.

## Plan de implementación

1. V15 nueva: precio aprobado, contratos y pagos con índices, claves compuestas e idempotencia.
2. Modelos, puerto de pasarela, persistencia y servicios de preparación, cancelación y conciliación.
3. Casos de uso con Clock, adaptador HTTP de Mercado Pago, configuración deshabilitada por defecto,
   notificaciones y reconciliación. Sin secretos en código, frontend o logs.
4. Contrato y hooks del frontend; contratación, estado, próxima fecha y cancelación en Planes.
   Reutilizar la confirmación accesible y marca de ENS-97.
5. Tests por capa, concurrencia y PostgreSQL real, proveedor HTTP local controlado, seguridad,
   regresión de ENS-94–97, frontend y navegador en escritorio/celular.
6. Documentación y guía de configuración. Brian todavía no creó la aplicación de Mercado Pago:
   la prueba externa con cuentas del proveedor queda identificada como pendiente, sin cobros reales.

## Fuentes consultadas

- [Creación con checkout pendiente](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/integration-configuration/subscription-no-associated-plan/pending-payments)
- [API de suscripciones](https://www.mercadopago.com.ar/developers/es/reference/online-payments/subscriptions/create-preapproval/post)
- [Facturas](https://www.mercadopago.com.ar/developers/es/reference/online-payments/subscriptions/authorized-payment-search/get)
- [Cancelación](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/subscription-management)
- [Notificaciones y tópicos](https://www.mercadopago.com.ar/developers/es/docs/subscriptions/additional-content/your-integrations/notifications/webhooks)

La documentación de notificaciones presenta una particularidad para Suscripciones: indica URL
durante la creación del recurso y también enumera eventos y firmas. Debe verificarse en las cuentas
de prueba qué notificaciones firma la aplicación; no se relaja la validación para compensarlo.
La conciliación autenticada a la API conserva el funcionamiento sin depender de esa entrega.

## Validación local y límites de esta entrega

Se verificó el contrato mediante HTTP real hacia un servidor ficticio en `127.0.0.1:8198`, backend
Spring Boot y PostgreSQL `ensambla_ens98_local_20261020`. Se reutilizó el contenedor existente,
sin recrearlo ni alterar la base habitual. Flyway aplicó V1–V15 sólo en la base aislada.

En navegador se registró `ens98.local@ensambla.test`, se activó la prueba y se contrató respetando
su fecha de fin (redondeada hacia arriba a milisegundos). Autorizar el contrato no cambió la prueba;
cancelarlo confirmó la baja sin eliminar sus días. Luego se preparó una prueba vencida exclusivamente
para esta cuenta en la base aislada y se creó un segundo contrato. Un pago ficticio aprobado habilitó
un período de treinta días y cien páginas mensuales. Cancelar mantuvo ese período hasta su fin.
Recargar conservó el estado. Se revisaron escritorio de 1.280 px y celular de 390 px, sin desbordamiento,
contraste y confirmaciones. Las capturas están fuera del repositorio en `.playwright-mcp`.

El adaptador valida contra receptor y aplicación configurados, referencia UUID del contrato, importe,
moneda y ambiente `live_mode`. Consulta los pagos de todas las páginas de facturas. El ciclo se calcula
desde `debit_date`, no desde la llegada del webhook. Un pago tardío habilita sólo el período facturado,
sin regalar otro ciclo por demoras de entrega. Los estados originales de pagos se conservan para auditoría.

La creación incierta se recupera por referencia; si no aparece ningún contrato, permanece en preparación
y no vuelve a enviar un POST a ciegas. Si Mercado Pago confirma que no lo creó, esa operación requiere
revisión antes de permitir otra contratación. Una cancelación incierta queda pendiente y se reintenta.

En la validación inicial con proveedor ficticio no se abrió ningún túnel, no se configuraron cuentas
externas ni se invocó la API real de Mercado Pago. Esa pasarela ficticia quedó detenida.
El siguiente paso es la [guía de configuración local](mercado-pago-configuracion-local.md), incluyendo
validación de `start_date` y firma de notificaciones. Habilitar producción no forma parte de esta entrega.

Frontend: **297 pruebas en 29 archivos aprobadas**, además de lint, tipos y build de producción.
Se corrigió una carrera en el test existente del tutorial: esperaba el texto del paso cuando la
navegación todavía estaba pendiente y pulsaba el logo de una pantalla que se desmontaba. Ahora espera
que termine esa navegación, sin aumentar los tiempos de espera ni modificar el comportamiento del tutorial.

Backend final: `mvnw.cmd -B clean verify`, **1.644 pruebas**, sin fallos, errores ni omitidas.
**486 clases, 4.818 líneas y 1.229 ramas** al 100 % por clase. Incluye el bloqueo de inicio de prueba
con una contratación abierta y la selección del contrato actual con fechas empatadas.
Evaluador: **39 pruebas aprobadas** con el jar recién compilado, sin proveedor de IA real.
La rama sigue en el mismo HEAD de ENS-94–96; ENS-97 y ENS-98 permanecen sin commits ni push.

### Validación externa con cuentas de prueba (20/10/2026)

Se verificaron las credenciales del vendedor argentino de prueba mediante consultas autenticadas.
El retorno HTTPS funciona con una base PostgreSQL independiente y cookies seguras. Para el circuito
visual se utiliza el modo parcial: autenticación, planes y pagos van al backend; el editor y las
preferencias pendientes conservan sus simulaciones existentes.

La primera contratación detectó una diferencia real del proveedor: `start_date` vuelve sin
fracciones de segundo. El redondeo inicial a milisegundos hacía fallar la comparación estricta
después de que Mercado Pago creara el contrato pendiente. El primer cobro ahora redondea hacia
arriba a segundos completos, manteniendo intacto el fin de la prueba y sin adelantar cargos.
Los tests incluyen fracciones de un nanosegundo hasta casi un segundo y reproducen el fallo previo.
Además, el proveedor exige tres cifras decimales en el texto de la fecha incluso cuando son ceros.
El adaptador emite siempre el formato `uuuu-MM-dd'T'HH:mm:ss.SSSXXX`; enviar un segundo completo
sin `.000` devolvió HTTP 400. El contrato HTTP tiene una aserción explícita de esa representación.

La recuperación de contratos consulta el filtro documentado `q` con la referencia UUID y limita
a dos resultados para rechazar ambigüedades. La validación de identidad y referencia sigue siendo
estricta. La búsqueda por correo permitió localizar el primer contrato durante el diagnóstico;
éste y el contrato técnico de comparación quedaron cancelados en Mercado Pago, sin autorizar tarjetas.
La preparación del segundo intento en la base aislada se cerró manualmente después de verificar
el rechazo por formato HTTP 400 y que no existía recurso remoto. No se automatiza ese cierre ante
errores inciertos. La siguiente contratación conserva el fin original de la prueba del mismo negocio.

Las 95 pruebas específicas de servicio y adaptador pasan con las correcciones. La verificación
final completa aprobó **1.648 pruebas**, sin fallos, errores ni omitidas, con las reglas de cobertura
de las 486 clases satisfechas. La contratación desde Planes creó correctamente un contrato remoto
por ARS 14.900 cada treinta días. El usuario completó la tarjeta de prueba y la confirmación final:
la API devolvió `authorized` y el enlace del checkout abrió Planes, que mostró la renovación autorizada
sin extender la prueba ni aumentar sus diez páginas. El recurso de pago mostrado en el checkout es
`card_validation`, aprobado por importe cero; no es una factura de Premium. La consulta de facturas
de la suscripción devolvió cero resultados. El primer débito informado por Mercado Pago quedó para
el 27 de octubre a las 15:56, después del fin original de la prueba a las 15:46 (hora argentina).

La consulta real de facturas detectó otra diferencia: el parámetro `limit` explícito devolvía HTTP 400
(`Invalid value for limit`), tanto con 100 como con 20. Sin ese parámetro, la API respondió HTTP 200
con su tamaño de página por defecto. El adaptador conserva la paginación por `offset` y la cantidad
real de resultados hasta cubrir `paging.total`, sin imponer el límite. El test de paginación reprodujo
el fallo antes de corregirlo. La suite final volvió a aprobar las 1.648 pruebas, con 486 clases,
4.821 líneas y 1.229 ramas cubiertas al 100 %.

La cancelación se confirmó desde Planes. Mercado Pago devolvió `cancelled`; Ensambla conservó
`CANCELADA` y la solicitud de baja. Una conciliación manual posterior devolvió HTTP 204, sin
facturas de suscripción. Se comprobó la misma fecha original de prueba, cero páginas usadas y
límite total de diez. Al finalizar se deshabilitaron nuevamente los pagos en la instancia local.
No se efectuaron cobros reales ni commits. Al cerrar ese circuito quedaban pendientes los escenarios externos de factura
aprobada/rechazada, renovación posterior, reembolso y entrega/firma de notificaciones; los escenarios
con proveedor ficticio y las pruebas automatizadas no los reemplazan.

### Factura de prueba aprobada y acceso pagado (20/10/2026)

Se creó un negocio en `ensambla_ens98_mp_facturas_20261020`, sin prueba gratuita, y se contrató
mediante el checkout existente con el comprador y la tarjeta de prueba. Mercado Pago procesó una
factura de ARS 14.900 con pago aprobado, distinto de la validación anterior por importe cero.

El adaptador inicial rechazaba el formato real: la búsqueda podía omitir el id de factura, el pago
no incluía `application_id` y devolvía `live_mode=true` aun con vendedor de prueba. Brian confirmó
el ajuste local: verificar la aplicación desde el contrato, exigir el enlace del pago a esa suscripción
cuando falta su aplicación y admitir el ambiente de prueba en ese formato únicamente tras confirmar
el receptor con `/users/me`, su etiqueta `test_user` y el email de prueba del comprador.
Referencia, importe, moneda, receptor e identidad del contrato siguen siendo obligatorios.

La factura tiene un id público numérico y una referencia interna `RECURRING_INVOICE` dentro del pago;
pueden tener valores distintos. Se usa la referencia interna cuando existe para mantener una identidad
estable aunque una consulta posterior incluya también el id público. Sin referencia interna se exige
el identificador del formato anterior. Referencias internas ambiguas o sin id impiden conciliar.
Los tests reprodujeron los fallos antes de corregirlos; una consulta real de sólo lectura verificó
el pago con el adaptador corregido antes de probar su aplicación en Ensambla.

«Actualizar pagos» activó Premium, sin cambiar el plan por SQL, durante 720 horas desde la fecha
de débito (`2026-10-21T00:47:59Z` a `2026-11-20T00:47:59Z`), con cien páginas mensuales de IA.
La cancelación desde Planes se confirmó en ambos sistemas y conservó el período pagado. Repetir
la actualización devolvió HTTP 204, mantuvo una única fila de pago y no extendió la fecha de fin.

Verificación final: **1.669 pruebas**, cero fallos, errores u omitidas; 486 clases, 4.866 líneas
y 1.267 ramas al 100 %. El rechazo se preparó en `ensambla_ens98_mp_rechazada_20261020`, sin
pagos ni prueba gratuita previos.

### Pago de factura rechazado y sincronización sin correo (20/10/2026)

Tras completar el checkout, Mercado Pago mostró «No pudimos procesar tu pago». La API confirmó
la factura `7033138574` en estado `recycling`, con pago `184244828319` rechazado por
`cc_rejected_other_reason`, importe ARS 14.900. El contrato remoto quedó `cancelled` y la renovación
local `CANCELADA`; la suscripción del negocio sigue `GRATUITO`.

El adaptador inicialmente bloqueaba ese pago porque la respuesta omite `payer.email` y
`validarAmbiente` exigía el correo para admitir `live_mode=true` con vendedor de prueba.
La consulta real y cuatro regresiones automatizadas reprodujeron el fallo antes de corregirlo.

Brian autorizó corregir este formato manteniendo todo local. El ajuste se limita al modo de
pruebas y al estado exacto `rejected` sin correo: sigue verificando al vendedor configurado con
etiqueta `test_user` y exige que `payer.id` coincida con `payer_id` del contrato ya validado.
No admite por esta excepción correos reales, compradores ausentes o distintos, ni pagos
aprobados, pendientes o reembolsados. Las demás validaciones de contrato, factura, importe,
moneda y referencia permanecen vigentes.

La consulta de sólo lectura con el adaptador corregido verificó el pago `rejected` por ARS 14.900.
Tras reiniciar la instancia aislada, dos actualizaciones autenticadas con CSRF devolvieron HTTP 204.
El plan permaneció `GRATUITO`, sin prueba activa, y la renovación `CANCELADA`. PostgreSQL confirmó
una única fila rechazada de la factura interna `3e88ae5db3fd4f869fba04b31c446ba4`, cero pagos
aprobados y ninguna concesión de acceso Premium. Se reutilizó el pago existente sin otro checkout.

Verificación final: **1.689 pruebas**, sin fallos, errores u omitidas; 486 clases, 4.873 líneas y
1.273 ramas al 100 %, con 94 casos del adaptador. Se conservan los controles para correos reales,
compradores ausentes o distintos, vendedores no verificados y estados que podrían conceder acceso.

También siguen pendientes renovación posterior, reembolso y notificaciones firmadas.
Sin cobros reales, commits ni push.

### Cierre de pruebas locales y publicación autorizada (21/10/2026)

Se agregaron cinco casos de integración contra PostgreSQL: el siguiente cobro pendiente o rechazado
conserva los días ya pagados; al vencer el período anterior un rechazo deja el negocio en Gratuito
sin borrar sus proyectos; devolver una factura futura conserva el período actual; devolver una
factura anterior conserva el nuevo período aprobado. La base previa a la actualización remota
aprobó 1.694 pruebas, sin fallos, errores ni omitidas, y verificó las reglas de cobertura.

El ensayo externo de reembolso del pago aprobado se detuvo: tras verificar vendedor y comprador
de prueba, referencia, importe y contrato cancelado, la API devolvió HTTP 401 con
`Unauthorized use of live credentials`. No se confirmó ninguna devolución y no se cambiaron las
claves por credenciales de una cuenta real. Una consulta previa había recibido HTTP 429.
El pago conserva su estado aprobado; esta limitación no se considera un reembolso probado.

Una petición HTTP sin firma al receptor devolvió 401 aunque el cuerpo afirmara un pago aprobado;
el estado persistido no cambió. La firma válida está cubierta por pruebas locales, pero no se
verificó todavía una entrega firmada por Mercado Pago. El panel abierto correspondía a otra
aplicación y el acceso a la configurada falló; no se sustituyeron claves ni se desactivó la firma.

La segunda factura a los treinta días, el reembolso externo y la entrega de notificaciones del
proveedor siguen pendientes. Los contratos de los circuitos terminados permanecen cancelados;
no se reactivan ni se cambian sus fechas para simular un cobro posterior. Esta entrega publica
el desarrollo y las comprobaciones locales; no habilita cobros reales ni declara validada producción.

Brian autorizó commits explicativos y push el 21/10/2026 con su cuenta `Sur4x`. Antes de publicar
se integraron los doce commits remotos de la rama, conservando sus autores, incluida la
configuración y ubicación del negocio y la reorganización de la lectura de imágenes.

Verificación del conjunto integrado antes de los commits: backend **1.817 pruebas**, sin fallos,
errores ni omitidas; **537 clases, 5.127 líneas y 1.322 ramas** al 100 % por clase. Evaluador:
**39 pruebas** con el jar recién compilado. Frontend: **344 pruebas en 32 archivos**, con lint,
tipos y build de producción aprobados. Las claves privadas, logs y recursos de prueba locales
quedan excluidos de la publicación.

### Devolución y entrega firmada desde el panel de pruebas (21/10/2026)

Se verificó el acceso con el vendedor de prueba de la aplicación configurada, sin reemplazar
las claves por credenciales de una cuenta real. Brian confirmó desde el panel la devolución
total de ARS 14.900 del pago `185274130912`. La API confirmó `refunded`, importe devuelto
de ARS 14.900 y devolución `approved`. La conciliación existente actualizó la única factura
local a `refunded`, mantuvo la renovación cancelada y dejó la suscripción `GRATUITO / ACTIVA`.
No se modificó el plan manualmente por SQL.

Brian configuró la firma secreta en el archivo privado excluido de Git. Se preparó un túnel
temporal que sólo acepta POST en la ruta del webhook; las demás rutas públicas devuelven 404.
Se desactivó temporalmente la ejecución periódica del conciliador mediante su demora inicial
para distinguir el procesamiento del webhook de una actualización en segundo plano.

El simulador del panel de Mercado Pago envió un evento `payment` con el id del pago devuelto.
El receptor recibió `x-signature` y `x-request-id`, validó HMAC y respondió HTTP 200 en 2.192 ms.
La fecha de sincronización del contrato cambió, y el plan se mantuvo en Gratuito con una sola
factura. Una repetición devolvió HTTP 502; un único reintento posterior volvió a devolver HTTP
200 en 1.528 ms, sin duplicar facturas ni cambiar el plan. No se determinó la causa del 502.
Los avisos sin firma y con una firma deliberadamente inválida devolvieron HTTP 401.

Esta evidencia comprueba la entrega firmada del simulador de Mercado Pago y el procesamiento
del recurso real de prueba. No demuestra todavía la emisión automática tras un nuevo evento
de suscripción ni una segunda factura generada por el proveedor a los treinta días. No se
crearon nuevos pagos ni suscripciones; se conserva la cancelación de los contratos terminados.
Estos ensayos se realizaron en local, sin commits ni push durante su ejecución y sin cobros reales.

### Notificación automática de cancelación sin autorizar un pago (21/10/2026)

La API impide modificar una suscripción ya cancelada. Desde Planes se preparó una nueva solicitud
pendiente en la misma base aislada: referencia local `907127ac-4385-4125-8e89-b8419b211027`,
contrato remoto `659c6b4dcb864084a70fe4ec82690f1a`. No se abrió ni autorizó el checkout ni se
asoció una tarjeta. El negocio permaneció en Gratuito.

En la aplicación del vendedor verificado como `test_user` se configuró también la dirección
temporal en la pestaña denominada «Modo productivo», que estaba vacía. Esto corresponde a esa
cuenta de prueba; no se cambiaron el vendedor, token, aplicación ni el indicador local de producción.
Se aceptó un cambio de descripción del contrato pendiente, pero no se observó un aviso por esa
acción. No se atribuye con esta evidencia una causa definitiva a la ausencia del aviso inicial.

Después se canceló la solicitud desde Planes. El receptor registró a las `2026-10-21T15:27:06.569Z`
un evento automático `subscription_preapproval` para ese contrato, con `x-signature` y
`x-request-id` presentes. La validación HMAC y el procesamiento devolvieron HTTP 200 en 453 ms.
No se utilizó «Enviar prueba» para este evento y el conciliador periódico permanecía demorado.

La API confirmó `cancelled`, ninguna tarjeta y cero facturas del nuevo contrato; PostgreSQL y
Planes confirmaron renovación cancelada y `GRATUITO / ACTIVA`. Esta prueba verifica la entrega
automática firmada de una cancelación. Sigue pendiente la segunda factura de un ciclo real de
treinta días; no se considera probado un cobro recurrente ni su notificación de pago por este caso.

### Segunda renovación acelerada con reloj de prueba (21/10/2026)

Se agregaron cuatro casos a `RenovacionPremiumIntegrationTest` para comprobar la segunda factura
sin esperar treinta días ni modificar contratos de Mercado Pago. Testcontainers utiliza PostgreSQL
aislado, el reloj del test avanza al límite del segundo período y las respuestas del proveedor se
simulan. El endpoint real de notificaciones se recorre mediante MockMvc con validación HMAC real
del adaptador y una clave exclusiva del test; no se emplean las credenciales privadas del vendedor.

Dos casos cubren una segunda factura aprobada con débito en el día 30 y aviso recibido en el día
32, para los tópicos `subscription_authorized_payment` y `payment`. Repetir el aviso conserva sólo
dos facturas y acceso hasta el día 60. Los otros dos cubren una segunda factura pendiente o
rechazada al vencer el primer período: no renuevan Premium y conservan el historial aprobado.
La clase de integración completa pasó sus 36 pruebas, incluidos estos cuatro casos.

La simulación comprueba la lógica de Ensambla y no demuestra la emisión de una segunda factura
automática de Mercado Pago. La comprobación del proveedor continúa pendiente. No se alteraron
las fechas del sistema operativo, las bases de los ensayos manuales ni el período comercial de
treinta días; no se generaron cobros, contratos ni commits en este seguimiento.

Verificación final en un directorio de compilación separado del jar en ejecución: `clean verify`
aprobado con **1.821 pruebas**, sin fallos, errores ni omitidas; **537 clases, 5.127 líneas y
1.322 ramas** al 100 % por clase. Se conservó el entorno manual en ejecución y se eliminó
el archivo Maven temporal usado para separar la salida de la compilación.

### Verificación después de integrar los cambios del equipo (21/10/2026)

Antes de publicar estas pruebas se respaldaron los tres archivos locales y se incorporaron
78 commits mediante avance directo hasta `1578c52`, que incluye el `dev` descargado. Los cambios
locales se recuperaron sin conflictos y se verificó que su parche fuera idéntico al respaldo.

El conjunto integrado aprobó `clean verify`: **2.270 casos**, sin fallos ni errores, con dos pruebas
reales de Bedrock omitidas por no tener configurado `SAMI_BEDROCK_PERFIL`. Las **678 clases,
6.827 líneas y 1.878 ramas** cumplen el 100 % de cobertura por clase. La clase de renovaciones
aprobó sus **36 pruebas**. El evaluador aprobó **39 pruebas** con el jar de esta compilación.

El frontend aprobó lint, tipos y build. La primera ejecución completa recorrió 42 archivos y
505 casos: 494 aprobados, nueve fallos mientras las pantallas seguían cargando y dos omitidos.
Se repitieron únicamente los nueve archivos afectados con dos workers: **153 pruebas aprobadas**,
sin modificar código ni tiempos de espera. No se presenta como una única corrida completa sin
reintentos; la PC tenía poca memoria libre durante las comprobaciones.

Las compilaciones se generaron en directorios separados de los servidores manuales. Los ensayos
externos descritos arriba corresponden a la instancia manual anterior; no se aplicaron las nuevas
migraciones a sus bases durante esta verificación. Las claves, capturas, respaldos y logs permanecen
fuera de Git. La emisión de una segunda factura automática de Mercado Pago sigue pendiente.

### Vencimiento de la prueba con reloj adelantado (21/10/2026)

Después de verificar Planes con V17 se ejecutaron pruebas existentes sobre el código `84ce2c3`,
sin modificar producción ni adelantar el reloj de Windows. PostgreSQL temporal de Testcontainers
aplicó V1–V17 y el reloj del caso de uso se fijó en el instante exacto de inicio de la prueba más
siete días. Estas comprobaciones no cambiaron las fechas de la prueba activa de la cuenta local.

El conjunto enfocado aprobó **18 pruebas del backend**, sin fallos, errores ni omitidas. Comprueba:

- Regreso a `GRATUITO / ACTIVA` en el instante exacto del vencimiento.
- Conservación de dos proyectos y 1.001 registros, aunque superen los límites de Gratuito.
- Repetir el vencimiento no borra datos ni permite crear un tercer proyecto por encima del límite.
- Una nueva activación responde `PRUEBA_UTILIZADA` y conserva el historial original.
- Las funciones Premium se restringen al vencer aunque todavía no se haya ejecutado el job.
- Un período Premium pagado después de seleccionar los candidatos no se revoca por vencer la prueba.
- El caso de uso utiliza el reloj del servidor y el worker programa su ejecución por defecto.

La prueba enfocada de la interfaz aprobó **un caso**: Planes muestra «Tu prueba terminó», informa
que se conservan los sistemas, registros y recetas, y deshabilita «Prueba ya utilizada». Los otros
diez casos de ese archivo se omitieron intencionalmente por el filtro de selección; no constituyen
una corrida completa nueva del frontend. No hubo llamadas de cobro a Mercado Pago ni contratos
nuevos. Los comandos para repetir esta comprobación son:

```powershell
# Desde backend/
.\mvnw.cmd "-Dtest=PruebaPremiumPostgresIntegrationTest#executeShouldExpireTrialAndPreserveData+validarFuncionShouldRejectTrialAtExactExpiryWithoutJob+vencerShouldPreservePaidPremiumAfterTrial,VencerPruebasUseCaseTest,VencimientoPruebasWorkerTest,PruebaPremiumServiceTest" test

# Desde frontend/
npm run test -- src/test/routes/planes.test.tsx -t "muestra la prueba finalizada" --maxWorkers=1 --minWorkers=1
```

### Pendientes para completar la validación externa

1. Validar el conjunto posterior `f54fa04` y su nueva migración V18 en una base independiente antes
   de repetir el circuito de Mercado Pago. Los resultados publicados arriba corresponden a V17.
2. Restablecer los túneles y repetir una factura de prueba aprobada con su notificación automática
   firmada en el conjunto actualizado. El evento automático ya comprobado fue una cancelación;
   los avisos de pago anteriores se enviaron desde el simulador del panel.
3. Observar la segunda factura automática del contrato de treinta días y su aviso de pago.
   Las simulaciones aceleradas validan Ensambla, pero no demuestran esa emisión del proveedor.
4. Investigar el HTTP 502 aislado al reenviar una notificación. El reintento respondió 200 y no
   duplicó la factura, pero la causa todavía no está determinada.

Las pruebas externas deben utilizar únicamente la aplicación, el vendedor, el comprador y las
tarjetas de prueba. El usuario completa autenticación, códigos y datos de pago. Los contratos de
los circuitos terminados permanecen cancelados. No se habilita producción ni se declaran probados
los pendientes por publicar esta documentación.
