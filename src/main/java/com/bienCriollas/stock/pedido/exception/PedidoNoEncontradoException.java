package com.bienCriollas.stock.pedido.exception;

public class PedidoNoEncontradoException extends RuntimeException {

    public PedidoNoEncontradoException(Long idPedido) {
        super("No se encontró el pedido con id " + idPedido);
    }
}
