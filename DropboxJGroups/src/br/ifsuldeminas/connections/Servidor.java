package br.ifsuldeminas.connections;

import java.io.*;
import java.util.*;
import org.jgroups.*;
import org.jgroups.util.*;

public class Servidor extends ReceiverAdapter {

    private static String nomeServidor;
    private final ArrayList<Arquivo> listaArquivosEstado;

    public Servidor(int numeroServidor) throws Exception {
        listaArquivosEstado = new ArrayList();
        nomeServidor = "Servidor" + numeroServidor;
        new File("../Servidores/" + nomeServidor).mkdirs();
    }

    public void tentarConexao() {
        try {
            new JChannel()
                    .setName(nomeServidor)
                    .connect("Servico")
                    .setReceiver(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receive(Message arquivo) {
        Arquivo arquivoRecebido = arquivo.getObject();

        if (arquivoRecebido.ehDiretorio()) {
            new File("../Servidores/" + nomeServidor
                    + "/" + arquivo.src()
                    + "/" + arquivoRecebido.getNomeArquivo())
                    .mkdirs();
        } else {
            if (arquivoRecebido.getArquivoBytes() != null) {
                File novoArquivo = new File("../Servidores/" + nomeServidor
                        + "/" + arquivo.src()
                        + "/" + arquivoRecebido.getDiretorioPai()
                        + "/" + arquivoRecebido.getNomeArquivo());

                new File("../Servidores/" + nomeServidor
                        + "/" + arquivo.src()
                        + "/" + arquivoRecebido.getDiretorioPai())
                        .mkdirs();
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
            } else {
                File arquivoDeleta = new File("../Servidores/" + nomeServidor
                        + "/" + arquivo.src()
                        + "/" + arquivoRecebido.getDiretorioPai()
                        + "/" + arquivoRecebido.getNomeArquivo());
                if (arquivoDeleta.delete()) {
                    System.out.println("Arquivo deletado");
                } else {
                    System.out.println("Arquivo Inexistente");
                }
            }
        }
    }

    @Override
    public void viewAccepted(View servicos) {
        System.out.println("Serviços online: " + servicos);
    }

    public static void main(String args[]) throws Exception {
        new Servidor(1).tentarConexao();
    }
}
