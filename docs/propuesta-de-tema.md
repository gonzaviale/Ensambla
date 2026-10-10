# Propuesta de tema — ENSAMBLA (v1.1, 10/10/2026)

Generado desde `originales/Propuesta_de_Tema_ENSAMBLA_2026_v1.1.docx`. El original es la fuente de verdad.

UNIVERSIDAD NACIONAL DE LA MATANZA

Departamento de Ingeniería e Investigaciones Tecnológicas

Tecnicatura en Desarrollo de Aplicaciones Web/Móviles

Taller Práctico Integrador / Trabajo Final

ENSAMBLA

Tu sistema, un bloque a la vez.

PROPUESTA DE TEMA

Plataforma que convierte los cuadernos en papel de emprendedores en un sistema de gestión digital, simple y ordenado

INTEGRANTES

CANO, Roxana Elisa

KLEHR, Brian Alexander

MOSQUERA, Ignacio Gabriel

MURUA, Thomas Nahuel

ORTEGA, Facundo Samuel

PALLEROS, Facundo Baltasar

VIALE, Gonzalo Leonel

2026  ·  2° Cuatrimestre

## INFORMACIÓN GENERAL

Datos institucionales, responsables y control del documento.

Referencias académicas

| Coordinación de Tecnicaturas | Ing. Cintia Gioia |
|---|---|
| Referente de Cátedra | Lic. Marcelo Juárez |
| Tutor principal | A completar |
| Tutor secundario | A completar |
| Tutor transversal | Lic. Marcelo Juárez |
| Tutor gráfico | A completar |
| Tutor técnico | A completar |
| Referentes involucrados | A confirmar |

Tutores de la cátedra

Lic. Juan De Cicco · Ing. Alejandro Rusticcini · Ing. Sergio Bonavento · Ing. Sebastián Ismael · Lic. Nahuel Rapetti · Lic. Serena Donato · Lic. Ivan Karlen · DG. Javier Da Costa · Tec. Mauro Ayala · Tec. Nuria Orquin · Ing. Facundo Campodonico.

Identificación del documento

| Versión | 1.1 |
|---|---|
| Última actualización | 10/10/2026 |

Control de versiones

| Versión | Autoría | Fecha | Motivo |
|---|---|---|---|
| 1.0 | Equipo Ensambla | 09/10/2026 | Propuesta inicial del proyecto |
| 1.1 | Equipo Ensambla | 10/10/2026 | Reenfoque del producto: digitalización de cuadernos con IA para emprendedores mayores |

Nota de edición: los datos de tutoría, referentes y distribución interna de roles deben completarse cuando se confirmen. La propuesta funcional y técnica es preliminar y está sujeta a validación del equipo.

## ÍNDICE

Estructura de la propuesta de tema.

| I · RESUMEN<br>Síntesis del producto | IX · MODELO DE NEGOCIO<br>Estrategia comercial |
|---|---|
| II · MERCADO<br>Público objetivo y alcance | X · EQUIPO<br>Composición y funciones |
| III · CONTEXTO<br>Digitalización de la gestión cotidiana | XI · RIESGOS<br>Aspectos técnicos y de adopción |
| IV · PROBLEMA<br>Dificultades de las herramientas actuales | XII · ESPECIALISTAS INVOLUCRADOS<br>Validación externa |
| V · PROPUESTA DE VALOR<br>Qué aporta Ensambla | XIII · TECNOLOGÍAS INVOLUCRADAS<br>Arquitectura tentativa |
| VI · SOLUCIÓN<br>Descripción, funcionamiento y MVP | XIV · BIBLIOGRAFÍA Y SITIOS DE INTERÉS<br>Fuentes de consulta |
| VII · COMPETENCIA<br>Soluciones existentes y brechas | XV · GLOSARIO<br>Terminología principal |
| VIII · JUSTIFICACIÓN<br>Diferenciación y oportunidad |  |

ENFOQUE DEL DOCUMENTO

