# Arquitectura del backend

Fuente única de las reglas de estructura del backend. Los skills de [`../.claude/skills`](../.claude/skills), su copia
para Codex en [`../.agents/skills`](../.agents/skills) y los `AGENTS.md` resumen lo que está acá; si algo cambia, se
cambia primero acá. `ArchitectureTest` verifica las reglas que se pueden comprobar sin dependencias nuevas.

## Capas y flujo

Clean Architecture en dos capas. `domain` no conoce a `infrastructure` ni a Spring.

```
Controller → PresentationMapper → Caso de uso → (Servicio compartido) → Puerto → RepositoryImpl → JPA
```

```
com.ensambla.backend
├── domain                                   Java plano: sin Spring, sin JPA
│   ├── model/<area>/                        modelos, reglas puras, value objects, ids (<Modelo>Id) y enums del área
│   │   └── common/                          Identificador, Pagina, TextoNormalizado, Huella
│   ├── repository/<area>/                   puertos de persistencia
│   ├── port/                                puertos de salida: TransaccionPort, LectorCuadernoPort, SamiIaPort…
│   ├── service/<area>/                      solo lógica de varios pasos que repiten dos o más casos de uso
│   ├── useCase/<area>/
│   │   ├── <Area>Item.java                  DTOs que comparten varios casos de uso del área
│   │   └── <accion>/                        un subpaquete por caso de uso
│   │       ├── <Accion>.java                el caso de uso: un único execute()
│   │       ├── <Accion>Request.java         entrada (record), si hace falta
│   │       └── <Accion>Response.java        salida (record) con su fábrica de()
│   └── exception/                           ResourceNotFound, DuplicateResource, BusinessRule…
└── infrastructure
    ├── config/useCase/<Area>UseCaseConfig   los @Bean de casos de uso, servicios y reglas del dominio
    ├── config/IdentificadoresConfig         ids tipados ↔ JSON y parámetros HTTP
    ├── presentation/                        controller, dto web, mapper de presentación, GlobalExceptionHandler
    └── repository/                          entity, jpaRepository/{interfaces,implementation}, mapper, TransaccionAdapter
```

El módulo de referencia es `plantilla` para lecturas y `proyecto` para el CRUD con aislamiento por negocio.

## Las reglas

### 1. Los tests con base usan Testcontainers, nunca H2

**Por qué:** H2 en "modo PostgreSQL" no es PostgreSQL. Se saltea Flyway, no tiene `jsonb`, ni los mismos bloqueos ni
las mismas restricciones, y un test verde no garantiza nada en producción.

**Cómo:** todo test que levanta base o contexto extiende `support/PostgresRepositoryTestSupport` (perfil
`postgres-test`, Flyway y `ddl-auto=validate`). Los tests unitarios no levantan nada.

```java
// ✅
class BloqueSeguridadIntegrationTest extends PostgresRepositoryTestSupport { … }

// ❌
@SpringBootTest
@ActiveProfiles("h2-test")
class BloqueSeguridadIntegrationTest { … }
```

### 2. Los ids son value objects con tipo propio

**Por qué:** con `UUID` crudo nada impide pasar un `bloqueId` donde se espera un `campoId`; el compilador lo acepta y
el error aparece como un 404 raro. Con un tipo por entidad, el error sale al compilar.

**Cómo:**

- Un record por entidad, en el paquete de su modelo, que implementa `model/common/Identificador`:
  `public record ProyectoId(UUID valor) implements Identificador`. Rechaza `null` y ofrece `de(UUID)`, `de(String)`,
  `nuevo()` y un `toString()` que devuelve el UUID.
- Lo usan todas las capas: modelo, puertos, casos de uso, servicios, DTOs, controllers y DTOs web.
- Las `*Entity` y los `I*JpaRepository` siguen con `UUID`: el esquema no cambia. La conversión vive en los
  `*RepositoryMapper` (`Identificador.opcional(entity.getId(), ProyectoId::de)`, `Identificador.valorDe(proyecto.getId())`)
  y en los `*RepositoryImpl` (`proyectoId.valor()`, `Identificador.valores(ids)`).
