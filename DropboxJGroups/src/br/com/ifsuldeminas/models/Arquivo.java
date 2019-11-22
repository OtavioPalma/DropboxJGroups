package br.com.ifsuldeminas.models;

import br.com.ifsuldeminas.enums.Codigo;
import java.io.Serializable;
import java.util.regex.Pattern;

public class Arquivo implements Serializable {

    private final byte[] arquivoBytes;
    private final String nomeArquivo;
    private final String diretorioArquivo;
    private final Codigo codigo;

    public Arquivo(byte[] arquivoBytes, String nomeArquivo, String diretorioArquivo, Codigo codigo) {
        this.arquivoBytes = arquivoBytes;
        this.nomeArquivo = nomeArquivo;
        this.diretorioArquivo = diretorioArquivo;
        this.codigo = codigo;
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

    public Codigo getCodigo() {
        return codigo;
    }
}
