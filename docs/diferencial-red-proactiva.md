# Ensambla — Diferencial: la red proactiva

> Anexo para sumar al documento *Nuevo rumbo del producto* · Oct 2026 · @Facu
> Los productos, nombres y números de los ejemplos son ilustrativos.

---

## En una frase

**Ensambla centraliza los precios, el stock y las ofertas entre comercios y proveedores, cruza esos datos y te avisa lo que importa: qué precio corregir, qué te falta y dónde conviene comprar. Vos solo confirmás con un toque.**

El documento ya identifica el dolor más caro (vender por debajo del costo) y la función que nos diferencia (importar la lista del proveedor y aplicar las reglas de cada comercio). Esta propuesta la lleva un paso más allá: **Ensambla deja de ser una app para un comercio y pasa a ser la red que conecta a los comercios con sus proveedores**, cruza los datos de todos y avisa por su cuenta cuando algo requiere atención.

Para defenderlo en una oración: **es la única herramienta que te dice cuánto cobrar y cuánto pagar, sin que tengas que aprender nada.** Treinta te deja *anotar*; Ensambla te dice *qué hacer*.

---

## 1. Por qué esto es el diferencial

Presentado como "una app para el almacenero", Ensambla compite con Treinta, Kyte y cualquier planilla. Como **red**, compite por algo que esas herramientas no tienen y no pueden copiar en un mes: los datos que se generan cuando muchos comercios y sus proveedores trabajan sobre el mismo catálogo emparejado.

```
 Proveedores y mayoristas            Ensambla                         Cada comercio
 suben su lista y ofertas   ──►   empareja productos, cruza    ──►   recibe SUS precios con SUS
 una sola vez                      costos, stock y ventas de          reglas, sus alertas y las
                                   toda la red y decide qué avisar    ofertas que le sirven

 + Cada comercio que se suma mejora la red: más emparejamientos aprendidos,
   mejores comparaciones de costos y más motivos para que el próximo proveedor suba su lista.
```

La pieza que lo hace posible ya está en el plan: el **emparejamiento de productos** ("HARINA 000 PUREZA X10 1KG" = "Harina Pureza 1 kg"). Una vez que dos comercios y un proveedor hablan del mismo producto, se puede comparar, avisar y recomendar. Sin emparejamiento no hay red; con él, cada lista sirve para todos.

**Sencillez + red, cada una con su trabajo:**

- **La sencillez es la puerta de entrada.** Es lo que hace que alguien que nunca usaría Treinta empiece a usar Ensambla. Se puede copiar.
- **La red es lo que hace que se quede.** No se copia, porque no está en el código: está en el uso acumulado.

---

## 2. Lo proactivo: no hay que ir a buscar un reporte, el sistema avisa

Cada aviso es una sola línea, en lenguaje del comercio, con una acción de un toque. Nada se aplica sin confirmación.

| Alerta | Qué detecta | Cómo le llega | Un toque |
|---|---|---|---|
| **Precio en riesgo** | Subió el costo y el margen quedó debajo del mínimo del comercio | "El aceite Natura 900 ml quedó con 9% de margen. ¿Lo paso a $2.450?" | Aplicar |
| **Lista nueva** | Llegó la lista de un proveedor y cambió algo de lo que el comercio vende | "Subieron 31 productos, 8% en promedio. Tus reglas ya calcularon los precios nuevos." | Revisar y aplicar |
| **Stock por agotarse** | El ritmo de ventas indica que un producto se termina antes de la próxima compra | "Al ritmo de esta semana, la yerba se termina el jueves." | Agregar al pedido |
| **Oferta que sirve** | Un proveedor publicó una oferta de algo que ese comercio realmente vende | "Distribuidora Sur tiene la Coca 2,25 L 8% más barata hasta el viernes." | Ver oferta |
| **Comprás caro** | El comercio paga un producto bastante más que el promedio de su zona | "La harina Pureza 1 kg te sale 14% más que a otros almacenes del barrio." | Ver proveedores |
| **Mejor proveedor** | Otro proveedor de la red tiene el mismo producto a menor costo | "El azúcar Ledesma está $120 más barato en Mayorista Norte." | Comparar |
| **Cambio raro** | Aumento fuera de lo normal o producto que el proveedor dejó de tener | "La lavandina Ayudín subió 35% de golpe. ¿Error de la lista?" | Confirmar o ignorar |
| **Resumen del día** | Ventas, márgenes y faltantes en una sola pantalla a la noche | "Vendiste $184.000. El aceite lo vendés casi al costo desde el martes. Faltan yerba y azúcar." | Compartir precios |

