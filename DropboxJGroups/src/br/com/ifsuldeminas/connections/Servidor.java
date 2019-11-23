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
            System.out.println("ERRO tentarConexao, " + e.getMessage());
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
            case DELETAR:
                deletar(arquivoRecebido);
                break;
            default:
                System.out.println("CÓDIGO INVÁLIDO");
                break;
        }
    }

    public void criarArquivo(Arquivo arquivo) {
        File novoArquivo = new File("../Servidores/" + nomeServidor
                + "/" + arquivo.getDiretorioArquivo()
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
        File novoArquivo = new File("../Servidores/" + nomeServidor
                + "/" + pasta.getDiretorioArquivo());
        novoArquivo.mkdirs();
    }

    public void deletar(Arquivo arquivo) {
        File novoArquivo = new File("../Servidores/" + nomeServidor
                + "/" + arquivo.getDiretorioArquivo()
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

    public static void main(String[] args) throws Exception {
        new Servidor(new Random().nextInt(10000)).tentarConexao();
    }
}
