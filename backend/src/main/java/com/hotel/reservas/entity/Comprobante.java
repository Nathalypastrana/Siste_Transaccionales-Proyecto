package com.hotel.reservas.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Comprobante")
@Getter
@Setter
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idComprobante;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idReserva", unique = true)
    private Reserva reserva;

    private String numeroComprobante;

    private LocalDateTime fechaGeneracion;
}
