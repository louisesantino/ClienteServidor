import java.io.*;
import java.net.*;
import java.util.*;

// Classe principal do servidor
public class Servidor {
    // Definição da porta e diretório de armazenamento
    private static final int PORT = 12346;
    private static final String STORAGE_ROOT = "armazenamento"; // Raiz do diretório para armazenar arquivos
    private static final Map<String, String> users = new HashMap<>(); // Mapa de usuários e senhas

    // Inicialização de usuários pré-cadastrados
    static {
        users.put("louise", "123");
        users.put("gabriel", "123");
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado na porta " + PORT);

            // Loop infinito para aceitar conexões dos clientes
            while (true) {
                // Aguardar a conexão de um cliente
                Socket clientSocket = serverSocket.accept();
                // Criar um novo thread para lidar com o cliente
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    // Classe para lidar com as requisições do cliente
    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private String currentUser; // Usuário autenticado
        private DataInputStream dis; // Entrada de dados do cliente
        private DataOutputStream dos; // Saída de dados para o cliente

        // Construtor que recebe o socket do cliente
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        // Método que gerencia a comunicação com o cliente
        public void run() {
            try {
                dis = new DataInputStream(clientSocket.getInputStream());
                dos = new DataOutputStream(clientSocket.getOutputStream());

                // Autenticação do usuário
                if (!authenticate()) {
                    return; // Se falhar na autenticação, encerra a conexão
                }

                // Cria as pastas do usuário no sistema de arquivos
                createUserFolders();

                // Menu principal com opções para o cliente
                while (true) {
                    int option = dis.readInt();
                    switch (option) {
                        case 1: // Listar arquivos
                            listFiles();
                            break;
                        case 2: // Download
                            downloadFile();
                            break;
                        case 3: // Upload
                            uploadFile();
                            break;
                        case 4: // Sair
                            return;
                        default:
                            dos.writeUTF("Opção inválida");
                    }
                }
            } catch (IOException e) {
                System.err.println("Erro no handler: " + e.getMessage());
            } finally {
                // Garantir o fechamento do socket
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Erro ao fechar socket: " + e.getMessage());
                }
            }
        }

        // Método que realiza a autenticação do usuário
        private boolean authenticate() throws IOException {
            dos.writeUTF("Digite seu nome de usuário:");
            String username = dis.readUTF();
            dos.writeUTF("Digite sua senha:");
            String password = dis.readUTF();

            // Verifica se o usuário e a senha são válidos
            if (users.containsKey(username) && users.get(username).equals(password)) {
                currentUser = username;
                dos.writeUTF("AUTENTICADO");
                return true;
            } else {
                dos.writeUTF("FALHA_AUTENTICACAO");
                return false;
            }
        }

        // Método para criar as pastas do usuário
        private void createUserFolders() {
            String userDir = STORAGE_ROOT + File.separator + currentUser;
            new File(userDir).mkdirs(); // Cria a pasta do usuário
            new File(userDir + File.separator + "pdf").mkdirs(); // Cria a pasta PDF
            new File(userDir + File.separator + "jpg").mkdirs(); // Cria a pasta JPG
            new File(userDir + File.separator + "txt").mkdirs(); // Cria a pasta TXT
        }

        // Método para listar os arquivos presentes no diretório do usuário
        private void listFiles() throws IOException {
            String userDir = STORAGE_ROOT + File.separator + currentUser;
            StringBuilder fileList = new StringBuilder();

            fileList.append("PDF:\n");
            File pdfDir = new File(userDir + File.separator + "pdf");
            if (pdfDir.exists()) {
                for (File f : Objects.requireNonNull(pdfDir.listFiles())) {
                    if (f.isFile()) fileList.append(f.getName()).append("\n");
                }
            }

            fileList.append("\nJPG:\n");
            File jpgDir = new File(userDir + File.separator + "jpg");
            if (jpgDir.exists()) {
                for (File f : Objects.requireNonNull(jpgDir.listFiles())) {
                    if (f.isFile()) fileList.append(f.getName()).append("\n");
                }
            }

            fileList.append("\nTXT:\n");
            File txtDir = new File(userDir + File.separator + "txt");
            if (txtDir.exists()) {
                for (File f : Objects.requireNonNull(txtDir.listFiles())) {
                    if (f.isFile()) fileList.append(f.getName()).append("\n");
                }
            }

            dos.writeUTF(fileList.toString()); // Retorna apenas a lista de arquivos
        }

        // Método para fazer o download de um arquivo
        private void downloadFile() throws IOException {
            dos.writeUTF("Digite o tipo de arquivo (pdf, jpg, txt):");
            String type = dis.readUTF().toLowerCase();
            dos.writeUTF("Digite o nome do arquivo:");
            String filename = dis.readUTF();

            // Caminho do arquivo a ser baixado
            String filePath = STORAGE_ROOT + File.separator + currentUser +
                    File.separator + type + File.separator + filename;
            File file = new File(filePath);

            // Verifica se o arquivo existe
            if (!file.exists()) {
                dos.writeUTF("ARQUIVO_NAO_ENCONTRADO");
                return;
            }

            // Envia a confirmação de que o arquivo foi encontrado e o tamanho do arquivo
            dos.writeUTF("ARQUIVO_ENCONTRADO");
            dos.writeLong(file.length());

            // Envia o arquivo em partes
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
        }

        // Método para fazer o upload de um arquivo
        private void uploadFile() throws IOException {
            dos.writeUTF("Digite o tipo de arquivo (pdf, jpg, txt):");
            String type = dis.readUTF().toLowerCase();

            // VALIDAÇÃO DO TIPO:
            if (!type.equals("pdf") && !type.equals("jpg") && !type.equals("txt")) {
                dos.writeUTF("Erro: Tipo de arquivo inválido.");
                return; // Interrompe o upload se for inválido
            }
            dos.writeUTF("Digite o nome do arquivo:");
            String filename = dis.readUTF();
            long fileSize = dis.readLong();

            String dirPath = STORAGE_ROOT + File.separator + currentUser +
                    File.separator + type;
            String filePath = dirPath + File.separator + filename;

            // Verifica se diretório existe
            new File(dirPath).mkdirs();

            // Verifica se o arquivo existe ou se o tamanho é 0 (erro)
            if (fileSize == 0) {
                dos.writeUTF("Erro no arquivo: Não encontrado ou arquivo vazio.");
                return; // Não envia mais nada, retorna para o cliente
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long remaining = fileSize;
                while (remaining > 0 &&
                        (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }

                // Envia mensagem de sucesso após o upload
                dos.writeUTF("Arquivo recebido com sucesso!");
            } catch (IOException e) {
                dos.writeUTF("Erro ao salvar o arquivo: " + e.getMessage());
            }
        }

    }
}
