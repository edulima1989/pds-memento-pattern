package ec.restaurante.memento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class MementoPedido {
    private final List<LineaPedido> lineas;
    private final String notas;
    private final String descripcion;
    private final LocalDateTime fechaHora;

    MementoPedido(List<LineaPedido> lineas, String notas, String descripcion, LocalDateTime fechaHora) {
        this.lineas = List.copyOf(lineas);
        this.notas = Objects.requireNonNull(notas, "Las notas son obligatorias");
        this.descripcion = Objects.requireNonNull(descripcion, "La descripción es obligatoria");
        this.fechaHora = Objects.requireNonNull(fechaHora, "La fecha es obligatoria");
    }

    public List<LineaPedido> getLineas() {
        return lineas;
    }

    public String getNotas() {
        return notas;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}