package br.ifsuldeminas.connections;

import static java.nio.file.StandardWatchEventKinds.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.regex.*;
import org.jgroups.*;
import org.jgroups.util.Util;

public class Cliente extends ReceiverAdapter {

    private JChannel channel;
    private WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final String nomeUsuario;
    private final ArrayList<Arquivo> listaArquivosEstado;

    public Cliente(String nomeUsuario) {
        listaArquivosEstado = new ArrayList();
        this.keys = new HashMap();
        new File("../Clientes/" + nomeUsuario).mkdirs();
        this.nomeUsuario = nomeUsuario;
    }

    public void tentarConexao() {
        try {
            channel = new JChannel()
                    .setName(nomeUsuario)
                    .connect("Servico");
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
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                Path nomeArquivo = ((WatchEvent<Path>) event).context();
                Path diretorioArquivo = dir.resolve(nomeArquivo);
                System.out.format("%s: %s\n", event.kind().name(), diretorioArquivo);

                if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
                    File arquivoOrigem = new File(diretorioArquivo.toString());

                    byte[] arquivoBytes = null;
                    if (!arquivoOrigem.isDirectory()) {
                        arquivoBytes = new byte[(int) arquivoOrigem.length()];
                        try {
                            FileInputStream fis = new FileInputStream(arquivoOrigem);
                            fis.read(arquivoBytes);
                            fis.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        if (Files.isDirectory(diretorioArquivo)) {
                            walkAndRegisterDirectories(diretorioArquivo);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        if (!arquivoOrigem.isDirectory()) {
                            if (dir.toString().equals("..\\Clientes\\" + nomeUsuario)) {
                                Arquivo novoArquivo = new Arquivo(arquivoBytes,
                                        nomeArquivo.toString(), false);
                                channel.send(new Message(null, novoArquivo));
                            } else {
                                String[] aux = dir.toString()
                                        .split(Pattern.quote("..\\Clientes\\" + nomeUsuario + "\\"));
                                Arquivo novoArquivo = new Arquivo(arquivoBytes,
                                        nomeArquivo.toString(), false, aux[1]);
                                channel.send(new Message(null, novoArquivo));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (kind == ENTRY_DELETE) {
                    if (dir.toString().equals("..\\Clientes\\" + nomeUsuario)) {
                        channel.send(new Message(null,
                                new Arquivo(null, nomeArquivo.toString(), false)));
                    } else {
                        String[] aux = dir.toString()
                                .split(Pattern.quote("..\\Clientes\\" + nomeUsuario + "\\"));
                        channel.send(new Message(null,
                                new Arquivo(null, nomeArquivo.toString(), false, aux[1])));
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
        if (!dir.toString().equals("..\\Clientes\\" + nomeUsuario)) {
            String[] aux = dir.toString().split(Pattern.quote("..\\Clientes\\" + nomeUsuario + "\\"));
            channel.send(new Message(null, new Arquivo(null, aux[1], true)));
        }
    }

    public static void main(String[] args) {
        new Cliente("Otavio").tentarConexao();
    }
}