- Los parámetros de `JdbcTemplate` llevan `.valor()`: el driver no conoce los ids tipados.
- `IdentificadoresConfig` serializa cualquier `Identificador` como el string del UUID y lo lee por tipo, y convierte
  `@PathVariable`/`@RequestParam`. El JSON no cambia y un id inválido sigue respondiendo 400.
- En el dominio solo los `*Id`, `Identificador`, `IdSistema` y `Huella` nombran `UUID`.

```java
// ✅
public Optional<Proyecto> findByIdAndNegocioId(ProyectoId proyectoId, NegocioId negocioId);

// ❌
public Optional<Proyecto> findByIdAndNegocioId(UUID proyectoId, UUID negocioId);
```

### 3. Los enums viven junto a su modelo

**Por qué:** un paquete `enums` agrupa por tipo de construcción, no por concepto; para entender `TipoCampo` hay que
saltar de paquete. Junto al modelo se lee todo el concepto en un lugar.

```
✅ domain/model/campo/TipoCampo.java
❌ domain/model/enums/TipoCampo.java
```

### 4. Los DTOs están en el subpaquete de su caso de uso

**Por qué:** el request y el response son parte del contrato del caso de uso; separarlos en `domain/dto` obliga a
navegar dos árboles paralelos y deja DTOs huérfanos cuando se borra el caso.

**Cómo:** `domain/useCase/<area>/<accion>/` contiene el caso de uso y sus DTOs. Un DTO que usan varios casos del área
sube a `domain/useCase/<area>/` (por ejemplo `ProyectoItem`). Si lo usan varias áreas, queda en el área dueña.

```
✅ domain/useCase/proyecto/crearProyecto/CrearProyectoRequest.java
❌ domain/dto/proyecto/CrearProyectoRequest.java
```

### 5. Los casos de uso no llevan el sufijo `UseCase`

**Por qué:** el paquete `useCase` ya dice qué es; el sufijo solo alarga los nombres.

**Cómo:** la clase se llama con la acción (`CrearProyecto`), su test `CrearProyectoTest` y su `@Bean` `crearProyecto()`.

```java
// ✅
public class CrearProyecto { public ProyectoItem execute(CrearProyectoRequest request) { … } }

// ❌
public class CrearProyectoUseCase { … }
```

### 6. Los DTOs se mapean solos; no hay mappers de dominio

**Por qué:** un mapper por caso de uso era una clase, un bean y un test más para lo que es una fábrica de una línea.

**Cómo:** dominio → DTO es una fábrica estática del record (`ProyectoItem.de(proyecto)`,
`ListarPlantillasResponse.de(plantillas)`). DTO → dominio, si hace falta, es un método del request. Los mappers de
infraestructura (`*PresentationMapper`, `*RepositoryMapper`) se mantienen: traducen entre capas, no dentro del dominio.

```java
// ✅
public record ListarPlantillasResponse(List<PlantillaItem> content) {

    public static ListarPlantillasResponse de(List<Plantilla> plantillas) {
        return new ListarPlantillasResponse(plantillas.stream().map(PlantillaItem::de).toList());
    }
}

// ❌
public class ListarPlantillasMapper { public ListarPlantillasResponse toResponse(List<Plantilla> plantillas) { … } }
```

### 7. Servicios solo para lógica compartida, sin Spring y sin saltos entre casos de uso

**Por qué:** un servicio que envuelve una línea (un `find…orElseThrow`, una llamada que pasa directo al puerto) es una
indirección: obliga a abrir dos archivos para leer un flujo y a mockear una capa más. Un caso de uso que llama a otro
caso de uso mezcla los flujos y esconde las dependencias reales. Y un dominio con `@Service`/`@Transactional` depende
del framework.

**Cómo:** cada pieza de lógica tiene un lugar, según qué es:

