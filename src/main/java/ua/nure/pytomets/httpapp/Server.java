package ua.nure.pytomets.httpapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Consumer;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class Server {

    public static void main(String[] args) throws Exception {
        try (var serverSocket = new ServerSocket(8080)) {
            while (true) {
                try (var client = serverSocket.accept()) {
                    handlePostScript(client);
                }
            }
        }
    }

    private static void handlePostScript(Socket socket) throws IOException, InterruptedException {
        var br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        var requestBuilder = new StringBuilder();
        String line;
        while (!(line = br.readLine()).isBlank()) {
            requestBuilder.append(line + "\r\n");
        }

        var request = requestBuilder.toString();
        var requestsLines = request.split("\r\n");
        var requestLine = requestsLines[0].split(" ");
        var method = requestLine[0];
        var path = requestLine[1].split("[?]")[0];
        var name = requestLine[1].split("[?]")[1].replace("name=", "");
        var version = requestLine[2];
        var host = requestsLines[1].split(" ")[1];

        var headers = new ArrayList<>();
        for (int h = 2; h < requestsLines.length; h++) {
            var header = requestsLines[h];
            headers.add(header);
        }

        var accessLog = format("Client %s, method %s, path %s, name %s, version %s, host %s, headers %s",
                socket.toString(), method, path, name, version, host, headers.toString());
        System.out.println(accessLog);


        var process = getRuntime().exec(format("sh .%s %s", path, name));
        var processor = new StreamProcessor(process.getInputStream(), System.out::println);
        newSingleThreadExecutor().submit(processor);
        int exitCode = process.waitFor();
        assert exitCode == 0;


        var filePath = getFilePath("./index.html");
        if (Files.exists(filePath)) {
            System.out.println("file exists");
            var contentType = guessContentType(filePath);
            sendResponse(socket, "200 OK", contentType, Files.readAllBytes(filePath));
        } else {
            byte[] notFoundContent = "<h1>Not found :(-</h1>".getBytes();
            sendResponse(socket, "404 Not Found", "text/html", notFoundContent);
        }

    }

    private static void sendResponse(Socket client, String status, String contentType, byte[] content) throws IOException {
        var clientOutput = client.getOutputStream();
        clientOutput.write(("HTTP/1.1 \r\n" + status).getBytes());
        clientOutput.write(("ContentType: " + contentType + "\r\n").getBytes());
        clientOutput.write("\r\n".getBytes());
        clientOutput.write(content);
        clientOutput.write("\r\n\r\n".getBytes());
        clientOutput.flush();
        client.close();
    }

    private static Path getFilePath(String path) {
        return Paths.get(path);
    }

    private static String guessContentType(Path filePath) throws IOException {
        return Files.probeContentType(filePath);
    }

    private static class StreamProcessor implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamProcessor(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}
