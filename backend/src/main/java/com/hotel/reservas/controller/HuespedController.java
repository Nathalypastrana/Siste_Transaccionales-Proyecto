package com.hotel.reservas.controller;

import com.hotel.reservas.dto.HuespedRequest;
import com.hotel.reservas.dto.HuespedResponse;
import com.hotel.reservas.service.HuespedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/huespedes")
@RequiredArgsConstructor
public class HuespedController {

    private final HuespedService huespedService;

    @GetMapping
    public List<HuespedResponse> listar() {
        return huespedService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HuespedResponse crear(@Valid @RequestBody HuespedRequest request) {
        return huespedService.crear(request);
    }
}
