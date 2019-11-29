package br.com.ifsuldeminas.connections;

import br.com.ifsuldeminas.models.Arquivo;
import br.com.ifsuldeminas.enums.Codigo;
import static java.nio.file.StandardWatchEventKinds.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.regex.Pattern;
import org.jgroups.*;
import org.jgroups.util.Util;

public class Cliente extends ReceiverAdapter {

    private JChannel channel;
    private WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final String nomeUsuario;
    private final ArrayList<Arquivo> listaArquivosEstado;

    public Cliente(String nomeUsuario) {
        this.listaArquivosEstado = new ArrayList();
        this.keys = new HashMap();
        this.nomeUsuario = nomeUsuario;
        new File("../Clientes/" + this.nomeUsuario).mkdirs();
    }

    public void tentarConexao() {
        try {
            channel = new JChannel()
                    .setName(nomeUsuario)
                    .connect("Servico")
                    .setReceiver(this)
                    .getState(null, 10000);
            rodarLoop();
            channel.close();
        } catch (Exception e) {
            System.out.println("ERRO tentarConexao, " + e.getMessage());
        }
    }

    public void rodarLoop() throws Exception {
        this.watcher = FileSystems.getDefault().newWatchService();
        walkAndRegisterDirectories(Paths.get("../Clientes/" + nomeUsuario));

        while (true) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            Path dir = keys.get(key);

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // ..\Clientes\%USERNAME%\%NOME_ARQUIVO
                Path diretorioArquivo = dir.resolve(((WatchEvent<Path>) event).context());
                //System.out.format("%s: %s\n", event.kind().name(), diretorioArquivo);
                File arquivoOrigem = new File(diretorioArquivo.toString());
                byte[] conteudoArquivo = null;

                // CRIAR
                if (kind == ENTRY_CREATE) {
                    // Se for uma pasta
                    if (arquivoOrigem.isDirectory()) {
                        try {
                            walkAndRegisterDirectories(diretorioArquivo);
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }

                        // Ao modificar a pasta, caso ela possua arquivos
                        // dentro dela, eles serão copiados aqui para o servidor
                        File[] conteudoPasta = arquivoOrigem.listFiles();

                        for (File arquivo : conteudoPasta) {
                            conteudoArquivo = arquivoParaBytes(arquivo);

                            Arquivo novoArquivo = new Arquivo(
                                    conteudoArquivo,
                                    arquivo.getName(),
                                    arquivo.getParent().split(Pattern.quote("..\\Clientes\\"))[1],
                                    Codigo.CRIAR_ARQUIVO);

                            Message novaMensagem = new Message(null, novoArquivo);

                            channel.send(novaMensagem);

                            synchronized (listaArquivosEstado) {
                                listaArquivosEstado.add(novoArquivo);
                            }
                        }
                        // Se for um arquivo, somente cria copiando seu conteudo
                        // para o servidor
                    } else {
                        conteudoArquivo = arquivoParaBytes(arquivoOrigem);

                        Arquivo novoArquivo = new Arquivo(
                                conteudoArquivo,
                                arquivoOrigem.getName(),
                                arquivoOrigem.getParent().split(Pattern.quote("..\\Clientes\\"))[1],
                                Codigo.CRIAR_ARQUIVO);

                        Message novaMensagem = new Message(null, novoArquivo);

                        channel.send(novaMensagem);

                        synchronized (listaArquivosEstado) {
                            listaArquivosEstado.add(novoArquivo);
                        }
                    }
                    // DELETAR
                } else if (kind == ENTRY_DELETE) {
                    Arquivo novoArquivo = new Arquivo(
                            null,
                            arquivoOrigem.getName(),
                            arquivoOrigem.getParent().replaceAll(Pattern.quote("..\\Clientes\\"), ""),
                            Codigo.DELETAR);

                    Message novaMensagem = new Message(null, novoArquivo);

                    channel.send(novaMensagem);

                    synchronized (listaArquivosEstado) {
                        listaArquivosEstado.add(novoArquivo);
                    }
                    //ALTERAR
                } else if (kind == ENTRY_MODIFY) {
                    // Se for um arquivo e o diretório existir (impedir conflito
                    // com modificação de pasta)
                    if (!arquivoOrigem.isDirectory() && arquivoOrigem.exists()) {
                        conteudoArquivo = arquivoParaBytes(arquivoOrigem);

                        Arquivo novoArquivo = new Arquivo(
                                conteudoArquivo,
                                arquivoOrigem.getName(),
                                arquivoOrigem.getParent().split(Pattern.quote("..\\Clientes\\"))[1],
                                Codigo.CRIAR_ARQUIVO);

                        Message novaMensagem = new Message(null, novoArquivo);

                        channel.send(novaMensagem);

                        synchronized (listaArquivosEstado) {
                            listaArquivosEstado.add(novoArquivo);
                        }
                    }
                } else {
                    System.out.println("CHAVE INVÁLIDA");
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    public byte[] arquivoParaBytes(File arquivo) {
        byte[] arquivoBytes = new byte[(int) arquivo.length()];
        try {
            Thread.sleep(1000);
            FileInputStream fis = new FileInputStream(arquivo);
            fis.read(arquivoBytes);
            fis.close();
        } catch (IOException e) {
            System.out.println("ERRO arquivosParaBytes, " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("ERRO Thread, " + e.getMessage());
        }
        return arquivoBytes;
    }

    private void walkAndRegisterDirectories(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                try {
                    registerDirectory(dir);
                } catch (Exception e) {
                    System.out.println("ERRO walkAndRegisterDirectories, " + e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerDirectory(Path dir) throws Exception {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);

        Arquivo novaPasta = new Arquivo(
                null,
                dir.getFileName().toString(),
                dir.toString().split(Pattern.quote("..\\Clientes"))[1],
                Codigo.CRIAR_PASTA);

        Message novaMensagem = new Message(null, novaPasta);

        channel.send(novaMensagem);

        synchronized (listaArquivosEstado) {
            listaArquivosEstado.add(novaPasta);
        }
    }

    public void criarArquivo(Arquivo arquivo) {
        File novoArquivo = new File(
                "../Clientes/"
                + arquivo.getDiretorioArquivo()
                + "/" + arquivo.getNomeArquivo());
        try {
            byte[] arquivoRecebidoBytes = arquivo.getArquivoBytes();
            FileOutputStream fos = new FileOutputStream(novoArquivo);
            fos.write(arquivoRecebidoBytes, 0, arquivoRecebidoBytes.length);
            fos.close();
        } catch (FileNotFoundException e) {
            System.out.println("ERRO criarArquivo (NotFound), " + e.getMessage());
        } catch (IOException e) {
            System.out.println("ERRO criarArquivo (IO), " + e.getMessage());
        }
    }

    public void criarPasta(Arquivo pasta) {
        File novoArquivo = new File("../Clientes/" + pasta.getDiretorioArquivo());
        novoArquivo.mkdirs();
    }

    public void deletar(Arquivo arquivo) {
        File novoArquivo = new File("../Clientes/" + arquivo.getDiretorioArquivo()
                + "/" + arquivo.getNomeArquivo());

        if (novoArquivo.isDirectory()) {
            for (File arq : novoArquivo.listFiles()) {
                arq.delete();
            }
        }

        if (!novoArquivo.delete()) {
            System.out.println("Erro ao apagar: " + novoArquivo.getName());
        }
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (listaArquivosEstado) {
            Util.objectToStream(listaArquivosEstado, new DataOutputStream(output));
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        ArrayList<Arquivo> lista = (ArrayList<Arquivo>) Util.objectFromStream(new DataInputStream(input));
        synchronized (listaArquivosEstado) {
            listaArquivosEstado.clear();
            listaArquivosEstado.addAll(lista);
        }

        for (Arquivo arquivo : listaArquivosEstado) {
            if (arquivo.getDiretorioArquivo().contains(this.nomeUsuario)) {
                switch (arquivo.getCodigo()) {
                    case CRIAR_ARQUIVO:
                        criarArquivo(arquivo);
                        break;
                    case CRIAR_PASTA:
                        criarPasta(arquivo);
                        break;
                    case DELETAR:
                        deletar(arquivo);
                        break;
                    default:
                        return;
                }
            }
        }
    }

    public static void main(String[] args) {
        new Cliente("Otavio").tentarConexao();
    }
}
