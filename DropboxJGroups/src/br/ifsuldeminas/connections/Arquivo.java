package br.ifsuldeminas.connections;

import java.io.Serializable;

public class Arquivo implements Serializable {

    private final byte[] arquivoBytes;
    private final String nomeArquivo;
    private final boolean ehDiretorio;
    private final String diretorioPai;

    public Arquivo(byte[] arquivoBytes, String nomeArquivo, boolean ehDiretorio) {
        this.arquivoBytes = arquivoBytes;
        this.nomeArquivo = nomeArquivo;
        this.ehDiretorio = ehDiretorio;
        this.diretorioPai = "";
    }

    public Arquivo(byte[] arquivoBytes, String nomeArquivo, boolean ehDiretorio, String diretorioPai) {
        this.arquivoBytes = arquivoBytes;
        this.nomeArquivo = nomeArquivo;
        this.ehDiretorio = ehDiretorio;
        this.diretorioPai = diretorioPai;
    }

    public byte[] getArquivoBytes() {
        return arquivoBytes;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public boolean ehDiretorio() {
        return ehDiretorio;
    }

    public String getDiretorioPai() {
        return diretorioPai;
    }
}
