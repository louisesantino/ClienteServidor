import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12346;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            // Autenticação
            if (!authenticate(dis, dos)) {
                System.out.println("Falha na autenticação. Encerrando...");
                return;
            }

            System.out.println("Autenticado com sucesso!");

            // Menu principal
            while (true) {
                printMenu();
                int option = scanner.nextInt();
                dos.writeInt(option);

                switch (option) {
                    case 1: // Listar arquivos
                        listFiles(dis);
                        break;
                    case 2: // Download
                        downloadFile(dis, dos);
                        break;
                    case 3: // Upload
                        uploadFile(dis, dos);
                        break;
                    case 4: // Sair
                        System.out.println("Encerrando cliente...");
                        return;
                    default:
                        System.out.println("Opção inválida!");
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no cliente: " + e.getMessage());
        }
    }

    private static boolean authenticate(DataInputStream dis, DataOutputStream dos) throws IOException {
        System.out.println(dis.readUTF());
        String username = scanner.next();
        dos.writeUTF(username);

        System.out.println(dis.readUTF());
        String password = scanner.next();
        dos.writeUTF(password);

        String response = dis.readUTF();
        return response.equals("AUTENTICADO");
    }

    private static void printMenu() {
        System.out.println("\n==== MENU ====");
        System.out.println("1. Listar arquivos");
        System.out.println("2. Download de arquivo");
        System.out.println("3. Upload de arquivo");
        System.out.println("4. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private static void listFiles(DataInputStream dis) throws IOException {
        System.out.println("\n=== SEUS ARQUIVOS ===");
        System.out.println(dis.readUTF());
    }

    private static void downloadFile(DataInputStream dis, DataOutputStream dos) throws IOException {
        System.out.println(dis.readUTF());
        String type = scanner.next();
        dos.writeUTF(type);

        System.out.println(dis.readUTF());
        String filename = scanner.next();
        dos.writeUTF(filename);

        String response = dis.readUTF();
        if (response.equals("ARQUIVO_NAO_ENCONTRADO")) {
            System.out.println("Arquivo não encontrado no servidor.");
            return;
        }

        long fileSize = dis.readLong();
        System.out.print("Digite o caminho para salvar o arquivo (incluindo nome do arquivo): ");
        String savePath = scanner.next();

        try (FileOutputStream fos = new FileOutputStream(savePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long remaining = fileSize;
            while (remaining > 0 &&
                    (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                fos.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
        }

        System.out.println("Arquivo baixado com sucesso!");
    }

    private static void uploadFile(DataInputStream dis, DataOutputStream dos) throws IOException {
        System.out.println(dis.readUTF());
        String type = scanner.next();
        dos.writeUTF(type);

        System.out.println(dis.readUTF());
        String filename = scanner.next();
        dos.writeUTF(filename);

        System.out.print("Digite o caminho completo do arquivo a ser enviado: ");
        String filePath = scanner.next();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Arquivo não encontrado localmente.");
            dos.writeLong(0);
            return;
        }

        dos.writeLong(file.length());

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }

        System.out.println(dis.readUTF());
    }
}