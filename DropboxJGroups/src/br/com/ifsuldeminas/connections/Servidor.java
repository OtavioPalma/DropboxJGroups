package br.com.ifsuldeminas.connections;

import br.com.ifsuldeminas.models.Arquivo;
import br.com.ifsuldeminas.enums.Codigo;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import org.jgroups.*;
import org.jgroups.util.*;

public class Servidor extends ReceiverAdapter {

    private static String nomeServidor;
    private final ArrayList<Arquivo> listaArquivosEstado;

    public Servidor(int numeroServidor) throws Exception {
        this.listaArquivosEstado = new ArrayList();
        nomeServidor = "Servidor" + numeroServidor;
        new File("../Servidores/" + nomeServidor).mkdirs();
    }

    public void tentarConexao() {
        try {
            new JChannel()
                    .setName(nomeServidor)
                    .connect("Servico")
                    .setReceiver(this)
                    .getState(null, 10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receive(Message mensagemRecebida) {
        Arquivo arquivoRecebido = mensagemRecebida.getObject();

        synchronized (listaArquivosEstado) {
            listaArquivosEstado.add(arquivoRecebido);
        }

        switch (arquivoRecebido.getCodigo()) {
            case CRIAR_ARQUIVO:
                criarArquivo(arquivoRecebido);
                break;
            case CRIAR_PASTA:
                criarPasta(arquivoRecebido);
                break;
            case APAGAR_ARQUIVO:
                apagarArquivo(arquivoRecebido);
                break;
            case APAGAR_PASTA:
                apagarPasta(arquivoRecebido);
                break;
            case MODIFICAR_PASTA:
                modificarPasta(arquivoRecebido);
                break;
            default:
                System.out.println("CÓDIGO INVÁLIDO");
                break;
        }
    }

    public void criarArquivo(Arquivo arquivo) {
        new File("../Servidores/" + nomeServidor
                + "/" + arquivo.getDiretorioArquivo())
                .mkdirs();

        File novoArquivo = new File("../Servidores/" + nomeServidor
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
        new File("../Servidores/" + nomeServidor
                + "/" + pasta.getDiretorioArquivo())
                .mkdirs();
    }

    public void apagarArquivo(Arquivo arquivo) {
        new File("../Servidores/" + nomeServidor
                + "/" + arquivo.getDiretorioArquivo()
                + "/" + arquivo.getNomeArquivo())
                .delete();
    }

    public void apagarPasta(Arquivo pasta) {
        System.out.println("..\\Servidores\\" + nomeServidor
                + pasta.getDiretorioArquivo());
        File[] arquivosDaPasta = new File("..\\Servidores\\" + nomeServidor
                + pasta.getDiretorioArquivo()).listFiles();

        for (File arquivosDaPasta1 : arquivosDaPasta) {
            apagarArquivo(new Arquivo(null,
                    arquivosDaPasta1.getName(),
                    arquivosDaPasta1.getParent()
                            .split(Pattern.quote("..\\Servidores\\" + nomeServidor + "\\"))[1],
                    null));
        }
        apagarArquivo(pasta);
    }

    public void modificarPasta(Arquivo pasta) {
        File[] arquivosDaPasta = new File("..\\Servidores\\"
                + nomeServidor + pasta.getDiretorioArquivo()
                + "\\" + pasta.getNomeArquivo()).listFiles();
        
        for (File arquivosDaPasta1 : arquivosDaPasta) {
            apagarArquivo(new Arquivo(null,
                    arquivosDaPasta1.getName(),
                    arquivosDaPasta1.getParent()
                            .split(Pattern.quote("..\\Servidores\\" + nomeServidor + "\\"))[1],
                    null));
        }
        apagarArquivo(pasta);
    }

    @Override
    public void viewAccepted(View servicos) {
        System.out.println("Serviços online: " + servicos);
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
                apagarArquivo(arquivo);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Servidor(3).tentarConexao();
    }
}