Ensambla propone que emprendedores que llevan su negocio en cuadernos de papel —y a quienes las planillas de cálculo y las aplicaciones de gestión les resultan difíciles— puedan pasar a una gestión digital ordenada sin aprender a usar esas herramientas: sacan una foto de su cuaderno y Ensambla arma el sistema. Este documento presenta el concepto, el alcance inicial y los principales supuestos a validar.

## I  ·  RESUMEN

Ensambla es una aplicación web, pensada primero para el celular, que ayuda a emprendedores adultos y adultos mayores a ordenar su emprendimiento a partir de lo que ya hacen: anotar en un cuaderno. La persona saca una foto de sus páginas de ventas, gastos, pedidos, stock o fiados; la plataforma, mediante inteligencia artificial, interpreta lo escrito, propone cómo organizarlo y lo convierte en registros digitales que se pueden revisar, corregir, consultar y ampliar con el tiempo. No hace falta saber usar Excel, diseñar tablas ni aprender términos técnicos.

El núcleo del producto —y lo que define su primer MVP— es ese recorrido: de la foto del cuaderno a un sistema ordenado, con la persona siempre confirmando lo que leyó la IA. Sobre esa base, Ensambla conserva su enfoque modular: los datos se organizan en bloques (Ventas, Clientes, Stock…) que pueden ajustarse o ampliarse, y se ofrecen plantillas para quien prefiera empezar sin fotos. Indicadores, roles, automatizaciones, integraciones y un mercado de plantillas se contemplan como líneas de evolución.

## II · MERCADO

Mercado objetivo.  Emprendedores y pequeños comercios —en particular personas adultas y adultas mayores— que hoy administran su actividad en cuadernos, libretas, talonarios o anotaciones sueltas, y que no usan planillas de cálculo ni sistemas de gestión porque les resultan complejos o ajenos.

Usuarios iniciales.  Emprendimientos unipersonales o familiares de baja complejidad (producción artesanal, venta por catálogo, almacenes, comidas, costura u oficios) que registran a mano ventas, gastos, pedidos, stock o cuentas de clientes y quieren saber con más claridad cómo les va.

Criterio de alcance.  La oportunidad se describe de forma cualitativa en esta etapa. La dimensión de mercado, el rango etario del segmento, los tipos de anotación más frecuentes y la disposición a pagar deberán validarse mediante entrevistas, pilotos y relevamiento de potenciales usuarios; no se presenta una cifra de mercado no verificada.

## III · CONTEXTO

La administración diaria de muchos emprendimientos sigue apoyándose en el papel: un cuaderno de ventas, una libreta de fiados, el talonario de pedidos. Es un método que la persona domina y en el que confía, pero que con el tiempo acumula páginas difíciles de sumar, buscar o comparar. Las alternativas digitales disponibles —hojas de cálculo, aplicaciones de gestión o plataformas no-code— suponen que el usuario se adapte a ellas: aprender fórmulas, diseñar columnas, volver a cargar a mano todo lo anterior y familiarizarse con interfaces pensadas para personas con experiencia digital.

Los avances recientes en modelos de inteligencia artificial capaces de leer imágenes, incluida la escritura manuscrita, abren una posibilidad nueva: en lugar de pedirle a la persona que pase sus datos a un sistema, el sistema puede partir de lo que ella ya anotó. Esto permite una transición gradual hacia la gestión digital, sin abandonar el cuaderno de un día para el otro.

## IV · PROBLEMA

Muchas personas que emprenden saben qué necesitan controlar —cuánto vendieron, cuánto gastaron, quién les debe, qué les queda en stock—, pero las herramientas digitales les exigen habilidades que no tienen o no quieren adquirir. El resultado es que la información queda en papel, dispersa y difícil de aprovechar.

Información en papel difícil de sumar, buscar o comparar entre semanas y meses, y expuesta a pérdidas o deterioro.

Planillas de cálculo y sistemas de gestión que requieren conocimientos previos (fórmulas, columnas, configuración) y presentan interfaces poco amigables para personas mayores o con poca experiencia digital.

