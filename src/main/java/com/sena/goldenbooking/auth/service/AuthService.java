package com.sena.goldenbooking.auth.service;


public interface AuthService {
    void logout(String token);   // ← NUEVO
}