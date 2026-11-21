package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.AccionCrearRegistro;
import com.ensambla.backend.domain.model.aviso.AccionDeAviso;
import com.ensambla.backend.domain.model.aviso.AccionEditarRegistro;
import com.ensambla.backend.domain.model.aviso.AccionInvalida;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.AvisoTestData.INVENTARIO_ID;
import static com.ensambla.backend.support.AvisoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.AvisoTestData.PROYECTO_ID;

/** Avisos, usuario y acciones de ejemplo para los casos de uso de avisos. */
public final class AvisosDePrueba {

    public static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    public static final AvisoId AVISO_ID = AvisoId.de("e0000000-0000-4000-8000-000000000001");
    public static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    public static final CampoId CAMPO_PRECIO_ID = CampoId.de("f0000000-0000-4000-8000-000000000009");
    public static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 10, 19, 12, 0, 0, 0, ZoneOffset.UTC);

    private AvisosDePrueba() {
    }

    public static Aviso avisoPendienteConAcciones(AccionDeAviso... acciones) {
        return Aviso.builder().id(AVISO_ID).negocioId(NEGOCIO_ID).estado(EstadoAviso.PENDIENTE)
                .prioridad((short) 2).acciones(List.of(acciones)).build();
    }

    public static Aviso avisoDelNegocio(String titulo, int prioridad, EstadoAviso estado, int hora) {
        return Aviso.builder()
                .id(AvisoId.nuevo()).negocioId(NEGOCIO_ID).titulo(titulo).prioridad((short) prioridad)
                .estado(estado).creadoEn(AHORA.withHour(hora)).acciones(List.of())
                .build();
    }

    public static AccionDeAviso editarPrecio(Map<String, Object> valores) {
        return new AccionEditarRegistro("Cambiar precio", PROYECTO_ID, INVENTARIO_ID, REGISTRO_ID, valores);
    }

    public static AccionDeAviso crearEnElInventario(Map<String, Object> valores) {
        return new AccionCrearRegistro("Crear el combo", PROYECTO_ID, INVENTARIO_ID, valores);
    }

    public static AccionDeAviso accionIncompleta(String tipo, String motivo) {
        return new AccionInvalida(tipo, motivo, Map.of());
    }

    public static List<Campo> camposDelInventario() {
        return List.of(
                campoTipado(AvisoTestData.CAMPO_NOMBRE_ID, "name", TipoCampo.TEXTO, true),
                campoTipado(AvisoTestData.CAMPO_CANTIDAD_ID, "quantity", TipoCampo.NUMERO, true),
                campoTipado(CAMPO_PRECIO_ID, "price", TipoCampo.MONEDA, false));
    }

    private static Campo campoTipado(CampoId id, String clave, TipoCampo tipo, boolean obligatorio) {
        return Campo.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(INVENTARIO_ID).clave(clave)
                .tipo(tipo).obligatorio(obligatorio).build();
    }
}
