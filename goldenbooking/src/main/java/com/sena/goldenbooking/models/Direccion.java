package com.sena.goldenbooking.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class Direccion {
    private String calle;
    private String carrera;
    private String ciudad;
    private String pais;
}