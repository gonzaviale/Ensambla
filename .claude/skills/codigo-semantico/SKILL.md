---
name: codigo-semantico
description: OBLIGATORIO siempre que se escriba, modifique, mueva o revise código Java del backend de Ensambla o sus tests, sin excepción. Impone código semántico y orientado a objetos - métodos cortos (≈10 líneas, un solo nivel de abstracción), método público que se lee como una frase que entiende alguien que no programa, todo el detalle en métodos privados con nombre de intención, cláusulas de guarda en lugar de if anidados, sin comentarios que repitan lo que el código dice y SIEMPRE herencia y polimorfismo (clase base abstracta + subclases, enum con constructor o fábrica) en lugar de switch o cadenas de if/else if por tipo o estado. En tests exige dado/cuando/entonces con helpers givenX/whenX/thenX, un comportamiento por test, sin lógica ni ternarios y base abstracta cuando varios tipos comparten comportamiento. Incluye receta de switch a jerarquía con el antes/después de Campo, anti-patrones, checklist de revisión, greps de control y la deuda conocida. Usar al crear, tocar o revisar cualquier clase de backend/src (main o test), al refactorizar, al revisar un PR y antes de cerrar cualquier tarea del backend. Si tocás un archivo, lo dejás semántico.
---

# Código semántico y polimorfismo (backend Ensambla)

Este skill es **obligatorio, no una sugerencia**. Nos criticaron tres cosas: código espagueti, falta de herencia y
polimorfismo, y código que no se lee solo. Ningún agente (Claude, Codex u otro) puede cerrar una tarea con un `switch`
por tipo, una cadena de `if/else if` por estado, métodos largos o tests ilegibles **en el código que tocó**. La calidad
va por encima de la velocidad: si hay que elegir entre entregar rápido y entregar legible, se entrega legible.

Las reglas de estructura (capas, ids, servicios) están en el skill `reglas-arquitectura` y en
[`backend/ARQUITECTURA.md`](../../../backend/ARQUITECTURA.md); la sección "Calidad de código" de ese documento tiene el
porqué de cada pilar. Este skill es cómo se escribe cada clase por dentro.

## Los pilares (todos obligatorios)

1. **Métodos cortos.** Un método tiene ≈10 líneas como máximo y **un solo nivel de abstracción**: o decide qué pasos
   siguen, o hace un detalle; nunca las dos cosas. Si no entra en 10 líneas, se parte **por significado**, no por
   cantidad de líneas.
2. **Semánticos.** El método público se lee como una frase y lo entiende alguien que no programa
   (`proyecto.exigirQueNoTengaRegistros()`, `suscripcion.activarPremium(pago, planId)`). Todo el detalle (`stream`,
   `filter`, comparaciones, armado de mensajes) va a **métodos privados con nombre de intención**
   (`tieneElPlanVencido()`, `esUnPagoDeOtraReferencia(pago)`), nunca nombres técnicos (`procesar`, `handle`, `check`,
   `aux`, `data`).
3. **Sin comentarios que expliquen lo que el código ya dice.** Si hace falta un comentario para entender *qué* hace, el
   método está mal nombrado o mal partido: renombralo o extraelo. El único comentario válido explica un **porqué no
   obvio** (una decisión de negocio, un workaround, una restricción externa). Nada de comentarios de sección
   (`// validar`, `// guardar`) ni código comentado.
4. **Los menos condicionales posibles.** Cláusulas de guarda al principio (`errorSi(...)`, `if (...) throw`) en lugar de
   `if` anidados; sin `else` después de un `return` o `throw`; los booleanos compuestos se nombran
   (`boolean esElegible = ...` o un método privado). Un `if` que pregunta *qué tipo es* no es una guarda: es
   polimorfismo faltante (pilar 5).
5. **Siempre polimorfismo y herencia, nunca `switch`/`if` por tipo o estado.** Cada vez que el comportamiento depende
   de un tipo, estado o categoría, ese comportamiento vive **en la clase de ese tipo**, no en quien lo consulta.
   Esto cubre `switch (tipo)`, `switch` sobre enum, cadenas de `if (x == Tipo.A) … else if (x == Tipo.B)`, `instanceof`
   en cascada y los ternarios encadenados por tipo.
