package com.ensambla.backend.domain.model.enums;

public enum TipoAviso {
    STOCK(GrupoAviso.STOCK),
    PRECIO_ZONA(GrupoAviso.VENTAS),
    ESTANCADO(GrupoAviso.VENTAS),
    MUY_VENDIDO(GrupoAviso.VENTAS),
    COMBO(GrupoAviso.VENTAS),
    CONSUMO_ZONA(GrupoAviso.ZONA);

    private final GrupoAviso grupo;

    TipoAviso(GrupoAviso grupo) {
        this.grupo = grupo;
    }

    public GrupoAviso grupo() {
        return grupo;
    }
}
