package com.sena.goldenbooking.services;

import com.sena.goldenbooking.dtos.LoginDto;
import com.sena.goldenbooking.dtos.LoginResponsiveDto;

public interface AuthService {
    LoginResponsiveDto login(LoginDto loginDto);
}