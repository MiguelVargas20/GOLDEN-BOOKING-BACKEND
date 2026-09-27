package com.sena.goldenbooking.usuarios.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sena.goldenbooking.usuarios.model.Direccion;
import com.sena.goldenbooking.usuarios.model.Documento;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UsuarioRegistroDto {

    // DATOS PARA PERFIL (Colección Usuario)
    @NotBlank(message = "El nombre es obligatorio.")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio.")
    private String apellido;
    @NotNull(message = "El documento es obligatorio.")
    @Valid
    private Documento documento; // Asegúrate que incluya tipo y numeroD

    @NotBlank(message = "El teléfono es obligatorio.")
    @Pattern(regexp = "^\\+?[0-9 ]{7,15}$", message = "El teléfono debe tener entre 7 y 15 dígitos.")
    private String telefono;

    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "El correo no tiene un formato válido.")
    private String email;
    @NotNull(message = "La dirección es obligatoria.")
    private Direccion direccion; // ciudad y país obligatorios (se valida en el servicio)

    @NotNull(message = "La fecha de nacimiento es obligatoria.")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada.")
    private LocalDate fechaNacimiento;

    // Se ignora lo que mande el cliente: toda cuenta nueva queda ACTIVO (lo fija el servicio)
    private EstadoUsuario estado;

    // DATOS PARA AUTH (Colección UsuarioAuth)
    // Eliminamos 'username' si vamos a usar el 'email' como login
    // O lo dejamos si quieres que el usuario elija un apodo (ej: "juanito123")
    // username sin @NotBlank permitía que dos registros con username=null
    // chocaran entre sí en authRepo.existsByUser(null), igual que pasó con
    // documento.numeroD. Lo obligamos aquí, antes de que llegue al service.
    @NotBlank(message = "El nombre de usuario es obligatorio.")
    @Size(min = 4, max = 20, message = "El nombre de usuario debe tener entre 4 y 20 caracteres.")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // NOTA DE SEGURIDAD: este DTO ya NO tiene un campo "roles".
    // El registro público (/api/usuarios/registro) SIEMPRE asigna ROL_CLIENTE
    // desde UsuarioServiceImpl — nunca debe depender de lo que mande el cliente.
    // Si en el futuro se necesita crear administradores, debe ser un endpoint
    // aparte protegido con hasAuthority("ROL_ADMIN"), nunca este.
}