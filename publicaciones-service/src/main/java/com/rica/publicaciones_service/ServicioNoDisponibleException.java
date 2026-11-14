package com.rica.publicaciones_service;

public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
