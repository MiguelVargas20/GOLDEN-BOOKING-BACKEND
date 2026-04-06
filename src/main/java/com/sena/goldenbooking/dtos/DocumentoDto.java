package com.sena.goldenbooking.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class DocumentoDto {
    
    private String tipoDocumento;
    private String numeroDocumento;
}
