package com.hotel.reservas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Reserva")
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idReserva")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idHuesped", nullable = false)
    private Huesped huesped;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idHabitacion", nullable = false)
    private Habitacion habitacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idTarifa", nullable = false)
    private Tarifa tarifa;

    @Column(name = "fechaEntrada", nullable = false)
    private LocalDate fechaEntrada;

    @Column(name = "fechaSalida", nullable = false)
    private LocalDate fechaSalida;

    @Column(name = "fechaReserva", nullable = false)
    private LocalDateTime fechaReserva = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @Column(name = "valorTotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;
}
