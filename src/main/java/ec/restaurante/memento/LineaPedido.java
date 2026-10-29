package ec.restaurante.memento;

import java.util.Objects;

public final class LineaPedido {
    private final Plato plato;
    private final int cantidad;

    public LineaPedido(Plato plato, int cantidad) {
        this.plato = Objects.requireNonNull(plato, "El plato es obligatorio");
        validarCantidad(cantidad);
        this.cantidad = cantidad;
    }

    public Plato getPlato() {
        return plato;
    }

    public int getCantidad() {
        return cantidad;
    }

    public LineaPedido conCantidad(int cantidad) {
        return new LineaPedido(plato, cantidad);
    }

    public double calcularSubtotal() {
        return plato.getPrecio() * cantidad;
    }

    private static void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
    }
}