| Qué es | Dónde va |
|---|---|
| Traer una entidad o responder 404 (`findX().orElseThrow(...)`) | un método `default` del puerto, escrito una sola vez: `NegocioRepository.obtenerIdDelUsuario`, `BloqueRepository.obtener`… |
| Una llamada que sólo pasa al puerto | el caso de uso llama directo al puerto |
| Una regla pura, sin I/O | el modelo (método, función estática o value object): `PropuestaLectura.proponer`, `Zona.masCercana`, `ConsultaDireccion.de` |
| Lógica de varios pasos que usa un solo caso de uso | método privado de ese caso de uso |
| Lógica de varios pasos que repiten dos o más casos de uso | un servicio en `domain/service/<area>` |

- Los servicios que quedan son cinco, y existen porque su lógica se repite en varios casos de uso: `ControlPlanService`
  (cupos del plan con bloqueo del negocio y acceso a contenidos), `RenovacionPremiumService` (sincronización con el
  proveedor de pagos), `RegistroDatosService` (edición y validación de conexiones entre registros),
  `ReporteService` y `RecetaService`. Antes de sumar uno, comprobá con grep que lo usan dos o más casos de uso y que
  no son una o dos líneas.
- **Un caso de uso nunca llama a otro caso de uso.** Lo compartido se baja a un servicio, a un método `default` del
  puerto o al modelo.
- Un método `default` de un puerto lleva su test, con `mock(Puerto.class, CALLS_REAL_METHODS)`.
- Sin `@Service`, `@Component`, `@Transactional` ni ningún import de `org.springframework` en `domain`. Casos de uso y
  servicios se registran a mano como `@Bean` en el `<Area>UseCaseConfig`; el orden de los argumentos es el orden de los
  campos (`@RequiredArgsConstructor`).
- La frontera de la transacción la pone el caso de uso con `TransaccionPort` (lo implementa `TransaccionAdapter` con un
  `TransactionTemplate`, propagación REQUIRED). En los tests unitarios se usa `support/TransaccionDirecta`.

```java
// ✅ el puerto resuelve la búsqueda y el caso de uso sólo orquesta
NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
Proyecto proyecto = proyectoRepository.obtener(negocioId, request.proyectoId());

// ❌ un servicio por una línea
@Service
public class NegocioService {
    public NegocioId obtenerDelUsuario(UsuarioId id) {
        return negocioRepository.findIdByDuenioId(id).orElseThrow(...);
    }
}

// ❌ un caso de uso que llama a otro
public class ActualizarRenovacion {
    private final SincronizarRenovacion sincronizarRenovacion;
}
```

### 8. Las variantes se resuelven con polimorfismo, no con `switch`

**Por qué:** un `switch` o una cadena de `if` por tipo junta en un solo método el conocimiento de todas las variantes.
Para sumar una hay que abrir ese método, y cada variante nueva lo agranda y suma casos a su test. Con una clase por
variante, la regla de cada una vive junto a ella y sumar una es sumar una clase.

**Cómo:**

- Si el comportamiento cambia según un tipo (tipo de campo, de aviso, canal, comando, formato), ese tipo es una
  **interfaz** con una clase por variante.
- Si las variantes comparten el flujo y solo cambia un paso, van como **clase base abstracta con un método plantilla**:
  la base define el orden de los pasos y cada variante sobrescribe el que le toca.
- Un enum solo identifica a la variante. El comportamiento no se pregunta con `switch (tipo)`: se le pide a la clase
  (`regla.evaluar(contexto)`).
- Las variantes se registran como `@Bean` en el `*UseCaseConfig`, y quien las usa recibe la `List<Interfaz>` (el motor de
  avisos recibe todas las `ReglaAviso`) o un catálogo que elige la que corresponde. Sumar una variante no toca a quien
  las usa.
- Una cadena de `if` que pregunta por el tipo (`tipo == …`, `instanceof`) es lo mismo que un `switch`. Pasar un valor a
  otro (un texto a una constante) se resuelve con un `Map` o un método del enum, tampoco con `switch`.
