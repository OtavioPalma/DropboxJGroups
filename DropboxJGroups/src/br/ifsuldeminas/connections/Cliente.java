package br.ifsuldeminas.connections;

import java.io.File;
import static java.nio.file.StandardWatchEventKinds.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Cliente {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final String username;
    private ArrayList<Arquivo> listaArquivos;

    public Cliente(String username) throws IOException {
        this.listaArquivos = new ArrayList();
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.username = username;
        walkAndRegisterDirectories(Paths.get("../Clientes/" + this.username));
    }

    private void walkAndRegisterDirectories(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerDirectory(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    void processEvents() {
        for (;;) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                @SuppressWarnings("rawtypes")
                WatchEvent.Kind kind = event.kind();

                @SuppressWarnings("unchecked")
                Path name = ((WatchEvent<Path>) event).context();
                Path child = dir.resolve(name);

                if (kind == ENTRY_CREATE) {
                    File newFile = new File(child.toString());
                    this.listaArquivos.add(new Arquivo(newFile.getName(), newFile));
                    
                    System.out.println("Arquivo criado: " + newFile.getName());
                    System.out.println("Diretório: " + newFile.getPath());
                    try {
                        if (Files.isDirectory(child)) {
                            walkAndRegisterDirectories(child);
                        }
                    } catch (IOException x) {
                        System.out.println(x.getMessage());
                    }
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

    public static void main(String[] args) throws IOException {
        String username = "Otavio";
        File userDir = new File("../Clientes/" + username);
        userDir.mkdirs();
        Cliente newClient = new Cliente(username);
        newClient.processEvents();
    }
}
