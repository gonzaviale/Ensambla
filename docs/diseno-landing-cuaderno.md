# Landing: cuaderno digital para comercios de barrio

Dirección aprobada el 19 de octubre de 2026. Implementación local en `feat/landing-page`, sin commit ni push.

## Objetivo y alcance

Que una persona que atiende un comercio reconozca su situación, entienda qué es Ensambla y encuentre una forma concreta de empezar. El mensaje principal es «Tu cuaderno de siempre. Tus cuentas, más claras»; la bajada lo acota a registrar y consultar ventas, gastos y fiados.

Se modifica la landing, sus enlaces del encabezado y el texto de entrada al registro. Se conservan el registro, la sesión y las funciones internas existentes. No se implementan exportación, respaldos, WhatsApp, audio ni nuevas capacidades de IA.

## Recorrido aprobado

1. Presentación: cuaderno digital, foto contextual del almacén y acceso al registro.
2. Franja breve: «Atendés. Comprás. Hacés las cuentas. Que tus anotaciones te den una mano».
3. Tres problemas cotidianos: encontrar un fiado, un gasto o las ventas del día.
4. Ejemplo ilustrativo grande: foto del cuaderno, revisión y guardado. Cambiar de paso sólo modifica la demostración, sin guardar datos ni llamar a la API.
5. Beneficios: menos buscar y más tiempo para atender, en una franja magenta.
6. Flexibilidad: empezar por las anotaciones necesarias, con Sami como apoyo secundario.
7. Propiedad de los datos: descarga prevista, explícitamente no disponible, sin botón que simule exportar.
8. Preguntas frecuentes y llamada final al registro.

Se retiran del recorrido los testimonios de muestra, la publicación local de comentarios y las promesas de audio, WhatsApp y monitoreo automático. No se eliminan datos del navegador.

## Diseño y contenido

- Fondo blanco, tinta petróleo, amarillo, celeste, coral, naranja y magenta como acento específico de esta landing aprobada. No se modifica la paleta global de la aplicación.
- Plus Jakarta Sans autoalojada; textos de lectura de 18 px y controles de al menos 48 px. Los metadatos mantienen un mínimo de 14 px.
- Altura según contenido, sin forzar una pantalla por sección. Composición con escena, franjas, filas abiertas, demostración y preguntas desplegables.
- Navegación por teclado, foco visible, encabezados ordenados, contraste y preferencia de movimiento reducido.
- La foto del almacén es una escena ficticia generada con imagegen para el boceto aprobado: comerciante anotando en un cuaderno detrás del mostrador, luz natural y productos genéricos. `frontend/public/landing/almacen-cuaderno.jpg` es la copia optimizada de ese recurso; no es una clienta ni un testimonio.
- Sami reutiliza el recurso existente `frontend/public/landing/sami-bloques.webp`.
- El ejemplo no prueba precisión de lectura. La configuración de IA y su validación real quedan fuera de este cambio.
- Una exportación legible en CSV y un respaldo restaurable del negocio son funciones distintas; ambas requieren desarrollo y validación propios antes de anunciarse como disponibles.

## Plan y validación

1. Adaptar los componentes y el estilo del boceto, incorporando el recurso local.
2. Ajustar las anclas y los enlaces a registro/ingreso. Mantener el resto de la app.
3. Sustituir las pruebas obsoletas de la landing por comprobaciones de navegación, ejemplo, preguntas, accesibilidad y ausencia de una descarga falsa.
4. Ejecutar las pruebas pertinentes, la revisión de tipos, el análisis de código y la compilación.
5. Revisar el resultado en navegador a 320 px, 390 px, tablet y escritorio, incluyendo teclado y movimiento reducido. La revisión visual pública puede simular exclusivamente la ausencia de sesión si el backend no está levantado; eso no valida la integración con el backend.

## Resultado de la validación local

- Compilación (incluye TypeScript) y ESLint: correctos.
- 10 pruebas de landing, 6 de sesión y 8 de accesibilidad de las pantallas: correctas. Las pruebas de accesibilidad en jsdom emiten un aviso por la falta de canvas; la comprobación adicional en navegador sí incluye contraste real.
- Axe en navegador sobre landing y encabezado, en escritorio y celular: sin infracciones detectadas después de corregir la tinta de las etiquetas de colores.
- Revisión visual a 320, 390, 768, 1024 y 1440 px: sin desbordes horizontales del documento. Se verificaron el ejemplo, preguntas, anclas con encabezado fijo, Escape/foco del menú y navegación a registro/ingreso.
- La vista que queda levantada utiliza `VITE_API_MOCKS=completo` sólo en el proceso de desarrollo. No se modificó `.env`. No se probaron escrituras ni integración con el backend real.
