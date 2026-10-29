package ec.restaurante.memento;

import java.io.IOException;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        new WebServer(port).start();
    }
}