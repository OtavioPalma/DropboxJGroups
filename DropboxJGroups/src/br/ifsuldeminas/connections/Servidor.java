package br.ifsuldeminas.connections;

import java.io.*;
import java.util.*;
import org.jgroups.*;
import org.jgroups.util.*;

public class Servidor extends ReceiverAdapter {

    private JChannel channel;
    private static String nomeServidor;
    private final ArrayList<Arquivo> listaArquivosEstado;

    public Servidor(int numeroServidor) throws Exception {
        listaArquivosEstado = new ArrayList();
        nomeServidor = "Servidor" + numeroServidor;
        new File("../Servidores/" + nomeServidor).mkdirs();
    }

    public void tentarConexao() {
        try {
            channel = new JChannel()
                    .setName(nomeServidor)
                    .setReceiver(this)
                    .connect("Servico");

            channel.send(new Message(null, "sincronizar"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receive(Message mensagemRecebida) {
        Arquivo arquivoRecebido = mensagemRecebida.getObject();

        if (arquivoRecebido.getArquivoBytes() != null) {
            File novoArquivo = new File("../Servidores/" + nomeServidor
                    + "/" + arquivoRecebido.getDiretorioArquivo()
                    + "/" + arquivoRecebido.getNomeArquivo());

            try {
                byte[] arquivoRecebidoBytes = arquivoRecebido.getArquivoBytes();
                FileOutputStream fos = new FileOutputStream(novoArquivo);
                fos.write(arquivoRecebidoBytes, 0, arquivoRecebidoBytes.length);
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        new File("../Servidores/" + nomeServidor
                + "/" + arquivoRecebido.getDiretorioArquivo())
                .mkdirs();

        /*}else {
                File arquivoDeleta = new File("../Servidores/" + nomeServidor
                        + "/" + mensagemRecebida.src()
                        + "/" + arquivoRecebido.getDiretorioPai()
                        + "/" + arquivoRecebido.getNomeArquivo());
                if (arquivoDeleta.delete()) {
                    System.out.println("Arquivo deletado");
                } else {
                    System.out.println("Arquivo Inexistente");
                }
            }*/
    }

    @Override
    public void viewAccepted(View servicos) {
        System.out.println("Serviços online: " + servicos);
    }

    public static void main(String args[]) throws Exception {
        new Servidor(3).tentarConexao();
    }
}
