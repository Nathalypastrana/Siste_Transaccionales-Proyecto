package com.hotel.reservas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Tarifa")
@Getter
@Setter
@NoArgsConstructor
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTarifa")
    private Integer id;

    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "fechaInicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fechaFin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idTipoHabitacion", nullable = false)
    private TipoHabitacion tipoHabitacion;
}
