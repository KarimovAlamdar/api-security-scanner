import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Tiny INTENTIONALLY VULNERABLE demo server for trying out the API Security Scanner.
 * It only returns canned responses (no database, no real data) and listens on 127.0.0.1 only.
 *
 * Run:  java VulnerableDemoServer.java        (optional argument: port, default 8080)
 */
public class VulnerableDemoServer {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", VulnerableDemoServer::handle);
        server.start();
        System.out.println("Vulnerable demo server running on http://127.0.0.1:" + port + "  (Ctrl+C to stop)");
    }

    private static void handle(HttpExchange ex) throws IOException {
        try {
            route(ex);
        } catch (Exception e) {
            send(ex, 500, "Internal Server Error");
        }
    }

    private static void route(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();     // already URL-decoded
        String query = ex.getRequestURI().getQuery();   // already URL-decoded
        String origin = ex.getRequestHeaders().getFirst("Origin");
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        boolean loggedIn = auth != null && !auth.isBlank();   // any token is accepted
        boolean quote = (query != null && query.contains("'")) || body.contains("'");

        // BUG 1: /products allows every origin (CORS wildcard)
        if (path.equals("/products")) {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        }
        // BUG 2: /users reflects any origin and allows credentials
        if (path.equals("/users") && origin != null) {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
            ex.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        }
        if (method.equals("OPTIONS")) {
            send(ex, 204, "");
            return;
        }

        if (path.equals("/health")) {
            send(ex, 200, "ok");

        } else if (path.equals("/products")) {
            // BUG 3: unexpected input -> bare 500
            send(ex, quote ? 500 : 200, quote ? "Internal Server Error" : "[]");

        } else if (path.equals("/users")) {
            if (method.equals("GET")) {
                // BUG 4: no authentication check + SQL error leak
                if (quote) {
                    send(ex, 500, "You have an error in your SQL syntax; check the manual that corresponds to "
                            + "your MySQL server version for the right syntax to use near ''' at line 1");
                } else {
                    send(ex, 200, "[]");
                }
            } else if (!loggedIn) {
                send(ex, 401, "unauthorized");
            } else {
                send(ex, 201, "{}");
            }

        } else if (path.startsWith("/users/")) {
            String id = path.substring("/users/".length());
            if (method.equals("DELETE")) {
                // BUG 5: DELETE without any authentication check
                send(ex, 204, "");
            } else if (!loggedIn) {
                send(ex, 401, "unauthorized");
            } else if (method.equals("GET") && !id.matches("\\d+")) {
                // BUG 6: stack trace leak
                send(ex, 500, "java.lang.NumberFormatException: For input string: \"" + id + "\"\n"
                        + "\tat java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:67)\n"
                        + "\tat com.demo.UserController.getUser(UserController.java:27)");
            } else {
                send(ex, 200, "{\"id\":\"" + id.replace("\"", "") + "\"}");
            }

        } else if (path.equals("/orders")) {
            if (!loggedIn) {
                send(ex, 401, "unauthorized");
            } else if (body.contains("'")) {
                // BUG 7: stack trace leak from the JSON body
                send(ex, 500, "java.lang.IllegalStateException: bad order\n"
                        + "\tat com.demo.OrderService.create(OrderService.java:42)");
            } else {
                send(ex, 201, "{}");
            }

        } else {
            send(ex, 404, "not found");
        }
    }

    private static void send(HttpExchange ex, int status, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        if (data.length == 0) {
            ex.sendResponseHeaders(status, -1);
        } else {
            ex.sendResponseHeaders(status, data.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(data);
            }
        }
        ex.close();
    }
}
