package com.arqui.webserver;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.cloud.StorageClient;
import com.google.gson.Gson;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class Webserve{
    
    private static final int PORT = 3000;
    private static final String STATIC_FILES_PATH = "src/main/resources/public";
    private static final Gson gson = new Gson();
    private static Map<String, Object> ultimoUsuarioLogado = new HashMap<>();

    public static void main(String[] args) throws Exception {
        FirebaseInitializer.init();
        
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        server.createContext("/", exchange -> serveStaticFiles(exchange));
        server.createContext("/api/gravar", exchange -> cadastrarUsuario(exchange));
        server.createContext("/api/gravar_funcionario", exchange -> cadastrarFuncionario(exchange));
        server.createContext("/api/login", exchange -> login(exchange));
        server.createContext("/api/login_admin", exchange -> loginAdmin(exchange));
        server.createContext("/api/user", exchange -> getUserInfo(exchange));
        server.createContext("/api/upload_foto", exchange -> uploadFotoPerfil(exchange));
        server.createContext("/api/pix/enviar", exchange -> enviarPix(exchange));
        server.createContext("/api/transacoes", exchange -> getTransacoes(exchange));
      server.createContext("/api/extrato/pdf", exchange -> gerarExtratoPDF(exchange));
        server.setExecutor(null);
        server.start();
        
        System.out.println("🌐 Servidor iniciado: http://localhost:" + PORT);
    }

    private static void serveStaticFiles(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        
        String urlPath = exchange.getRequestURI().getPath();
        if (urlPath.equals("/")) urlPath = "/index.html";
        
        Path filePath = Paths.get(STATIC_FILES_PATH + urlPath);
        
        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            String contentType = getContentType(filePath.toString());
            byte[] content = Files.readAllBytes(filePath);
            
            exchange.getResponseHeaders().add("Content-Type", contentType);
            sendResponse(exchange, 200, content);
        } else {
            sendResponse(exchange, 404, "404 - Arquivo não encontrado");
        }
    }

    private static void cadastrarUsuario(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        
        try {
            Map<String, String> formData = getFormData(exchange);
            Map<String, Object> dadosUsuario = criarDadosUsuario(formData);
            
            Firestore db = FirebaseInitializer.getDb();
            
            if (cpfJaExiste(db, "Empresa", formData.get("username")) || 
                cpfJaExiste(db, "Funcionario", formData.get("username"))) {
                String errorHtml = "<script>alert('CPF já cadastrado!'); window.location.href='/Cadastro.html';</script>";
                sendHtmlResponse(exchange, 409, errorHtml);
                return;
            }
            
            ApiFuture<DocumentReference> future = db.collection("Empresa").add(dadosUsuario);
            future.get();
            
            criarInformacaoUsuario(db, formData.get("username"));
            
            String successHtml = "<script>alert('Cadastro realizado com sucesso!'); window.location.href='/Principal_usuario.html';</script>";
            sendHtmlResponse(exchange, 200, successHtml);
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorHtml = "<script>alert('Erro ao cadastrar: " + e.getMessage() + "'); window.history.back();</script>";
            sendHtmlResponse(exchange, 500, errorHtml);
        }
    }

    private static void cadastrarFuncionario(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        
        try {
            Map<String, String> formData = getFormData(exchange);
            Map<String, Object> dadosFuncionario = criarDadosUsuario(formData);
            
            Firestore db = FirebaseInitializer.getDb();
            
            if (cpfJaExiste(db, "Empresa", formData.get("username")) || 
                cpfJaExiste(db, "Funcionario", formData.get("username"))) {
                String errorHtml = "<script>alert('CPF já cadastrado!'); window.location.href='/Cadastro_ADM.html';</script>";
                sendHtmlResponse(exchange, 409, errorHtml);
                return;
            }
            
            ApiFuture<DocumentReference> future = db.collection("Funcionario").add(dadosFuncionario);
            future.get();
            
            criarInformacaoUsuario(db, formData.get("username"));
            
            String successHtml = "<script>alert('Funcionário cadastrado com sucesso!'); window.location.href='/Principal_adem.html';</script>";
            sendHtmlResponse(exchange, 200, successHtml);
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorHtml = "<script>alert('Erro ao cadastrar: " + e.getMessage() + "'); window.history.back();</script>";
            sendHtmlResponse(exchange, 500, errorHtml);
        }
    }

    private static void login(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        
        try {
            Map<String, String> formData = getFormData(exchange);
            String cpf = formData.get("username");
            String senha = formData.get("password");
            
            Firestore db = FirebaseInitializer.getDb();
            
            ApiFuture<QuerySnapshot> future = db.collection("Empresa")
                    .whereEqualTo("cpf", cpf)
                    .whereEqualTo("senha", senha)
                    .get();
            
            QuerySnapshot documents = future.get();
            
            if (!documents.isEmpty()) {
                ultimoUsuarioLogado.clear();
                ultimoUsuarioLogado.putAll(documents.getDocuments().get(0).getData());
                
                String successHtml = "<script>alert('Login realizado com sucesso!'); window.location.href='/Principal_usuario.html';</script>";
                sendHtmlResponse(exchange, 200, successHtml);
            } else {
                String errorHtml = "<script>alert('CPF ou senha inválidos!'); window.history.back();</script>";
                sendHtmlResponse(exchange, 401, errorHtml);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorHtml = "<script>alert('Erro interno no servidor!'); window.history.back();</script>";
            sendHtmlResponse(exchange, 500, errorHtml);
        }
    }
    

    private static void loginAdmin(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        
        try {
            Map<String, String> formData = getFormData(exchange);
            String cpf = formData.get("username");
            String senha = formData.get("password");
            
            Firestore db = FirebaseInitializer.getDb();
            
            ApiFuture<QuerySnapshot> future = db.collection("Funcionario")
                    .whereEqualTo("cpf", cpf)
                    .whereEqualTo("senha", senha)
                    .get();
            
            QuerySnapshot documents = future.get();
            
            if (!documents.isEmpty()) {
                ultimoUsuarioLogado.clear();
                ultimoUsuarioLogado.putAll(documents.getDocuments().get(0).getData());
                
                String successHtml = "<script>alert('Login realizado com sucesso!'); window.location.href='/Principal_adem.html';</script>";
                sendHtmlResponse(exchange, 200, successHtml);
            } else {
                String errorHtml = "<script>alert('CPF ou senha inválidos!'); window.history.back();</script>";
                sendHtmlResponse(exchange, 401, errorHtml);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorHtml = "<script>alert('Erro interno no servidor!'); window.history.back();</script>";
            sendHtmlResponse(exchange, 500, errorHtml);
        }
    }

    private static void getUserInfo(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        
        try {
            String cpfUsuario = (String) ultimoUsuarioLogado.get("cpf");
            
            if (cpfUsuario == null || cpfUsuario.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("erro", "Usuário não logado");
                sendJsonResponse(exchange, 401, error);
                return;
            }
            
            Firestore db = FirebaseInitializer.getDb();
            
            ApiFuture<QuerySnapshot> futureEmpresa = db.collection("Empresa")
                    .whereEqualTo("cpf", cpfUsuario)
                    .get();
            
            QuerySnapshot snapshotEmpresa = futureEmpresa.get();
            
            Map<String, Object> dados = null;
            
            if (!snapshotEmpresa.isEmpty()) {
                dados = new HashMap<>(snapshotEmpresa.getDocuments().get(0).getData());
            } else {
                ApiFuture<QuerySnapshot> futureFuncionario = db.collection("Funcionario")
                        .whereEqualTo("cpf", cpfUsuario)
                        .get();
                
                QuerySnapshot snapshotFuncionario = futureFuncionario.get();
                
                if (!snapshotFuncionario.isEmpty()) {
                    dados = new HashMap<>(snapshotFuncionario.getDocuments().get(0).getData());
                }
            }
            
            if (dados == null) {
                Map<String, String> error = new HashMap<>();
                error.put("erro", "Usuário não encontrado");
                sendJsonResponse(exchange, 404, error);
                return;
            }
            
            if (!dados.containsKey("dinheiro")) {
                dados.put("dinheiro", 0);
            }
            
            if (!dados.containsKey("fotoPerfil")) {
                dados.put("fotoPerfil", "");
            }
            
            dados.remove("senha");
            
            sendJsonResponse(exchange, 200, dados);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("erro", e.getMessage());
            sendJsonResponse(exchange, 500, error);
        }
    }
private static void gerarExtratoPDF(HttpExchange exchange) throws IOException {
    addCorsHeaders(exchange);
    
    if (!"GET".equals(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(405, -1);
        return;
    }
    
    try {
        String cpfUsuario = (String) ultimoUsuarioLogado.get("cpf");
        
        if (cpfUsuario == null || cpfUsuario.isEmpty()) {
            sendResponse(exchange, 401, "Usuário não logado");
            return;
        }
        
        Firestore db = FirebaseInitializer.getDb();
        
        // Buscar dados do usuário
        ApiFuture<QuerySnapshot> futureUsuario = db.collection("Empresa")
                .whereEqualTo("cpf", cpfUsuario)
                .get();
        
        QuerySnapshot snapshotUsuario = futureUsuario.get();
        
        if (snapshotUsuario.isEmpty()) {
            sendResponse(exchange, 404, "Usuário não encontrado");
            return;
        }
        
        Map<String, Object> dadosUsuario = snapshotUsuario.getDocuments().get(0).getData();
        
        // Buscar transações
        ApiFuture<QuerySnapshot> futureTransacoes = db.collection("transacoes")
                .whereEqualTo("cpf", cpfUsuario)
                .get();
        
        QuerySnapshot snapshotTransacoes = futureTransacoes.get();
        List<QueryDocumentSnapshot> transacoesList = snapshotTransacoes.getDocuments();
        
        // Gerar PDF
        String pdfContent = criarPDFSimples(dadosUsuario, transacoesList);
        byte[] pdfBytes = pdfContent.getBytes(StandardCharsets.UTF_8);
        
        // Enviar como download
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        exchange.getResponseHeaders().add("Content-Disposition", 
            "attachment; filename=extrato_" + cpfUsuario.replace(".", "").replace("-", "") + ".txt");
        
        exchange.sendResponseHeaders(200, pdfBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(pdfBytes);
        }
        
    } catch (Exception e) {
        e.printStackTrace();
        sendResponse(exchange, 500, "Erro ao gerar extrato: " + e.getMessage());
    }
}

    private static void uploadFotoPerfil(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        
        try {
            String cpfUsuario = (String) ultimoUsuarioLogado.get("cpf");
            
            if (cpfUsuario == null || cpfUsuario.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("erro", "Usuário não logado");
                sendJsonResponse(exchange, 401, error);
                return;
            }
            
            byte[] fotoBytes = exchange.getRequestBody().readAllBytes();
            
            String cpfLimpo = cpfUsuario.replace(".", "").replace("-", "");
            String nomeArquivo = "perfil_" + cpfLimpo + "_" + System.currentTimeMillis() + ".jpg";
            
            Storage storage = StorageOptions.getDefaultInstance().getService();
            String bucketName = StorageClient.getInstance().bucket().getName();
            
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, "fotos_perfil/" + nomeArquivo)
                    .setContentType("image/jpeg")
                    .build();
            
            Blob blob = storage.create(blobInfo, fotoBytes);
            
            String fotoUrl = String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                    bucketName, 
                    "fotos_perfil%2F" + nomeArquivo);
            
            Firestore db = FirebaseInitializer.getDb();
            
            ApiFuture<QuerySnapshot> futureEmpresa = db.collection("Empresa")
                    .whereEqualTo("cpf", cpfUsuario)
                    .get();
            
            QuerySnapshot snapshotEmpresa = futureEmpresa.get();
            
            if (!snapshotEmpresa.isEmpty()) {
                String docId = snapshotEmpresa.getDocuments().get(0).getId();
                db.collection("Empresa").document(docId).update("fotoPerfil", fotoUrl);
            } else {
                ApiFuture<QuerySnapshot> futureFuncionario = db.collection("Funcionario")
                        .whereEqualTo("cpf", cpfUsuario)
                        .get();
                
                QuerySnapshot snapshotFuncionario = futureFuncionario.get();
                
                if (!snapshotFuncionario.isEmpty()) {
                    String docId = snapshotFuncionario.getDocuments().get(0).getId();
                    db.collection("Funcionario").document(docId).update("fotoPerfil", fotoUrl);
                }
            }
            
            ultimoUsuarioLogado.put("fotoPerfil", fotoUrl);
            
            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("fotoUrl", fotoUrl);
            sendJsonResponse(exchange, 200, response);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("erro", e.getMessage());
            sendJsonResponse(exchange, 500, error);
        }
    }

    private static void enviarPix(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        
        try {
            String cpfUsuario = (String) ultimoUsuarioLogado.get("cpf");
            
            if (cpfUsuario == null || cpfUsuario.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("erro", "Usuário não logado");
                sendJsonResponse(exchange, 401, error);
                return;
            }
            
            Map<String, String> formData = getFormData(exchange);
            String chavePix = formData.get("chavePix");
            String valorStr = formData.get("valor");
            
            if (chavePix == null || chavePix.isEmpty() || valorStr == null || valorStr.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("erro", "Chave PIX e valor são obrigatórios");
                sendJsonResponse(exchange, 400, error);
                return;
            }
            
            double valor = Double.parseDouble(valorStr.replace(",", "."));
            
            if (valor <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("erro", "Valor deve ser maior que zero");
                sendJsonResponse(exchange, 400, error);
                return;
            }
            
            Firestore db = FirebaseInitializer.getDb();
            
            ApiFuture<QuerySnapshot> futureUsuario = db.collection("Empresa")
                    .whereEqualTo("cpf", cpfUsuario)
                    .get();
            
            QuerySnapshot snapshotUsuario = futureUsuario.get();
            
            if (snapshotUsuario.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("erro", "Usuário não encontrado");
                sendJsonResponse(exchange, 404, error);
                return;
            }
            
            DocumentSnapshot docUsuario = snapshotUsuario.getDocuments().get(0);
            String docId = docUsuario.getId();
            
            Object dinheiroObj = docUsuario.get("dinheiro");
            double saldoAtual = 0;
            
            if (dinheiroObj != null) {
                if (dinheiroObj instanceof Long) {
                    saldoAtual = ((Long) dinheiroObj).doubleValue();
                } else if (dinheiroObj instanceof Double) {
                    saldoAtual = (Double) dinheiroObj;
                } else if (dinheiroObj instanceof Integer) {
                    saldoAtual = ((Integer) dinheiroObj).doubleValue();
                }
            }
            
            if (saldoAtual < valor) {
                Map<String, String> error = new HashMap<>();
                error.put("erro", "Saldo insuficiente");
                error.put("saldoAtual", String.format("%.2f", saldoAtual));
                sendJsonResponse(exchange, 400, error);
                return;
            }
            
            double novoSaldo = saldoAtual - valor;
            db.collection("Empresa").document(docId).update("dinheiro", novoSaldo);
            
            Map<String, Object> transacao = new HashMap<>();
            transacao.put("cpf", cpfUsuario);
            transacao.put("tipo", "PIX_ENVIADO");
            transacao.put("chavePix", chavePix);
            transacao.put("valor", valor);
            transacao.put("data", new java.util.Date().toString());
            transacao.put("saldoAnterior", saldoAtual);
            transacao.put("saldoNovo", novoSaldo);
            
            db.collection("transacoes").add(transacao);
            
            ultimoUsuarioLogado.put("dinheiro", novoSaldo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensagem", "PIX enviado com sucesso!");
            response.put("valor", valor);
            response.put("novoSaldo", novoSaldo);
            response.put("chavePix", chavePix);
            
            sendJsonResponse(exchange, 200, response);
            
        } catch (NumberFormatException e) {
            Map<String, String> error = new HashMap<>();
            error.put("erro", "Valor inválido");
            sendJsonResponse(exchange, 400, error);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("erro", "Erro ao processar PIX: " + e.getMessage());
            sendJsonResponse(exchange, 500, error);
        }
    }

    private static Map<String, Object> criarDadosUsuario(Map<String, String> formData) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", formData.get("nome"));
        dados.put("email", formData.get("email"));
        dados.put("cpf", formData.get("username"));
        dados.put("senha", formData.get("password"));
        dados.put("endereco", formData.get("Endereco"));
        dados.put("cep", formData.get("CEP"));
        dados.put("telefone", formData.get("telefone"));
        dados.put("dataNascimento", formData.get("data_nascimento"));
        dados.put("rendaMensal", formData.getOrDefault("renda_mensal", "0"));
        dados.put("estadoCivil", formData.get("estado_civil"));
        dados.put("profissao", formData.get("profissao"));
        dados.put("empresa", formData.get("empresa"));
        dados.put("cargo", formData.get("cargo"));
        dados.put("tempoEmpresa", formData.get("tempo_empresa"));
        dados.put("telefoneEmpresa", formData.get("telefone_empresa"));
        dados.put("referencia", formData.get("referencia"));
        dados.put("telefoneReferencia", formData.get("telefone_referencia"));
        dados.put("observacoes", formData.get("observacoes"));
        dados.put("termos", formData.getOrDefault("termos", "false"));
        return dados;
    }
    
    private static void criarInformacaoUsuario(Firestore db, String cpf) {
        Map<String, Object> info = new HashMap<>();
        info.put("cpf", cpf);
        info.put("dinheiro", 0);
        info.put("contaVinculada", "");
        
        try {
            db.collection("informacao").add(info).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static boolean cpfJaExiste(Firestore db, String collection, String cpf) {
        try {
            ApiFuture<QuerySnapshot> future = db.collection(collection)
                    .whereEqualTo("cpf", cpf)
                    .get();
            return !future.get().isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private static Map<String, String> getFormData(HttpExchange exchange) throws IOException {
        String requestBody = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines()
                .reduce("", (acc, line) -> acc + line);
        
        return parseFormData(requestBody);
    }
    
    private static Map<String, String> parseFormData(String body) {
        Map<String, String> data = new HashMap<>();
        String[] pairs = body.split("&");
        
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(parts[0], "UTF-8");
                String value = parts.length > 1 ? URLDecoder.decode(parts[1], "UTF-8") : "";
                data.put(key, value);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        
        return data;
    }
    
    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            try {
                exchange.sendResponseHeaders(204, -1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void getTransacoes(HttpExchange exchange) throws IOException {
    addCorsHeaders(exchange);
    
    if (!"GET".equals(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(405, -1);
        return;
    }
    
    try {
        String cpfUsuario = (String) ultimoUsuarioLogado.get("cpf");
        
        if (cpfUsuario == null || cpfUsuario.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("erro", "Usuário não logado");
            sendJsonResponse(exchange, 401, error);
            return;
        }
        
        Firestore db = FirebaseInitializer.getDb();
        
        ApiFuture<QuerySnapshot> future = db.collection("transacoes")
                .whereEqualTo("cpf", cpfUsuario)
                .get();
        
        QuerySnapshot snapshot = future.get();
        
        java.util.List<Map<String, Object>> transacoes = new java.util.ArrayList<>();
        
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Map<String, Object> transacao = new HashMap<>(doc.getData());
            transacao.put("id", doc.getId());
            transacoes.add(transacao);
        }
        
        sendJsonResponse(exchange, 200, transacoes);
        
    } catch (Exception e) {
        e.printStackTrace();
        Map<String, String> error = new HashMap<>();
        error.put("erro", e.getMessage());
        sendJsonResponse(exchange, 500, error);
    }
}
    private static void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] response = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, byte[] content) throws IOException {
        exchange.sendResponseHeaders(statusCode, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }
    
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    private static void sendHtmlResponse(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".woff") || path.endsWith(".woff2")) return "font/woff2";
        if (path.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }

    private static String criarPDFSimples(Map<String, Object> dadosUsuario, List<QueryDocumentSnapshot> transacoesList) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}