package ec.restaurante.memento;

import java.util.Objects;

public final class Plato {
    private final String nombre;
    private final double precio;
    private final boolean esVegetariano;

    public Plato(String nombre, double precio, boolean esVegetariano) {
        this.nombre = Objects.requireNonNull(nombre, "El nombre es obligatorio").trim();
        if (this.nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (!Double.isFinite(precio) || precio < 0) {
            throw new IllegalArgumentException("El precio debe ser un valor finito no negativo");
        }
        this.precio = precio;
        this.esVegetariano = esVegetariano;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public boolean isVegetariano() {
        return esVegetariano;
    }
}