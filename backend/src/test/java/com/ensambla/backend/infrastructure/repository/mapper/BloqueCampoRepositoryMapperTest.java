package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueCampoRepositoryMapperTest {

    private static final BloqueId ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final UUID PADRE_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final UUID RELACIONADO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-09T12:00:00Z");
    private static final OffsetDateTime ACTUALIZADO = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private final BloqueRepositoryMapper bloqueMapper = new BloqueRepositoryMapper();
    private final CampoRepositoryMapper campoMapper = new CampoRepositoryMapper();

    @Test
    @DisplayName("Pasa todos los campos del bloque al modelo")
    void unBloqueGuardadoSePasaAlModeloConTodosSusDatos() {
        BloqueEntity entidad = dadoUnaEntidadDeBloqueProductos();

        Bloque bloque = cuandoSePasaAlModelo(entidad);

        entoncesElBloqueTieneTodosLosDatosDeProductos(bloque);
    }

    @Test
    @DisplayName("Pasa todos los campos del modelo del bloque a la entidad")
    void unBloqueDelModeloSePasaALaEntidadConTodosSusDatos() {
        Bloque bloque = dadoUnBloqueClientes();

        BloqueEntity entidad = cuandoSePasaALaEntidad(bloque);

        entoncesLaEntidadTieneTodosLosDatosDeClientes(entidad);
    }

    @Test
    @DisplayName("Pasa todos los campos del campo al modelo")
    void unCampoGuardadoSePasaAlModeloConTodosSusDatos() {
        CampoEntity entidad = dadoUnaEntidadDeCampoRubro();

        Campo campo = cuandoSePasaAlModelo(entidad);

        entoncesElCampoTieneTodosLosDatosDeRubro(campo);
    }

    @Test
    @DisplayName("Pasa todos los datos del modelo del campo a la entidad")
    void unCampoDelModeloSePasaALaEntidadConTodosSusDatos() {
        Campo campo = dadoUnCampoPagado();

        CampoEntity entidad = cuandoSePasaALaEntidad(campo);

        entoncesLaEntidadTieneTodosLosDatosDePagado(entidad);
    }

    @Test
    @DisplayName("Un campo nuevo sin id pasa a una entidad sin id para que la base lo genere")
    void unCampoNuevoSinIdPasaAUnaEntidadSinIdParaQueLaBaseLoGenere() {
        Campo campo = dadoUnCampoNuevoSinId();

        CampoEntity entidad = cuandoSePasaALaEntidad(campo);

        entoncesLaEntidadNoTieneIdYConservaElNombre(entidad);
    }

    // --- dado ---
    private BloqueEntity dadoUnaEntidadDeBloqueProductos() {
        return BloqueEntity.builder()
                .id(ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .proyectoId(PADRE_ID)
                .nombre("Productos")
                .posicion((short) 2)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();
    }

    private Bloque dadoUnBloqueClientes() {
        return Bloque.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID)
                .proyectoId(ProyectoId.de(PADRE_ID))
                .nombre("Clientes")
                .posicion((short) 3)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();
    }

    private CampoEntity dadoUnaEntidadDeCampoRubro() {
        return CampoEntity.builder()
                .id(ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .bloqueId(PADRE_ID)
                .nombre("Rubro")
                .tipo(TipoCampo.OPCION)
                .obligatorio(true)
                .valorInicial("Velas")
                .opciones(List.of("Velas", "Moldes"))
                .bloqueRelacionadoId(RELACIONADO_ID)
                .posicion((short) 1)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();
    }

    private Campo dadoUnCampoPagado() {
        return Campo.builder()
                .id(CampoId.de(ID.valor()))
                .negocioId(NEGOCIO_ID)
                .bloqueId(BloqueId.de(PADRE_ID))
                .nombre("Pagado")
                .tipo(TipoCampo.SI_NO)
                .obligatorio(false)
                .valorInicial("true")
                .opciones(List.of())
                .posicion((short) 4)
                .creadoEn(CREADO)
                .actualizadoEn(ACTUALIZADO)
                .build();
    }

    private Campo dadoUnCampoNuevoSinId() {
        return Campo.builder().nombre("Nuevo").build();
    }

    // --- cuando ---
    private Bloque cuandoSePasaAlModelo(BloqueEntity entidad) {
        return bloqueMapper.toDomain(entidad);
    }

    private BloqueEntity cuandoSePasaALaEntidad(Bloque bloque) {
        return bloqueMapper.toEntity(bloque);
    }

    private Campo cuandoSePasaAlModelo(CampoEntity entidad) {
        return campoMapper.toDomain(entidad);
    }

    private CampoEntity cuandoSePasaALaEntidad(Campo campo) {
        return campoMapper.toEntity(campo);
    }

    // --- entonces ---
    private void entoncesElBloqueTieneTodosLosDatosDeProductos(Bloque bloque) {
        assertThat(bloque.getId()).isEqualTo(ID);
        assertThat(bloque.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(bloque.getProyectoId()).isEqualTo(ProyectoId.de(PADRE_ID));
        assertThat(bloque.getNombre()).isEqualTo("Productos");
        assertThat(bloque.getPosicion()).isEqualTo((short) 2);
        assertThat(bloque.getCreadoEn()).isEqualTo(CREADO);
        assertThat(bloque.getActualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    private void entoncesLaEntidadTieneTodosLosDatosDeClientes(BloqueEntity entidad) {
        assertThat(entidad.getId()).isEqualTo(ID.valor());
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getProyectoId()).isEqualTo(PADRE_ID);
        assertThat(entidad.getNombre()).isEqualTo("Clientes");
        assertThat(entidad.getPosicion()).isEqualTo((short) 3);
        assertThat(entidad.getCreadoEn()).isEqualTo(CREADO);
        assertThat(entidad.getActualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    private void entoncesElCampoTieneTodosLosDatosDeRubro(Campo campo) {
        assertThat(campo.getId()).isEqualTo(CampoId.de(ID.valor()));
        assertThat(campo.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(campo.getBloqueId()).isEqualTo(BloqueId.de(PADRE_ID));
        assertThat(campo.getNombre()).isEqualTo("Rubro");
        assertThat(campo.getTipo()).isEqualTo(TipoCampo.OPCION);
        assertThat(campo.getObligatorio()).isTrue();
        assertThat(campo.getValorInicial()).isEqualTo("Velas");
        assertThat(campo.getOpciones()).containsExactly("Velas", "Moldes");
        assertThat(campo.getPosicion()).isEqualTo((short) 1);
        assertThat(campo.getCreadoEn()).isEqualTo(CREADO);
        assertThat(campo.getActualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    private void entoncesLaEntidadTieneTodosLosDatosDePagado(CampoEntity entidad) {
        assertThat(entidad.getId()).isEqualTo(ID.valor());
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getBloqueId()).isEqualTo(PADRE_ID);
        assertThat(entidad.getNombre()).isEqualTo("Pagado");
        assertThat(entidad.getTipo()).isEqualTo(TipoCampo.SI_NO);
        assertThat(entidad.getObligatorio()).isFalse();
        assertThat(entidad.getValorInicial()).isEqualTo("true");
        assertThat(entidad.getOpciones()).isEmpty();
        assertThat(entidad.getPosicion()).isEqualTo((short) 4);
        assertThat(entidad.getCreadoEn()).isEqualTo(CREADO);
        assertThat(entidad.getActualizadoEn()).isEqualTo(ACTUALIZADO);
    }

    private void entoncesLaEntidadNoTieneIdYConservaElNombre(CampoEntity entidad) {
        assertThat(entidad.getId()).isNull();
        assertThat(entidad.getNombre()).isEqualTo("Nuevo");
    }
}