Pasar a mano lo que ya está anotado es lento y tedioso: la carga inicial es la principal barrera para empezar a usar cualquier sistema.

Dificultad para tener una visión simple del estado del negocio (ventas del mes, gastos, deudas pendientes) sin depender de un familiar o de terceros.

## V · PROPUESTA DE VALOR

«Sacale una foto a tu cuaderno. Ensambla lo ordena.»

Ensambla parte de cómo trabaja hoy la persona, en lugar de pedirle que cambie. Sube fotos de su cuaderno, la inteligencia artificial interpreta lo escrito y propone cómo organizarlo, y la persona revisa y confirma antes de guardar. El resultado es un sistema propio, ordenado y fácil de consultar, sin haber diseñado una tabla ni aprendido una herramienta nueva.

Sin carga manual inicial: lo que ya está anotado en papel se digitaliza a partir de fotos tomadas con el celular.

Sin saber Excel: la estructura (qué datos guardar y cómo) la propone el sistema; la persona solo confirma o corrige.

Pensado para personas mayores: letra grande, botones claros, pocos pasos y lenguaje cotidiano, sin términos técnicos.

Control y confianza: la foto original se ve junto a lo que leyó la IA, las dudas se resaltan y nada se guarda sin confirmación.

Crecimiento por etapas: cada nueva foto se suma a lo ya cargado; bloques y plantillas permiten ampliar el sistema cuando haga falta.

Accesibilidad de entrada: una propuesta gratuita de base y opciones de pago para mayor volumen o funciones avanzadas.

## VI · SOLUCIÓN

### 1. Descripción

Ensambla será una aplicación web adaptable, pensada principalmente para usarse desde el celular, cuyo eje es la digitalización asistida de registros en papel. La persona fotografía las páginas de su cuaderno; un modelo de inteligencia artificial capaz de leer imágenes y escritura manuscrita identifica qué tipo de información contienen (ventas, gastos, pedidos, stock, cuentas de clientes), propone una estructura de bloques y campos, y extrae cada renglón como un registro. Antes de guardar, la persona revisa el resultado junto a la foto, corrige lo que haga falta y confirma.

Por ejemplo, una emprendedora que vende velas artesanales anota sus ventas en un cuaderno: fecha, clienta, producto, cantidad y si le pagaron. Al subir las fotos de un mes, Ensambla crea el bloque «Ventas» con esos campos, carga cada venta como un registro y marca los datos que no pudo leer con seguridad. Desde ese momento puede buscar a una clienta, ver cuánto vendió en el mes o quién le debe, y la semana siguiente sumar nuevas páginas al mismo bloque. Los bloques siguen siendo configurables y existen plantillas para quien prefiera empezar sin fotos.

### 2. Funcionamiento

1. Crear la cuenta.  La persona se registra con pocos datos y se crea el espacio de su emprendimiento.

2. Sacar la foto.  Desde la pantalla principal elige «Subir foto de mi cuaderno» y fotografía una o varias páginas con el celular, o las elige de la galería.

3. Ensambla lo lee.  La inteligencia artificial interpreta las páginas, propone cómo organizarlas (por ejemplo, un bloque «Ventas» con fecha, cliente, producto y monto) y extrae los registros.

4. Revisar y confirmar.  La persona ve la foto junto a los datos leídos, con las dudas resaltadas; corrige lo necesario y confirma. Nada se guarda sin su aprobación.

5. Consultar y seguir sumando.  Busca, ordena y ve totales simples; cada nueva foto se agrega a lo ya cargado. Si lo necesita, ajusta los bloques o suma una plantilla.

### 3. Lista de funcionalidades y alcance del MVP

Digitalización con IA (núcleo).  Subida de fotos desde la cámara o la galería; lectura de escritura manuscrita e impresa; propuesta automática de bloques y campos; extracción de registros con indicación de datos dudosos; pantalla de revisión y confirmación; posibilidad de sumar nuevas fotos a un bloque existente.

Acceso y espacio del negocio.  Registro, inicio de sesión y aislamiento de la información de cada emprendimiento.

