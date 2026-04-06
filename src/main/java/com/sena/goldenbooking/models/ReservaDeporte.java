    package com.sena.goldenbooking.models;
    
    import lombok.Data;

    @Data

    public class ReservaDeporte {
        
        private String id;

        private String tCancha; // Tenis, Futbol, Squash

        private String impleAlqul; // raquetas, balones

        private boolean rquireEntrenador;

    }
