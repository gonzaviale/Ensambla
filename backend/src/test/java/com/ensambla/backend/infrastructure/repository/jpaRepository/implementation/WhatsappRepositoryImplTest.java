package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.infrastructure.repository.entity.MensajeWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.entity.VinculoWhatsappEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMensajeWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IVinculoWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.MensajeWhatsappRepositoryMapper;
import com.ensambla.backend.infrastructure.repository.mapper.VinculoWhatsappRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ensambla.backend.support.WhatsappTestData.AHORA;
import static com.ensambla.backend.support.WhatsappTestData.CODIGO;
import static com.ensambla.backend.support.WhatsappTestData.MENSAJE_ID;
import static com.ensambla.backend.support.WhatsappTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.WhatsappTestData.TELEFONO;
import static com.ensambla.backend.support.WhatsappTestData.VINCULO_ID;
import static com.ensambla.backend.support.WhatsappTestData.WA_MENSAJE_ID;
import static com.ensambla.backend.support.WhatsappTestData.audio;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsappRepositoryImplTest {

    @Mock
    private IVinculoWhatsappJpaRepository vinculoWhatsappJpaRepository;

    @Mock
    private IMensajeWhatsappJpaRepository mensajeWhatsappJpaRepository;

    private VinculoWhatsappRepositoryImpl vinculoWhatsappRepository;
    private MensajeWhatsappRepositoryImpl mensajeWhatsappRepository;

    @BeforeEach
    void setUp() {
        vinculoWhatsappRepository = new VinculoWhatsappRepositoryImpl(
                vinculoWhatsappJpaRepository, new VinculoWhatsappRepositoryMapper());
        mensajeWhatsappRepository = new MensajeWhatsappRepositoryImpl(
                mensajeWhatsappJpaRepository, new MensajeWhatsappRepositoryMapper());
    }

    @Test
    @DisplayName("Guarda el vínculo y devuelve el modelo con el id generado")
    void saveVinculoShouldReturnPersistedVinculo() {
        when(vinculoWhatsappJpaRepository.saveAndFlush(any())).thenReturn(vinculoEntity());

        VinculoWhatsapp result = vinculoWhatsappRepository.save(VinculoWhatsapp.nuevo(NEGOCIO_ID, AHORA));

        assertThat(result.getId()).isEqualTo(VINCULO_ID);
        assertThat(result.getTelefono()).isEqualTo(TELEFONO);
    }

    @Test
    @DisplayName("Busca el vínculo por negocio")
    void findByNegocioIdShouldMapTheEntity() {
        when(vinculoWhatsappJpaRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(vinculoEntity()));

        Optional<VinculoWhatsapp> result = vinculoWhatsappRepository.findByNegocioId(NEGOCIO_ID);

        assertThat(result).get().extracting(VinculoWhatsapp::getId).isEqualTo(VINCULO_ID);
    }

    @Test
    @DisplayName("Busca el vínculo por código")
    void findByCodigoShouldMapTheEntity() {
        when(vinculoWhatsappJpaRepository.findByCodigo(CODIGO)).thenReturn(Optional.of(vinculoEntity()));

        Optional<VinculoWhatsapp> result = vinculoWhatsappRepository.findByCodigo(CODIGO);

        assertThat(result).get().extracting(VinculoWhatsapp::getNegocioId).isEqualTo(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Un número sin vínculo no devuelve nada")
    void findByTelefonoShouldBeEmptyWhenMissing() {
        when(vinculoWhatsappJpaRepository.findByTelefono(TELEFONO)).thenReturn(Optional.empty());

        Optional<VinculoWhatsapp> result = vinculoWhatsappRepository.findByTelefono(TELEFONO);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Avisa si un código ya está en uso")
    void existsByCodigoShouldDelegateToJpa() {
        when(vinculoWhatsappJpaRepository.existsByCodigo(CODIGO)).thenReturn(true);

        boolean result = vinculoWhatsappRepository.existsByCodigo(CODIGO);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Guarda el mensaje y devuelve el modelo con el id generado")
    void saveMensajeShouldReturnPersistedMensaje() {
        MensajeWhatsappEntity entity = new MensajeWhatsappRepositoryMapper()
                .toEntity(MensajeWhatsapp.nuevo(NEGOCIO_ID, audio(), AHORA));
        entity.setId(MENSAJE_ID);
        when(mensajeWhatsappJpaRepository.saveAndFlush(any())).thenReturn(entity);

        MensajeWhatsapp result = mensajeWhatsappRepository.save(MensajeWhatsapp.nuevo(NEGOCIO_ID, audio(), AHORA));

        assertThat(result.getId()).isEqualTo(MENSAJE_ID);
    }

    @Test
    @DisplayName("Avisa si un mensaje de Meta ya se guardó")
    void existsByWaMensajeIdShouldDelegateToJpa() {
        when(mensajeWhatsappJpaRepository.existsByWaMensajeId(WA_MENSAJE_ID)).thenReturn(true);

        boolean result = mensajeWhatsappRepository.existsByWaMensajeId(WA_MENSAJE_ID);

        assertThat(result).isTrue();
    }

    // --- helpers ---
    private VinculoWhatsappEntity vinculoEntity() {
        return VinculoWhatsappEntity.builder()
                .id(VINCULO_ID)
                .negocioId(NEGOCIO_ID)
                .telefono(TELEFONO)
                .vinculadoEn(AHORA)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }
}
