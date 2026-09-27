package com.sena.goldenbooking.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

class OrigenesPermitidosTest {

    @Test
    void separaPorComaYQuitaEspaciosVaciosYBarraFinal() {
        assertThat(OrigenesPermitidos.parsear(" https://a.vercel.app/ , ,http://localhost:5173"))
                .containsExactly("https://a.vercel.app", "http://localhost:5173");
        assertThat(OrigenesPermitidos.parsear(null)).isEmpty();
    }

    @Test
    void conComodinAceptaLosPreviewsDeVercelYRechazaOtrosDominios() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(OrigenesPermitidos.parsear(
                "https://goldenbooking.vercel.app,https://goldenbooking-*.vercel.app"));

        assertThat(config.checkOrigin("https://goldenbooking.vercel.app")).isNotNull();
        assertThat(config.checkOrigin("https://goldenbooking-git-test-miguel.vercel.app")).isNotNull();
        assertThat(config.checkOrigin("https://otro-sitio.vercel.app")).isNull();
        assertThat(config.checkOrigin("https://evil.com")).isNull();
    }
}
