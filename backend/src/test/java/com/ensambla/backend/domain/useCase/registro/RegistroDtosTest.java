package com.ensambla.backend.domain.useCase.registro;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.useCase.registro.crearRegistro.CrearRegistroResponse;
import com.ensambla.backend.domain.useCase.registro.editarRegistro.EditarRegistroResponse;
import com.ensambla.backend.domain.useCase.registro.listarRegistros.ListarRegistrosResponse;
import com.ensambla.backend.domain.useCase.registro.obtenerRegistro.ObtenerRegistroResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class RegistroDtosTest {

    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private RegistroItem item;
    private ListarRegistrosResponse listado;

    @Test
    @DisplayName("Crear devuelve el registro con todos sus datos")
    void crearDevuelveElRegistroConTodosSusDatos() {
        cuandoSeArmaLaRespuestaDeCrear();

        entoncesElItemTieneTodosSusDatos();
    }

    @Test
    @DisplayName("Obtener devuelve el registro con todos sus datos")
    void obtenerDevuelveElRegistroConTodosSusDatos() {
        cuandoSeArmaLaRespuestaDeObtener();

        entoncesElItemTieneTodosSusDatos();
    }

    @Test
    @DisplayName("Editar devuelve el registro con todos sus datos")
    void editarDevuelveElRegistroConTodosSusDatos() {
        cuandoSeArmaLaRespuestaDeEditar();

        entoncesElItemTieneTodosSusDatos();
    }

    @Test
    @DisplayName("Listar devuelve los registros en orden con los datos de la página")
    void listarDevuelveLosRegistrosEnOrdenConLosDatosDeLaPagina() {
        Registro segundo = dadoOtroRegistroDelMismoBloque();

        cuandoSeArmaLaRespuestaDeListarLaPaginaDos(segundo);

        entoncesSeListanLosDosRegistrosConLosDatosDeLaPagina(segundo);
    }

    @Test
    @DisplayName("Listar devuelve una página vacía cuando no hay registros")
    void listarDevuelveUnaPaginaVaciaCuandoNoHayRegistros() {
        cuandoSeArmaLaRespuestaDeListarSinRegistros();

        entoncesLaPaginaEstaVaciaConUnaSolaPagina();
    }

    // --- dado ---
    private Registro dadoOtroRegistroDelMismoBloque() {
        return Registro.builder().id(RegistroId.nuevo()).bloqueId(BLOQUE_ID).build();
    }

    private static Registro unRegistro() {
        return Registro.builder()
                .id(REGISTRO_ID)
                .bloqueId(BLOQUE_ID)
                .valores(Map.of("campo", "dato"))
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();
    }

    // --- cuando ---
    private void cuandoSeArmaLaRespuestaDeCrear() {
        item = CrearRegistroResponse.de(unRegistro()).registro();
    }

    private void cuandoSeArmaLaRespuestaDeObtener() {
        item = ObtenerRegistroResponse.de(unRegistro()).registro();
    }

    private void cuandoSeArmaLaRespuestaDeEditar() {
        item = EditarRegistroResponse.de(unRegistro()).registro();
    }

    private void cuandoSeArmaLaRespuestaDeListarLaPaginaDos(Registro segundo) {
        Pagina<Registro> pagina = new Pagina<>(List.of(unRegistro(), segundo), 2, 20, 45);
        TotalesRegistros totales = new TotalesRegistros(45, Map.of(CAMPO_ID, new BigDecimal("1500.50")));
        listado = ListarRegistrosResponse.de(pagina, totales);
    }

    private void cuandoSeArmaLaRespuestaDeListarSinRegistros() {
        listado = ListarRegistrosResponse.de(new Pagina<>(List.of(), 0, 20, 0), new TotalesRegistros(0, Map.of()));
    }

    // --- entonces ---
    private void entoncesElItemTieneTodosSusDatos() {
        assertThat(item.id()).isEqualTo(REGISTRO_ID);
        assertThat(item.bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(item.valores()).containsEntry("campo", "dato");
        assertThat(item.creadoEn()).isEqualTo(CREADO);
        assertThat(item.actualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    private void entoncesSeListanLosDosRegistrosConLosDatosDeLaPagina(Registro segundo) {
        assertThat(listado.contenido()).extracting(RegistroItem::id).containsExactly(REGISTRO_ID, segundo.getId());
        assertThat(listado.pagina()).isEqualTo(2);
        assertThat(listado.tamanio()).isEqualTo(20);
        assertThat(listado.totalElementos()).isEqualTo(45);
        assertThat(listado.totalPaginas()).isEqualTo(3);
        assertThat(listado.totales().cantidad()).isEqualTo(45);
        assertThat(listado.totales().sumas()).containsOnly(entry(CAMPO_ID, new BigDecimal("1500.50")));
    }

    private void entoncesLaPaginaEstaVaciaConUnaSolaPagina() {
        assertThat(listado.contenido()).isEmpty();
        assertThat(listado.totales().cantidad()).isZero();
        assertThat(listado.totales().sumas()).isEmpty();
        assertThat(listado.totalPaginas()).isEqualTo(1);
    }
}
