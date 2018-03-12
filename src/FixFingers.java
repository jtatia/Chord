import java.net.InetSocketAddress;
import java.util.Random;

public class FixFingers implements Runnable{

    Node local;
    boolean alive;
    Thread t;
    Random random;
    int x;

    public FixFingers(Node local) {
        this.local = local;
        this.alive = true;
        random = new Random();
        this.x=0;
        this.t =new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        while (alive) {
            x=x%5;
            int id = ((local.getId()+(1<<(x)))%(32));
            Ipbind ithfinger=local.findSuccessor(id);
            local.updateIthFingerEntry(x,ithfinger);
            x++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void toDie(){
        alive = false;
    }
}
