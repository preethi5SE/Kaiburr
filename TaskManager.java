import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class TaskManager {
    private static final Map<String, JSONObject> tasks = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/tasks", new TaskHandler());
        server.createContext("/tasks/search", new SearchHandler());
        server.createContext("/tasks/execute", new ExecuteHandler());

        server.setExecutor(Executors.newFixedThreadPool(5)); // Multi-threaded execution
        server.start();
        System.out.println("Server started on port 8080...");
    }

    static class TaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if (method.equalsIgnoreCase("GET")) {
                handleGet(exchange);
            } else if (method.equalsIgnoreCase("POST")) {
                handlePost(exchange);
            } else if (method.equalsIgnoreCase("DELETE")) {
                handleDelete(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }

        private void handleGet(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("id=")) {
                String id = query.split("=")[1];
                JSONObject task = tasks.get(id);
                if (task == null) {
                    sendResponse(exchange, 404, "Task Not Found");
                } else {
                    sendResponse(exchange, 200, task.toString());
                }
            } else {
                JSONArray taskArray = new JSONArray(tasks.values());
                sendResponse(exchange, 200, taskArray.toString());
            }
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject task = new JSONObject(requestBody);

            String id = task.optString("id", "").trim();
            if (id.isEmpty() || tasks.containsKey(id)) {
                sendResponse(exchange, 400, "Invalid or duplicate Task ID");
                return;
            }

            task.put("taskExecutions", new JSONArray()); // Initialize executions list
            tasks.put(id, task);
            sendResponse(exchange, 201, "Task Created: " + task.getString("name"));
        }

        private void handleDelete(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("id=")) {
                String id = query.split("=")[1];
                if (tasks.remove(id) != null) {
                    sendResponse(exchange, 200, "Task Deleted");
                } else {
                    sendResponse(exchange, 404, "Task Not Found");
                }
            } else {
                sendResponse(exchange, 400, "Invalid request: Provide task ID");
            }
        }
    }

    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.startsWith("name=")) {
                sendResponse(exchange, 400, "Missing 'name' parameter");
                return;
            }

            String searchName = query.split("=")[1].toLowerCase();
            List<JSONObject> filteredTasks = tasks.values().stream()
                    .filter(task -> task.getString("name").toLowerCase().contains(searchName))
                    .toList();

            if (filteredTasks.isEmpty()) {
                sendResponse(exchange, 404, "No tasks found");
            } else {
                sendResponse(exchange, 200, new JSONArray(filteredTasks).toString());
            }
        }
    }

    static class ExecuteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.startsWith("id=")) {
                sendResponse(exchange, 400, "Missing 'id' parameter");
                return;
            }

            String id = query.split("=")[1];
            JSONObject task = tasks.get(id);
            if (task == null) {
                sendResponse(exchange, 404, "Task Not Found");
                return;
            }

            String command = task.optString("command", "").trim();
            if (command.isEmpty()) {
                sendResponse(exchange, 400, "Invalid Task: Missing Command");
                return;
            }

            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    processBuilder.command("cmd.exe", "/c", command);
                } else {
                    processBuilder.command("bash", "-c", command);
                }

                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder outputBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                }

                int exitCode = process.waitFor();
                String output = outputBuilder.toString().trim();

                JSONObject execution = new JSONObject();
                execution.put("startTime", new Date().toString());
                execution.put("endTime", new Date().toString());
                execution.put("exitCode", exitCode);
                execution.put("output", output.isEmpty() ? "No Output" : output);

                synchronized (tasks) {
                    task.getJSONArray("taskExecutions").put(execution);
                }

                sendResponse(exchange, 200, "Task Executed Successfully: " + output);
            } catch (Exception e) {
                sendResponse(exchange, 500, "Execution Failed: " + e.getMessage());
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