- Los tests siguen el mismo esquema: una clase abstracta con el contrato de la interfaz y un test por implementación que
  la extiende (skill `escribir-tests`).
- `ArchitectureTest` falla ante cualquier `switch` que no esté en la lista de «Deuda conocida». Esa lista solo puede bajar.

```java
// ✅ una clase por variante; el motor no sabe cuántas hay
public interface ReglaAviso {
    TipoAviso tipo();
    List<ResultadoRegla> evaluar(ContextoNegocio contexto);
}
public class ReglaStockBajo implements ReglaAviso { … }
public class ReglaPrecioZona implements ReglaAviso { … }

// ✅ la base fija el flujo y cada canal pone su paso
public abstract class CanalPromocion {
    public final String redactar(Promocion promocion) { … armarPedido(promocion) … ajustarAlLargo(texto) … }
    protected abstract String armarPedido(Promocion promocion);
}

// ❌ todas las variantes en un solo método (Campo.errorDe)
return switch (tipo) {
    case TEXTO, TEXTO_LARGO, EMAIL, TELEFONO, HORA -> errorDeTexto(valor);
    case NUMERO, MONEDA -> errorSi(!esNumeroFinito(valor), "Tiene que ser un número");
    …
};
```

### 9. Un método se explica solo: el detalle va en métodos privados

**Por qué:** si hay que leer veinte líneas para entender qué hace un método, el nombre no dice nada y el código se
vuelve espagueti. Un método bien escrito se lee como una frase y cada paso se entiende por su nombre.

**Cómo:**

- El método público (`execute()` en un caso de uso) es una **lista corta de pasos**. Cada línea llama a un método con un
  nombre que dice qué hace (`faltantesDe(proyecto)`, `agruparPorProveedor(...)`, `respuestaCon(...)`). No hay bloques de
  lógica inline ni comentarios que expliquen lo que el nombre debería decir.
- **Todo** el detalle va en métodos privados, con un nombre de dominio en español. Cada método opera en un solo nivel de
  abstracción: o decide qué pasos dar, o hace uno de ellos.
- Sin `if` anidados. Se usan cláusulas de guarda, predicados con nombre (`estaAgotado()`, `tieneProveedor()`) o
  polimorfismo (regla 8).
- Sin números ni textos mágicos: constantes con nombre (`HORA_DEL_RESUMEN`, `MAXIMO_DE_ITEMS`).
- Los tests siguen igual: un test es `given → acción → then` y toda la mecánica va en métodos privados (skill
  `escribir-tests`).

```java
// ✅ se lee como una frase
public ArmarPedidoProveedorResponse execute(ArmarPedidoProveedorRequest request) {
    NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
    Proyecto proyecto = proyectoRepository.obtener(negocioId, request.proyectoId());
    PedidoReposicion pedido = pedidoDeLoQueFalta(proyecto, request);
    return respuestaConPdf(pedido, proyecto);
}

// ❌ la lógica queda a la vista y hay que descifrarla
public ArmarPedidoProveedorResponse execute(ArmarPedidoProveedorRequest request) {
    …
    for (Registro r : inventario.registros()) {
        if (cantidad != null) {
            if (cantidad.signum() <= 0 || (minimo != null && cantidad.compareTo(minimo) <= 0)) {
                BigDecimal pedir = minimo == null ? BigDecimal.TEN : minimo.multiply(BigDecimal.valueOf(2))…
```

## Otras reglas que siguen vigentes

- **Aislamiento por negocio:** el `NegocioId` sale de la sesión, jamás del body, la URL ni un query param. Un recurso
  ajeno responde 404, igual que uno inexistente.
- **Tiempo:** solo los casos de uso tienen `Clock`; los servicios reciben el instante por parámetro.
- **Flyway:** una migración aplicada nunca se edita.
- **Nombres de dominio en español.**
- **Cobertura:** `./mvnw clean verify` exige 100 % de líneas y ramas por clase.

