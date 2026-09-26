package com.sena.goldenbooking.reservas.service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.reservas.model.Reserva;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;

import lombok.extern.slf4j.Slf4j;

/**
 * Las reservas se vinculan al cliente por su NÚMERO DE DOCUMENTO. Si el admin
 * corrige el documento de un usuario, sus reservas deben pasar al número
 * nuevo; si no, el cliente dejaría de verlas en "Mis reservas".
 */
@Slf4j
@Service
public class ReservasPorDocumentoService {

    private final MongoTemplate mongoTemplate;

    public ReservasPorDocumentoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** @return cuántos documentos de reserva se actualizaron en total */
    public long trasladarDocumento(String documentoAnterior, String documentoNuevo) {
        long deporte = mongoTemplate.updateMulti(
                Query.query(Criteria.where("docUsuario").is(documentoAnterior)),
                Update.update("docUsuario", documentoNuevo), ReservaDeporte.class).getModifiedCount();
        long hotel = mongoTemplate.updateMulti(
                Query.query(Criteria.where("docUsuario").is(documentoAnterior)),
                Update.update("docUsuario", documentoNuevo), ReservaHotel.class).getModifiedCount();
        long padres = mongoTemplate.updateMulti(
                Query.query(Criteria.where("documentoUsuario").is(documentoAnterior)),
                Update.update("documentoUsuario", documentoNuevo), Reserva.class).getModifiedCount();
        log.info("Documento {} → {}: {} reservas deportivas, {} hoteleras y {} registros padre actualizados.",
                documentoAnterior, documentoNuevo, deporte, hotel, padres);
        return deporte + hotel + padres;
    }
}
