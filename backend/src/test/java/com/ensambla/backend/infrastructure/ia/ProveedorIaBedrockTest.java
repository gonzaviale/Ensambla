package com.ensambla.backend.infrastructure.ia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProveedorIaBedrockTest {

    private static final AjustesIa AJUSTES = new AjustesIa("bedrock", null, "us-east-1", null, "rapido", "avanzado",
            "vision", Duration.ofSeconds(30));
    private static final List<DefinicionHerramienta> HERRAMIENTAS = List.of(new DefinicionHerramienta(
            "renombrar_sistema", "Renombra el sistema.", Map.of("type", "object", "properties", Map.of())));

    private final List<AjustesIa> conexiones = new ArrayList<>();
    private BedrockRuntimeClient cliente;
    private ProveedorIaBedrock proveedor;
    private ClienteModeloVision vision;
    private ClienteModeloTexto texto;
    private ClienteModeloConHerramientas conHerramientas;
    private String regionConPerfil;
    private String regionSinPerfil;
    private String regionConPerfilEnBlanco;

    @BeforeEach
    void setUp() {
        cliente = mock(BedrockRuntimeClient.class);
        proveedor = new ProveedorIaBedrock(ajustes -> {
            conexiones.add(ajustes);
            return cliente;
        });
    }

    @Test
    @DisplayName("Arma las tres puertas con los clientes de Bedrock")
    void armaLasTresPuertasConLosClientesDeBedrock() {
        cuandoSeArmanLasTresPuertas();

        entoncesSeArmanLosTresClientesDeBedrock();
    }

    @Test
    @DisplayName("Reusa un cliente por región, perfil y timeout")
    void reusaUnClientePorRegionPerfilYTimeout() {
        cuandoSePidenPuertasEnDosRegiones();

        entoncesSeAbrioUnaConexionPorRegion();
    }

    @Test
    @DisplayName("Al cerrar cierra los clientes que abrió")
    void alCerrarCierraLosClientesQueAbrio() {
        cuandoSeAbreLaVisionYSeCierraElProveedor();

        entoncesSeCerroElCliente();
    }

    @Test
    @DisplayName("Conecta con el perfil de AWS indicado o con las credenciales por defecto, y la región por defecto")
    void conectaConElPerfilDeAwsIndicadoOConLasCredencialesPorDefectoYLaRegionPorDefecto() {
        cuandoSeConectaConPerfilSinPerfilYConPerfilEnBlanco();

        entoncesLasTresConexionesUsanUsEast1();
    }

    @Test
    @DisplayName("Sin la función de prueba, conecta de verdad (sin llamar todavía a la red)")
    void sinLaFuncionDePruebaConectaDeVerdadSinLlamarTodaviaALaRed() {
        cuandoSeAbreUnProveedorReal();

        entoncesElTextoEsDeBedrock();
    }

    // --- cuando ---

    private void cuandoSeArmanLasTresPuertas() {
        vision = proveedor.vision(AJUSTES);
        texto = proveedor.texto(AJUSTES, "avanzado", 700);
        conHerramientas = proveedor.conHerramientas(AJUSTES, 900, HERRAMIENTAS);
    }

    private void cuandoSePidenPuertasEnDosRegiones() {
        proveedor.vision(AJUSTES);
        proveedor.texto(AJUSTES, "rapido", 700);
        proveedor.texto(new AjustesIa("bedrock", null, "us-west-2", null, "rapido", null, null,
                Duration.ofSeconds(30)), "rapido", 700);
    }

    private void cuandoSeAbreLaVisionYSeCierraElProveedor() {
        proveedor.vision(AJUSTES);
        proveedor.close();
    }

    private void cuandoSeConectaConPerfilSinPerfilYConPerfilEnBlanco() {
        try (BedrockRuntimeClient conPerfil = ProveedorIaBedrock.conectar(ajustes("us-east-1", "perfil-de-prueba"));
             BedrockRuntimeClient sinPerfil = ProveedorIaBedrock.conectar(ajustes(null, null));
             BedrockRuntimeClient perfilEnBlanco = ProveedorIaBedrock.conectar(ajustes("  ", "  "))) {
            regionConPerfil = conPerfil.serviceClientConfiguration().region().id();
            regionSinPerfil = sinPerfil.serviceClientConfiguration().region().id();
            regionConPerfilEnBlanco = perfilEnBlanco.serviceClientConfiguration().region().id();
        }
    }

    private void cuandoSeAbreUnProveedorReal() {
        try (ProveedorIaBedrock real = new ProveedorIaBedrock()) {
            texto = real.texto(AJUSTES, "rapido", 700);
        }
    }

    // --- entonces ---

    private void entoncesSeArmanLosTresClientesDeBedrock() {
        assertThat(proveedor.nombre()).isEqualTo("bedrock");
        assertThat(vision).isInstanceOf(ClienteModeloVisionBedrock.class);
        assertThat(texto).isInstanceOf(ClienteModeloTextoBedrock.class);
        assertThat(conHerramientas).isInstanceOf(ClienteModeloConHerramientasBedrock.class);
    }

    private void entoncesSeAbrioUnaConexionPorRegion() {
        assertThat(conexiones).extracting(AjustesIa::region).containsExactly("us-east-1", "us-west-2");
    }

    private void entoncesSeCerroElCliente() {
        verify(cliente).close();
    }

    private void entoncesLasTresConexionesUsanUsEast1() {
        assertThat(regionConPerfil).isEqualTo("us-east-1");
        assertThat(regionSinPerfil).isEqualTo("us-east-1");
        assertThat(regionConPerfilEnBlanco).isEqualTo("us-east-1");
    }

    private void entoncesElTextoEsDeBedrock() {
        assertThat(texto).isInstanceOf(ClienteModeloTextoBedrock.class);
    }

    // --- datos ---

    private AjustesIa ajustes(String region, String perfil) {
        return new AjustesIa("bedrock", null, region, perfil, "rapido", null, null, Duration.ofSeconds(5));
    }
}