6. **Nombres de dominio en español** (clases, métodos, variables, tests) y de intención, no de mecánica.
7. **Calidad por sobre todo.** No dejes deuda "para después" en lo que tocaste ni dejes un archivo peor de como lo
   encontraste.
8. **Los métodos públicos no llaman a repositorios, puertos ni servicios.** El `execute()` de un caso de uso y los
   métodos públicos de un servicio solo llaman a métodos privados con nombre de intención (y a métodos del modelo):
   cada llamada a un repositorio, puerto u otro servicio se extrae a un privado. Mal:
   `Campo campo = campoRepository.obtener(request.campoId(), negocioId);`. Bien:
   `Campo campo = obtenerCampo(request.campoId(), negocioId);`. Lo mismo en los tests: un `@Test` no usa `when(...)`,
   `verify(...)` ni arma datos inline; todo va a `givenX`/`whenX`/`thenX`.
9. **El dominio no conoce formatos de persistencia ni de transporte.** Nada de prefijos de clave (`"servidor:"`), JSON,
   ni `Map<?, ?>` con claves de texto (`"tipo"`, `"bloqueId"`) en `domain`: la infraestructura los convierte a objetos
   tipados (interfaz sellada + records, polimorfismo, mapa tipo→fábrica en el mapper) y el dominio solo habla de ids y
   tipos propios (`AvisoId`, `AccionDeAviso`).

## Regla de oro: si tocás un archivo, lo dejás semántico

No alcanza con que *tu línea* sea prolija. Si abrís un archivo para modificarlo o revisarlo y encontrás un método largo,
un `switch` por tipo o una cadena de `if/else if` por estado, **lo dejás semántico antes de cerrar** (con los tests
verdes y sin cambiar el comportamiento). Si el refactor excede la tarea (cientos de líneas, varios módulos), lo hacés
igual para el método que tocaste y anotás el resto en la lista de deuda de más abajo; nunca lo ignorás en silencio.

## Receta: de `switch` por enum a jerarquía

Pasos, en este orden y sin saltear ninguno:

1. **Identificá todos los sitios** que despachan por el mismo enum: `grep -rn "switch (" src/main/java/…/domain` y
   `grep -rn "== TipoX\." …`. Un mismo enum despachado en varios lugares es la señal más clara.
2. **Listá las preguntas que cada sitio le hace al tipo** (¿es válido este valor?, ¿cómo lo convierto?, ¿cómo lo
   muestro?, ¿es numérico?). Cada una es un **método de la clase base**.
3. **Creá la clase base abstracta** con el estado común y esos métodos. Los que todos resuelven igual quedan concretos;
   los que cambian por tipo son `abstract`; los predicados (`esNumerico()`, `esRelacion()`) devuelven `false` por
   defecto y los sobreescribe quien corresponda.
4. **Creá una subclase por *comportamiento*, no por valor del enum.** Los tipos que solo cambian el nombre cuelgan de
   una intermedia abstracta con el comportamiento compartido (`CampoDeTexto` → texto, email, teléfono, hora).
5. **Convertí el enum en discriminador con fábrica**: cada constante recibe el constructor de su clase
   (`TEXTO(CampoTexto::new)`) y expone `crear(...)`. **Sin `switch`** para elegir la clase.
6. **Reemplazá cada `switch`** por la llamada al método del objeto (`campo.errorDe(valor)`) y borrá el `switch`.
7. **Conservá la API pública** (builder, getters) para no romper consumidores; si cambiar de tipo antes mutaba, ahora
   devuelve el objeto nuevo del tipo correcto (`campo.reconfigurado(...)`).
8. **Pasá los tests existentes sin cambiar aserciones** y agregá una base abstracta de test por tipo (ver Tests).
9. **Corré los greps de control** (más abajo) y `./mvnw clean verify`.

### Antes / después: `Campo`

❌ **Antes:** la regla por tipo está en un `switch` dentro de la clase plana, repetido en cuatro lugares
(`Campo.errorDe`, `ConfiguracionCampo.errorDeValorInicial`, `FilaPropuesta.tipar`, `TablaReporte.celda`):

