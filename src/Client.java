import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client implements Runnable {

    private Node localNode;
    
    private Socket localSocket;

    private Thread t;

    public Client(Socket socket, Node node) {

        this.localNode = node;
        this.localSocket = socket;
        t= new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        String request = "";
        try {
            request = Helper.inputToString(localSocket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Exception in Obtaining Response");
        }
        String response = processRequest(request);
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(localSocket.getOutputStream());
            response+="\n\n";
            dataOutputStream.writeBytes(response);
        } catch (IOException e) {
            throw new RuntimeException("Unable to send Request");
        }
    }

    String processRequest(String request){
        String check = request.substring(0,request.indexOf("_"));
        request=request.substring(request.indexOf("_")+1);
        String response = "";
        switch (check)
        {
            case "FINDSUCC":
                response = localNode.findSuccessor(Integer.parseInt(request)).toString();
                break;
            case "GETPRED":
                response = localNode.findPredecessor().toString();
                break;
            case "IAMPRE":
                localNode.notified(request);
                break;
            case "UPD":
                Ipbind data = new Ipbind(request.substring(0,request.indexOf("_")));
                int i = Integer.parseInt(request.substring(request.indexOf("_")+1));
                localNode.updateFingerTable(data,i);
                break;
            case "GETSUCC":
                response = localNode.findLocalSuccessor().toString();
                break;
            case "FINDPRED":
                response = localNode.findPredecesor(Integer.parseInt(request)).toString();
                break;
            case "SETPRED":
                Ipbind pred = new Ipbind(request);
                localNode.setPredecessor(pred.getAddress());
                break;
            case "GETFILES":
                int nId = Integer.parseInt(request.substring(0,request.indexOf("_")));
                int pId = Integer.parseInt(request.substring(request.indexOf("_")+1));
                response = Helper.sendFiles(pId,nId,this.localNode);
                break;
            case "GRACEFULEXIT":
                Ipbind newSuccessor = new Ipbind(request);
                localNode.updateIthFingerEntry(0,newSuccessor);
                if (localNode.getLocalAddress().equals(localNode.findLocalSuccessor().getAddress()))
                    localNode.setPredecessor(localNode.getLocalAddress());
                break;
            case "GRACEFULTRANSFER":
                localNode.createFiles(request);
                break;
        }

        return response;
    }
}