**Reglas para que lo proactivo no se vuelva ruido:**

- **Pocas alertas y relevantes:** se agrupan ("5 productos con margen bajo") y se ordenan por plata en juego.
- **El comercio decide el umbral:** margen mínimo, cuándo avisar faltantes, qué ofertas quiere ver.
- **Cada alerta explica el porqué** en una línea ("subió el costo de $1.900 a $2.150").
- **Si una alerta se ignora siempre, se apaga sola** o pregunta si la quiere seguir recibiendo.

---

## 3. En conjunto con la sencillez: la complejidad la absorbe la red, no el comerciante

Cruzar datos de muchos comercios y proveedores es complejo; usar Ensambla no tiene que serlo. El diferencial solo funciona si se mantiene la promesa del documento: **no tener que aprender a usarlo**.

| Principio | Cómo se ve en el producto |
|---|---|
| **Vive donde ya está el comerciante** | En el celular. Reenviar la lista o contar lo que pasó, escrito o por audio, como en WhatsApp. Ideal a futuro: un número de WhatsApp de Ensambla, sin instalar nada. |
| **Una línea, un toque** | Toda alerta y toda carga se confirma en una línea: "Entendí: compra de 10 cajas de Coca 2 L a $18.000. ¿Guardo?" |
| **Nada automático sin permiso** | Los precios nuevos se proponen; la primera vez de cada emparejamiento se confirma; después el sistema lo recuerda. |
| **Sin tablas ni menús** | El comerciante ve productos, proveedores, ofertas y avisos: las palabras que ya usa. |
| **Se arma solo** | El relato del onboarding prende los bloques; las alertas vienen activadas con valores razonables para el rubro. |

---

## 4. ¿Por qué nos eligen y por qué no nos copian?

Copiar la app, cualquiera puede. La pregunta es por qué alguien se quedaría con nosotros y no con un clon. De más fuerte a más débil:

1. **El catálogo maestro (el activo central).** Cada vez que un comercio confirma que "HARINA 000 PUREZA X10 1KG" del mayorista A, "Harina Pureza 1kg" del mayorista B y "harina pureza" de su cuaderno son el mismo producto, ese emparejamiento sirve para **todos** los comercios. Con el tiempo se arma el diccionario de todos los productos del barrio, con todos sus nombres y todos sus costos. Un clon arranca con ese diccionario vacío: tiene la app, pero no el mapa.
2. **Lo que cada comercio ya le enseñó al sistema.** Sus reglas de precio, sus sinónimos ("Coca" = Coca-Cola 2,25 L), su historial de costos y sus proveedores. Irse es volver a enseñarle todo a otra app desde cero.
3. **Efecto de red de dos lados.** Más comercios → los mayoristas quieren estar. Más mayoristas → los comercios reciben más listas y ofertas. Y la comparación "comprás caro" **solo existe** si hay suficientes comercios en la zona: un clon con tres usuarios no la puede ofrecer.
4. **Neutralidad.** Si un mayorista hace su propia app, nunca va a mostrar que otro vende más barato. Ensambla sí, porque está del lado del comercio. Esa es una razón para elegirnos que ningún proveedor puede igualar.
5. **Hábito.** La lista de las 8, contar lo que pasa durante el día, el resumen de las 21. Si se vuelve parte del día, cambiarse cuesta aunque el otro sea igual.

**Con honestidad:** al principio no hay barrera; las barreras aparecen con la **densidad**. Por eso conviene crecer **barrio por barrio**: 30 almacenes de un barrio valen más que 300 dispersos, porque ahí la comparación y el catálogo funcionan de verdad.

### ¿Eliminamos intermediarios?

No conviene decirlo así.

- **El mayorista no se elimina:** el almacén le sigue comprando. Presentarlo como "sacamos al intermediario" convierte a los mayoristas en enemigos.
- **Lo que se elimina es el trabajo manual con la información:** pasar listas a mano, comparar en tres chats, calcular márgenes en el cuaderno.
- **Lo que se reduce es la asimetría de información:** hoy el mayorista sabe todos los precios y el almacenero no puede comparar.
- En los hechos, **pasamos a ser el intermediario de la información**. Ese es el lugar valioso.

### Cómo arrancar sin depender de los mayoristas

No hace falta que los mayoristas se sumen para empezar: **los comercios ya reciben las listas** por WhatsApp; con que las reenvíen, la red se arma desde abajo. Cada lista reenviada enriquece el catálogo para todos los que le compran a ese mayorista. Cuando haya 50 almacenes de un mayorista en Ensambla, va a ser él quien quiera subir la lista directamente.