Consulta y uso diario.  Tablas de registros con búsqueda, orden y totales simples; ficha de cada registro con acceso a la foto de origen; edición, eliminación y carga manual cuando haga falta.

Ajustes y plantillas.  Renombrar bloques y campos, agregar o quitar campos, y 2-3 plantillas base para empezar sin fotos.

Experiencia accesible.  Diseño pensado para el celular, tipografía grande, alto contraste, botones amplios y textos sin tecnicismos.

Fuera del MVP inicial.  Panel de indicadores y gráficos, importación de planillas, carga por audio o WhatsApp, automatizaciones, integraciones comerciales, equipos con roles, marketplace de plantillas y analítica avanzada. Son posibles evoluciones, no prestaciones comprometidas para la primera entrega.

## VII · COMPETENCIA

### 1. Identificación

Competencia o alternativas cercanas: aplicaciones de gestión para micronegocios desde el celular, como Treinta; plataformas no-code y de organización de datos, como Airtable, Glide, Google AppSheet y Notion; y funciones que convierten una foto en tabla, como «Insertar datos desde imagen» de Microsoft Excel o los asistentes de IA de uso general. Competencia indirecta: el propio cuaderno, las planillas de cálculo, los sistemas de gestión específicos y la ayuda de familiares o contadores.

### 2. Fortalezas de las alternativas

Son soluciones conocidas y maduras. Las aplicaciones para micronegocios permiten registrar ventas, gastos e inventario con rapidez; las plataformas no-code ofrecen gran flexibilidad; y Excel o los asistentes de IA ya pueden convertir la foto de una tabla en datos digitales con buena precisión en muchos casos.

### 3. Limitaciones y espacio de diferenciación

Para este perfil, ninguna combina los tres elementos del núcleo de Ensambla: partir de lo ya escrito en papel, organizarlo sin que la persona diseñe la estructura y conservarlo en un sistema que crece con cada nueva foto. Las aplicaciones de micronegocios exigen volver a cargar todo a mano; las plataformas no-code requieren diseñar tablas y manejar terminología técnica; la captura de Excel devuelve una planilla que la persona igualmente debe saber usar; y los asistentes de IA generales dependen de saber pedir y no mantienen un registro ordenado del negocio. Estas ventajas son hipótesis de producto que deberán comprobarse con usuarios.

## VIII · JUSTIFICACIÓN

### 1. Disrupción

La innovación no reside en la lectura de imágenes con IA —una tecnología ya disponible— sino en aplicarla para invertir el punto de partida: en lugar de que la persona aprenda una herramienta y cargue sus datos, la herramienta parte de los registros que la persona ya tiene y arma el sistema a partir de ellos, con una revisión humana simple que garantiza la confianza en el resultado.

### 2. Oportunidad

Una parte de los emprendimientos se sigue gestionando en papel y quedó al margen de la digitalización, no por falta de necesidad sino por la barrera de aprendizaje. La madurez reciente de los modelos multimodales permite atender ese segmento con un costo por página accesible y validarlo rápidamente mediante pilotos con pocos usuarios.

### 3. Ventaja competitiva planteada

La foto como forma principal de carga; estructura propuesta automáticamente y ajustable; revisión con la imagen original a la vista; diseño accesible para personas mayores; y crecimiento por bloques sin reconstruir el sistema. Son objetivos de diseño que deberán medirse frente a competidores.

### 4. Beneficios esperados

Para usuarios: dejar de depender solo del papel sin cambiar su forma de anotar, ahorro del tiempo de carga manual, información respaldada y fácil de consultar, y mayor autonomía para entender el estado de su emprendimiento. Para el proyecto: un caso de uso concreto y demostrable que funciona como puerta de entrada a funciones de gestión más completas en etapas posteriores.

## IX · MODELO DE NEGOCIO

Modalidad.  Servicio de software en la nube (SaaS), con acceso mediante navegador y suscripciones para capacidades ampliadas.

Plan gratuito.  Una cantidad mensual limitada de páginas digitalizadas con IA, junto con la consulta y la carga manual, para que la persona compruebe el valor antes de pagar.

