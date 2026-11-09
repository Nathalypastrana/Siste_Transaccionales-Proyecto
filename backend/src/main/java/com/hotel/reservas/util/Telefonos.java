package com.hotel.reservas.util;

/** Utilidades para números de teléfono (WhatsApp envía el número con indicativo y solo dígitos). */
public final class Telefonos {

    private Telefonos() {
    }

    /** Últimos 10 dígitos: permite comparar "573001112233" con "300 111 2233". */
    public static String ultimos10(String telefono) {
        if (telefono == null) {
            return "";
        }
        String digitos = telefono.replaceAll("\\D", "");
        return digitos.length() > 10 ? digitos.substring(digitos.length() - 10) : digitos;
    }

    /** Para los registros del sistema: deja visibles solo los 4 últimos dígitos. */
    public static String enmascarar(String telefono) {
        String d = telefono == null ? "" : telefono.replaceAll("\\D", "");
        return d.length() <= 4 ? "****" : "******" + d.substring(d.length() - 4);
    }
}
