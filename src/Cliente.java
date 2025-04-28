import java.io.*;
import java.net.*;
import java.util.Scanner;



 /// Este programa conecta a um servidor para realizar operações de:
 /// Autenticação de usuário
 /// Listagem de arquivos
 /// Upload e download de arquivos

public class Cliente {

    /// Endereço IP ou nome do servidor
    private static final String SERVER_ADDRESS = "localhost";
    /// Porta para conexão com o servidor
    private static final int SERVER_PORT = 12346;
    /// Scanner para entrada de dados do usuário
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            // Realiza a autenticação do usuário
            if (!realizarAutenticacao(input, output)) {
                System.out.println("Falha na autenticação. Encerrando...");
                return;
            }

            System.out.println("Autenticado com sucesso!");

            // Exibe o menu até o usuário escolher sair
            while (true) {
                exibirMenu();
                int opcao = scanner.nextInt();
                output.writeInt(opcao);

                switch (opcao) {
                    case 1:
                        listarArquivosDisponiveis(input);
                        break;
                    case 2:
                        fazerDownloadDeArquivo(input, output);
                        break;
                    case 3:
                        fazerUploadDeArquivo(input, output);
                        break;
                    case 4:
                        System.out.println("Encerrando cliente...");
                        return;
                    default:
                        System.out.println("Opção inválida. Tente novamente.");
                }
            }

        } catch (IOException e) {
            System.err.println("Erro ao se comunicar com o servidor: " + e.getMessage());
        }
    }

    ///Realiza a autenticação com o servidor.
    private static boolean realizarAutenticacao(DataInputStream input, DataOutputStream output) throws IOException {
        System.out.println(input.readUTF()); // Mensagem pedindo usuário
        String username = scanner.next();
        output.writeUTF(username);

        System.out.println(input.readUTF()); // Mensagem pedindo senha
        String password = scanner.next();
        output.writeUTF(password);

        String resposta = input.readUTF();
        return resposta.equalsIgnoreCase("AUTENTICADO");
    }

    /// Exibe o menu principal de opções.
    private static void exibirMenu() {
        System.out.println("\n==== MENU ====");
        System.out.println("1. Listar arquivos");
        System.out.println("2. Download de arquivo");
        System.out.println("3. Upload de arquivo");
        System.out.println("4. Sair");
        System.out.print("Escolha uma opção: ");
    }

    /// Lista os arquivos disponíveis no servidor.
    private static void listarArquivosDisponiveis(DataInputStream input) throws IOException {
        System.out.println("\n=== ARQUIVOS DISPONÍVEIS ===");
        String resposta = input.readUTF();

        // Se a resposta for uma lista válida de arquivos, mostra ela
        if (resposta.startsWith("PDF") || resposta.startsWith("JPG") || resposta.startsWith("TXT")) {
            System.out.println(resposta);
        } else {
            // Caso seja uma mensagem de erro, não tentamos exibir a lista de arquivos
            System.out.println("Erro ao listar arquivos. Tente novamente.");
        }
    }

     /// Faz o download de um arquivo do servidor.
    private static void fazerDownloadDeArquivo(DataInputStream input, DataOutputStream output) throws IOException {
        System.out.println(input.readUTF()); // Pergunta tipo
        String tipoArquivo = scanner.next();
        output.writeUTF(tipoArquivo);

        System.out.println(input.readUTF()); // Pergunta nome
        String nomeArquivo = scanner.next();
        output.writeUTF(nomeArquivo);

        String resposta = input.readUTF();
        if (resposta.equals("ARQUIVO_NAO_ENCONTRADO")) {
            System.out.println("Arquivo não encontrado no servidor.");
            return;
        }

        long tamanhoArquivo = input.readLong();
        System.out.print("Digite o caminho para salvar o arquivo (com nome e extensão): ");
        String caminhoSalvar = scanner.next();

        File arquivoDestino = new File(caminhoSalvar);

        // Verifica se o diretório de destino existe antes de realizar o download
        if (!arquivoDestino.getParentFile().exists()) {
            System.out.println("Diretório não encontrado. Criando diretórios necessários...");
            boolean dirsCriados = arquivoDestino.getParentFile().mkdirs(); // Cria diretórios
            if (!dirsCriados) {
                System.out.println("Falha ao criar diretórios. Verifique permissões.");
                return;
            }
        }

        // Tratamento do download
        try (FileOutputStream fos = new FileOutputStream(arquivoDestino)) {
            byte[] buffer = new byte[4096];
            int bytesLidos;
            long restante = tamanhoArquivo;

            while (restante > 0 &&
                    (bytesLidos = input.read(buffer, 0, (int) Math.min(buffer.length, restante))) != -1) {
                fos.write(buffer, 0, bytesLidos);
                restante -= bytesLidos;
            }

            System.out.println("Arquivo baixado com sucesso em: " + arquivoDestino.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Erro ao salvar o arquivo: " + e.getMessage());
        }
    }

    ///Faz o upload de um arquivo para o servidor.
    private static void fazerUploadDeArquivo(DataInputStream input, DataOutputStream output) throws IOException {
        System.out.println(input.readUTF()); // Pergunta tipo
        String tipoArquivo = scanner.next();
        output.writeUTF(tipoArquivo);

        System.out.println(input.readUTF()); // Pergunta nome
        String nomeArquivo = scanner.next();
        output.writeUTF(nomeArquivo);

        System.out.print("Digite o caminho completo do arquivo a ser enviado: ");
        String caminhoArquivo = scanner.next();

        File arquivo = new File(caminhoArquivo);

        if (!arquivo.exists() || !arquivo.isFile()) {
            System.out.println("Arquivo não encontrado no caminho informado.");
            output.writeLong(0); // Informa que não existe arquivo
            return;
        }

        output.writeLong(arquivo.length());

        try (FileInputStream fis = new FileInputStream(arquivo)) {
            byte[] buffer = new byte[4096];
            int bytesLidos;
            while ((bytesLidos = fis.read(buffer)) != -1) {
                output.write(buffer, 0, bytesLidos);
            }

            // Aguarda a resposta do servidor após o upload
            String resposta = input.readUTF();

            if (resposta.contains("Erro")) {
                System.out.println("Erro no upload: " + resposta);
            } else {
                System.out.println(resposta); // Sucesso no upload
            }

        } catch (IOException e) {
            System.err.println("Erro ao enviar o arquivo: " + e.getMessage());
        }
    }
}
