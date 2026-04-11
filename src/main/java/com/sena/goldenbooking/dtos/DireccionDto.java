package com.sena.goldenbooking.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class DireccionDto {

    private String calle;
    private String carrera;
    private String ciudad;
    private String pais;


}