Además, cada comercio obtiene valor el primer día aunque esté solo (precios con sus reglas, alertas de margen). La red suma encima.

---

## 5. El otro lado de la red: qué gana el proveedor y quién paga

El documento menciona el efecto de red como una nota al pie. Lo proponemos como el centro del modelo: **conseguir un mayorista es conseguir a todos sus clientes.**

| Actor | Qué gana | Qué podría pagar *(hipótesis a validar)* |
|---|---|---|
| **Comercio** | Precios siempre al día, avisos de margen y faltantes, ofertas que le sirven, saber si compra caro | Plan básico gratis; plan con comparación de proveedores y más alertas |
| **Proveedor / mayorista** | Canal directo con sus clientes, lista cargada una vez, ofertas que llegan solo a quien vende ese producto, demanda agregada y anónima de la zona | Suscripción para publicar listas y ofertas; costo por oferta enviada o por pedido generado |

**Regla de oro del modelo:** el mayorista paga por **llegar a más comercios**, nunca por **esconder información**. Si paga para que no se vea que otro vende más barato, perdemos la neutralidad, que es el diferencial.

**Test del documento aplicado:**

| Pregunta | Carga por audio sola | Red proactiva |
|---|---|---|
| ¿Le ahorra algo medible? | Sí, algo de tiempo | Sí: plata (margen recuperado, compras más baratas) y tiempo |
| ¿Sigue siendo nuestro si Treinta lo copia en un mes? | No | Sí: la red de proveedores, los emparejamientos aprendidos y los datos de precios por zona no se copian |
| ¿Se explica en una oración sin decir "IA"? | Sí | "Tu proveedor manda la lista y vos ya tenés tus precios, tus faltantes y las ofertas que te convienen." |

**Cuidados que son condición, no detalle:**

- **Privacidad:** los datos de un comercio nunca se muestran a otro. Las comparaciones son agregadas y anónimas, y solo aparecen cuando hay suficientes comercios en la zona (por ejemplo, 5 o más).
- **Neutralidad:** si un mayorista paga, no puede esconder ofertas mejores de otro.
- **Confianza en el emparejamiento:** un producto mal emparejado es un precio mal calculado. Se muestra la coincidencia y su seguridad, y se confirma la primera vez.
- **Términos claros** sobre qué datos se usan y para qué, aceptados en el onboarding.

---

## 6. Cómo se extiende a servicios: el caso del instalador de aires acondicionados

El instalador tiene el mismo dolor que el almacenero: **le compra a proveedores con precios que cambian todo el tiempo y cobra con números que se desactualizan.** Cambia lo que vende:

- **El almacén** vende **productos**: el precio sale del costo.
- **El instalador** vende **trabajos que consumen productos**: el precio sale de materiales + mano de obra, y su "precio" es el **presupuesto**.

**Su dolor más caro:** pasa un presupuesto el lunes, el cliente confirma el viernes, va a comprar y el caño de cobre, el gas y los soportes subieron. Hace el trabajo al precio que dio y **pierde plata sin enterarse**. Es el mismo "vender por debajo del costo", con otra forma.

### Cómo lo usaría

**Onboarding:** cuenta *"Instalo aires acondicionados, le compro a dos casas de refrigeración, trabajo solo con mi camioneta"*, y Ensambla prende otros bloques:

| Bloque | Para qué |
|---|---|
| Materiales y proveedores | Siempre, igual que el almacén: caño de cobre, gas R410A, soportes, cables |
| **Presupuestos** | Materiales a costo actual + mano de obra + su margen |
| Trabajos (agenda) | Qué instala, dónde y cuándo |
| Stock de la camioneta | Lo que lleva encima, no un depósito |
| Cuentas de clientes | Señas y saldos ("me pagó la mitad") |
| Mantenimientos | Clientes a los que hay que volver a ver cada año |

**Contando lo que pasa:**

- *"Presupuesto para Gómez, split de 4500 frigorías, 5 metros de caño"* → lo arma con los costos de hoy y su regla de mano de obra, y genera la imagen o el PDF para WhatsApp con la validez ("válido 7 días").
- *"Instalé en lo de Pérez, usé 4 metros de caño y un soporte, me pagó la mitad"* → registra el trabajo, descuenta el material de la camioneta y anota el saldo de Pérez.
- *Reenvía la lista de la casa de refrigeración* → igual que el almacén: se empareja con sus materiales y se actualizan los costos.

**Lo proactivo, adaptado:**

