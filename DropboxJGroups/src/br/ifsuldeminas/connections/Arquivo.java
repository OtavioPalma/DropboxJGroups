package br.ifsuldeminas.connections;

import java.io.File;

public class Arquivo {

    private String nomeArquivo;
    private File arquivo;

    public Arquivo(String nomeArquivo, File arquivo) {
        this.nomeArquivo = nomeArquivo;
        this.arquivo = arquivo;
        //System.out.println("Arquivo: " + this.nomeArquivo + "\nPath: " + arquivo.getPath());
    }
}
