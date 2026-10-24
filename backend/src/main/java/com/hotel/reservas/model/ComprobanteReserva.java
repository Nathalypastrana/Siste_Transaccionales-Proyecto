package com.hotel.reservas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Mapea la tabla Comprobante. */
@Entity
@Table(name = "Comprobante")
@Getter
@Setter
@NoArgsConstructor
public class ComprobanteReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idComprobante")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idReserva", nullable = false, unique = true)
    private Reserva reserva;

    @Column(name = "fechaGeneracion", nullable = false)
    private LocalDateTime fechaGeneracion = LocalDateTime.now();

    @Column(name = "numeroComprobante", nullable = false, length = 30, unique = true)
    private String numeroComprobante;
}
