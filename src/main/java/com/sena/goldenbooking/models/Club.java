package com.sena.goldenbooking.models;



import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "Club")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data


public class Club {
 
    @Id

    private String id;
 
    /** Razón social / nombre legal del club */
    private String rznSocial;
 
    /** NIT del club */
    private String nit;
 
    /** Teléfono del club */
    private String tel;
 
    /** Correo institucional */
    private String correo;
 
    /** Dirección física */
    private String dir;
 
    /** Descripción general del club */
    private String desc;
  
}
 





