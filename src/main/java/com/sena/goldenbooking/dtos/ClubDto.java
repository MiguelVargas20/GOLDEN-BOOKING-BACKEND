package com.sena.goldenbooking.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class ClubDto {
    private String id;
 
    /** Razón social / nombre legal del club */
    private String razonSocial;
 
    /** NIT del club */
    private String nitClub;
 
    /** Teléfono del club */
    private String telefono;
 
    /** Correo institucional */
    private String email;
 
    /** Dirección física */
    private String direccion;
 
    /** Descripción general del club */
    private String descripcion;
  

}