Planes pagos.  Mayor volumen de páginas procesadas por mes y funciones avanzadas sujetas a validación, como indicadores, reportes, carga por audio o WhatsApp, permisos por rol e integraciones. El costo de procesamiento por página condiciona los límites de cada plan.

Prueba inicial.  Se contempla evaluar una prueba de 7 días de funciones premium; condiciones comerciales y límites de uso todavía deben definirse.

Otras vías de ingresos.  Digitalización asistida de cuadernos de años anteriores como servicio puntual y marketplace de plantillas con comisión por venta, en etapas posteriores.

Validación comercial.  Pilotos con emprendedores reales, entrevistas, análisis de uso, medición del costo de IA por usuario y prueba de precios antes de fijar niveles de suscripción y estimar el punto de equilibrio.

## X · EQUIPO

El grupo está integrado por las personas consignadas en la carátula del documento de origen: Roxana Elisa Cano, Brian Alexander Klehr, Ignacio Gabriel Mosquera, Thomas Nahuel Murua, Facundo Samuel Ortega, Facundo Baltasar Palleros y Gonzalo Leonel Viale.

La distribución formal de responsabilidades queda pendiente de definición. Para ejecutar el proyecto se requieren tareas de análisis funcional, investigación con usuarios mayores, diseño de experiencia e interfaz accesible, desarrollo frontend y backend, integración y evaluación de modelos de inteligencia artificial, modelado de datos, pruebas, documentación y evaluación del modelo de negocio.

## XI · RIESGOS

Precisión de la lectura.  La escritura manuscrita, las abreviaturas y las fotos de baja calidad pueden generar errores. Mitigar con revisión obligatoria antes de guardar, resaltado de datos dudosos, consejos para sacar la foto y un set de fotos reales de cuadernos para medir la precisión ante cada cambio.

Costo y dependencia del proveedor de IA.  Medir el costo por página, fijar límites de uso por plan y encapsular el proveedor detrás de un servicio propio para poder reemplazarlo.

Privacidad y seguridad de los datos.  Las fotos pueden contener nombres, teléfonos o montos de clientes. Diseñar autenticación, aislamiento por negocio y almacenamiento privado de imágenes desde el inicio, y revisar las condiciones de uso de datos del proveedor de IA.

Usabilidad y accesibilidad.  Priorizar textos comprensibles, tipografía grande, contraste, pocos pasos y pruebas periódicas con personas mayores sin experiencia técnica.

Alcance excesivo.  Concentrar el MVP en el recorrido foto → revisión → registros y postergar indicadores avanzados, integraciones, automatizaciones y marketplace.

Adopción y sostenibilidad.  Validar el beneficio concreto, la confianza en la IA, los costos de infraestructura y la disposición a pagar mediante pilotos con emprendedores reales.

## XII · ESPECIALISTAS INVOLUCRADOS

Se propone consultar al menos tres perfiles externos para contrastar la factibilidad y utilidad de la solución. Los datos de contacto deberán incorporarse cuando se confirmen las entrevistas.

| Perfil recomendado | Nombre y apellido | Correo electrónico | Celular |
|---|---|---|---|
| Emprendedor/a adulto/a mayor que registre su actividad en papel | A confirmar | A confirmar | A confirmar |
| Especialista en UX/accesibilidad para personas mayores | A confirmar | A confirmar | A confirmar |
| Profesional de software / IA aplicada | A confirmar | A confirmar | A confirmar |

## XIII · TECNOLOGÍAS INVOLUCRADAS

La arquitectura definitiva se definirá a partir del repositorio del proyecto y de las capacidades del equipo. Como lineamiento técnico preliminar se considera:

Frontend: aplicación web responsiva con React y TypeScript, priorizando el uso desde el celular (captura con la cámara, compresión de imágenes) y criterios de accesibilidad; posible instalación como aplicación web progresiva (PWA).

Backend: API de negocio y autenticación; evaluación de Node.js/TypeScript u otra alternativa alineada con el trabajo del equipo.