```java
public String errorDe(Object valor) {
    switch (tipo) {
        case TEXTO, EMAIL, TELEFONO, HORA -> { return errorDeTexto(valor); }
        case NUMERO, MONEDA -> { return errorDeNumero(valor); }
        case FECHA -> { return errorDeFecha(valor); }
        case OPCION -> { return errorDeOpcion(valor); }
        case SI_NO -> { return errorDeBooleano(valor); }
        default -> { return null; }
    }
}
```

✅ **Después:** `Campo` es abstracta y cada subclase sabe validarse; agregar un tipo es agregar una clase, no tocar cuatro
`switch`:

```java
public abstract class Campo {

    public abstract TipoCampo tipo();

    public abstract String errorDe(Object valor);

    public abstract String errorDeValorInicial(String valorInicial);

    public abstract Object tipar(String valor);

    public boolean esNumerico() {
        return false;
    }
}

public class CampoFecha extends Campo {

    @Override
    public String errorDe(Object valor) {
        return esFechaIso(valor) ? null : "debe ser una fecha válida";
    }
}

public enum TipoCampo {

    TEXTO(CampoTexto::new),
    NUMERO(CampoNumero::new),
    FECHA(CampoFecha::new);

    private final Function<DatosCampo, Campo> constructor;

    public Campo crear(DatosCampo datos) {
        return constructor.apply(datos);
    }
}
```

Jerarquía: `Campo` (abstracta) → `CampoDeTexto` (abstracta) → `CampoTexto`, `CampoTextoLargo`, `CampoEmail`,
`CampoTelefono`, `CampoHora`; `CampoNumerico` (abstracta) → `CampoNumero`, `CampoMoneda`; `CampoFecha`, `CampoOpcion`,
`CampoSiNo`, `CampoRelacion`, `CampoFormula`. El que consulta (`TablaReporte`, `FilaPropuesta`, `EditarCampo`) solo
llama `campo.celdaDeReporte(...)`, `campo.tipar(valor)`, `campo.esNumerico()`.

### El mismo patrón para estados

Un `if (estado == X) … else if (estado == Y) …` sobre un estado (suscripción activa/cancelada, renovación vencida) se
resuelve igual: el estado es una clase (o un enum con métodos por constante) que sabe qué hacer. Si el estado solo
cambia un dato, un enum con constructor alcanza; si cambia comportamiento, jerarquía.

## Cómo se ve un método semántico

❌ Largo, con detalle mezclado y comentarios que repiten el código:

```java
public void aplicarPago(PagoRemoto pago, Renovacion renovacion) {
    // verifico el pago
    if (pago == null || !pago.getReferencia().equals(renovacion.getReferencia())
            || pago.getMonto().compareTo(renovacion.getMonto()) != 0 || pago.getEstado() != Estado.APROBADO) {
        throw new BusinessRuleException("Pago inválido");
    }
    // activo premium
    suscripcion.setPlanId(planId);
    suscripcion.setEstado(Estado.ACTIVA);
    suscripcion.setVence(ahora.plusMonths(1));
}
```

✅ Corto, se lee como una frase, el detalle en el modelo:

```java
public void aplicarPago(PagoRemoto pago, RenovacionPremium renovacion, Suscripcion suscripcion) {
    renovacion.exigirPagoCoincidente(pago);
    suscripcion.activarPremium(pago, renovacion.planId());
}
```

## Métodos públicos sin llamadas a repositorios ni servicios

Ejemplo completo con `EditarCampo`:

```java
// ❌ el método público habla con los repositorios
public CampoItem execute(EditarCampoRequest request) {
    NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
    Campo campo = campoRepository.obtener(request.campoId(), negocioId);
    ...
}

// ✅ solo pasos con nombre de intención; el detalle (y los puertos) en privados
public CampoItem execute(EditarCampoRequest request) {
    NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
    Campo campo = obtenerCampo(request.campoId(), negocioId);
    Bloque bloque = obtenerBloqueDelCampo(campo, negocioId);
    return CampoItem.de(editarGuardando(bloque, campo, request));
}

private Campo obtenerCampo(CampoId campoId, NegocioId negocioId) {
    return campoRepository.obtener(campoId, negocioId);
}
```

El caso de uso se lee como una lista de pasos del negocio; dónde se busca cada cosa queda en los privados.

