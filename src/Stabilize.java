import java.net.InetSocketAddress;

/**
 * Stabilize periodically asks successor for its predecessor
 * and determines whether current node should update or
 */
public class Stabilize implements Runnable{

    Node local;
    boolean alive;
    Thread t;

    public Stabilize(Node local){
        this.local = local;
        alive = true;
        this.t = new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        while (alive) {
            InetSocketAddress successor = local.findLocalSuccessor().getAddress();
            if (successor == null || successor.equals(local.getLocalAddress())) {
                local.fillSuccessor();
            }
            successor = local.findLocalSuccessor().getAddress();
            if (successor != null && !successor.equals(local.getLocalAddress())) {
                InetSocketAddress x = Helper.getInetSocketAddressFromResponse(Helper.requestResponseHandler(successor,ChordConstants.GET_PREDECESSOR));

                // if successor's predecessor is not itself
                if (!x.equals(successor)) {
                    //Check Condition
                    int sucessor_relative = Helper.calculateRelativeId(local.findLocalSuccessor().getId(),local.getId());
                    int x_relative_id = Helper.calculateRelativeId(Helper.getIdFromAddress(x),local.getId());
                    if (x_relative_id>0 && x_relative_id < sucessor_relative) {
                        local.updateIthFingerEntry(0,new Ipbind(Helper.getIdFromAddress(x),x));
                    }
                }
                // successor's predecessor is successor itself, then notify successor
                else {
                    local.notify(local.findLocalSuccessor().getAddress());
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void toDie(){
        alive = false;
    }
}