Inteligencia artificial: modelo multimodal (visión y lenguaje) accedido por API, con salida estructurada en JSON validada contra un esquema. El proveedor se elegirá comparando precisión, costo por página y tiempo de respuesta sobre fotos reales de cuadernos; se evaluará OCR tradicional como apoyo o alternativa de menor costo.

Procesamiento asíncrono: cola de trabajos para procesar las fotos en segundo plano, con estados, reintentos y límites de uso.

Almacenamiento de imágenes: almacenamiento de objetos privado (compatible con S3) con acceso mediante URL firmadas.

Persistencia: base de datos para usuarios, negocios, bloques, campos, registros, fotos y procesamientos, vinculando cada registro con la imagen de la que surgió; motor a validar según el modelo de datos.

Infraestructura y trabajo colaborativo: Git/GitHub, entornos separados, configuración mediante variables de entorno, pruebas y despliegue reproducible con contenedores si resulta conveniente.

Seguridad: contraseñas protegidas, sesiones seguras, control de acceso por negocio, validación de entradas, respaldos y tratamiento cuidadoso de los datos enviados al proveedor de IA.

Estos elementos son opciones de implementación, no una afirmación de tecnologías ya adoptadas. La elección final debe documentarse tras la definición de arquitectura.

## XIV · BIBLIOGRAFÍA Y SITIOS DE INTERÉS

Microsoft Excel — función «Insertar datos desde imagen»: https://support.microsoft.com/office/insert-data-from-picture-3c1bb58d-2c59-4bc0-b04a-a671a6868fd7

Treinta — referencia de gestión de micronegocios desde el celular: https://www.treinta.co/

Airtable — referencia de bases de datos y aplicaciones configurables: https://www.airtable.com/

Google AppSheet — referencia de creación de apps sin programación: https://about.appsheet.com/

Glide — referencia de desarrollo de aplicaciones a partir de datos: https://www.glideapps.com/

Notion — referencia de espacios de trabajo y bases de datos: https://www.notion.com/

W3C WAI — pautas de accesibilidad WCAG: https://www.w3.org/WAI/standards-guidelines/wcag/

React — documentación oficial de interfaz: https://react.dev/

OWASP — referencias de seguridad para aplicaciones web: https://owasp.org/

Las referencias de productos se incluyen como puntos de comparación funcional; no constituyen un estudio cuantitativo de mercado.

## XV · GLOSARIO

| Bloque | Componente reutilizable que aporta una función a un sistema construido en Ensambla. |
|---|---|
| Constructor | Interfaz desde la cual se configuran los bloques, campos y vistas. |
| No-code | Creación de herramientas digitales sin escribir código. |
| Low-code | Creación de software con apoyo visual y poca programación. |
| MVP | Producto mínimo viable: primera versión acotada con la que se valida el valor. |
| Plantilla | Configuración inicial reutilizable para un caso de uso. |
| SaaS | Software como servicio, accesible habitualmente mediante navegador. |
| Integración | Conexión con servicios o sistemas externos. |
| Automatización | Acción realizada por el sistema ante una condición o evento. |
| Rol | Conjunto de permisos asignado a un usuario. |
| API | Interfaz para intercambiar datos o acciones entre sistemas. |
| PyME | Pequeña y mediana empresa. |
| Digitalización asistida | Proceso por el cual Ensambla convierte fotos de registros en papel en datos digitales, con revisión y confirmación de la persona. |
| IA multimodal | Modelo de inteligencia artificial capaz de interpretar imágenes y texto, incluida la escritura manuscrita. |
| OCR | Reconocimiento óptico de caracteres: técnica que extrae texto de una imagen. |
| Extracción estructurada | Obtención de datos ordenados en campos (fecha, monto, cliente…) a partir de una imagen o un texto libre. |
| Nivel de confianza | Indicador de cuán segura está la IA de un dato leído; los valores bajos se resaltan para revisión. |
| Registro | Cada fila de información guardada en un bloque, por ejemplo, una venta. |