## El dominio habla en tipos, no en formatos

```java
// ❌ el dominio conoce el formato de transporte y de persistencia
String clave = "servidor:" + aviso.getId();
String tipo = (String) accion.get("tipo");            // Map<?, ?> con claves de texto
UUID bloqueId = UUID.fromString((String) accion.get("bloqueId"));

// ✅ el dominio habla de objetos tipados; la infraestructura convierte
public sealed interface AccionDeAviso permits EditarRegistro, CrearRegistro, Abrir, VerZona {}

public record EditarRegistro(RegistroId registroId) implements AccionDeAviso {}

// en el mapper de infraestructura: mapa tipo → fábrica, sin switch
private static final Map<String, Function<JsonNode, AccionDeAviso>> FABRICAS = Map.of(
        "editarRegistro", EditarRegistroDeJson::leer,
        "crearRegistro", CrearRegistroDeJson::leer);
// y la clave de contenido la arma la infraestructura: "servidor:" + avisoId.valor()
```

Las acciones del aviso (`AccionDeAviso`: `EditarRegistro`, `CrearRegistro`, `Abrir`, `VerZona`) y la clave de contenido
(el dominio habla de `AvisoId`, la infraestructura arma `"servidor:" + id`) son el caso de referencia.

## Cláusulas de guarda y condicionales

```java
// ❌ anidado
if (campo != null) {
    if (campo.esFormula()) {
        if (!usadoPorOtra) { eliminar(campo); }
    }
}

// ✅ guardas y un solo camino feliz
exigirQueExista(campo);
exigirQueNoLoUsenOtrasFormulas(campo);
eliminar(campo);
```

## Anti-patrones (prohibidos)

- `switch` o cadena de `if/else if` por tipo/estado/enum; `instanceof` en cascada; ternarios encadenados por tipo.
- Constantes `Set<TipoX> NUMERICOS = Set.of(...)` para preguntar "¿es de esta familia?" desde fuera (la clase responde
  `esNumerico()`).
- Métodos de más de ≈10 líneas o con dos niveles de abstracción mezclados.
- Métodos públicos que no se leen como una frase (`procesar`, `ejecutarLogica`, `manejar`).
- Comentarios que traducen el código (`// incrementa el contador`), comentarios de sección o código comentado.
- Fragmentar por fragmentar: un método de una línea sin significado propio (`paso1()`, `paso2()`) o que solo renombra
  una llamada. Un método extraído tiene que **nombrar una intención**.
- Booleanos compuestos inline de más de dos términos sin nombre.
- Clases "Utils"/"Helper" que concentran reglas de un tipo: esa regla va a la clase del tipo.
- `Object`/`String` como "tipo" con `if` por valor: se modela con enum o clase.
- Un método público (`execute()`, método de servicio) que llama directo a `*Repository`, `*Port` u otro servicio.
- Un `@Test` con `when(...)`, `verify(...)`, `assertThat` o datos armados inline: van a `givenX/whenX/thenX`.
- Prefijos de clave, JSON o `Map<?, ?>` con claves de texto dentro de `domain`.
- Lógica o ternarios en los tests (ver abajo).

## Tests: dado / cuando / entonces

Los tests siguen el mismo estándar y se leen como una especificación:

1. **Dado/cuando/entonces con helpers.** El cuerpo del test son tres líneas: `givenX()`, `whenX()`, `thenX()`
   (`givenContratoConPrecioAjeno()`, `whenSeActualizaElContrato()`, `thenSeRechazaElContrato()`). Todo el detalle
   (stubs, builders, `assertThat`, `verify`) va a helpers privados con nombre de intención, agrupados al final.
2. **Un comportamiento por test**, con `@DisplayName` en español que se entiende sin leer el código. Nombre
   `metodoShouldHacerXWhenY` en camelCase.
3. **Sin lógica en el test:** ni `if`, ni `for` con aserciones, ni ternarios, ni `caso == 6 ? … : …`. Si un
   `@ParameterizedTest` necesita decidir qué esperar según el caso, son tests distintos: uno por escenario.
