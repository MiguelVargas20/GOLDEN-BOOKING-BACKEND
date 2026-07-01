package com.sena.goldenbooking.services;

import java.util.List;
import java.util.Map;
import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.dtos.UsuarioRegistroDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    /** * Operación Maestra: Registra en UsuarioPerfil y UsuarioAuth con el mismo ID.
     */
    UsuarioRegistroDto registrarUsuario(UsuarioRegistroDto registroDto);

    /** * Obtiene todos los usuarios del sistema.
     */
    List<UsuarioDto> listarUsuarios();

    /** * Busca un usuario por su ID de MongoDB.
     */
    UsuarioDto obtenerPorId(String id);

    /** * Busca un usuario por su número de documento (Usando el Query de UsuarioRepository).
     */
    UsuarioDto obtenerPorDocNum(String docnum);

    /** * Actualiza datos de perfil (nombre, dirección, etc.). No toca credenciales.
     */
    UsuarioDto actualizarUsuario(String id, UsuarioDto usuarioDto);

    /** * Elimina el perfil del usuario. 
     * Nota: En el Impl deberás decidir si también eliminas su Auth.
     */
    void eliminarUsuario(String id);

    /**
     * Valida si existe un usuario por su documento antes de crear otro.
     */
    boolean existePorDocumento(String docnum);

    // Paginación
    Page<UsuarioDto> listarUsuariosPaginados(Pageable pageable);

    // Actualiza el perfil del usuario con los campos proporcionados en el mapa.
    UsuarioDto actualizarPerfil(String id, Map<String, String> campos);

    /**
     * Resuelve el número de documento del usuario a partir de su username
     * (el "subject" que viaja en el JWT). Se usa para validar que un
     * usuario solo pueda operar sobre sus propias reservas.
     */
    String obtenerDocumentoPorUsername(String username);
}