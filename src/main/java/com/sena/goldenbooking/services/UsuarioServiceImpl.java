package com.sena.goldenbooking.services;

import com.sena.goldenbooking.config.PasswordEncoder;
import com.sena.goldenbooking.dtos.UsuarioDto;

import org.springframework.stereotype.Service;

import com.sena.goldenbooking.mapper.UsuarioMapper;
import com.sena.goldenbooking.models.Usuario;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;
import com.sena.goldenbooking.repositories.UsuarioRepository;

@Service
public interface UsuarioServiceImpl implements UsuarioService{
    
    private final UsuarioRepository userRepo;

    private final UsuarioAuthRepository authRepo;

    private final UsuarioMapper userMapper;

    private final PasswordEndcoder passwordEndcoder;

    public UsuarioServiceImpl(UsuarioRepository userRepo, UsuarioAuthRepository authRepo, UsuarioMapper userMapper, PasswordEncoder passwordEndcoder, PasswordEncoder passwordEncoder){
        this.userRepo = userRepo;
        this.authRepo = authRepo;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    //Crear un nuevo usuario
    @Override
    public UsuarioDto crearUsuario(UsuarioDto usuarioDto){
        Usuario usuario = userMapper.toUsuario(usuarioDto);
        return userMapper.toDto(userRepo.save(usuario));
    }

    //Lista todos los usuarios
    @Override
    public List<UsuarioDto> ListUsuarios(){
        return.userMapper.toDtoList(userRepo.findAll());
    }

    //Actualizar un usuario existente
    @Override
    public UsuarioDto update(String DocNum, UsuarioDto usuarioDto) {
        Usuario usuario = userRepo.findByDocNum(DocNum)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado con Numero de Documento:" + DocNum));
        userMapper.updateUsuario(usuarioDto, usuario);
        return userMapper.toDto(userRepo.save(usuario));
    }

}
