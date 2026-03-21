package com.sena.goldenbooking.services;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.dtos.UsuarioRegistroDto;
import com.sena.goldenbooking.mapper.UsuarioMapper;
import com.sena.goldenbooking.models.Usuario;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;
import com.sena.goldenbooking.repositories.UsuarioRepository;

@Service
public class UsuarioServiceImpl implements UsuarioService{

    private final UsuarioRepository userRepo;
    private final UsuarioAuthRepository authRepo;
    private final UsuarioMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository userRepo, UsuarioAuthRepository authRepo, UsuarioMapper userMapper, PasswordEncoder passwordEncoder){
        this.userRepo = userRepo;
        this.authRepo = authRepo;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UsuarioDto create(UsuarioDto usuarioDto){
        Usuario usuario = userMapper.toUsuario(usuarioDto);
        return userMapper.toDto(userRepo.save(usuario));
    }

    @Override
    public List<UsuarioDto> ListUsuarios(){
        return userMapper.toDtoList(userRepo.findAll());
    }

    @Override
    public UsuarioDto update(String id, UsuarioDto usuarioDto){
        Usuario usuario = userRepo.findById(id)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id:" + id));
        userMapper.updateUsuario(usuarioDto, usuario);
        return userMapper.toDto(userRepo.save(usuario));
    }

    @Override
    public void delete(String id) {
        if (!userRepo.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado con id:" + id);
        }
        userRepo.deleteById(id);
    }

    @Override
    public UsuarioDto UsuarioByDocNum(String docnum) {
        Usuario usuario = userRepo.findByDocNum(docnum)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado con numero de documento:" + docnum));
        return userMapper.toDto(usuario);
    }

    @Override
    public UsuarioDto UsuarioByDocum(String docnum) {
        return userRepo.findByDocNum(docnum)
        .map(userMapper::toDto)
        .orElseThrow(() -> new RuntimeException(
            "Usuario no encontrado con el documento:" + docnum
        ));
    }

    @Transactional
    @Override
    public UsuarioRegistroDto registrarUsuario(UsuarioRegistroDto dto){
        Usuario perfil = Usuario.builder()
        .id(UUID.randomUUID().toString())
        .nom(dto.getNombre())
        .ape(dto.getApellido())
        .doc(dto.getDocumento())
        .email(dto.getEmail())
        .build();
    }
}