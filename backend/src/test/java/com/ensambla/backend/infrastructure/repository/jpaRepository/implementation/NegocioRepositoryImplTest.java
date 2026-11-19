package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.NegocioRepositoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NegocioRepositoryImplTest {

    private final INegocioJpaRepository negocioJpaRepository = mock(INegocioJpaRepository.class);
    private final NegocioRepositoryImpl negocioRepository = new NegocioRepositoryImpl(
            negocioJpaRepository, new NegocioRepositoryMapper());

    private static final UUID ZONA_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");

    private Negocio guardado;
    private Optional<NegocioId> idEncontrado;
    private Optional<String> emailEncontrado;
    private List<NegocioId> ids;
    private Optional<ZonaId> zonaEncontrada;

    @Test
    @DisplayName("Devuelve el negocio con el id generado por persistencia")
    void devuelveElNegocioConElIdGeneradoPorPersistencia() {
        dadoQueGuardarDevuelveElNegocioMiNegocio();

        cuandoSeGuardaUnNegocioNuevo();

        entoncesSeDevuelveElNegocioPersistidoConSuIdYSuDuenio();
    }

    @Test
    @DisplayName("Resuelve el id del negocio del dueño autenticado")
    void resuelveElIdDelNegocioDelDuenioAutenticado() {
        when(negocioJpaRepository.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(USUARIO_ID.valor()))
                .thenReturn(Optional.of(NegocioEntity.builder().id(NEGOCIO_ID.valor()).build()));

        cuandoSeBuscaElNegocioDelDuenio();

        entoncesSeEncuentraElIdDelNegocio();
    }

    @Test
    @DisplayName("Resuelve el email del dueño del negocio")
    void resuelveElEmailDelDuenioDelNegocio() {
        when(negocioJpaRepository.findEmailDuenioById(NEGOCIO_ID.valor()))
                .thenReturn(Optional.of("dueño@example.com"));

        cuandoSeBuscaElEmailDelDuenio();

        entoncesSeEncuentraElEmail("dueño@example.com");
    }

    @Test
    @DisplayName("Lista los ids de todos los negocios")
    void listaLosIdsDeTodosLosNegocios() {
        when(negocioJpaRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID.valor()));

        cuandoSeListanLosIdsDeLosNegocios();

        entoncesSeListaElIdDelNegocio();
    }

    @Test
    @DisplayName("Resuelve la zona del negocio")
    void resuelveLaZonaDelNegocio() {
        when(negocioJpaRepository.findById(NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(NegocioEntity.builder().id(NEGOCIO_ID.valor()).zonaId(ZONA_ID).build()));

        cuandoSeBuscaLaZonaDelNegocio();

        entoncesSeEncuentraLaZona();
    }

    @Test
    @DisplayName("Un negocio sin zona no tiene id de zona")
    void unNegocioSinZonaNoTieneIdDeZona() {
        when(negocioJpaRepository.findById(NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(NegocioEntity.builder().id(NEGOCIO_ID.valor()).build()));

        cuandoSeBuscaLaZonaDelNegocio();

        entoncesNoSeEncuentraNingunaZona();
    }

    @Test
    @DisplayName("Un negocio inexistente no tiene id de zona")
    void unNegocioInexistenteNoTieneIdDeZona() {
        when(negocioJpaRepository.findById(NEGOCIO_ID.valor())).thenReturn(Optional.empty());

        cuandoSeBuscaLaZonaDelNegocio();

        entoncesNoSeEncuentraNingunaZona();
    }

    // --- dado ---
    private void dadoQueGuardarDevuelveElNegocioMiNegocio() {
        when(negocioJpaRepository.saveAndFlush(any())).thenReturn(NegocioEntity.builder()
                .id(USUARIO_ID.valor())
                .nombre("Mi negocio")
                .duenioId(USUARIO_ID.valor())
                .build());
    }

    // --- cuando ---
    private void cuandoSeGuardaUnNegocioNuevo() {
        guardado = negocioRepository.save(Negocio.builder().nombre("Mi negocio").build());
    }

    private void cuandoSeBuscaElNegocioDelDuenio() {
        idEncontrado = negocioRepository.findIdByDuenioId(USUARIO_ID);
    }

    private void cuandoSeBuscaElEmailDelDuenio() {
        emailEncontrado = negocioRepository.findEmailDuenioById(NEGOCIO_ID);
    }

    private void cuandoSeListanLosIdsDeLosNegocios() {
        ids = negocioRepository.findAllIds();
    }

    private void cuandoSeBuscaLaZonaDelNegocio() {
        zonaEncontrada = negocioRepository.findZonaIdById(NEGOCIO_ID);
    }

    // --- entonces ---
    private void entoncesSeDevuelveElNegocioPersistidoConSuIdYSuDuenio() {
        assertThat(guardado.getId()).isEqualTo(NegocioId.de(USUARIO_ID.valor()));
        assertThat(guardado.getNombre()).isEqualTo("Mi negocio");
        assertThat(guardado.getDuenioId()).isEqualTo(USUARIO_ID);
    }

    private void entoncesSeEncuentraElIdDelNegocio() {
        assertThat(idEncontrado).contains(NEGOCIO_ID);
    }

    private void entoncesSeEncuentraElEmail(String esperado) {
        assertThat(emailEncontrado).contains(esperado);
    }

    private void entoncesSeListaElIdDelNegocio() {
        assertThat(ids).containsExactly(NEGOCIO_ID);
    }

    private void entoncesSeEncuentraLaZona() {
        assertThat(zonaEncontrada).contains(ZonaId.de(ZONA_ID));
    }

    private void entoncesNoSeEncuentraNingunaZona() {
        assertThat(zonaEncontrada).isEmpty();
    }
}
