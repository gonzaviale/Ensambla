package com.ensambla.backend.domain.model.marca;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** La marca del negocio: logo, dos colores, tipografía y tono. De ella salen todos sus carteles. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KitDeMarca {

    public static final ColoresSugeridos COLORES_DE_ENSAMBLA =
            new ColoresSugeridos(ColorHex.de("#00506A"), ColorHex.de("#EE8000"));

    private KitDeMarcaId id;

    private NegocioId negocioId;

    private ColorHex colorPrincipal;

    private ColorHex colorSecundario;

    private Tipografia tipografia;

    private TonoDeMarca tono;

    private LogoDeMarca logo;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    /** El kit con el que arranca un negocio que todavía no armó el suyo: la paleta de Ensambla. */
    public static KitDeMarca predeterminado(NegocioId negocioId, OffsetDateTime ahora) {
        return KitDeMarca.builder()
                .id(KitDeMarcaId.nuevo())
                .negocioId(negocioId)
                .colorPrincipal(COLORES_DE_ENSAMBLA.principal())
                .colorSecundario(COLORES_DE_ENSAMBLA.secundario())
                .tipografia(Tipografia.PLUS_JAKARTA_SANS)
                .tono(TonoDeMarca.CERCANO)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    public void personalizar(ColorHex principal, ColorHex secundario, Tipografia tipografia, TonoDeMarca tono,
                             OffsetDateTime ahora) {
        exigirColoresDistintos(principal, secundario);
        this.colorPrincipal = principal;
        this.colorSecundario = secundario;
        this.tipografia = tipografia;
        this.tono = tono;
        this.actualizadoEn = ahora;
    }

    public void ponerLogo(LogoDeMarca logo, OffsetDateTime ahora) {
        this.logo = logo;
        this.actualizadoEn = ahora;
    }

    public boolean tieneLogo() {
        return logo != null;
    }

    private void exigirColoresDistintos(ColorHex principal, ColorHex secundario) {
        if (principal.equals(secundario)) {
            throw new BusinessRuleException("El color principal y el secundario tienen que ser distintos");
        }
    }
}
