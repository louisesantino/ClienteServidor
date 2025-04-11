import java.io.*;
import java.net.*;
import java.util.*;
//yes guurl
public class Servidor {
    private static final int PORT = 12346;
    private static final String STORAGE_ROOT = "armazenamento";
    private static final Map<String, String> users = new HashMap<>();

    static {
        // Usuários e senhas pré-cadastrados
        users.put("louise", "123");
        users.put("gabriel", "123");
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado na porta " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private String currentUser;
        private DataInputStream dis;
        private DataOutputStream dos;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                dis = new DataInputStream(clientSocket.getInputStream());
                dos = new DataOutputStream(clientSocket.getOutputStream());

                // Autenticação
                if (!authenticate()) {
                    return;
                }

                // Cria estrutura de pastas para o usuário
                createUserFolders();

                // Menu principal
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
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Erro ao fechar socket: " + e.getMessage());
                }
            }
        }

        private boolean authenticate() throws IOException {
            dos.writeUTF("Digite seu nome de usuário:");
            String username = dis.readUTF();
            dos.writeUTF("Digite sua senha:");
            String password = dis.readUTF();

            if (users.containsKey(username) && users.get(username).equals(password)) {
                currentUser = username;
                dos.writeUTF("AUTENTICADO");
                return true;
            } else {
                dos.writeUTF("FALHA_AUTENTICACAO");
                return false;
            }
        }

        private void createUserFolders() {
            String userDir = STORAGE_ROOT + File.separator + currentUser;
            new File(userDir).mkdirs();
            new File(userDir + File.separator + "pdf").mkdirs();
            new File(userDir + File.separator + "jpg").mkdirs();
            new File(userDir + File.separator + "txt").mkdirs();
        }

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

            dos.writeUTF(fileList.toString());
        }

        private void downloadFile() throws IOException {
            dos.writeUTF("Digite o tipo de arquivo (pdf, jpg, txt):");
            String type = dis.readUTF().toLowerCase();
            dos.writeUTF("Digite o nome do arquivo:");
            String filename = dis.readUTF();

            String filePath = STORAGE_ROOT + File.separator + currentUser +
                    File.separator + type + File.separator + filename;
            File file = new File(filePath);

            if (!file.exists()) {
                dos.writeUTF("ARQUIVO_NAO_ENCONTRADO");
                return;
            }

            dos.writeUTF("ARQUIVO_ENCONTRADO");
            dos.writeLong(file.length());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
        }

        private void uploadFile() throws IOException {
            dos.writeUTF("Digite o tipo de arquivo (pdf, jpg, txt):");
            String type = dis.readUTF().toLowerCase();
            dos.writeUTF("Digite o nome do arquivo:");
            String filename = dis.readUTF();
            long fileSize = dis.readLong();

            String dirPath = STORAGE_ROOT + File.separator + currentUser +
                    File.separator + type;
            String filePath = dirPath + File.separator + filename;

            // Verifica se diretório existe
            new File(dirPath).mkdirs();

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long remaining = fileSize;
                while (remaining > 0 &&
                        (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }

            dos.writeUTF("Arquivo recebido com sucesso!");
        }
    }
}