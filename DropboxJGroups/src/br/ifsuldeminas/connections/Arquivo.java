package br.ifsuldeminas.connections;

import java.io.Serializable;
import java.util.regex.Pattern;

public class Arquivo implements Serializable {

    private final byte[] arquivoBytes;
    private final String nomeArquivo;
    private final String diretorioArquivo;

    public Arquivo(byte[] arquivoBytes, String nomeArquivo, String diretorioArquivo) {
        this.arquivoBytes = arquivoBytes;
        this.nomeArquivo = nomeArquivo;
        this.diretorioArquivo = diretorioArquivo.split(Pattern.quote("..\\Clientes"))[1];
    }

    public byte[] getArquivoBytes() {
        return arquivoBytes;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }
    
    public String getDiretorioArquivo() {
        return diretorioArquivo;
    }
}
