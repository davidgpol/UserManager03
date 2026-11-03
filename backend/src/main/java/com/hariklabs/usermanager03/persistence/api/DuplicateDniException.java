package com.hariklabs.usermanager03.persistence.api;

public class DuplicateDniException extends RuntimeException {
    public DuplicateDniException() { super("DNI_UNIQUE_CONFLICT"); }
}
