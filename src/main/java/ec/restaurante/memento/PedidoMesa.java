package ec.restaurante.memento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PedidoMesa {
    private final int numeroMesa;
    private List<LineaPedido> lineas;
    private String notas;

    public PedidoMesa(int numeroMesa) {
        if (numeroMesa <= 0) {
            throw new IllegalArgumentException("El número de mesa debe ser mayor que cero");
        }
        this.numeroMesa = numeroMesa;
        this.lineas = new ArrayList<>();
        this.notas = "";
    }

    public void agregarPlato(Plato plato, int cantidad) {
        Objects.requireNonNull(plato, "El plato es obligatorio");
        validarCantidad(cantidad);

        for (int indice = 0; indice < lineas.size(); indice++) {
            LineaPedido linea = lineas.get(indice);
            if (linea.getPlato().getNombre().equals(plato.getNombre())) {
                lineas.set(indice, linea.conCantidad(Math.addExact(linea.getCantidad(), cantidad)));
                return;
            }
        }
        lineas.add(new LineaPedido(plato, cantidad));
    }

    public void quitarPlato(String nombrePlato) {
        Objects.requireNonNull(nombrePlato, "El nombre del plato es obligatorio");
        lineas.removeIf(linea -> linea.getPlato().getNombre().equals(nombrePlato));
    }

    public void cambiarCantidad(String nombrePlato, int cantidad) {
        Objects.requireNonNull(nombrePlato, "El nombre del plato es obligatorio");
        validarCantidad(cantidad);

        for (int indice = 0; indice < lineas.size(); indice++) {
            LineaPedido linea = lineas.get(indice);
            if (linea.getPlato().getNombre().equals(nombrePlato)) {
                lineas.set(indice, linea.conCantidad(cantidad));
                return;
            }
        }
    }

    public void cambiarNotas(String notas) {
        this.notas = Objects.requireNonNull(notas, "Las notas son obligatorias");
    }

    public double calcularTotal() {
        return lineas.stream().mapToDouble(LineaPedido::calcularSubtotal).sum();
    }

    public MementoPedido guardar(String descripcion) {
        return new MementoPedido(lineas, notas, descripcion, LocalDateTime.now());
    }

    public void restaurar(MementoPedido memento) {
        Objects.requireNonNull(memento, "El memento es obligatorio");
        this.lineas = new ArrayList<>(memento.getLineas());
        this.notas = memento.getNotas();
    }

    public int getNumeroMesa() {
        return numeroMesa;
    }

    private static void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
    }
}