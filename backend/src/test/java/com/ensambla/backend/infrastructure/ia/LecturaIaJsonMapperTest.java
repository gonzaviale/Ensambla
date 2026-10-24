package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.CampoContexto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.BloqueJson;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.CampoJson;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.DatoJson;
import com.ensambla.backend.infrastructure.ia.LecturaIaJson.FilaJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LecturaIaJsonMapperTest {

    private static final int PAGINAS = 2;
    private static final BloqueId BLOQUE_VENTAS_ID = BloqueId.nuevo();
    private static final List<BloqueConocido> BLOQUES = List.of(
            new BloqueConocido(BLOQUE_VENTAS_ID, "Ventas", List.of(
                    new CampoContexto("Fecha", TipoCampo.FECHA, List.of()),
                    new CampoContexto("Monto", TipoCampo.NUMERO, List.of()))));
    private static final ConsumoIa CONSUMO = new ConsumoIa("simulado", "modelo", "v1", PAGINAS, 10, 20, 5);
    private static final CampoJson FECHA = new CampoJson("fecha", "Fecha", "FECHA", List.of());
    private static final CampoJson MONTO = new CampoJson("monto", "Monto", "numero", null);

    private LecturaIaJsonMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LecturaIaJsonMapper();
    }

    @Test
    @DisplayName("Convierte una lectura válida con tipo, bloque, filas, advertencias y consumo")
    void toDomainShouldMapValidLectura() {
        LecturaIaJson json = givenLectura(givenBloque(null, FECHA, MONTO),
                List.of(givenFila(1, 1, Map.of("fecha", new DatoJson("2026-10-03", 0.9), "monto", new DatoJson("100", 0.5)))),
                List.of("Página borrosa"));

        LecturaCuaderno result = mapper.toDomain(json, BLOQUES, PAGINAS, CONSUMO);

        thenLecturaIsMapped(result);
    }

    @Test
    @DisplayName("Sin filas ni advertencias devuelve listas vacías")
    void toDomainShouldReturnEmptyListsWhenFilasAndAdvertenciasAreNull() {
        LecturaIaJson json = givenLectura(givenBloque(null, FECHA), null, null);

        LecturaCuaderno result = mapper.toDomain(json, BLOQUES, PAGINAS, CONSUMO);

        assertThat(result.filas()).isEmpty();
        assertThat(result.advertencias()).isEmpty();
    }

    @Test
    @DisplayName("Una fila sin datos queda vacía")
    void toDomainShouldAcceptFilaWithoutDatos() {
        LecturaIaJson json = givenLectura(givenBloque(null, FECHA), List.of(givenFila(2, 3, null)), List.of());

        FilaLeida fila = mapper.toDomain(json, BLOQUES, PAGINAS, CONSUMO).filas().getFirst();

        assertThat(fila.pagina()).isEqualTo(2);
        assertThat(fila.renglon()).isEqualTo(3);
        assertThat(fila.datos()).isEmpty();
    }

    @Test
    @DisplayName("Acepta el bloque existente que sugiere la IA si es del negocio")
    void toDomainShouldKeepBloqueExistenteWhenItBelongsToNegocio() {
        LecturaIaJson json = givenLectura(givenBloque(BLOQUE_VENTAS_ID.toString().toUpperCase(), FECHA), List.of(), List.of());

        LecturaCuaderno result = mapper.toDomain(json, BLOQUES, PAGINAS, CONSUMO);

        assertThat(result.bloque().bloqueExistente()).contains(BLOQUE_VENTAS_ID);
    }

    @Test
    @DisplayName("Descarta un bloque existente que no es del negocio y propone uno nuevo")
    void toDomainShouldDropBloqueExistenteWhenItIsUnknown() {
        LecturaIaJson json = givenLectura(givenBloque(UUID.randomUUID().toString(), FECHA), List.of(), List.of());

        LecturaCuaderno result = mapper.toDomain(json, BLOQUES, PAGINAS, CONSUMO);

        assertThat(result.bloque().bloqueExistente()).isEmpty();
    }

    @Test
    @DisplayName("Acepta un campo de conexión que el bloque existente elegido ya tiene")
    void toDomainShouldKeepRelacionCampoOfExistingBloque() {
        List<BloqueConocido> bloques = List.of(new BloqueConocido(BLOQUE_VENTAS_ID, "Ventas", List.of(
                new CampoContexto("Cliente", TipoCampo.RELACION, List.of(), "Clientes"))));
        CampoJson cliente = new CampoJson("cliente", " cliente ", "RELACION", List.of());
        LecturaIaJson json = givenLectura(givenBloque(BLOQUE_VENTAS_ID.toString(), cliente), List.of(), List.of());

        LecturaCuaderno result = mapper.toDomain(json, bloques, PAGINAS, CONSUMO);

        assertThat(result.bloque().campos().getFirst().tipo()).isEqualTo(TipoCampo.RELACION);
    }

    @Test
    @DisplayName("Una conexión que el bloque no tiene se lee como el texto escrito")
    void toDomainShouldDegradeInventedRelacionToTexto() {
        CampoJson cliente = new CampoJson("cliente", "Cliente", "RELACION", List.of());
        LecturaIaJson json = givenLectura(givenBloque(null, cliente), List.of(), List.of());

        LecturaCuaderno result = mapper.toDomain(json, BLOQUES, PAGINAS, CONSUMO);

        assertThat(result.bloque().campos().getFirst().tipo()).isEqualTo(TipoCampo.TEXTO);
    }

    @Test
    @DisplayName("Una conexión sin nombre se rechaza como cualquier campo sin nombre")
    void toDomainShouldRejectRelacionWithoutName() {
        CampoJson sinNombre = new CampoJson("cliente", null, "RELACION", List.of());
        LecturaIaJson json = givenLectura(givenBloque(null, sinNombre), List.of(), List.of());

        thenRejects(json, "falta el nombre del campo cliente");
    }

    @Test
    @DisplayName("Acepta un campo OPCION con opciones")
    void toDomainShouldMapOpcionCampoWithOptions() {
        CampoJson producto = new CampoJson("producto", "Producto", "OPCION", List.of("Vela", "Difusor"));
        LecturaIaJson json = givenLectura(givenBloque(null, producto), List.of(), List.of());

        LecturaCuaderno result = mapper.toDomain(json, BLOQUES, PAGINAS, CONSUMO);

        assertThat(result.bloque().campos().getFirst().opciones()).containsExactly("Vela", "Difusor");
    }

    @Test
    @DisplayName("Rechaza un tipo de registro desconocido")
    void toDomainShouldRejectUnknownTipoRegistro() {
        LecturaIaJson json = new LecturaIaJson("RECETAS", givenBloque(null, FECHA), List.of(), List.of());

        thenRejects(json, "tipoRegistro es desconocido: RECETAS");
    }

    @Test
    @DisplayName("Rechaza una lectura sin tipo de registro")
    void toDomainShouldRejectMissingTipoRegistro() {
        LecturaIaJson json = new LecturaIaJson(null, givenBloque(null, FECHA), List.of(), List.of());

        thenRejects(json, "falta tipoRegistro");
    }

    @Test
    @DisplayName("Rechaza una lectura sin bloque")
    void toDomainShouldRejectMissingBloque() {
        thenRejects(givenLectura(null, List.of(), List.of()), "falta el bloque propuesto");
    }

    @Test
    @DisplayName("Rechaza un bloque sin campos")
    void toDomainShouldRejectBloqueWithoutCampos() {
        thenRejects(givenLectura(new BloqueJson("Ventas", null, null), List.of(), List.of()), "el bloque no tiene campos");
    }

    @Test
    @DisplayName("Rechaza un bloque con nombre en blanco")
    void toDomainShouldRejectBlankBloqueNombre() {
        thenRejects(givenLectura(new BloqueJson("  ", null, List.of(FECHA)), List.of(), List.of()),
                "falta el nombre del bloque");
    }

    @Test
    @DisplayName("Rechaza un campo con tipo desconocido")
    void toDomainShouldRejectUnknownTipoCampo() {
        CampoJson raro = new CampoJson("foto", "Foto", "IMAGEN", List.of());

        thenRejects(givenLectura(givenBloque(null, raro), List.of(), List.of()), "el tipo del campo foto es desconocido");
    }

    @Test
    @DisplayName("Rechaza un campo OPCION sin opciones")
    void toDomainShouldRejectOpcionCampoWithoutOptions() {
        CampoJson producto = new CampoJson("producto", "Producto", "OPCION", List.of());

        thenRejects(givenLectura(givenBloque(null, producto), List.of(), List.of()), "es OPCION y no tiene opciones");
    }

    @Test
    @DisplayName("Rechaza un campo sin clave")
    void toDomainShouldRejectCampoWithoutClave() {
        CampoJson sinClave = new CampoJson(null, "Fecha", "FECHA", List.of());

        thenRejects(givenLectura(givenBloque(null, sinClave), List.of(), List.of()), "falta la clave de un campo");
    }

    @Test
    @DisplayName("Rechaza claves de campo repetidas")
    void toDomainShouldRejectDuplicatedClaves() {
        thenRejects(givenLectura(givenBloque(null, FECHA, FECHA), List.of(), List.of()), "la clave fecha está repetida");
    }

    @Test
    @DisplayName("Rechaza una fila que menciona una página que no se envió")
    void toDomainShouldRejectFilaFromPageNotSent() {
        LecturaIaJson json = givenLectura(givenBloque(null, FECHA), List.of(givenFila(3, 1, Map.of())), List.of());

        thenRejects(json, "la página 3 y sólo se enviaron 2");
    }

    @Test
    @DisplayName("Rechaza una fila sin página")
    void toDomainShouldRejectFilaWithoutPagina() {
        LecturaIaJson json = givenLectura(givenBloque(null, FECHA), List.of(givenFila(null, 1, Map.of())), List.of());

        thenRejects(json, "la página de una fila debe ser 1 o mayor");
    }

    @Test
    @DisplayName("Rechaza una fila con renglón cero")
    void toDomainShouldRejectFilaWithRenglonZero() {
        LecturaIaJson json = givenLectura(givenBloque(null, FECHA), List.of(givenFila(1, 0, Map.of())), List.of());

        thenRejects(json, "el renglón de una fila debe ser 1 o mayor");
    }

    @Test
    @DisplayName("Rechaza una fila que usa una clave que no está entre los campos")
    void toDomainShouldRejectUnknownClaveInFila() {
        LecturaIaJson json = givenLectura(givenBloque(null, FECHA),
                List.of(givenFila(1, 1, Map.of("cliente", new DatoJson("Marta", 0.9)))), List.of());

        thenRejects(json, "la clave cliente que no está entre los campos");
    }

    @Test
    @DisplayName("Rechaza un dato nulo")
    void toDomainShouldRejectNullDato() {
        Map<String, DatoJson> datos = new HashMap<>();
        datos.put("fecha", null);
        LecturaIaJson json = givenLectura(givenBloque(null, FECHA), List.of(givenFila(1, 1, datos)), List.of());

        thenRejects(json, "la confianza de fecha debe estar entre 0 y 1");
    }

    @Test
    @DisplayName("Rechaza un dato sin confianza")
    void toDomainShouldRejectDatoWithoutConfianza() {
        thenRejectsConfianza(null);
    }

    @Test
    @DisplayName("Rechaza una confianza negativa")
    void toDomainShouldRejectNegativeConfianza() {
        thenRejectsConfianza(-0.1);
    }

    @Test
    @DisplayName("Rechaza una confianza mayor a 1")
    void toDomainShouldRejectConfianzaAboveOne() {
        thenRejectsConfianza(1.5);
    }

    // --- arrange ---
    private LecturaIaJson givenLectura(BloqueJson bloque, List<FilaJson> filas, List<String> advertencias) {
        return new LecturaIaJson("ventas", bloque, filas, advertencias);
    }

    private BloqueJson givenBloque(String bloqueExistenteId, CampoJson... campos) {
        return new BloqueJson(" Ventas ", bloqueExistenteId, List.of(campos));
    }

    private FilaJson givenFila(Integer pagina, Integer renglon, Map<String, DatoJson> datos) {
        return new FilaJson(pagina, renglon, datos);
    }

    // --- assert ---
    private void thenLecturaIsMapped(LecturaCuaderno result) {
        assertThat(result.tipoRegistro()).isEqualTo(TipoRegistroCuaderno.VENTAS);
        assertThat(result.bloque().nombre()).isEqualTo("Ventas");
        assertThat(result.bloque().bloqueExistente()).isEmpty();
        assertThat(result.bloque().campos()).extracting(campo -> campo.tipo())
                .containsExactly(TipoCampo.FECHA, TipoCampo.NUMERO);
        assertThat(result.bloque().campos().get(1).opciones()).isEmpty();
        FilaLeida fila = result.filas().getFirst();
        assertThat(fila.datos()).containsEntry("fecha", new DatoLeido("2026-10-03", 0.9))
                .containsEntry("monto", new DatoLeido("100", 0.5));
        assertThat(result.advertencias()).containsExactly("Página borrosa");
        assertThat(result.consumo()).isEqualTo(CONSUMO);
    }

    private void thenRejectsConfianza(Double confianza) {
        LecturaIaJson json = givenLectura(givenBloque(null, FECHA),
                List.of(givenFila(1, 1, Map.of("fecha", new DatoJson("2026-10-03", confianza)))), List.of());

        thenRejects(json, "la confianza de fecha debe estar entre 0 y 1");
    }

    private void thenRejects(LecturaIaJson json, String detalle) {
        assertThatThrownBy(() -> mapper.toDomain(json, BLOQUES, PAGINAS, CONSUMO))
                .isInstanceOf(LecturaCuadernoException.class)
                .hasMessageStartingWith(LecturaIaJsonMapper.PREFIJO_ERROR)
                .hasMessageContaining(detalle);
    }
}
