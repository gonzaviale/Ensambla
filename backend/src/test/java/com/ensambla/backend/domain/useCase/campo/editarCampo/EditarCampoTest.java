package com.ensambla.backend.domain.useCase.campo.editarCampo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
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

import static com.ensambla.backend.support.CamposDePrueba.referenciaA;
import static com.ensambla.backend.support.CamposDePrueba.unCampoNumerico;
import static com.ensambla.backend.support.CamposDePrueba.unaFormulaQueNoUsaCampos;
import static com.ensambla.backend.support.CamposDePrueba.unaFormulaQueUsa;
import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EditarCampoTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("e0000000-0000-4000-8000-000000000001");
    private static final ProyectoId OTRO_PROYECTO_ID = ProyectoId.de("e0000000-0000-4000-8000-000000000002");
    private static final BloqueId RELACIONADO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class, CALLS_REAL_METHODS);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class, CALLS_REAL_METHODS);
    private final EditarCampo editarCampo = new CampoUseCaseConfig().editarCampo(
            negocioRepository,
            bloqueRepository,
            campoRepository,
            new TransaccionDirecta(),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    private CampoItem campoEditado;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioConSuBloque() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(unBloqueDelSistema(BLOQUE_ID, PROYECTO_ID)));
    }

    @Test
    @DisplayName("Editar reemplaza la configuración del campo y lo guarda con la hora del reloj")
    void editarReemplazaLaConfiguracionDelCampoYLoGuardaConLaHoraDelReloj() {
        Campo precio = unCampoDelBloque("Precio");
        dadoUnBloqueCon(precio);

        cuandoSeEditaComoTexto(precio, "Costo");

        entoncesElCampoEditadoSeLlamaCostoEsDeTextoYSeActualizoAhora();
        entoncesSeGuardaUnCampo();
    }

    @Test
    @DisplayName("Editar con el nombre de otro campo del bloque no guarda nada")
    void editarConElNombreDeOtroCampoDelBloqueNoGuardaNada() {
        Campo precio = unCampoDelBloque("Precio");
        dadoUnBloqueCon(precio, unCampoDelBloque("Producto"));

        cuandoSeIntentaEditarComoTexto(precio, "Producto");

        entoncesSeRechazaPorNombreRepetido();
        entoncesNoSeGuardaNingunCampo();
    }

    @Test
    @DisplayName("Editar no deja convertir el campo en una conexión con una tabla de otro sistema")
    void editarNoDejaConvertirElCampoEnUnaConexionConUnaTablaDeOtroSistema() {
        Campo precio = unCampoDelBloque("Precio");
        dadoUnBloqueCon(precio);
        dadoUnaTablaRelacionadaDelSistema(OTRO_PROYECTO_ID);

        cuandoSeIntentaEditarComoConexion(precio, "Cliente");

        entoncesSeRechazaPorConfiguracionInvalida();
        entoncesNoSeGuardaNingunCampo();
    }

    @Test
    @DisplayName("Editar guarda una conexión con una tabla del mismo sistema")
    void editarGuardaUnaConexionConUnaTablaDelMismoSistema() {
        Campo precio = unCampoDelBloque("Precio");
        dadoUnBloqueCon(precio);
        dadoUnaTablaRelacionadaDelSistema(PROYECTO_ID);

        cuandoSeEditaComoConexion(precio, "Cliente");

        entoncesElCampoEditadoApuntaALaTablaRelacionada();
    }

    @Test
    @DisplayName("Editar un campo para que sea una fórmula que se cita a sí misma no guarda nada")
    void editarUnCampoParaQueSeaUnaFormulaQueSeCitaASiMismoNoGuardaNada() {
        Campo total = unCampoDelBloque("Total");
        dadoUnBloqueCon(total);

        cuandoSeIntentaEditarComoFormulaQueSeCitaASiMisma(total);

        entoncesSeRechazaPorConfiguracionInvalida();
        entoncesNoSeGuardaNingunCampo();
    }

    @Test
    @DisplayName("Un campo usado por una fórmula no puede cambiar de tipo")
    void unCampoUsadoPorUnaFormulaNoPuedeCambiarDeTipo() {
        Campo cantidad = unCampoDelBloque("Cantidad");
        dadoUnBloqueCon(cantidad, unaFormulaQueUsa(cantidad, "Doble"));

        cuandoSeIntentaEditarComoTexto(cantidad, "Cantidad");

        entoncesSeRechazaPorque("No se puede cambiar el tipo de «Cantidad»: la fórmula «Doble» lo usa");
        entoncesNoSeGuardaNingunCampo();
    }

    @Test
    @DisplayName("Un campo que ninguna fórmula usa puede cambiar de tipo")
    void unCampoQueNingunaFormulaUsaPuedeCambiarDeTipo() {
        Campo cantidad = unCampoDelBloque("Cantidad");
        dadoUnBloqueCon(cantidad, unaFormulaQueNoUsaCampos("Doble"));

        cuandoSeEditaComoTexto(cantidad, "Cantidad");

        entoncesElCampoEditadoEsDeTipo("TEXTO");
    }

    @Test
    @DisplayName("Un campo numérico usado por una fórmula puede seguir siendo numérico")
    void unCampoNumericoUsadoPorUnaFormulaPuedeSeguirSiendoNumerico() {
        Campo cantidad = unCampoDelBloque("Cantidad");
        dadoUnBloqueCon(cantidad, unaFormulaQueUsa(cantidad, "Doble"));

        cuandoSeEditaComoNumero(cantidad, "Cantidad");

        entoncesElCampoEditadoEsDeTipo("NUMERO");
    }

    @Test
    @DisplayName("Un campo ajeno se informa como no encontrado sin consultar el bloque ni editar")
    void unCampoAjenoSePropagaComoNoEncontradoSinConsultarElBloqueNiEditar() {
        Campo ajeno = unCampoDelBloque("Precio");
        dadoUnCampoAjeno(ajeno);

        cuandoSeIntentaEditarComoNumero(ajeno, "Costo");

        entoncesSeRechazaPorqueElCampoNoExiste();
        entoncesNoSeGuardaNingunCampo();
    }

    private void dadoUnBloqueCon(Campo... campos) {
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(campos));
        for (Campo campo : campos) {
            when(campoRepository.findByIdAndNegocioId(campo.getId(), NEGOCIO_ID)).thenReturn(Optional.of(campo));
        }
        when(campoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoUnaTablaRelacionadaDelSistema(ProyectoId proyectoId) {
        when(bloqueRepository.findByIdAndNegocioId(RELACIONADO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(unBloqueDelSistema(RELACIONADO_ID, proyectoId)));
    }

    private void dadoUnCampoAjeno(Campo campo) {
        when(campoRepository.findByIdAndNegocioId(campo.getId(), NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private Bloque unBloqueDelSistema(BloqueId bloqueId, ProyectoId proyectoId) {
        return Bloque.builder().id(bloqueId).negocioId(NEGOCIO_ID).proyectoId(proyectoId).build();
    }

    private Campo unCampoDelBloque(String nombre) {
        return unCampoNumerico(nombre).ubicadoEn(NEGOCIO_ID, BLOQUE_ID, (short) 0, AHORA);
    }

    private void cuandoSeEditaComoTexto(Campo campo, String nombre) {
        campoEditado = editar(campo, nombre, "TEXTO", null, null);
    }

    private void cuandoSeEditaComoNumero(Campo campo, String nombre) {
        campoEditado = editar(campo, nombre, "NUMERO", null, null);
    }

    private void cuandoSeEditaComoConexion(Campo campo, String nombre) {
        campoEditado = editar(campo, nombre, "RELACION", RELACIONADO_ID, null);
    }

    private void cuandoSeIntentaEditarComoTexto(Campo campo, String nombre) {
        rechazo = catchThrowable(() -> cuandoSeEditaComoTexto(campo, nombre));
    }

    private void cuandoSeIntentaEditarComoNumero(Campo campo, String nombre) {
        rechazo = catchThrowable(() -> cuandoSeEditaComoNumero(campo, nombre));
    }

    private void cuandoSeIntentaEditarComoConexion(Campo campo, String nombre) {
        rechazo = catchThrowable(() -> cuandoSeEditaComoConexion(campo, nombre));
    }

    private void cuandoSeIntentaEditarComoFormulaQueSeCitaASiMisma(Campo campo) {
        String expresion = referenciaA(campo) + " * 2";
        rechazo = catchThrowable(() -> editar(campo, "Total", "FORMULA", null, expresion));
    }

    private CampoItem editar(Campo campo, String nombre, String tipo, BloqueId tabla, String formula) {
        return editarCampo.execute(new EditarCampoRequest(
                USUARIO_ID, campo.getId(), nombre, tipo, null, null, null, tabla, formula));
    }

    private void entoncesElCampoEditadoSeLlamaCostoEsDeTextoYSeActualizoAhora() {
        assertThat(campoEditado.nombre()).isEqualTo("Costo");
        assertThat(campoEditado.tipo()).isEqualTo("TEXTO");
        assertThat(campoEditado.actualizadoEn()).isEqualTo(AHORA);
    }

    private void entoncesElCampoEditadoEsDeTipo(String tipo) {
        assertThat(campoEditado.tipo()).isEqualTo(tipo);
    }

    private void entoncesElCampoEditadoApuntaALaTablaRelacionada() {
        assertThat(campoEditado.bloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
    }

    private void entoncesSeRechazaPorNombreRepetido() {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class);
    }

    private void entoncesSeRechazaPorConfiguracionInvalida() {
        assertThat(rechazo).isInstanceOf(CampoInvalidoException.class);
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }

    private void entoncesSeRechazaPorqueElCampoNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesSeGuardaUnCampo() {
        verify(campoRepository).save(any(Campo.class));
    }

    private void entoncesNoSeGuardaNingunCampo() {
        verify(campoRepository, never()).save(any());
    }
}
