package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.BloquePlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.CampoPlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.useCase.sami.ColumnaSamiItem;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-119: mide cuántas descripciones resuelve el algoritmo propio, sin IA, contra
 * {@code sami/frases-prueba.json}. Imprime el reporte en cada corrida para ver cómo evoluciona el diccionario.
 */
class AciertoSamiTest {

    private static final double UMBRAL = 0.7;
    private static final double META_ACIERTO = 0.80;
    private static final double META_PRECISION_DE_LAS_DECIDIDAS = 0.90;
    private static final int MINIMO_DE_FRASES = 40;

    private DiccionarioSami diccionario;
    private List<PlantillaProyecto> plantillas;

    @BeforeEach
    void setUp() throws IOException {
        diccionario = leer("/ia/sami-diccionario.json", DiccionarioSami.class);
        plantillas = List.of(
                plantilla("Inventario", "Nombre:TEXTO", "Stock:NUMERO", "Rubro:OPCION"),
                plantilla("Pedidos", "Cliente:TEXTO", "Entrega:FECHA", "Entregado:SI_NO"),
                plantilla("Clientes", "Nombre:TEXTO", "Teléfono:TEXTO"),
                plantilla("Ventas", "Fecha:FECHA", "Producto:TEXTO", "Cantidad:NUMERO", "Monto:NUMERO", "Cliente:TEXTO"),
                plantilla("Gastos", "Fecha:FECHA", "Detalle:TEXTO", "Monto:NUMERO"));
    }

    @Test
    @DisplayName("El algoritmo propio acierta la plantilla en al menos el 80 % de las frases de prueba")
    void executeShouldGuessTemplateInAtLeastEightyPercentOfPhrases() throws IOException {
        List<FraseDePrueba> frases = frases();

        Resultado resultado = medir(frases);

        System.out.println(resultado.reporte());
        assertThat(frases).hasSizeGreaterThanOrEqualTo(MINIMO_DE_FRASES);
        assertThat(resultado.aciertoPlantilla()).as(resultado.reporte()).isGreaterThanOrEqualTo(META_ACIERTO);
    }

    @Test
    @DisplayName("Lo que el algoritmo da por resuelto casi siempre es correcto y lo vago no se da por resuelto")
    void executeShouldOnlyDecideWhenSure() throws IOException {
        Resultado resultado = medir(frases());

        assertThat(resultado.precisionDeLasDecididas()).as(resultado.reporte())
                .isGreaterThanOrEqualTo(META_PRECISION_DE_LAS_DECIDIDAS);
        assertThat(resultado.vagasDecididas()).as(resultado.reporte()).isZero();
    }

    private Resultado medir(List<FraseDePrueba> frases) {
        int conPlantilla = 0;
        int aciertos = 0;
        int decididas = 0;
        int decididasCorrectas = 0;
        int vagasDecididas = 0;
        int columnasEsperadas = 0;
        int columnasAcertadas = 0;
        int columnasSugeridas = 0;
        List<String> errores = new ArrayList<>();
        for (FraseDePrueba frase : frases) {
            Optional<PropuestaSamiItem> resultado = SamiPruebas.interpretar(diccionario, frase.texto(), plantillas);
            String plantilla = resultado.map(PropuestaSamiItem::plantilla).orElse(null);
            double confianza = resultado.map(PropuestaSamiItem::confianza).orElse(0.0);
            boolean decidida = plantilla != null && confianza >= UMBRAL;
            boolean correcta = Objects.equals(plantilla, frase.plantilla());
            if (frase.plantilla() == null) {
                vagasDecididas += decidida ? 1 : 0;
            } else {
                conPlantilla++;
                aciertos += correcta ? 1 : 0;
                Set<String> sugeridas = nombres(resultado.map(PropuestaSamiItem::columnas).orElse(List.of()));
                columnasEsperadas += frase.columnas().size();
                columnasSugeridas += sugeridas.size();
                columnasAcertadas += (int) frase.columnas().stream().filter(sugeridas::contains).count();
            }
            if (decidida) {
                decididas++;
                decididasCorrectas += correcta ? 1 : 0;
            }
            if (!correcta) {
                errores.add("  - «%s»: esperaba %s, dio %s (%.2f)"
                        .formatted(frase.texto(), frase.plantilla(), plantilla, confianza));
            }
        }
        return new Resultado(frases.size(), conPlantilla, aciertos, decididas, decididasCorrectas, vagasDecididas,
                columnasEsperadas, columnasAcertadas, columnasSugeridas, errores);
    }

    private Set<String> nombres(List<ColumnaSamiItem> columnas) {
        Set<String> nombres = new HashSet<>();
        columnas.forEach(columna -> nombres.add(columna.nombre()));
        return nombres;
    }

    private List<FraseDePrueba> frases() throws IOException {
        return leer("/sami/frases-prueba.json", new TypeReference<>() { });
    }

    private <T> T leer(String recurso, Class<T> tipo) throws IOException {
        try (InputStream json = getClass().getResourceAsStream(recurso)) {
            return JsonMapper.builder().build().readValue(json, tipo);
        }
    }

    private <T> T leer(String recurso, TypeReference<T> tipo) throws IOException {
        try (InputStream json = getClass().getResourceAsStream(recurso)) {
            return JsonMapper.builder().build().readValue(json, tipo);
        }
    }

    private PlantillaProyecto plantilla(String nombre, String... campos) {
        List<CampoPlantilla> lista = java.util.Arrays.stream(campos)
                .map(campo -> campo.split(":"))
                .map(partes -> new CampoPlantilla(partes[0], TipoCampo.valueOf(partes[1]), false, null, null))
                .toList();
        return new PlantillaProyecto(PlantillaId.nuevo(), nombre, "",
                new EstructuraPlantilla(List.of(new BloquePlantilla(nombre, lista))));
    }

    private record FraseDePrueba(String texto, String plantilla, List<String> columnas) {
    }

    private record Resultado(
            int frases,
            int conPlantilla,
            int aciertos,
            int decididas,
            int decididasCorrectas,
            int vagasDecididas,
            int columnasEsperadas,
            int columnasAcertadas,
            int columnasSugeridas,
            List<String> errores
    ) {

        double aciertoPlantilla() {
            return (double) aciertos / conPlantilla;
        }

        double precisionDeLasDecididas() {
            return decididas == 0 ? 1.0 : (double) decididasCorrectas / decididas;
        }

        String reporte() {
            return ("Sami sin IA: %d frases | plantilla %d/%d = %.0f %% | resueltas sin preguntar %d (precisión %.0f %%)"
                    + " | vagas resueltas por error %d | columnas esperadas %d, acertadas %d, sugeridas %d%n%s")
                    .formatted(frases, aciertos, conPlantilla, aciertoPlantilla() * 100, decididas,
                            precisionDeLasDecididas() * 100, vagasDecididas, columnasEsperadas, columnasAcertadas,
                            columnasSugeridas, String.join("\n", errores));
        }
    }
}
