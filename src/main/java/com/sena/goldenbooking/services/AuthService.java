package com.sena.goldenbooking.services;


public interface AuthService {
    void logout(String token);   // ← NUEVO
}