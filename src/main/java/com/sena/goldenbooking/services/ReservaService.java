package com.sena.goldenbooking.services;

import java.util.List;
import com.sena.goldenbooking.dtos.ReservaDto;

public interface ReservaService {

    ReservaDto crearReserva(ReservaDto dto);

    List<ReservaDto> listarReservas();

    ReservaDto obtenerPorId(String id);

    List<ReservaDto> obtenerPorUsuario(String docUsuario);

    ReservaDto actualizarReserva(String id, ReservaDto dto);

    void cancelarReserva(String id);
}