4. **Base abstracta cuando varios tipos comparten comportamiento.** Una `CampoPorTipoTest` abstracta con los tests
   semánticos (`aceptaUnValorValido`, `rechazaUnValorInvalido`, `aceptaUnValorInicialValido`) y un test concreto por
   subclase que **solo aporta datos** (`valorValido()`, `valorInvalido()`, `mensajeDeError()`). Así cada subclase
   delgada queda cubierta por la base compartida (100 % por clase).
5. **Nombres de datos que se leen sin pensar** (`PRECIO_AJENO`, `OTRA_REFERENCIA`), no `X1`, `valor2`, `dto`.
6. Más detalle y plantillas por capa: skill `escribir-tests`.

```java
@Test
@DisplayName("Rechaza el contrato cuando el precio es de otro plan")
void actualizarContratoShouldRejectWhenPrecioIsAjeno() {
    givenContratoConPrecioAjeno();

    whenSeActualizaElContrato();

    thenSeRechazaElContrato();
}
```

## Checklist de revisión (todo en "sí" antes de cerrar)

- [ ] ¿Ningún método supera ≈10 líneas ni mezcla niveles de abstracción?
- [ ] ¿Cada método público se lee como una frase que entiende alguien que no programa?
- [ ] ¿Todo el detalle está en métodos privados (o en el modelo) con nombre de intención?
- [ ] ¿No hay comentarios que repitan el código (solo porqués no obvios)?
- [ ] ¿Hay cláusulas de guarda y no `if` anidados ni `else` tras `return`/`throw`?
- [ ] ¿No hay `switch` ni cadenas de `if/else if` por tipo o estado? ¿Hay jerarquía o enum con constructor?
- [ ] ¿Los métodos extraídos nombran una intención (no se fragmentó por fragmentar)?
- [ ] ¿Los métodos públicos llaman solo a privados con nombre de intención (ni un repositorio, puerto o servicio directo)?
- [ ] ¿El dominio habla en tipos propios, sin prefijos de clave, JSON ni `Map<?, ?>` con claves de texto?
- [ ] ¿Los tests son dado/cuando/entonces, un comportamiento por test, sin lógica ni ternarios?
- [ ] ¿Hay base abstracta de test cuando varios tipos comparten comportamiento?
- [ ] ¿Los archivos que toqué quedaron semánticos (regla de oro)?
- [ ] ¿Los greps de control salen vacíos y `./mvnw clean verify` está en verde?

## Greps de control

Desde `backend/`:

```bash
grep -rn "switch (" src/main/java/com/ensambla/backend/domain/model/campo   # vacío (lo verifica ArchitectureTest)
grep -rn "switch (" src/main/java/com/ensambla/backend/domain               # revisar cada resultado
grep -rn "else if" src/main/java/com/ensambla/backend/domain                # revisar cada resultado
grep -rnE "== TipoCampo\.|instanceof " src/main/java/com/ensambla/backend/domain
grep -nE "=\s*\w+(Repository|Port|Service)\.\w+\(" src/main/java/com/ensambla/backend/domain/useCase   # solo líneas dentro de métodos privados
grep -rn "servidor:" src/main   # solo en infrastructure
grep -rnE "Map<\?, ?\?>" src/main/java/com/ensambla/backend/domain   # vacío
```

En `domain/model/campo` el `switch (` está prohibido y `ArchitectureTest` falla. En el resto de `domain`, cada resultado
de los greps o se convierte en polimorfismo o se justifica por escrito (por ejemplo, un `switch` sobre un tipo externo
que no controlamos, como un código de respuesta de un proveedor). "Ya estaba así" no es una justificación.

## Deuda conocida (todavía con `switch` o ramas por tipo)

No es permiso para copiarlos: es la lista de lo que hay que convertir. Si tocás uno, lo dejás semántico (regla de oro).

- `LecturaCuaderno.validarDato`
- `InterpreteRegistrosSimulado`
- `SistemaItem.TIPOS_DE_CAMPO`
- `LecturaPresentationMapper`
- `ConversarEnTaller`
- `AtenderMensajeWhatsapp`

Cuando los resuelvas, borralos de esta lista en la fuente (`.claude/skills/codigo-semantico/SKILL.md`) y en la copia
para Codex.

Operan sobre records o strings, no sobre `Campo`: la salida es modelar el tipo (enum con comportamiento o jerarquía) y
delegar.
