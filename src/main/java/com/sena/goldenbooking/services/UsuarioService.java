package com.sena.goldenbooking.services;

import java.util.List;
import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.dtos.UsuarioRegistroDto;

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
}