package com.sena.goldenbooking.services;

import org.springframework.stereotype.Service;

import com.sena.goldenbooking.mapper.UsuarioMapper;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;
import com.sena.goldenbooking.repositories.UsuarioRepository;

@Service
public interface UsuarioServiceImpl implements UsuarioService{
    
    private final UsuarioRepository userRepo;

    private final UsuarioAuthRepository authRepo;

    private final UsuarioMapper userMapper;

    private final PasswordEnconder passwordEndcoder;

    public UsuarioServiceImpl(UsuarioRepository userRepo, UsuarioAuthRepository authRepo, UsuarioMapper userMapper, PasswordEncoder passwordEndcoder)

}