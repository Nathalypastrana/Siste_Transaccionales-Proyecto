package com.hotel.reservas.asistente.intencion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Clasificador de intención del mensaje del huésped (sección "Modelado de una red neuronal simple").
 * Extrae seis señales binarias y las pasa por la red 6-4-5:
 * x1 menciona fechas, x2 pide precio, x3 incluye código de reserva, x4 palabras de cancelación,
 * x5 indica número de personas, x6 el huésped tiene una reserva vigente (dato del sistema).
 *
 * La red se entrena al arrancar con ejemplos etiquetados (las 64 combinaciones de señales).
 * Su salida es una PISTA para el modelo de lenguaje, que sigue redactando la respuesta con las herramientas;
 * no ejecuta acciones por sí sola.
 */
@Slf4j
@Component
public class ClasificadorIntencion {

    public static final double UMBRAL = 0.5;

    public record Resultado(Intencion intencion, double probabilidad, boolean clara) {
    }

    private static final Pattern FECHAS = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}|\\bdel \\d{1,2} al \\d{1,2}\\b|\\d{1,2} de (enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre)|\\b(manana|hoy|fin de semana|puente)\\b");
    private static final Pattern PRECIO = Pattern.compile(
            "cuanto (cuesta|vale|cobran|sale)|precio|tarifa|valor|costo|cobran");
    private static final Pattern CODIGO = Pattern.compile("\\bcr-?[a-z0-9]{8}\\b");
    private static final Pattern CANCELACION = Pattern.compile("cancel|reembols|devoluc|devuelv|anular");
    private static final Pattern PERSONAS = Pattern.compile(
            "\\d+ (personas|adultos|huespedes|ninos)|para (una|dos|tres|cuatro|cinco|seis|\\d+)\\b|somos (\\d+|dos|tres|cuatro|cinco|seis)");

    private final RedIntencion red = new RedIntencion();

    public ClasificadorIntencion() {
        entrenar();
    }

    private void entrenar() {
        List<double[]> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();
        for (int combo = 0; combo < 64; combo++) {
            double[] x = new double[RedIntencion.ENTRADAS];
            for (int k = 0; k < RedIntencion.ENTRADAS; k++) {
                x[k] = (combo >> k) & 1;
            }
            xs.add(x);
            ys.add(etiqueta(x).ordinal());
        }
        double[][] x = xs.toArray(new double[0][]);
        int[] y = ys.stream().mapToInt(Integer::intValue).toArray();
        red.entrenar(x, y, 4000, 0.05, 42L);

        int aciertos = 0;
        for (int i = 0; i < x.length; i++) {
            if (argmax(red.predecir(x[i])) == y[i]) {
                aciertos++;
            }
        }
        log.info("Clasificador de intención entrenado: {}/{} combinaciones correctas.", aciertos, x.length);
    }

    /** Intención esperada de cada combinación de señales (los ejemplos con los que aprende la red). */
    private static Intencion etiqueta(double[] x) {
        boolean fechas = x[0] == 1, precio = x[1] == 1, codigo = x[2] == 1, cancela = x[3] == 1, activa = x[5] == 1;
        if (cancela && codigo) {
            return Intencion.DERIVAR;                  // cancelar una reserva concreta lo hace el recepcionista
        }
        if (cancela) {
            return Intencion.POLITICA_CANCELACION;
        }
        if (codigo) {
            return Intencion.ESTADO_RESERVA;
        }
        if (precio) {
            return Intencion.TARIFAS;
        }
        if (fechas || x[4] == 1) {
            return Intencion.DISPONIBILIDAD;
        }
        return activa ? Intencion.ESTADO_RESERVA : Intencion.DERIVAR;
    }

    public Resultado clasificar(String mensaje, boolean tieneReservaActiva) {
        String t = normalizar(mensaje);
        double[] x = {
                FECHAS.matcher(t).find() ? 1 : 0,
                PRECIO.matcher(t).find() ? 1 : 0,
                CODIGO.matcher(t).find() ? 1 : 0,
                CANCELACION.matcher(t).find() ? 1 : 0,
                PERSONAS.matcher(t).find() ? 1 : 0,
                tieneReservaActiva ? 1 : 0
        };
        double[] p = red.predecir(x);
        int mejor = argmax(p);
        return new Resultado(Intencion.values()[mejor], p[mejor], p[mejor] >= UMBRAL);
    }

    private static int argmax(double[] p) {
        int m = 0;
        for (int i = 1; i < p.length; i++) {
            if (p[i] > p[m]) {
                m = i;
            }
        }
        return m;
    }

    static String normalizar(String s) {
        String sinTildes = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase();
    }
}