## Qué verifica `ArchitectureTest`

| Regla | Cómo |
|---|---|
| `domain` no depende de `infrastructure` ni de Spring | bytecode de `domain` sin `com/ensambla/backend/infrastructure` ni `org/springframework` |
| Sin sufijo `UseCase` | ninguna clase de `domain/useCase` termina en `UseCase` |
| Casos de uso independientes | el bytecode de un caso de uso no referencia a otro caso de uso |
| Sin `domain/dto`, `domain/mapper` ni `domain/model/enums` | los paquetes no existen |
| Ids tipados | en los fuentes de `domain`, solo los `*Id`, `Identificador`, `IdSistema` y `Huella` nombran `UUID` |
| Sin H2 | no hay `application-h2-test.properties` ni `org.h2.Driver` en el classpath |
| Sin `switch` nuevos | en los fuentes de `src/main/java`, solo los archivos de «Deuda conocida» tienen un `switch`; y esa lista no guarda archivos que ya lo eliminaron |

La regla 9 (métodos que se explican solos) y que las variantes tengan su test de contrato no se pueden verificar
automáticamente: se revisan a mano antes de cerrar.

## Deuda conocida (regla 8)

Código anterior a la regla 8, que decide por tipo con un `switch`. No se toca mientras no se acomode esa parte, y cada
archivo que se corrija se saca de la lista `SWITCH_CONOCIDOS` de `ArchitectureTest`.

| Archivo | Qué decide con un `switch` |
|---|---|
| `domain/model/campo/Campo` | el error de un valor según el `TipoCampo` |
| `domain/model/campo/ConfiguracionCampo` | el error del valor inicial según el `TipoCampo` |
| `domain/model/reporte/TablaReporte` | cómo se convierte la celda de un reporte según el `TipoCampo` |
| `domain/model/lectura/LecturaCuaderno` | cómo se comprueba el dato leído según el tipo del campo |
| `domain/model/whatsapp/FilaPropuesta` | cómo se tipa un valor según el tipo del campo (y un texto de sí o no) |
| `domain/model/zona/VistaZona` | cómo se ordena la zona según la pestaña |
| `domain/model/pago/RenovacionPremium` | el estado propio que corresponde al que informa el proveedor |
| `domain/model/registro/DireccionOrden` | pasar un texto a una dirección de orden |
| `domain/useCase/sami/conversarConSami/ConversarConSamiResponse` | la propuesta de la respuesta según el tipo de paso |
| `infrastructure/pago/MercadoPagoSuscripcionesAdapter` | cómo se identifica el contrato según el tipo de notificación |
| `infrastructure/reporte/ExportadorReporteXlsxAdapter` | cómo se escribe una celda según la clase del valor |
| `infrastructure/presentation/mapper/zona/ZonaPresentationMapper` | pasar el texto de una vista a `VistaZona` |
| `infrastructure/ia/InterpreteRegistrosSimulado` | el dato simulado según el tipo del campo |

El acomodo previsto es el de siempre: una interfaz por variante y una clase por tipo (por ejemplo, un validador por
cada `TipoCampo`). La receta está en el skill `refactor-use-case`.

## Controles rápidos

Desde `backend/`, todos deberían salir vacíos:

```bash
grep -rl "org.springframework" src/main/java/com/ensambla/backend/domain
find src/main/java/com/ensambla/backend/domain -name "*UseCase.java"
ls -d src/main/java/com/ensambla/backend/domain/{dto,mapper,model/enums} 2>/dev/null
grep -rlw "UUID" src/main/java/com/ensambla/backend/domain | grep -v "Id.java$\|Identificador\|IdSistema\|Huella"
grep -n "h2database" pom.xml; ls src/test/resources/application-h2-test.properties 2>/dev/null
grep -rlE "\bswitch\s*\(" src/main/java        # solo los de «Deuda conocida»
```
