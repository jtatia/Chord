import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Listener implements Runnable {

    private Node node;

    private ServerSocket serverSocket;

    private boolean alive;

    private Thread t;

    public Listener(Node n){
        node = n;
        alive = true;
        InetSocketAddress address = node.getLocalAddress();
        int port = address.getPort();
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException("\nCannot open listener port "+port+". Now exit.\n", e);
        }
        t= new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        while (alive){
            Socket socket = null;
            try {
               socket = serverSocket.accept();
                Client client = new Client(socket, node);
            } catch (IOException e) {
                throw new RuntimeException("Error Cannot accept connection \n"+e);
            }

        }
    }

    public void toDie(){
        alive = false;
    }
}
