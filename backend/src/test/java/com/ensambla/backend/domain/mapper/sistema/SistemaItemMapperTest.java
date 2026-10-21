package com.ensambla.backend.domain.mapper.sistema;

import com.ensambla.backend.domain.dto.sistema.SistemaItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.BloqueItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.CampoItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.OpcionesItem;
import com.ensambla.backend.domain.dto.sistema.SistemaItem.SeccionItem;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.AnchoBloque;
import com.ensambla.backend.domain.model.enums.ColorBloque;
import com.ensambla.backend.domain.model.enums.DistribucionProyecto;
import com.ensambla.backend.domain.model.enums.PlantillaSistema;
import com.ensambla.backend.domain.model.enums.TipoBloque;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.seccion.Seccion;
import com.ensambla.backend.domain.model.sistema.IdSistema;
import com.ensambla.backend.domain.model.sistema.Sistema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SistemaItemMapperTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID SISTEMA_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID SECCION_ID = UUID.fromString("d1000000-0000-4000-8000-000000000001");
    private static final UUID OTRA_SECCION_ID = UUID.fromString("d1000000-0000-4000-8000-000000000002");
    private static final UUID INVENTARIO_ID = UUID.fromString("d2000000-0000-4000-8000-000000000001");
    private static final UUID CLIENTES_ID = UUID.fromString("d2000000-0000-4000-8000-000000000002");
    private static final UUID NOTA_ID = UUID.fromString("d2000000-0000-4000-8000-000000000003");
    private static final UUID FILA_ID = UUID.fromString("d3000000-0000-4000-8000-000000000001");
    private static final UUID CLIENTE_FILA_ID = UUID.fromString("d3000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-01-10T09:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-21T10:00:00Z");

    private final SistemaItemMapper mapper = new SistemaItemMapper();

    // ===== pantalla -> backend =====

    @Test
    @DisplayName("Mapea el proyecto: nombre, plantilla, distribución, opciones, demo y fechas")
    void toSistemaShouldMapProyecto() {
        SistemaItem item = givenItem("e" + SISTEMA_ID, List.of(givenSeccion(SECCION_ID.toString(), List.of())),
                "shop", new OpcionesItem(true, false), "two", true, "2026-01-10T09:00:00Z");

        Proyecto proyecto = mapper.toSistema(NEGOCIO_ID, "e" + SISTEMA_ID, item, AHORA).proyecto();

        assertThat(proyecto.getId()).isEqualTo(SISTEMA_ID);
        assertThat(proyecto.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(proyecto.getNombre()).isEqualTo("Mi almacén");
        assertThat(proyecto.getPlantillaClave()).isEqualTo(PlantillaSistema.ALMACEN);
        assertThat(proyecto.getDistribucion()).isEqualTo(DistribucionProyecto.DOS);
        assertThat(proyecto.getConMovimientos()).isTrue();
        assertThat(proyecto.getConGastos()).isFalse();
        assertThat(proyecto.getEsDemo()).isTrue();
        assertThat(proyecto.getCreadoEn()).isEqualTo(CREADO);
        assertThat(proyecto.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Sin plantilla, distribución ni opciones usa los valores por defecto")
    void toSistemaShouldUseDefaultsForMissingProyectoData() {
        SistemaItem item = givenItem(SISTEMA_ID.toString(), List.of(), null, null, null, false, null);

        Proyecto proyecto = mapper.toSistema(NEGOCIO_ID, SISTEMA_ID.toString(), item, AHORA).proyecto();

        assertThat(proyecto.getPlantillaClave()).isNull();
        assertThat(proyecto.getDistribucion()).isEqualTo(DistribucionProyecto.MOSAICO);
        assertThat(proyecto.getConMovimientos()).isFalse();
        assertThat(proyecto.getConGastos()).isFalse();
        assertThat(proyecto.getEsDemo()).isFalse();
        assertThat(proyecto.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Los gastos se activan solos y una distribución desconocida vuelve al mosaico")
    void toSistemaShouldReadExpensesAndUnknownLayout() {
        SistemaItem item = givenItem(SISTEMA_ID.toString(), List.of(), "warehouse", new OpcionesItem(false, true),
                "otra", false, "no es una fecha");

        Proyecto proyecto = mapper.toSistema(NEGOCIO_ID, SISTEMA_ID.toString(), item, AHORA).proyecto();

        assertThat(proyecto.getPlantillaClave()).isEqualTo(PlantillaSistema.DEPOSITO);
        assertThat(proyecto.getConMovimientos()).isFalse();
        assertThat(proyecto.getConGastos()).isTrue();
        assertThat(proyecto.getDistribucion()).isEqualTo(DistribucionProyecto.MOSAICO);
        assertThat(proyecto.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Mapea secciones y bloques con su posición, su sección y sus datos")
    void toSistemaShouldMapSeccionesAndBloques() {
        BloqueItem inventario = givenBloque(INVENTARIO_ID.toString(), "inventory", "Productos", "coral", "wide",
                List.of(), List.of());
        BloqueItem nota = new BloqueItem(NOTA_ID.toString(), "note", "Notas", null, "petrol", "full", List.of(),
                List.of(), "Llamar al proveedor", true, -1);
        SistemaItem item = givenItem(SISTEMA_ID.toString(), List.of(
                givenSeccion(SECCION_ID.toString(), List.of(inventario)),
                givenSeccion(OTRA_SECCION_ID.toString(), List.of(nota))), null, null, null, false, null);

        Sistema sistema = mapper.toSistema(NEGOCIO_ID, SISTEMA_ID.toString(), item, AHORA);

        assertThat(sistema.secciones()).extracting(Seccion::getId).containsExactly(SECCION_ID, OTRA_SECCION_ID);
        assertThat(sistema.secciones()).extracting(Seccion::getPosicion).containsExactly((short) 0, (short) 1);
        assertThat(sistema.secciones().getFirst().getNombre()).isEqualTo("Vista general");
        assertThat(sistema.secciones().getFirst().getProyectoId()).isEqualTo(SISTEMA_ID);
        List<Bloque> bloques = sistema.proyecto().getBloques();
        assertThat(bloques).extracting(Bloque::getId).containsExactly(INVENTARIO_ID, NOTA_ID);
        assertThat(bloques).extracting(Bloque::getPosicion).containsExactly((short) 0, (short) 1);
        assertThat(bloques).extracting(Bloque::getSeccionId).containsExactly(SECCION_ID, OTRA_SECCION_ID);
        Bloque primero = bloques.getFirst();
        assertThat(primero.getTipo()).isEqualTo(TipoBloque.INVENTARIO);
        assertThat(primero.getColor()).isEqualTo(ColorBloque.CORAL);
        assertThat(primero.getAncho()).isEqualTo(AnchoBloque.ANCHO);
        assertThat(primero.getDescripcion()).isEqualTo("Lo que vendo");
        assertThat(primero.getContenido()).isEmpty();
        assertThat(primero.getEmpiezaFila()).isFalse();
        assertThat(primero.getDireccionStock()).isNull();
        Bloque segundo = bloques.getLast();
        assertThat(segundo.getTipo()).isEqualTo(TipoBloque.NOTA);
        assertThat(segundo.getDescripcion()).isEmpty();
        assertThat(segundo.getContenido()).isEqualTo("Llamar al proveedor");
        assertThat(segundo.getEmpiezaFila()).isTrue();
        assertThat(segundo.getDireccionStock()).isEqualTo((short) -1);
    }

    @Test
    @DisplayName("Un tipo, color o ancho desconocido usa el valor por defecto y la dirección de stock debe ser 1 o -1")
    void toSistemaShouldDefaultUnknownBlockData() {
        BloqueItem desconocido = new BloqueItem(INVENTARIO_ID.toString(), "raro", "X", "", "violeta", "enorme",
                List.of(), List.of(), "", null, 2);
        BloqueItem entradas = new BloqueItem(CLIENTES_ID.toString(), "entries", "Entradas", "", "yellow", "third",
                List.of(), List.of(), "", false, 1);
        SistemaItem item = givenItem(SISTEMA_ID.toString(),
                List.of(givenSeccion(SECCION_ID.toString(), List.of(desconocido, entradas))),
                null, null, null, false, null);

        List<Bloque> bloques = mapper.toSistema(NEGOCIO_ID, SISTEMA_ID.toString(), item, AHORA).proyecto().getBloques();

        assertThat(bloques.getFirst().getTipo()).isEqualTo(TipoBloque.FORMULARIO);
        assertThat(bloques.getFirst().getColor()).isEqualTo(ColorBloque.CELESTE);
        assertThat(bloques.getFirst().getAncho()).isEqualTo(AnchoBloque.MEDIO);
        assertThat(bloques.getFirst().getDireccionStock()).isNull();
        assertThat(bloques.getLast().getTipo()).isEqualTo(TipoBloque.ENTRADAS);
        assertThat(bloques.getLast().getColor()).isEqualTo(ColorBloque.AMARILLO);
        assertThat(bloques.getLast().getAncho()).isEqualTo(AnchoBloque.TERCIO);
        assertThat(bloques.getLast().getDireccionStock()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("Mapea las columnas con su tipo, clave, opciones, mínimo y conexión")
    void toSistemaShouldMapCampos() {
        List<CampoItem> campos = List.of(
                new CampoItem("name", "Producto", "text", true, null, null, null),
                new CampoItem("price", "Precio", "currency", false, null, new BigDecimal("0"), null),
                new CampoItem("category", "Categoría", "select", false, List.of("Insumos", "Terminados"), null, null),
                new CampoItem("client", "Cliente", "relation", false, null, null, CLIENTES_ID.toString()),
                new CampoItem("notes", "Notas", "textarea", false, null, null, null),
                new CampoItem("ok", "Activo", "checkbox", false, null, null, null),
                new CampoItem("day", "Día", "date", false, null, null, null),
                new CampoItem("hour", "Hora", "time", false, null, null, null),
                new CampoItem("mail", "Correo", "email", false, null, null, null),
                new CampoItem("phone", "Teléfono", "tel", false, null, null, null),
                new CampoItem("qty", "Cantidad", "number", false, null, null, null));
        SistemaItem item = givenItem(SISTEMA_ID.toString(), List.of(givenSeccion(SECCION_ID.toString(), List.of(
                givenBloque(INVENTARIO_ID.toString(), "inventory", "Productos", "blue", "half", campos, List.of()),
                givenBloque(CLIENTES_ID.toString(), "clients", "Clientes", "blue", "half", List.of(), List.of())))),
                null, null, null, false, null);

        List<Campo> resultado = mapper.toSistema(NEGOCIO_ID, SISTEMA_ID.toString(), item, AHORA)
                .proyecto().getBloques().getFirst().getCampos();

        assertThat(resultado).extracting(Campo::getTipo).containsExactly(
                TipoCampo.TEXTO, TipoCampo.MONEDA, TipoCampo.OPCION, TipoCampo.RELACION, TipoCampo.TEXTO_LARGO,
                TipoCampo.SI_NO, TipoCampo.FECHA, TipoCampo.HORA, TipoCampo.EMAIL, TipoCampo.TELEFONO,
                TipoCampo.NUMERO);
        assertThat(resultado).extracting(Campo::getClave).startsWith("name", "price", "category", "client");
        assertThat(resultado).extracting(Campo::getPosicion).startsWith((short) 0, (short) 1, (short) 2);
        assertThat(resultado.getFirst().getNombre()).isEqualTo("Producto");
        assertThat(resultado.getFirst().esObligatorio()).isTrue();
        assertThat(resultado.get(1).getMinimo()).isEqualByComparingTo("0");
        assertThat(resultado.get(2).getOpciones()).containsExactly("Insumos", "Terminados");
        assertThat(resultado.getFirst().getOpciones()).isEmpty();
        assertThat(resultado.get(3).getBloqueRelacionadoId()).isEqualTo(CLIENTES_ID);
        assertThat(resultado.getFirst().getBloqueRelacionadoId()).isNull();
        assertThat(resultado.getFirst().getId()).isEqualTo(IdSistema.aUuid(INVENTARIO_ID, "name"));
    }

    @Test
    @DisplayName("Corrige lo que el modelo no admite: opciones vacías, conexión a un bloque ausente, claves inválidas o repetidas")
    void toSistemaShouldRepairInvalidCampos() {
        UUID campoConUuid = UUID.fromString("d4000000-0000-4000-8000-000000000001");
        List<CampoItem> campos = List.of(
                new CampoItem("estado", "Estado", "select", false, List.of(), null, null),
                new CampoItem("sin-opciones", "Sin opciones", "select", false, null, null, null),
                new CampoItem("cliente", "Cliente", "relation", false, null, null, "bloque-que-no-esta"),
                new CampoItem("sin-destino", "Sin destino", "relation", false, null, null, null),
                new CampoItem("repetida", "Una", "text", false, null, null, null),
                new CampoItem("repetida", "Otra", "text", false, null, null, null),
                new CampoItem(campoConUuid.toString(), "Con UUID", "text", false, null, null, null),
                new CampoItem("no valida!", "Clave inválida", "text", false, null, null, null),
                new CampoItem("rara", "Rara", "tipo-que-no-existe", false, null, null, null));
        SistemaItem item = givenItem(SISTEMA_ID.toString(), List.of(givenSeccion(SECCION_ID.toString(), List.of(
                givenBloque(INVENTARIO_ID.toString(), "inventory", "Productos", "blue", "half", campos, List.of())))),
                null, null, null, false, null);

        List<Campo> resultado = mapper.toSistema(NEGOCIO_ID, SISTEMA_ID.toString(), item, AHORA)
                .proyecto().getBloques().getFirst().getCampos();

        assertThat(resultado.get(0).getTipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(resultado.get(1).getTipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(resultado.get(2).getTipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(resultado.get(2).getBloqueRelacionadoId()).isNull();
        assertThat(resultado.get(3).getTipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(resultado.get(4).getClave()).isEqualTo("repetida");
        assertThat(resultado.get(5).getClave()).isNull();
        assertThat(resultado.get(6).getId()).isEqualTo(campoConUuid);
        assertThat(resultado.get(7).getClave()).isNull();
        assertThat(resultado.get(8).getTipo()).isEqualTo(TipoCampo.TEXTO);
    }

    @Test
    @DisplayName("Guarda las filas por el id de la columna y respeta el orden")
    void toSistemaShouldMapFilasByCampoId() {
        List<CampoItem> campos = List.of(
                new CampoItem("name", "Producto", "text", false, null, null, null),
                new CampoItem("price", "Precio", "currency", false, null, null, null),
                new CampoItem("client", "Cliente", "relation", false, null, null, CLIENTES_ID.toString()));
        Map<String, Object> primera = fila("e" + FILA_ID, "name", "Harina", "price", 1200, "client", "e" + CLIENTE_FILA_ID);
        primera.put("__example", true);
        primera.put("__source", "Lectura de foto");
        primera.put("desconocida", "se descarta");
        Map<String, Object> segunda = fila(FILA_ID.toString().replace("d3000000-0000-4000-8000-000000000001",
                "d3000000-0000-4000-8000-000000000009"), "name", null, "client", "");
        Map<String, Object> tercera = new HashMap<>();
        tercera.put("name", "Sin id");
        tercera.put("client", null);
        tercera.put("__source", 7);
        SistemaItem item = givenItem(SISTEMA_ID.toString(), List.of(givenSeccion(SECCION_ID.toString(), List.of(
                givenBloque(INVENTARIO_ID.toString(), "inventory", "Productos", "blue", "half", campos,
                        List.of(primera, segunda, tercera)),
                givenBloque(CLIENTES_ID.toString(), "clients", "Clientes", "blue", "half", List.of(), List.of())))),
                null, null, null, false, null);

        Sistema sistema = mapper.toSistema(NEGOCIO_ID, SISTEMA_ID.toString(), item, AHORA);

        List<Registro> filas = sistema.registrosPorBloque().get(INVENTARIO_ID);
        UUID nombreId = IdSistema.aUuid(INVENTARIO_ID, "name");
        UUID precioId = IdSistema.aUuid(INVENTARIO_ID, "price");
        UUID clienteId = IdSistema.aUuid(INVENTARIO_ID, "client");
        assertThat(filas).extracting(Registro::getOrden).containsExactly(0, 1, 2);
        Registro harina = filas.getFirst();
        assertThat(harina.getId()).isEqualTo(FILA_ID);
        assertThat(harina.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(harina.getBloqueId()).isEqualTo(INVENTARIO_ID);
        assertThat(harina.getValores()).containsOnly(
                Map.entry(nombreId.toString(), "Harina"),
                Map.entry(precioId.toString(), 1200),
                Map.entry(clienteId.toString(), CLIENTE_FILA_ID.toString()));
        assertThat(harina.getEsEjemplo()).isTrue();
        assertThat(harina.getOrigen()).isEqualTo("Lectura de foto");
        assertThat(filas.get(1).getValores()).containsEntry(nombreId.toString(), null).containsEntry(clienteId.toString(), "");
        assertThat(filas.get(1).getEsEjemplo()).isFalse();
        assertThat(filas.get(1).getOrigen()).isNull();
        assertThat(filas.get(2).getId()).isNotNull();
        assertThat(filas.get(2).getOrigen()).isNull();
        assertThat(sistema.registrosPorBloque().get(CLIENTES_ID)).isEmpty();
    }

    @Test
    @DisplayName("Si dos columnas comparten clave, la fila usa la primera; un origen largo se recorta")
    void toSistemaShouldUseFirstColumnForRepeatedKeysAndTrimOrigin() {
        List<CampoItem> campos = List.of(
                new CampoItem("repetida", "Una", "text", false, null, null, null),
                new CampoItem("repetida", "Otra", "text", false, null, null, null));
        Map<String, Object> fila = fila(FILA_ID.toString(), "repetida", "valor");
        fila.put("__source", "x".repeat(300));
        SistemaItem item = givenItem(SISTEMA_ID.toString(), List.of(givenSeccion(SECCION_ID.toString(), List.of(
                givenBloque(INVENTARIO_ID.toString(), "inventory", "Productos", "blue", "half", campos,
                        List.of(fila))))), null, null, null, false, null);

        Sistema sistema = mapper.toSistema(NEGOCIO_ID, SISTEMA_ID.toString(), item, AHORA);

        Registro registro = sistema.registrosPorBloque().get(INVENTARIO_ID).getFirst();
        List<Campo> columnas = sistema.proyecto().getBloques().getFirst().getCampos();
        assertThat(registro.getValores()).containsOnlyKeys(columnas.getFirst().getId().toString());
        assertThat(registro.getOrigen()).hasSize(120);
    }

    @Test
    @DisplayName("Los nombres vacíos se reemplazan y los largos se recortan")
    void toSistemaShouldSanitizeNames() {
        BloqueItem bloque = new BloqueItem(INVENTARIO_ID.toString(), "form", "   ", "", "blue", "half",
                List.of(new CampoItem("a", "x".repeat(200), "text", false, null, null, null),
                        new CampoItem("b", null, "text", false, null, null, null)),
                List.of(), "", false, null);
        SistemaItem item = new SistemaItem(SISTEMA_ID.toString(), "  " + "n".repeat(150) + "  ", null, null,
                List.of(new SeccionItem(SECCION_ID.toString(), null, List.of(bloque))), null, null, null, false);

        Sistema sistema = mapper.toSistema(NEGOCIO_ID, SISTEMA_ID.toString(), item, AHORA);

        assertThat(sistema.proyecto().getNombre()).hasSize(120).startsWith("nnn");
        assertThat(sistema.secciones().getFirst().getNombre()).isEqualTo("Sin nombre");
        Bloque resultado = sistema.proyecto().getBloques().getFirst();
        assertThat(resultado.getNombre()).isEqualTo("Sin nombre");
        assertThat(resultado.getCampos().getFirst().getNombre()).hasSize(120);
        assertThat(resultado.getCampos().getLast().getNombre()).isEqualTo("Sin nombre");
    }

    // ===== backend -> pantalla =====

    @Test
    @DisplayName("Devuelve el documento que espera la pantalla, con ids, claves y filas por clave")
    void toResponseShouldBuildDocument() {
        Sistema sistema = givenSistema();

        SistemaItem item = mapper.toResponse(sistema);

        assertThat(item.id()).isEqualTo(SISTEMA_ID.toString());
        assertThat(item.name()).isEqualTo("Mi almacén");
        assertThat(item.createdAt()).isEqualTo("2026-01-10T09:00:00Z");
        assertThat(item.updatedAt()).isEqualTo("2026-10-21T10:00:00Z");
        assertThat(item.template()).isEqualTo("shop");
        assertThat(item.options()).isEqualTo(new OpcionesItem(true, false));
        assertThat(item.layout()).isEqualTo("three");
        assertThat(item.demoAlmacen()).isTrue();
        assertThat(item.sections()).extracting(SeccionItem::id)
                .containsExactly(SECCION_ID.toString(), OTRA_SECCION_ID.toString());
        SeccionItem general = item.sections().getFirst();
        assertThat(general.name()).isEqualTo("Vista general");
        assertThat(general.blocks()).extracting(BloqueItem::id)
                .containsExactly(INVENTARIO_ID.toString(), CLIENTES_ID.toString());
        BloqueItem inventario = general.blocks().getFirst();
        assertThat(inventario.type()).isEqualTo("inventory");
        assertThat(inventario.color()).isEqualTo("orange");
        assertThat(inventario.width()).isEqualTo("half");
        assertThat(inventario.description()).isEqualTo("Lo que vendo");
        assertThat(inventario.content()).isEmpty();
        assertThat(inventario.startRow()).isFalse();
        assertThat(inventario.stockDirection()).isEqualTo(1);
        assertThat(inventario.fields()).extracting(CampoItem::key).containsExactly("name", "categoria", "cliente");
        assertThat(inventario.fields().getFirst()).isEqualTo(
                new CampoItem("name", "Producto", "text", true, null, new BigDecimal("1"), null));
        assertThat(inventario.fields().get(1).type()).isEqualTo("select");
        assertThat(inventario.fields().get(1).options()).containsExactly("Insumos");
        assertThat(inventario.fields().get(2).type()).isEqualTo("relation");
        assertThat(inventario.fields().get(2).target()).isEqualTo(CLIENTES_ID.toString());
    }

    @Test
    @DisplayName("Las filas vuelven con su id, las marcas y los valores por la clave de la columna")
    void toResponseShouldBuildRowsByKey() {
        Sistema sistema = givenSistema();

        BloqueItem inventario = mapper.toResponse(sistema).sections().getFirst().blocks().getFirst();

        assertThat(inventario.rows()).hasSize(2);
        Map<String, Object> primera = inventario.rows().getFirst();
        assertThat(primera).containsEntry("id", FILA_ID.toString())
                .containsEntry("name", "Harina")
                .containsEntry("__example", true)
                .containsEntry("__source", "Aviso de Sami")
                .containsEntry("cliente", CLIENTE_FILA_ID.toString())
                .doesNotContainKey("categoria");
        Map<String, Object> segunda = inventario.rows().getLast();
        assertThat(segunda).containsOnlyKeys("id", "name").containsEntry("name", null);
    }

    @Test
    @DisplayName("Una columna sin clave usa su id, y una fórmula se muestra como número")
    void toResponseShouldFallBackToCampoIdAndFormulaAsNumber() {
        UUID sinClaveId = UUID.fromString("d4000000-0000-4000-8000-000000000001");
        UUID formulaId = UUID.fromString("d4000000-0000-4000-8000-000000000002");
        Bloque bloque = givenBloque(INVENTARIO_ID, SECCION_ID, 0, TipoBloque.GASTOS, List.of(
                Campo.builder().id(formulaId).bloqueId(INVENTARIO_ID).nombre("Total").tipo(TipoCampo.FORMULA)
                        .obligatorio(false).posicion((short) 1).build(),
                Campo.builder().id(sinClaveId).bloqueId(INVENTARIO_ID).nombre("Monto").tipo(TipoCampo.MONEDA)
                        .obligatorio(true).posicion((short) 0).build()));
        Registro fila = Registro.builder().id(FILA_ID).bloqueId(INVENTARIO_ID).esEjemplo(false)
                .valores(Map.of(sinClaveId.toString(), 50)).build();
        Sistema sistema = new Sistema(givenProyecto(List.of(bloque)), List.of(givenSeccion(SECCION_ID, 0)),
                Map.of(INVENTARIO_ID, List.of(fila)));

        BloqueItem resultado = mapper.toResponse(sistema).sections().getFirst().blocks().getFirst();

        assertThat(resultado.type()).isEqualTo("expenses");
        assertThat(resultado.fields()).extracting(CampoItem::key)
                .containsExactly(sinClaveId.toString(), formulaId.toString());
        assertThat(resultado.fields().getLast().type()).isEqualTo("number");
        assertThat(resultado.rows().getFirst()).containsEntry(sinClaveId.toString(), 50);
    }

    @Test
    @DisplayName("Un proyecto sin secciones se muestra con una sección Vista general que contiene sus bloques")
    void toResponseShouldCreateGeneralSectionWhenThereAreNone() {
        Bloque bloque = givenBloque(INVENTARIO_ID, null, 0, TipoBloque.FORMULARIO, List.of());
        Sistema sistema = new Sistema(givenProyecto(List.of(bloque)), List.of(), Map.of());

        SistemaItem item = mapper.toResponse(sistema);

        assertThat(item.sections()).singleElement().satisfies(seccion -> {
            assertThat(seccion.name()).isEqualTo("Vista general");
            assertThat(seccion.id()).isEqualTo(IdSistema.aFront(IdSistema.aUuid(SISTEMA_ID, "general")));
            assertThat(seccion.blocks()).extracting(BloqueItem::id).containsExactly(INVENTARIO_ID.toString());
        });
        assertThat(item.template()).isNull();
    }

    @Test
    @DisplayName("Los bloques sin sección van a la primera y los demás a la suya, ordenados por posición")
    void toResponseShouldPlaceOrphanBlocksInFirstSection() {
        Bloque huerfano = givenBloque(NOTA_ID, null, 5, TipoBloque.NOTA, List.of());
        Bloque ajeno = givenBloque(CLIENTES_ID, UUID.randomUUID(), 3, TipoBloque.CLIENTES, List.of());
        Bloque propio = givenBloque(INVENTARIO_ID, SECCION_ID, 1, TipoBloque.INVENTARIO, List.of());
        Bloque deLaOtra = givenBloque(UUID.fromString("d2000000-0000-4000-8000-000000000009"), OTRA_SECCION_ID, 0,
                TipoBloque.VENTAS, List.of());
        Sistema sistema = new Sistema(givenProyecto(List.of(huerfano, ajeno, propio, deLaOtra)),
                List.of(givenSeccion(OTRA_SECCION_ID, 1), givenSeccion(SECCION_ID, 0)), Map.of());

        List<SeccionItem> secciones = mapper.toResponse(sistema).sections();

        assertThat(secciones).extracting(SeccionItem::id)
                .containsExactly(SECCION_ID.toString(), OTRA_SECCION_ID.toString());
        assertThat(secciones.getFirst().blocks()).extracting(BloqueItem::id)
                .containsExactly(INVENTARIO_ID.toString(), CLIENTES_ID.toString(), NOTA_ID.toString());
        assertThat(secciones.getLast().blocks()).hasSize(1);
    }

    @Test
    @DisplayName("Mapea una lista de sistemas")
    void toResponseShouldMapList() {
        assertThat(mapper.toResponse(List.of(givenSistema(), givenSistema()))).hasSize(2);
    }

    @Test
    @DisplayName("Guardar y volver a leer devuelve el mismo documento")
    void roundTripShouldKeepDocument() {
        List<CampoItem> campos = List.of(
                new CampoItem("name", "Producto", "text", true, null, null, null),
                new CampoItem("category", "Categoría", "select", false, List.of("Insumos"), null, null),
                new CampoItem("client", "Cliente", "relation", false, null, null, CLIENTES_ID.toString()));
        Map<String, Object> fila = fila(FILA_ID.toString(), "name", "Harina", "category", "Insumos",
                "client", CLIENTE_FILA_ID.toString());
        SistemaItem item = new SistemaItem(SISTEMA_ID.toString(), "Mi almacén", "2026-01-10T09:00:00Z",
                "2026-10-21T10:00:00Z", List.of(givenSeccion(SECCION_ID.toString(), List.of(
                givenBloque(INVENTARIO_ID.toString(), "inventory", "Productos", "orange", "half", campos, List.of(fila)),
                givenBloque(CLIENTES_ID.toString(), "clients", "Clientes", "orange", "half", List.of(), List.of())))),
                "shop", new OpcionesItem(true, true), "mosaic", false);

        SistemaItem resultado = mapper.toResponse(mapper.toSistema(NEGOCIO_ID, SISTEMA_ID.toString(), item, AHORA));

        assertThat(resultado.sections().getFirst().blocks().getFirst().fields())
                .usingRecursiveComparison().isEqualTo(campos);
        assertThat(resultado.sections().getFirst().blocks().getFirst().rows()).containsExactly(fila);
        assertThat(resultado.options()).isEqualTo(new OpcionesItem(true, true));
        assertThat(resultado.template()).isEqualTo("shop");
        assertThat(resultado.layout()).isEqualTo("mosaic");
    }

    // --- arrange ---
    private SistemaItem givenItem(String id, List<SeccionItem> secciones, String plantilla, OpcionesItem opciones,
                                  String distribucion, boolean demo, String creado) {
        return new SistemaItem(id, "Mi almacén", creado, null, secciones, plantilla, opciones, distribucion, demo);
    }

    private SeccionItem givenSeccion(String id, List<BloqueItem> bloques) {
        return new SeccionItem(id, "Vista general", bloques);
    }

    private BloqueItem givenBloque(String id, String tipo, String nombre, String color, String ancho,
                                   List<CampoItem> campos, List<Map<String, Object>> filas) {
        return new BloqueItem(id, tipo, nombre, "Lo que vendo", color, ancho, campos, filas, null, null, null);
    }

    private Map<String, Object> fila(String id, Object... clavesYValores) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", id);
        for (int i = 0; i < clavesYValores.length; i += 2) {
            fila.put((String) clavesYValores[i], clavesYValores[i + 1]);
        }
        return fila;
    }

    private Sistema givenSistema() {
        UUID nombreId = UUID.fromString("d4000000-0000-4000-8000-000000000001");
        UUID categoriaId = UUID.fromString("d4000000-0000-4000-8000-000000000002");
        UUID clienteId = UUID.fromString("d4000000-0000-4000-8000-000000000003");
        Bloque inventario = givenBloque(INVENTARIO_ID, SECCION_ID, 0, TipoBloque.INVENTARIO, List.of(
                Campo.builder().id(clienteId).bloqueId(INVENTARIO_ID).nombre("Cliente").tipo(TipoCampo.RELACION)
                        .obligatorio(false).clave("cliente").bloqueRelacionadoId(CLIENTES_ID).posicion((short) 2).build(),
                Campo.builder().id(nombreId).bloqueId(INVENTARIO_ID).nombre("Producto").tipo(TipoCampo.TEXTO)
                        .obligatorio(true).clave("name").minimo(new BigDecimal("1")).posicion((short) 0).build(),
                Campo.builder().id(categoriaId).bloqueId(INVENTARIO_ID).nombre("Categoría").tipo(TipoCampo.OPCION)
                        .obligatorio(false).clave("categoria").opciones(List.of("Insumos")).posicion((short) 1).build()));
        inventario.setColor(ColorBloque.NARANJA);
        inventario.setDireccionStock((short) 1);
        Bloque clientes = givenBloque(CLIENTES_ID, SECCION_ID, 1, TipoBloque.CLIENTES, List.of());
        Registro harina = Registro.builder().id(FILA_ID).bloqueId(INVENTARIO_ID).esEjemplo(true)
                .origen("Aviso de Sami").orden(0)
                .valores(Map.of(nombreId.toString(), "Harina", clienteId.toString(), CLIENTE_FILA_ID.toString()))
                .build();
        Map<String, Object> vacios = new HashMap<>();
        vacios.put(nombreId.toString(), null);
        Registro sinNombre = Registro.builder().id(CLIENTE_FILA_ID).bloqueId(INVENTARIO_ID).esEjemplo(false)
                .orden(1).valores(vacios).build();
        Proyecto proyecto = givenProyecto(new ArrayList<>(List.of(inventario, clientes)));
        proyecto.setPlantillaClave(PlantillaSistema.ALMACEN);
        proyecto.setDistribucion(DistribucionProyecto.TRES);
        proyecto.setConMovimientos(true);
        proyecto.setEsDemo(true);
        return new Sistema(proyecto,
                List.of(givenSeccion(OTRA_SECCION_ID, 1), givenSeccion(SECCION_ID, 0)),
                Map.of(INVENTARIO_ID, List.of(harina, sinNombre)));
    }

    private Proyecto givenProyecto(List<Bloque> bloques) {
        return Proyecto.builder().id(SISTEMA_ID).negocioId(NEGOCIO_ID).nombre("Mi almacén")
                .creadoEn(CREADO).actualizadoEn(AHORA).bloques(bloques).build();
    }

    private Seccion givenSeccion(UUID id, int posicion) {
        return Seccion.builder().id(id).proyectoId(SISTEMA_ID).nombre(id.equals(SECCION_ID) ? "Vista general" : "Ventas")
                .posicion((short) posicion).build();
    }

    private Bloque givenBloque(UUID id, UUID seccionId, int posicion, TipoBloque tipo, List<Campo> campos) {
        return Bloque.builder().id(id).proyectoId(SISTEMA_ID).seccionId(seccionId).nombre("Bloque")
                .posicion((short) posicion).tipo(tipo).descripcion(tipo == TipoBloque.INVENTARIO ? "Lo que vendo" : "")
                .campos(campos).build();
    }
}
