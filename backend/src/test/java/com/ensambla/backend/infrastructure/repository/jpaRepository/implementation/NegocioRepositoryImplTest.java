package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.port.NegocioActivoPort;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NegocioRepositoryImplTest {

    private final INegocioJpaRepository negocioJpaRepository = mock(INegocioJpaRepository.class);
    private final NegocioActivoPort negocioActivoPort = mock(NegocioActivoPort.class);
    private final NegocioRepositoryImpl negocioRepository = new NegocioRepositoryImpl(
            negocioJpaRepository, new NegocioRepositoryMapper(), negocioActivoPort);

    @Test
    @DisplayName("Devuelve el negocio con el id generado por persistencia")
    void saveShouldReturnPersistedNegocio() {
        when(negocioJpaRepository.saveAndFlush(any())).thenReturn(NegocioEntity.builder()
                .id(USUARIO_ID.valor())
                .nombre("Mi negocio")
                .duenioId(USUARIO_ID.valor())
                .build());

        Negocio result = negocioRepository.save(Negocio.builder().nombre("Mi negocio").build());

        assertThat(result.getId()).isEqualTo(NegocioId.de(USUARIO_ID.valor()));
        assertThat(result.getNombre()).isEqualTo("Mi negocio");
        assertThat(result.getDuenioId()).isEqualTo(USUARIO_ID);
    }
    
    @Test
    @DisplayName("Resuelve el id del negocio del dueño autenticado")
    void findIdByDuenioIdShouldMapId() {
        var jpa = mock(INegocioJpaRepository.class);
        var mapper = new NegocioRepositoryMapper();
        when(jpa.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(USUARIO_ID.valor()))
                .thenReturn(Optional.of(NegocioEntity.builder().id(NEGOCIO_ID.valor()).build()));

        var result = new NegocioRepositoryImpl(jpa, mapper, negocioActivoPort).findIdByDuenioId(USUARIO_ID);

        assertThat(result).contains(NEGOCIO_ID);
        verify(negocioActivoPort, never()).activar(any(), any());
    }

    @Test
    @DisplayName("Usa el negocio activo de la sesión cuando todavía pertenece a la cuenta")
    void findByDuenioIdShouldUseActiveBusiness() {
        NegocioEntity activo = negocioEntity(NEGOCIO_ID.valor(), "Sucursal activa");
        when(negocioActivoPort.obtener(USUARIO_ID)).thenReturn(Optional.of(NEGOCIO_ID));
        when(negocioJpaRepository.findByIdAndDuenioId(NEGOCIO_ID.valor(), USUARIO_ID.valor()))
                .thenReturn(Optional.of(activo));

        assertThat(negocioRepository.findByDuenioId(USUARIO_ID))
                .get().extracting(Negocio::getNombre).isEqualTo("Sucursal activa");

        verify(negocioJpaRepository).findByIdAndDuenioId(NEGOCIO_ID.valor(), USUARIO_ID.valor());
    }

    @Test
    @DisplayName("Si la sesión apunta a un negocio inválido recupera el más antiguo sin escribir durante la lectura")
    void findByDuenioIdShouldReplaceStaleActiveBusiness() {
        NegocioId inexistente = NegocioId.nuevo();
        NegocioEntity primero = negocioEntity(NEGOCIO_ID.valor(), "Casa central");
        when(negocioActivoPort.obtener(USUARIO_ID)).thenReturn(Optional.of(inexistente));
        when(negocioJpaRepository.findByIdAndDuenioId(inexistente.valor(), USUARIO_ID.valor()))
                .thenReturn(Optional.empty());
        when(negocioJpaRepository.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(USUARIO_ID.valor()))
                .thenReturn(Optional.of(primero));

        assertThat(negocioRepository.findByDuenioId(USUARIO_ID))
                .get().extracting(Negocio::getId).isEqualTo(NEGOCIO_ID);
        verify(negocioActivoPort, never()).activar(any(), any());
    }

    @Test
    @DisplayName("Devuelve vacío si la cuenta no tiene ningún negocio")
    void findByDuenioIdShouldReturnEmptyWithoutBusinesses() {
        when(negocioJpaRepository.findFirstByDuenioIdOrderByCreadoEnAscIdAsc(USUARIO_ID.valor()))
                .thenReturn(Optional.empty());

        assertThat(negocioRepository.findByDuenioId(USUARIO_ID)).isEmpty();
        verify(negocioActivoPort).obtener(USUARIO_ID);
        verify(negocioActivoPort, never()).activar(any(), any());
    }

    @Test
    @DisplayName("Bloquea el negocio activo al preparar una actualización")
    void findByDuenioIdParaActualizarShouldLockActiveBusiness() {
        when(negocioActivoPort.obtener(USUARIO_ID)).thenReturn(Optional.of(NEGOCIO_ID));
        when(negocioJpaRepository.findByIdAndDuenioIdParaActualizar(NEGOCIO_ID.valor(), USUARIO_ID.valor()))
                .thenReturn(Optional.of(negocioEntity(NEGOCIO_ID.valor(), "Activa")));

        assertThat(negocioRepository.findByDuenioIdParaActualizar(USUARIO_ID)).isPresent();
        verify(negocioJpaRepository).findByIdAndDuenioIdParaActualizar(NEGOCIO_ID.valor(), USUARIO_ID.valor());
    }

    @Test
    @DisplayName("Al actualizar recupera con bloqueo el negocio más antiguo sin escribir durante la lectura")
    void findByDuenioIdParaActualizarShouldFallbackWithoutActivating() {
        when(negocioJpaRepository.findTopByDuenioIdOrderByCreadoEnAscIdAsc(USUARIO_ID.valor()))
                .thenReturn(Optional.of(negocioEntity(NEGOCIO_ID.valor(), "Primera")));

        assertThat(negocioRepository.findByDuenioIdParaActualizar(USUARIO_ID)).isPresent();
        verify(negocioActivoPort, never()).activar(any(), any());
    }

    @Test
    @DisplayName("Lista en orden todos los negocios de una cuenta")
    void findAllByDuenioIdShouldMapBusinesses() {
        NegocioId segundoId = NegocioId.nuevo();
        when(negocioJpaRepository.findAllByDuenioIdOrderByCreadoEnAscIdAsc(USUARIO_ID.valor()))
                .thenReturn(List.of(negocioEntity(NEGOCIO_ID.valor(), "Primero"),
                        negocioEntity(segundoId.valor(), "Segundo")));

        assertThat(negocioRepository.findAllByDuenioId(USUARIO_ID))
                .extracting(Negocio::getNombre).containsExactly("Primero", "Segundo");
    }

    @Test
    @DisplayName("Lista con bloqueo todos los negocios de una cuenta")
    void findAllByDuenioIdParaActualizarShouldMapLockedBusinesses() {
        when(negocioJpaRepository.findAllByDuenioIdParaActualizar(USUARIO_ID.valor()))
                .thenReturn(List.of(negocioEntity(NEGOCIO_ID.valor(), "Primero")));

        assertThat(negocioRepository.findAllByDuenioIdParaActualizar(USUARIO_ID))
                .extracting(Negocio::getId).containsExactly(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Busca un negocio solamente dentro de la cuenta indicada")
    void findByIdAndDuenioIdShouldMapBusiness() {
        when(negocioJpaRepository.findByIdAndDuenioId(NEGOCIO_ID.valor(), USUARIO_ID.valor()))
                .thenReturn(Optional.of(negocioEntity(NEGOCIO_ID.valor(), "Propio")));

        assertThat(negocioRepository.findByIdAndDuenioId(NEGOCIO_ID, USUARIO_ID))
                .get().extracting(Negocio::getNombre).isEqualTo("Propio");
    }

    @Test
    @DisplayName("Elimina y confirma inmediatamente el borrado")
    void deleteByIdShouldDeleteAndFlush() {
        negocioRepository.deleteById(NEGOCIO_ID);

        verify(negocioJpaRepository).deleteById(NEGOCIO_ID.valor());
        verify(negocioJpaRepository).flush();
    }

    @Test
    @DisplayName("Resuelve la cuenta dueña del negocio")
    void findDuenioIdByIdShouldMapAccount() {
        when(negocioJpaRepository.findDuenioIdById(NEGOCIO_ID.valor())).thenReturn(Optional.of(USUARIO_ID.valor()));

        assertThat(negocioRepository.findDuenioIdById(NEGOCIO_ID)).contains(USUARIO_ID);
    }

    @Test
    @DisplayName("Resuelve el email del dueño del negocio")
    void findEmailDuenioByIdShouldDelegateToJpa() {
        when(negocioJpaRepository.findEmailDuenioById(NEGOCIO_ID.valor())).thenReturn(Optional.of("dueño@example.com"));

        assertThat(negocioRepository.findEmailDuenioById(NEGOCIO_ID)).contains("dueño@example.com");
    }

    @Test
    @DisplayName("Lista los ids de todos los negocios")
    void findAllIdsShouldDelegateToJpa() {
        when(negocioJpaRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID.valor()));

        assertThat(negocioRepository.findAllIds()).containsExactly(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Resuelve la zona del negocio")
    void findZonaIdByIdShouldMapZonaId() {
        UUID zonaId = UUID.randomUUID();
        when(negocioJpaRepository.findById(NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(NegocioEntity.builder().id(NEGOCIO_ID.valor()).zonaId(zonaId).build()));

        assertThat(negocioRepository.findZonaIdById(NEGOCIO_ID)).contains(ZonaId.de(zonaId));
    }

    @Test
    @DisplayName("Un negocio sin zona no tiene id de zona")
    void findZonaIdByIdShouldReturnEmptyWhenNegocioHasNoZona() {
        when(negocioJpaRepository.findById(NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(NegocioEntity.builder().id(NEGOCIO_ID.valor()).build()));

        assertThat(negocioRepository.findZonaIdById(NEGOCIO_ID)).isEmpty();
    }

    @Test
    @DisplayName("Un negocio inexistente no tiene id de zona")
    void findZonaIdByIdShouldReturnEmptyWhenNegocioDoesNotExist() {
        when(negocioJpaRepository.findById(NEGOCIO_ID.valor())).thenReturn(Optional.empty());

        assertThat(negocioRepository.findZonaIdById(NEGOCIO_ID)).isEmpty();
    }

    private NegocioEntity negocioEntity(UUID id, String nombre) {
        return NegocioEntity.builder().id(id).nombre(nombre).duenioId(USUARIO_ID.valor()).build();
    }
}
