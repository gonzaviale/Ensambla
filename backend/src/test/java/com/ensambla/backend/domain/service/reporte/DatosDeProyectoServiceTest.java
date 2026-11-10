package com.ensambla.backend.domain.service.reporte;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DatosDeProyectoServiceTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId INVENTARIO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId ENTRADAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000003");
    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000004");
    private static final BloqueId NOTA_ID = BloqueId.de("b0000000-0000-4000-8000-000000000005");
    private static final CampoId TOTAL_ID = CampoId.de("f0000000-0000-4000-8000-000000000023");

    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final DatosDeProyectoService service = new DatosDeProyectoService(registroRepository);

    @Test
    @DisplayName("Carga las tablas de inventario, ventas y gastos con sus campos por clave y sin las filas de ejemplo")
    void cargarShouldLoadInventoryExpensesAndSalesWithoutExamples() {
        Registro real = registro(INVENTARIO_ID, false);
        Registro ejemplo = registro(INVENTARIO_ID, true);
        Registro venta = registro(VENTAS_ID, false);
        Registro gasto = registro(GASTOS_ID, false);
        when(registroRepository.findAllByBloqueIdAndNegocioId(INVENTARIO_ID, NEGOCIO_ID)).thenReturn(List.of(real, ejemplo));
        when(registroRepository.findAllByBloqueIdAndNegocioId(VENTAS_ID, NEGOCIO_ID)).thenReturn(List.of(venta));
        when(registroRepository.findAllByBloqueIdAndNegocioId(GASTOS_ID, NEGOCIO_ID)).thenReturn(List.of(gasto));
        Proyecto proyecto = Proyecto.builder().nombre("Almacén").bloques(List.of(
                bloque(INVENTARIO_ID, TipoBloque.INVENTARIO, campo("name"), campo(null)),
                bloque(ENTRADAS_ID, TipoBloque.ENTRADAS), bloque(VENTAS_ID, TipoBloque.VENTAS, campo("product")),
                bloque(GASTOS_ID, TipoBloque.GASTOS, campo("total")), bloque(NOTA_ID, TipoBloque.NOTA))).build();

        DatosDeProyecto datos = service.cargar(proyecto, NEGOCIO_ID);

        assertThat(datos.nombre()).isEqualTo("Almacén");
        assertThat(datos.inventarios()).singleElement().satisfies(inventario -> {
            assertThat(inventario.bloque().getId()).isEqualTo(INVENTARIO_ID);
            assertThat(inventario.entradasId()).isEqualTo(ENTRADAS_ID);
            assertThat(inventario.camposPorClave()).containsOnlyKeys("name");
            assertThat(inventario.registros()).containsExactly(real);
        });
        assertThat(datos.ventas()).singleElement().satisfies(ventas -> {
            assertThat(ventas.bloque().getId()).isEqualTo(VENTAS_ID);
            assertThat(ventas.registros()).containsExactly(venta);
        });
        assertThat(datos.gastos()).singleElement().satisfies(gastos -> {
            assertThat(gastos.bloque().getId()).isEqualTo(GASTOS_ID);
            assertThat(gastos.tieneImporte()).isTrue();
            assertThat(gastos.registros()).containsExactly(gasto);
            assertThat(gastos.valorDe(gasto, "total")).isEqualTo(1500);
            assertThat(gastos.valorDe(gasto, "date")).isNull();
        });
    }

    @Test
    @DisplayName("Sin tabla de entradas el inventario no la nombra, y una tabla de gastos sin importe no se puede sumar")
    void cargarShouldAllowProjectsWithoutEntriesOrExpenseAmount() {
        when(registroRepository.findAllByBloqueIdAndNegocioId(INVENTARIO_ID, NEGOCIO_ID)).thenReturn(List.of());
        when(registroRepository.findAllByBloqueIdAndNegocioId(GASTOS_ID, NEGOCIO_ID)).thenReturn(List.of());
        Proyecto proyecto = Proyecto.builder().nombre("Almacén").bloques(List.of(
                bloque(INVENTARIO_ID, TipoBloque.INVENTARIO), bloque(GASTOS_ID, TipoBloque.GASTOS))).build();

        DatosDeProyecto datos = service.cargar(proyecto, NEGOCIO_ID);

        assertThat(datos.inventarios().getFirst().entradasId()).isNull();
        assertThat(datos.ventas()).isEmpty();
        assertThat(datos.gastos().getFirst().tieneImporte()).isFalse();
    }

    @Test
    @DisplayName("Carga el nombre de cada fila de las tablas de clientes, sin las de ejemplo ni las sin nombre")
    void cargarShouldLoadClientNamesById() {
        CampoId nombreId = CampoId.de("f0000000-0000-4000-8000-000000000031");
        BloqueId clientesId = BloqueId.de("b0000000-0000-4000-8000-000000000006");
        BloqueId clientesSinNombreId = BloqueId.de("b0000000-0000-4000-8000-000000000007");
        Registro rosa = clienteConNombre(clientesId, nombreId, "  Doña Rosa ", false);
        Registro ejemplo = clienteConNombre(clientesId, nombreId, "Cliente de ejemplo", true);
        Registro enBlanco = clienteConNombre(clientesId, nombreId, "   ", false);
        Registro sinNombre = Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(clientesId)
                .valores(java.util.Map.of()).build();
        when(registroRepository.findAllByBloqueIdAndNegocioId(clientesId, NEGOCIO_ID))
                .thenReturn(List.of(rosa, ejemplo, enBlanco, sinNombre));
        when(registroRepository.findAllByBloqueIdAndNegocioId(clientesSinNombreId, NEGOCIO_ID)).thenReturn(List.of());
        Proyecto proyecto = Proyecto.builder().nombre("Almacén").bloques(List.of(
                Bloque.builder().id(clientesId).negocioId(NEGOCIO_ID).tipo(TipoBloque.CLIENTES)
                        .campos(List.of(Campo.builder().id(nombreId).clave("name").build())).build(),
                bloque(clientesSinNombreId, TipoBloque.CLIENTES, campo("phone")))).build();

        DatosDeProyecto datos = service.cargar(proyecto, NEGOCIO_ID);

        assertThat(datos.nombresDeClientes()).containsOnly(java.util.Map.entry(rosa.getId().toString(), "Doña Rosa"));
    }

    @Test
    @DisplayName("Reutiliza las filas de un sistema cargado y excluye sus ejemplos sin consultar el repositorio")
    void cargarShouldReuseLoadedSystemWithoutRepositoryQueries() {
        Registro real = registro(INVENTARIO_ID, false);
        Registro ejemplo = registro(INVENTARIO_ID, true);
        Bloque inventario = bloque(INVENTARIO_ID, TipoBloque.INVENTARIO, campo("name"));
        Proyecto proyecto = Proyecto.builder().nombre("Almacén").bloques(List.of(inventario)).build();
        Sistema sistema = new Sistema(proyecto, List.of(), Map.of(INVENTARIO_ID, List.of(real, ejemplo)));

        DatosDeProyecto datos = service.cargar(sistema);

        assertThat(datos.inventarios()).singleElement().satisfies(resultado ->
                assertThat(resultado.registros()).containsExactly(real));
        verifyNoInteractions(registroRepository);
    }

    // --- helpers ---
    private static Registro clienteConNombre(BloqueId bloqueId, CampoId nombreId, String nombre, boolean esEjemplo) {
        return Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(bloqueId).esEjemplo(esEjemplo)
                .valores(java.util.Map.of(nombreId.toString(), nombre)).build();
    }

    private static Bloque bloque(BloqueId id, TipoBloque tipo, Campo... campos) {
        return Bloque.builder().id(id).negocioId(NEGOCIO_ID).tipo(tipo).campos(List.of(campos)).build();
    }

    private static Campo campo(String clave) {
        return Campo.builder().id("total".equals(clave) ? TOTAL_ID : CampoId.nuevo()).clave(clave).build();
    }

    private static Registro registro(BloqueId bloqueId, boolean esEjemplo) {
        return Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(bloqueId).esEjemplo(esEjemplo)
                .valores(java.util.Map.of(TOTAL_ID.toString(), 1500)).build();
    }
}
