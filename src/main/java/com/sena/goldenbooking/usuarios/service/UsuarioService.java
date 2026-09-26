package com.sena.goldenbooking.usuarios.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.dto.UsuarioRegistroDto;

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
    /**
     * Edición completa por el ADMIN (null en un campo = no cambiarlo).
     * @param usernameSolicitante admin que edita: no puede quitarse su propio
     *        rol de administrador ni desactivarse a sí mismo.
     */
    UsuarioDto actualizarUsuario(String id, UsuarioDto usuarioDto, String usernameSolicitante);

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

    /**
     * Resuelve el ID de Mongo (el mismo que usan UsuarioPerfil y UsuarioAuth)
     * del usuario autenticado a partir de su username (subject del JWT).
     * Se usa para validar que un usuario solo pueda ver/editar SU PROPIO
     * perfil en /api/usuarios/perfil/{id} (protección IDOR).
     */
    String obtenerIdPorUsername(String username);

    /**
     * Resuelve varios usuarios por su número de documento en una sola consulta
     * a Mongo (fix hallazgo #11 — N+1 en RecordatorioService), devueltos como
     * mapa docNum -> UsuarioDto para que el llamador haga lookups en memoria
     * en vez de una consulta por cada reserva dentro de un forEach.
     */
    Map<String, UsuarioDto> obtenerMapaPorDocNums(List<String> docNums);
}