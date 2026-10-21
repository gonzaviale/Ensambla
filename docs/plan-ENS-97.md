# ENS-97 · Planes y activación de prueba desde la aplicación

Continuación autorizada por Brian el 20 de octubre de 2026 del diseño de ENS-21.
Se trabaja en `feat/ens-21-planes-suscripciones`, únicamente en local y sin commits ni push.

## Diseño

Página privada `/planes`, accesible desde la navegación de la aplicación, con la misma marca,
componentes y adaptación a celular que el resto de Ensambla. Comparar Gratuito y Premium con
los valores del catálogo del servidor. El gratuito se muestra sin costo; el precio premium
pendiente se presenta como «Precio por definir», sin inventar moneda ni ofrecer un cobro inexistente.

Mostrar plan efectivo, sistemas, registros y páginas de IA admitidas o reservadas. Durante la
prueba, mostrar el contador total de diez páginas y su fecha de fin; fuera de ella, el consumo
del mes calendario UTC. Conservar el mensaje de prueba finalizada al volver al gratuito.

Iniciar la prueba mediante confirmación explícita de siete días y diez páginas totales,
sin tarjeta ni cargo automático. Bloquear el botón mientras se procesa; refrescar el estado
después de activar, recuperar la página, cambiar de pestaña o vencer la prueba. Cuando ya se
utilizó la prueba o existe premium vigente, el servidor determina que no se pueda repetir.

## Implementación

1. Agregar `GET /api/planes`, privado y sin caché HTTP, con catálogo, plan efectivo, elegibilidad,
   historial de prueba y uso. Una consulta por negocio, con el bloqueo compartido de planes,
   produce una lectura coherente frente a activación o vencimiento concurrente. Sin esquema nuevo.
2. Mantener `POST /api/planes/prueba`, sesión y CSRF. El navegador no decide negocio, fechas,
   plan, consumo ni elegibilidad. Los DTOs web y de dominio siguen las capas del proyecto.
3. Agregar contrato zod, cliente API y hooks de TanStack Query; separar red, estado y presentación.
   Preservar los códigos de error de plan y no confundir un 403 comercial con renovación de CSRF.
4. Crear la página, la confirmación accesible y la entrada de navegación. Mostrar carga, error
   recuperable, gratuito, prueba activa/agotada/finalizada y premium vigente.
5. Simular este contrato sólo en MSW completo para pruebas y demostración. Los modos parcial y
   ninguno consultan el backend. Probar la integración real con una base PostgreSQL local aislada.

## Validación

Tests del backend con reloj controlado, cuotas coherentes y aislamiento de sesión; verificación
completa `clean verify` con 100 % de líneas y ramas por clase. Tests del frontend para navegación,
contrato, CSRF, activación única, errores, estados de prueba y accesibilidad; lint, tipos y build.
Revisar en navegador escritorio y celular, incluyendo un circuito con backend real.

ENS-98 sigue pendiente: no se implementa pasarela de pago ni se define precio o moneda en esta etapa.

## Resultado comprobado el 20 de octubre de 2026

- Backend: `mvnw.cmd -B clean verify`, 1.512 pruebas sin fallos, errores u omitidas.
  JaCoCo: 456 clases, 4.351 líneas y 1.036 ramas, 100 % de líneas y ramas por clase.
- Evaluador: 39 pruebas aprobadas con `ENSAMBLA_EVAL_JAR` apuntando al jar nuevo.
  Se usa IA simulada; esto no valida precisión de OCR con fotos reales.
- Frontend: 288 pruebas en 28 archivos aprobadas; lint, tipos y build de producción aprobados.
  Incluye navegación privada, contrato, CSRF, aislamiento, doble clic, activación rechazada,
  recuperación de errores, prueba activa/agotada/finalizada, premium y accesibilidad de página/modal.
- Navegador real: comparación y activación desde la UI, sesión conservada tras recarga,
  segunda activación rechazada con `PRUEBA_UTILIZADA`, uso de diez páginas y actualización automática
  al alcanzar el fin de prueba. Dos sistemas creados vía API durante premium permanecen consultables;
  una nueva alta al vencer responde `LIMITE_SISTEMAS`. Precio pendiente sin importe ficticio.
- Escritorio de 1.280 px y celulares de 390/320 px: contenido sin desbordamiento horizontal.
  Navegación en una segunda fila con desplazamiento propio cuando hace falta; confirmación legible,
  foco inicial en cancelar y retorno de foco al cerrar. Contraste premium revisado visualmente.

### Entorno de la prueba real

Se reutilizó el PostgreSQL local existente sin recrear el contenedor ni sus volúmenes. Se creó
`ensambla_ens97_local_20261020`, base independiente donde Flyway aplicó V1–V14 desde cero.
La base habitual `ensambla` quedó intacta. ENS-97 no agrega migraciones.

Backend local en el puerto 8080, proveedor IA simulado y workers deshabilitados explícitamente.
Frontend en `http://localhost:5173/planes` con MSW parcial: autenticación y planes reales; otros
módulos aún simulados. La cuenta de validación `ens97.local@ensambla.test` conserva una prueba
finalizada y dos sistemas reales para revisar ese estado. La contraseña es exclusivamente un dato
local de prueba. También se puede registrar otro negocio para probar una activación nueva.

El cupo agotado y el vencimiento cercano se prepararon modificando exclusivamente los datos de
esta cuenta en la base aislada: contador de prueba y consumo diario en diez, fechas coherentes
de 168 horas con fin próximo. Se verificó el cambio automático sin recargar ni ejecutar el worker.
Es una validación de estados de suscripción, sin procesar fotos ni consumir un proveedor externo.

Las capturas están fuera del repositorio, en la carpeta `.playwright-mcp` del workspace exterior.
La validación de esta etapa se realizó antes de publicar los cambios. Brian autorizó los commits
y el push el 21/10/2026 en `feat/ens-21-planes-suscripciones`, usando su perfil de GitHub `Sur4x`.
