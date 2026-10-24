package com.ensambla.backend.domain.useCase.campo.editarCampo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.campo.CampoItem;
import com.ensambla.backend.infrastructure.config.useCase.CampoUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EditarCampoTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000001");
    private static final CampoId OTRO_CAMPO_ID = CampoId.de("d0000000-0000-4000-8000-000000000002");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("e0000000-0000-4000-8000-000000000001");
    private static final ProyectoId OTRO_PROYECTO_ID = ProyectoId.de("e0000000-0000-4000-8000-000000000002");
    private static final BloqueId RELACIONADO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");

    private final NegocioService negocioService = mock(NegocioService.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final EditarCampo editarCampo = new CampoUseCaseConfig().editarCampo(
            negocioService,
            new CampoService(campoRepository, bloqueRepository, new TransaccionDirecta()),
            new BloqueService(bloqueRepository, new TransaccionDirecta()),
            campoRepository,
            new TransaccionDirecta(),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @BeforeEach
    void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.of(
                Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).build()));
    }

    @Test
    @DisplayName("Editar reemplaza la configuración del campo y lo guarda con la hora del reloj")
    void executeShouldReconfigureAndSave() {
        Campo campo = givenCampo(CAMPO_ID.valor(), "Precio", 0);
        givenCamposDelBloque(givenCampo(CAMPO_ID.valor(), "Precio", 0));
        givenSaveReturnsArgument();

        CampoItem result = editar(campo, "Costo", "TEXTO", null, null);

        assertThat(result.nombre()).isEqualTo("Costo");
        assertThat(result.tipo()).isEqualTo("TEXTO");
        assertThat(result.actualizadoEn()).isEqualTo(AHORA);
        verify(campoRepository).save(campo);
    }

    @Test
    @DisplayName("Editar con el nombre de otro campo del bloque no guarda nada")
    void executeShouldNotSaveWhenNameIsTaken() {
        Campo campo = givenCampo(CAMPO_ID.valor(), "Precio", 0);
        givenCamposDelBloque(campo, givenCampo(OTRO_CAMPO_ID.valor(), "Producto", 1));

        assertThatThrownBy(() -> editar(campo, "Producto", "TEXTO", null, null))
                .isInstanceOf(DuplicateResourceException.class);
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar rechaza convertir el campo en una conexión con una tabla de otro sistema")
    void executeShouldRejectRelacionWhenBloqueIsOfOtherProyecto() {
        Campo campo = givenCampo(CAMPO_ID.valor(), "Precio", 0);
        givenCamposDelBloque(campo);
        givenBloqueRelacionado(OTRO_PROYECTO_ID);

        assertThatThrownBy(() -> editar(campo, "Cliente", "RELACION", RELACIONADO_ID, null))
                .isInstanceOf(CampoInvalidoException.class);
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar guarda una conexión con una tabla del mismo sistema")
    void executeShouldSaveRelacionWhenBloqueIsOfSameProyecto() {
        Campo campo = givenCampo(CAMPO_ID.valor(), "Precio", 0);
        givenCamposDelBloque(campo);
        givenBloqueRelacionado(PROYECTO_ID);
        givenSaveReturnsArgument();

        CampoItem result = editar(campo, "Cliente", "RELACION", RELACIONADO_ID, null);

        assertThat(result.bloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
    }

    @Test
    @DisplayName("Editar un campo para que sea una fórmula que se cita a sí misma no guarda nada")
    void executeShouldRejectFormulaThatUsesItself() {
        Campo campo = givenCampo(CAMPO_ID.valor(), "Total", 0);
        givenCamposDelBloque(givenCampo(CAMPO_ID.valor(), "Total", 0));

        assertThatThrownBy(() -> editar(campo, "Total", "FORMULA", null, "{" + CAMPO_ID + "} * 2"))
                .isInstanceOf(CampoInvalidoException.class);
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar no deja cambiar el tipo de un campo que usa una fórmula")
    void executeShouldRejectChangingTypeOfFieldUsedByFormula() {
        Campo cantidad = givenCampo(CAMPO_ID.valor(), "Cantidad", 0);
        givenCamposDelBloque(cantidad, givenFormula(OTRO_CAMPO_ID.valor(), "Doble", "{" + CAMPO_ID + "} * 2"));

        assertThatThrownBy(() -> editar(cantidad, "Cantidad", "TEXTO", null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede cambiar el tipo de «Cantidad»: la fórmula «Doble» lo usa");
        verify(campoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar deja cambiar el tipo de un campo que ninguna fórmula usa")
    void executeShouldAllowChangingTypeOfFieldNotUsedByFormula() {
        Campo cantidad = givenCampo(CAMPO_ID.valor(), "Cantidad", 0);
        givenCamposDelBloque(cantidad, givenFormula(OTRO_CAMPO_ID.valor(), "Doble", "2 * 2"));
        givenSaveReturnsArgument();

        CampoItem result = editar(cantidad, "Cantidad", "TEXTO", null, null);

        assertThat(result.tipo()).isEqualTo("TEXTO");
    }

    @Test
    @DisplayName("Editar un campo numérico que usa una fórmula no revisa las fórmulas")
    void executeShouldKeepNumericFieldUsedByFormula() {
        Campo cantidad = givenCampo(CAMPO_ID.valor(), "Cantidad", 0);
        givenCamposDelBloque(cantidad, givenFormula(OTRO_CAMPO_ID.valor(), "Doble", "{" + CAMPO_ID + "} * 2"));
        givenSaveReturnsArgument();

        CampoItem result = editar(cantidad, "Cantidad", "NUMERO", null, null);

        assertThat(result.tipo()).isEqualTo("NUMERO");
    }

    @Test
    @DisplayName("Un campo ajeno propaga el 404 sin consultar el bloque ni editar")
    void executeShouldStopWhenCampoIsForeign() {
        when(campoRepository.findByIdAndNegocioId(CAMPO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> editarCampo.execute(
                new EditarCampoRequest(USUARIO_ID, CAMPO_ID, "Costo", "NUMERO", false, "0", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(campoRepository, never()).save(any());
    }

    // --- arrange ---
    private void givenCamposDelBloque(Campo... campos) {
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(campos));
    }

    private void givenSaveReturnsArgument() {
        when(campoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenBloqueRelacionado(ProyectoId proyectoId) {
        Bloque relacionado = Bloque.builder().id(RELACIONADO_ID).negocioId(NEGOCIO_ID).proyectoId(proyectoId).build();
        when(bloqueRepository.findByIdAndNegocioId(RELACIONADO_ID, NEGOCIO_ID)).thenReturn(Optional.of(relacionado));
    }

    private Campo givenCampo(UUID id, String nombre, int posicion) {
        return Campo.builder()
                .id(CampoId.de(id))
                .negocioId(NEGOCIO_ID)
                .bloqueId(BLOQUE_ID)
                .nombre(nombre)
                .tipo(TipoCampo.NUMERO)
                .posicion((short) posicion)
                .build();
    }

    private Campo givenFormula(UUID id, String nombre, String expresion) {
        return Campo.builder().id(CampoId.de(id)).negocioId(NEGOCIO_ID).bloqueId(BLOQUE_ID).nombre(nombre)
                .tipo(TipoCampo.FORMULA).formula(expresion).posicion((short) 1).build();
    }

    // --- act ---
    private CampoItem editar(Campo campo, String nombre, String tipo, BloqueId bloqueRelacionadoId, String formula) {
        when(campoRepository.findByIdAndNegocioId(campo.getId(), NEGOCIO_ID)).thenReturn(Optional.of(campo));
        return editarCampo.execute(new EditarCampoRequest(
                USUARIO_ID, campo.getId(), nombre, tipo, null, null, null, bloqueRelacionadoId, formula));
    }
}