- *"El presupuesto de Gómez quedó desactualizado: el caño subió 12%. Si lo hacés a ese precio, ganás $18.000 menos. ¿Lo actualizo antes de que confirme?"*
- *"Para los 3 trabajos de esta semana te faltan 2 soportes y 6 metros de caño."*
- *"El presupuesto de Fernández vence mañana y no respondió."*
- *"Hace un año instalaste en lo de Ruiz: ¿le ofrecés el mantenimiento?"* (trabajo que vuelve y hoy se pierde)
- *"Refrigeración Sur tiene el gas R410A 10% más barato esta semana."*

### Por qué suma al diferencial

1. **El núcleo es el mismo:** costos del proveedor, emparejamiento, reglas y alertas. Solo cambian los bloques que prende el onboarding, que es lo que promete el documento: *el sistema que se adapta a tu negocio*.
2. **La red crece en vertical:** las casas de refrigeración tienen cientos de instaladores como clientes, y el catálogo maestro de materiales sirve para todos.
3. **Para servicios, la alerta de presupuesto desactualizado pega más fuerte que en el almacén:** el almacenero cambia el precio mañana; el instalador ya se comprometió con el cliente.

**Alcance:** el MVP sigue siendo **un solo rubro, el almacén**. Los servicios se presentan como **la prueba de que el modelo se extiende**, no como algo para construir ahora. En la defensa responde a *"¿esto sirve solo para almacenes?"*: es el mismo motor, con otros bloques prendidos por el relato.

---

## 7. Cómo lo validamos antes de programarlo

### Prueba "mago de Oz": 1 mayorista, 5 almacenes, 2 semanas

Antes de construir la red, la simulamos a mano. Si el resultado hecho a mano no le cambia la semana a un almacenero, el software tampoco lo va a hacer. Y si le sirve, nos quedamos con las listas y frases reales que el MVP necesita.

| Paso | Qué hacemos |
|---|---|
| 1. Reclutar | Un mayorista de barrio y 5 almacenes que le compran y reciben la lista por WhatsApp, PDF o Excel. |
| 2. Medir hoy | Cronometrar cómo actualiza precios cada almacén con su método actual y qué deja sin hacer. |
| 3. Simular la red | Cuando llega la lista, el equipo (a mano, con una planilla) devuelve por WhatsApp los precios nuevos con las reglas de cada almacén y las alertas de la sección 2. |
| 4. Observar | Qué alertas usan, cuáles ignoran, qué precios aplican, si vuelven a reenviar la lista solos. |
| 5. Pedir compromiso | Al almacén: seña o preventa simbólica. Al mayorista: carta de intención para ofrecerlo a sus clientes. |

### Qué cuenta como éxito (fijado antes de empezar)

| Métrica | Umbral propuesto |
|---|---|
| Productos por debajo del costo detectados en casos reales | Al menos 1 caso real por almacén |
| Almacenes que aplican los precios propuestos sin ayuda | 4 de 5 |
| Almacenes que vuelven a reenviar la lista sin recordatorio | 3 de 5 |
| Alertas accionadas / alertas enviadas | Más de la mitad |
| Compromiso concreto | Mayorista con carta de intención, o 2 de 5 almacenes con seña |

Los umbrales son decisiones del equipo para este piloto, no estándares del mercado. Para la defensa, la evidencia más fuerte no es el tiempo ahorrado sino los casos reales: *"en dos semanas encontramos N productos que se vendían por debajo del costo"*.

---

## 8. Qué cambia en el MVP

El orden del documento se mantiene. Se suma poco, porque lo proactivo reutiliza lo que ya está planeado:

- **Dentro del MVP:** alertas de precio en riesgo y de lista nueva (usan las reglas de precio que ya están en el MVP) y aviso de stock por agotarse (usa los movimientos de compras y ventas contadas).
- **Para la demo:** datos sembrados con 2 proveedores y 3 comercios, para mostrar la comparación de costos y una oferta que llega solo a quien vende ese producto.
- **Después del MVP:** red de proveedores real, ofertas publicadas por mayoristas, comparación por zona con datos reales, canal de WhatsApp y el rubro servicios (instaladores).

---

## En síntesis

- **La sencillez** hace que entren.
- **El catálogo maestro y la red** hacen que se queden.
- **La neutralidad** hace que nos elijan antes que a la app de un mayorista.
- **El arranque** es desde abajo: los comercios reenvían las listas que ya reciben, barrio por barrio.
- **El mismo motor** sirve para comercios que venden productos y para servicios que venden trabajos.
