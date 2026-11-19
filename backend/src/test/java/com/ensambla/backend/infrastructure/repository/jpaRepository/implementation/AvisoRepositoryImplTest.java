package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IAvisoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.AvisoRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvisoRepositoryImplTest {

    private static final UUID ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");

    private final IAvisoJpaRepository jpa = mock(IAvisoJpaRepository.class);
    private final AvisoRepositoryImpl repository = new AvisoRepositoryImpl(jpa, new AvisoRepositoryMapper());

    private Aviso guardado;
    private Optional<Aviso> encontrado;
    private List<Aviso> avisos;

    @Test
    @DisplayName("Guarda el aviso y devuelve el que persistió")
    void guardaElAvisoYDevuelveElQuePersistio() {
        dadoQueGuardarDevuelveElAviso();

        cuandoSeGuardaUnAvisoNuevo();

        entoncesSeDevuelveElAvisoPersistido();
    }

    @Test
    @DisplayName("Busca el aviso por id dentro del negocio")
    void buscaElAvisoPorIdDentroDelNegocio() {
        when(jpa.findByIdAndNegocioId(ID, NEGOCIO_ID.valor())).thenReturn(Optional.of(unaEntidad()));

        cuandoSeBuscaElAvisoPorId();

        entoncesSeEncuentraElAviso();
    }

    @Test
    @DisplayName("Busca el aviso por huella dentro del negocio")
    void buscaElAvisoPorHuellaDentroDelNegocio() {
        when(jpa.findByNegocioIdAndHuella(NEGOCIO_ID.valor(), "h")).thenReturn(Optional.of(unaEntidad()));

        cuandoSeBuscaElAvisoPorHuella();

        entoncesSeEncuentraElAviso();
    }

    @Test
    @DisplayName("Lista los avisos de un tipo y estado")
    void listaLosAvisosDeUnTipoYEstado() {
        when(jpa.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID.valor(), TipoAviso.STOCK, EstadoAviso.PENDIENTE))
                .thenReturn(List.of(unaEntidad()));

        cuandoSeListanLosAvisosDeStockPendientes();

        entoncesSeListaElAviso();
    }

    @Test
    @DisplayName("Trae de una vez los avisos del negocio con esos ids, pasando al JPA los UUID")
    void traeDeUnaVezLosAvisosDelNegocioConEsosIdsPasandoAlJpaLosUuid() {
        when(jpa.findByIdInAndNegocioId(List.of(ID), NEGOCIO_ID.valor())).thenReturn(List.of(unaEntidad()));

        cuandoSeTraenLosAvisosPorIds();

        entoncesSeListaElAviso();
    }

    @Test
    @DisplayName("Lista todos los avisos del negocio")
    void listaTodosLosAvisosDelNegocio() {
        when(jpa.findByNegocioId(NEGOCIO_ID.valor())).thenReturn(List.of(unaEntidad()));

        cuandoSeListanLosAvisosDelNegocio();

        entoncesSeListaElAviso();
    }

    @Test
    @DisplayName("Lista los avisos del negocio en un estado")
    void listaLosAvisosDelNegocioEnUnEstado() {
        when(jpa.findByNegocioIdAndEstado(NEGOCIO_ID.valor(), EstadoAviso.PENDIENTE))
                .thenReturn(List.of(unaEntidad()));

        cuandoSeListanLosAvisosPendientes();

        entoncesSeListaElAviso();
    }

    // --- dado ---
    private void dadoQueGuardarDevuelveElAviso() {
        when(jpa.saveAndFlush(any(AvisoEntity.class))).thenReturn(unaEntidad());
    }

    private static AvisoEntity unaEntidad() {
        return AvisoEntity.builder().id(ID).negocioId(NEGOCIO_ID.valor()).tipo(TipoAviso.STOCK).grupo(GrupoAviso.STOCK)
                .estado(EstadoAviso.PENDIENTE).build();
    }

    // --- cuando ---
    private void cuandoSeGuardaUnAvisoNuevo() {
        guardado = repository.save(Aviso.builder().negocioId(NEGOCIO_ID).acciones(List.of()).build());
    }

    private void cuandoSeBuscaElAvisoPorId() {
        encontrado = repository.findByIdAndNegocioId(AvisoId.de(ID), NEGOCIO_ID);
    }

    private void cuandoSeBuscaElAvisoPorHuella() {
        encontrado = repository.findByNegocioIdAndHuella(NEGOCIO_ID, "h");
    }

    private void cuandoSeListanLosAvisosDeStockPendientes() {
        avisos = repository.findByNegocioIdAndTipoAndEstado(NEGOCIO_ID, TipoAviso.STOCK, EstadoAviso.PENDIENTE);
    }

    private void cuandoSeTraenLosAvisosPorIds() {
        avisos = repository.findAllByIdsAndNegocioId(List.of(AvisoId.de(ID)), NEGOCIO_ID);
    }

    private void cuandoSeListanLosAvisosDelNegocio() {
        avisos = repository.findByNegocioId(NEGOCIO_ID);
    }

    private void cuandoSeListanLosAvisosPendientes() {
        avisos = repository.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.PENDIENTE);
    }

    // --- entonces ---
    private void entoncesSeDevuelveElAvisoPersistido() {
        assertThat(guardado.getId()).isEqualTo(AvisoId.de(ID));
    }

    private void entoncesSeEncuentraElAviso() {
        assertThat(encontrado).map(Aviso::getId).contains(AvisoId.de(ID));
    }

    private void entoncesSeListaElAviso() {
        assertThat(avisos).extracting(Aviso::getId).containsExactly(AvisoId.de(ID));
    }
}
