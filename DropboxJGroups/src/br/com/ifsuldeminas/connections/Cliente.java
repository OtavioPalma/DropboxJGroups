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
            e.printStackTrace();
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

                Path diretorioArquivo = dir.resolve(((WatchEvent<Path>) event).context());
                System.out.format("%s: %s\n", event.kind().name(), diretorioArquivo);
                File arquivoOrigem = new File(diretorioArquivo.toString());

                if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
                    byte[] arquivoBytes = null;

                    if (new File(dir.toString()).exists()) {
                        if (!arquivoOrigem.isDirectory()) {
                            arquivoBytes = arquivoParaBytes(arquivoOrigem);
                        }

                        try {
                            if (Files.isDirectory(diretorioArquivo)) {
                                walkAndRegisterDirectories(diretorioArquivo);
                                if (kind == ENTRY_CREATE) {
                                    File[] listaDeArquivos = new File(diretorioArquivo.toString()).listFiles();

                                    for (File arquivo : listaDeArquivos) {
                                        arquivoBytes = arquivoParaBytes(arquivo);

                                        Arquivo novoArquivo = new Arquivo(arquivoBytes,
                                                arquivo.getName(),
                                                diretorioArquivo.toString().split(Pattern.quote("..\\Clientes"))[1],
                                                Codigo.CRIAR_ARQUIVO);

                                        channel.send(new Message(null, novoArquivo));
                                    }
                                }
                            } else {
                                Arquivo novoArquivo = new Arquivo(arquivoBytes,
                                        arquivoOrigem.getName(),
                                        dir.toString().split(Pattern.quote("..\\Clientes"))[1],
                                        Codigo.CRIAR_ARQUIVO);

                                channel.send(new Message(null, novoArquivo));

                                synchronized (listaArquivosEstado) {
                                    listaArquivosEstado.add(novoArquivo);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        channel.send(new Message(null,
                                new Arquivo(null,
                                        "",
                                        dir.toString().split(Pattern.quote("..\\Clientes"))[1],
                                        Codigo.APAGAR_PASTA)));
                    }
                } else if (kind == ENTRY_DELETE) {
                    if (new File(diretorioArquivo.toString()).exists()) {
                        Arquivo pasta = new Arquivo(null,
                                arquivoOrigem.getName(),
                                dir.toString().split(Pattern.quote("..\\Clientes"))[1],
                                Codigo.APAGAR_ARQUIVO);
                        channel.send(new Message(null, pasta));
                    } else {
                        Arquivo pasta = new Arquivo(null,
                                arquivoOrigem.getName(),
                                dir.toString().split(Pattern.quote("..\\Clientes"))[1],
                                Codigo.MODIFICAR_PASTA);
                        channel.send(new Message(null, pasta));
                    }
                } else {
                    System.out.println("Código Inválido");
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
            FileInputStream fis = new FileInputStream(arquivo);
            fis.read(arquivoBytes);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
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
                    e.printStackTrace();
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerDirectory(Path dir) throws Exception {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
        if (!dir.getParent().toString().equals("..\\Clientes")) {
            channel.send(new Message(null,
                    new Arquivo(null,
                            dir.getFileName().toString(),
                            dir.toString().split(Pattern.quote("..\\Clientes"))[1],
                            Codigo.CRIAR_PASTA)));
        }
    }

    public void criarArquivo(Arquivo arquivo) {
        new File("../Clientes/"
                + "/" + arquivo.getDiretorioArquivo())
                .mkdirs();

        File novoArquivo = new File("../Clientes/"
                + "/" + arquivo.getDiretorioArquivo()
                + "/" + arquivo.getNomeArquivo());
        try {
            byte[] arquivoRecebidoBytes = arquivo.getArquivoBytes();
            FileOutputStream fos = new FileOutputStream(novoArquivo);
            fos.write(arquivoRecebidoBytes, 0, arquivoRecebidoBytes.length);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void criarPasta(Arquivo pasta) {
        new File("../Clientes/"
                + "/" + pasta.getDiretorioArquivo())
                .mkdirs();
    }

    public void deletar(Arquivo arquivo) {
        new File("../Clientes/"
                + "/" + arquivo.getDiretorioArquivo()
                + "/" + arquivo.getNomeArquivo())
                .delete();
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
            if (arquivo.getCodigo() == Codigo.CRIAR_ARQUIVO) {
                criarArquivo(arquivo);
            } else if (arquivo.getCodigo() == Codigo.CRIAR_PASTA) {
                criarPasta(arquivo);
            } else if (arquivo.getCodigo() == Codigo.APAGAR_ARQUIVO) {
                deletar(arquivo);
            }
        }

    }

    public static void main(String[] args) {
        new Cliente("Carlos").tentarConexao();
    }
}
