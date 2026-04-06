package com.sena.goldenbooking.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Direccion {
    private String cll;
    private String crr;
    private String cd;
    private String ps;

}
