import java.net.InetSocketAddress;

public class Ipbind {

    private int id;

    private InetSocketAddress address;

    public Ipbind(int id, InetSocketAddress address) {
        this.id = id;
        this.address = address;
    }

    public Ipbind(String fingerEntry) {
        InetSocketAddress address = Helper.getInetSocketAddressFromResponse(fingerEntry);
        this.id = Helper.getIdFromAddress(address);
        this.address = address;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public String toString(){
        String val = address.getAddress().getHostAddress()+":"+address.getPort()+"\n";
        return val;
    }

}
