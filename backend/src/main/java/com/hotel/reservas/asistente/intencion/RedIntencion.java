package com.hotel.reservas.asistente.intencion;

import java.util.Random;

/**
 * Red neuronal simple (perceptrón multicapa) del documento del proyecto: 6 entradas, una capa oculta de
 * 4 neuronas con ReLU y 5 salidas con softmax. Se entrena con retropropagación del error.
 * Sin dependencias: solo aritmética de arreglos.
 */
public class RedIntencion {

    public static final int ENTRADAS = 6;
    public static final int OCULTAS = 4;
    public static final int SALIDAS = 5;

    private final double[][] w1 = new double[OCULTAS][ENTRADAS];
    private final double[] b1 = new double[OCULTAS];
    private final double[][] w2 = new double[SALIDAS][OCULTAS];
    private final double[] b2 = new double[SALIDAS];

    /** Entrena con descenso de gradiente estocástico (una muestra por paso, orden barajado). */
    public void entrenar(double[][] x, int[] y, int epocas, double tasa, long semilla) {
        Random azar = new Random(semilla);
        for (int j = 0; j < OCULTAS; j++) {
            b1[j] = 0.1;
            for (int k = 0; k < ENTRADAS; k++) {
                w1[j][k] = (azar.nextDouble() - 0.5);
            }
        }
        for (int o = 0; o < SALIDAS; o++) {
            for (int j = 0; j < OCULTAS; j++) {
                w2[o][j] = (azar.nextDouble() - 0.5);
            }
        }
        int[] orden = new int[x.length];
        for (int i = 0; i < orden.length; i++) {
            orden[i] = i;
        }
        for (int e = 0; e < epocas; e++) {
            for (int i = orden.length - 1; i > 0; i--) {
                int r = azar.nextInt(i + 1);
                int t = orden[i];
                orden[i] = orden[r];
                orden[r] = t;
            }
            for (int idx : orden) {
                paso(x[idx], y[idx], tasa);
            }
        }
    }

    private void paso(double[] entrada, int clase, double tasa) {
        double[] z1 = new double[OCULTAS];
        double[] h = new double[OCULTAS];
        for (int j = 0; j < OCULTAS; j++) {
            double s = b1[j];
            for (int k = 0; k < ENTRADAS; k++) {
                s += w1[j][k] * entrada[k];
            }
            z1[j] = s;
            h[j] = Math.max(0, s);
        }
        double[] p = salida(h);
        double[] dh = new double[OCULTAS];
        for (int o = 0; o < SALIDAS; o++) {
            double dz2 = p[o] - (o == clase ? 1.0 : 0.0);
            for (int j = 0; j < OCULTAS; j++) {
                dh[j] += w2[o][j] * dz2;
                w2[o][j] -= tasa * dz2 * h[j];
            }
            b2[o] -= tasa * dz2;
        }
        for (int j = 0; j < OCULTAS; j++) {
            double dz1 = z1[j] > 0 ? dh[j] : 0.0;
            for (int k = 0; k < ENTRADAS; k++) {
                w1[j][k] -= tasa * dz1 * entrada[k];
            }
            b1[j] -= tasa * dz1;
        }
    }

    private double[] salida(double[] h) {
        double[] z = new double[SALIDAS];
        double max = Double.NEGATIVE_INFINITY;
        for (int o = 0; o < SALIDAS; o++) {
            double s = b2[o];
            for (int j = 0; j < OCULTAS; j++) {
                s += w2[o][j] * h[j];
            }
            z[o] = s;
            max = Math.max(max, s);
        }
        double suma = 0;
        for (int o = 0; o < SALIDAS; o++) {
            z[o] = Math.exp(z[o] - max);
            suma += z[o];
        }
        for (int o = 0; o < SALIDAS; o++) {
            z[o] /= suma;
        }
        return z;
    }

    /** Propagación hacia adelante: devuelve cinco probabilidades que suman 1. */
    public double[] predecir(double[] entrada) {
        double[] h = new double[OCULTAS];
        for (int j = 0; j < OCULTAS; j++) {
            double s = b1[j];
            for (int k = 0; k < ENTRADAS; k++) {
                s += w1[j][k] * entrada[k];
            }
            h[j] = Math.max(0, s);
        }
        return salida(h);
    }
}
