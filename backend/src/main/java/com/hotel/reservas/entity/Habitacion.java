package com.hotel.reservas.entity;

import com.hotel.reservas.enums.EstadoHabitacion;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Habitacion")
@Getter
@Setter
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idHabitacion;

    private String numero;

    private Integer piso;

    @Enumerated(EnumType.STRING)
    private EstadoHabitacion estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idTipoHabitacion")
    private TipoHabitacion tipoHabitacion;
}


