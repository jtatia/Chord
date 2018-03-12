import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.*;

public class Node {

    public static int range =5;

    private int id; //id mapped using SHA

    private InetSocketAddress localAddress;

    private InetSocketAddress predecessor;

    private HashMap<Integer, Ipbind> fingerTable;

    private Listener listener;

    private Stabilize stabilize;

    private FixFingers fixFingers;

    private String path;

    HashMap<Integer, ArrayList<String>> files;

    //Joining An already Existing Chord Ring
    public Node(InetSocketAddress localAddress){
        this.localAddress = localAddress;
        this.id = Helper.getIdFromAddress(localAddress);

        //Initialize Empty Finger Table
        fingerTable = new HashMap<>();
        this.path = "Files"+id;
        files = new HashMap<>();
        predecessor = null;

    }

    public boolean create(){
        Ipbind ipbind = new Ipbind(id,localAddress);
        for (int i=0;i<=4;i++){
            updateIthFingerEntry(i,ipbind);
        }
        predecessor = localAddress;

        for (int i=1;i<=100;i++){
            String filename = "file"+i;
            int mapId = Helper.getIdFromName(filename);
            if (files.containsKey(mapId)){
                files.get(mapId).add(filename);
            }
            else files.put(mapId,new ArrayList<>(Arrays.asList(filename)));
        }

        this.listener = new Listener(this);
   //     this.stabilize = new Stabilize(this);
        this.fixFingers = new FixFingers(this);
        return true;
    }

    public boolean join(InetSocketAddress knownNode){
        if (knownNode!=null && !knownNode.equals(localAddress)) {
            initFingerTable(knownNode);
            updateOthers();
        }else if(knownNode!=null && knownNode.equals(localAddress)){
            for(int i=1;i<=range;i++){
                fingerTable.put(i,new Ipbind(id,localAddress));
            }
            this.setPredecessor(localAddress);
        }

        String fileList = Helper.requestResponseHandler(
                findLocalSuccessor().getAddress(),"GETFILES_"+id+"_"+Helper.getIdFromAddress(predecessor));
              createFiles(fileList);

            this.listener = new Listener(this);
            this.fixFingers = new FixFingers(this);

        return true;
    }

    public boolean createFiles(String filesList){
        if (filesList.length()==0)
            return false;
        String array[] = filesList.split("\\$");
        for (String s : array){
            int mapId = Helper.getIdFromName(s);
            if (files.containsKey(mapId)){
                files.get(mapId).add(s);
            }
            else files.put(mapId,new ArrayList<>(Arrays.asList(s)));
        }

        File dir = new File(path);
        // attempt to create the directory here
        if(!dir.isDirectory()){
            dir.mkdir();
        }


        for (String x: array){
                File f = new File(path+"/"+x);
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
    }

    public void notify(InetSocketAddress successor) {
        Ipbind ipbind = new Ipbind(id,localAddress);
        if (successor != null && !successor.equals(localAddress))
            Helper.requestResponseHandler(successor, "IAMPRE_" + ipbind.toString());
        }

    public void notified (String newpre) {
        Ipbind pred = new Ipbind(newpre);
        if (predecessor != null || !predecessor.equals(localAddress)) {
            this.setPredecessor(pred.getAddress());
        }
        else {
            int newpre_relative_id = Helper.calculateRelativeId(pred.getId(),Helper.getIdFromAddress(predecessor));
            int local_relative_id = Helper.calculateRelativeId(id,Helper.getIdFromAddress(predecessor));
            if (newpre_relative_id < local_relative_id)
                this.setPredecessor(pred.getAddress());
        }
    }


    public void initFingerTable(InetSocketAddress contact){
        InetSocketAddress successor = Helper.getInetSocketAddressFromResponse(
                Helper.requestResponseHandler(contact,"FINDSUCC_"+(id+1)%32));
        fingerTable.put(0,new Ipbind(Helper.getIdFromAddress(successor),successor));
        predecessor = Helper.getInetSocketAddressFromResponse(
                Helper.requestResponseHandler(this.findLocalSuccessor().getAddress(),ChordConstants.GET_PREDECESSOR));
        Ipbind cur = new Ipbind(id,localAddress);
        Helper.requestResponseHandler(this.findLocalSuccessor().getAddress(),"SETPRED_"+cur.toString());
        for(int i=2;i<=range;i++){
            int id = ((this.id+(1<<(i-1)))%(32));
            int query_id_relative = Helper.calculateRelativeId(id,this.id);
            int last_finger_relative = Helper.calculateRelativeId(fingerTable.get(i-2).getId(),this.id);
            if(query_id_relative<=last_finger_relative){
                fingerTable.put(i-1,fingerTable.get(i-2));
            }else{
                String fingerEntry = Helper.requestResponseHandler(contact,"FINDSUCC_"+id);
                fingerTable.put(i-1,new Ipbind(fingerEntry));
            }
        }
    }

    public Ipbind findSuccessor(int id){
        Ipbind n = this.findPredecesor(id);
        Ipbind s = new Ipbind(Helper.requestResponseHandler(n.getAddress(),"GETSUCC_"));
        //System.out.println("Successor Returned : "+s.toString());
        return s;
    }

    public Ipbind findPredecesor(int id){
        //Check Condition
        int query_id_relative=Helper.calculateRelativeId(id,this.id);
        int successor_relative = Helper.calculateRelativeId(this.findLocalSuccessor().getId(),this.id);
        if(query_id_relative<=successor_relative){
            Ipbind d = new Ipbind(this.id,this.localAddress);
            return d;
        }
        InetSocketAddress broadcastNode = closestPrecedingFinger(id);
        String broadcastIp = Helper.requestResponseHandler(broadcastNode,"FINDPRED_"+id);
        Ipbind result = null;
        try{
            result = new Ipbind(broadcastIp);
        }catch(Exception exc){
            exc.printStackTrace();
        }
        return result;
    }

    public InetSocketAddress closestPrecedingFinger(int id) {
        //Check Condition
        InetSocketAddress pred = null;
        for (int i = range-1; i >= 0; i--) {
            //Check Condition
            int finger_relative = Helper.calculateRelativeId(fingerTable.get(i).getId(),this.id);
            int id_relative = Helper.calculateRelativeId(id,this.id);
            if (finger_relative < id_relative) {
                pred = fingerTable.get(i).getAddress();
                break;
            }
        }
        return pred;
    }


    public boolean updateOthers(){
        for(int i=0;i<range;i++){
            int arg = id-(1<<(i));
            arg = (arg<0)?32+arg:arg;
            Ipbind p = this.findPredecesor((arg)%(32));
            Ipbind pSucc = null;
            if (!p.getAddress().equals(this.getLocalAddress()))
            {
                pSucc = new Ipbind(Helper.requestResponseHandler(p.getAddress(),"GETSUCC_1"));
            }
            else
                pSucc = this.findLocalSuccessor();
            if (pSucc.getId()==arg)
                p=pSucc;
            if(!p.getAddress().equals(localAddress)) {
                Ipbind local= new Ipbind(id,localAddress);
                Helper.requestResponseHandler(p.getAddress(), "UPD_" + local.toString() + "_" + i);
            }
        }
        return true;
    }
    public void updateFingerTable(Ipbind p,int i){
        int p_relative = Helper.calculateRelativeId(p.getId(),this.id);
        int finger_relative = Helper.calculateRelativeId(fingerTable.get(i).getId(),this.id);
        if(p_relative>0 && p_relative<finger_relative){
            fingerTable.put(i,p);
            InetSocketAddress predecessorNode = predecessor;
            if(!p.getAddress().equals(predecessor)) {
             Ipbind pred = new Ipbind(Helper.getIdFromAddress(predecessor),predecessor);
                Helper.requestResponseHandler(predecessorNode, "UPD_" + pred.toString() + "_" + i);
            }
        }
    }

    public void gracefulExit(){
        Helper.requestResponseHandler(predecessor,ChordConstants.GRACEFUL_EXIT+findLocalSuccessor().toString());
        String files = Helper.sendFiles(Helper.getIdFromAddress(predecessor),id,this);
        Helper.requestResponseHandler(findLocalSuccessor().getAddress(),ChordConstants.GRACEFUL_TRANSFER+files);
     }


    public void fillSuccessor() {
        InetSocketAddress successor = this.findLocalSuccessor().getAddress();
        if (successor == null || successor.equals(localAddress)) {
            for (int i = 1; i <= 32; i++) {
                InetSocketAddress ithfinger = fingerTable.get(i).getAddress();
                if (ithfinger!=null && !ithfinger.equals(localAddress)) {
                    for (int j = i-1; j >=0; j--) {
                        updateIthFingerEntry(j,fingerTable.get(i));
                    }
                    break;
                }
            }
        }
        successor = findLocalSuccessor().getAddress();
        if ((successor == null || successor.equals(localAddress)) && predecessor!=null && !predecessor.equals(localAddress)) {
            updateIthFingerEntry(0,new Ipbind(Helper.getIdFromAddress(predecessor),predecessor));
        }
    }


    public Ipbind findPredecessor(){
        return new Ipbind(Helper.getIdFromAddress(predecessor),predecessor);
    }


    public Ipbind findLocalSuccessor(){
        return fingerTable.get(0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void updateIthFingerEntry(int i, Ipbind value){
        fingerTable.put(i,value);
        if (i==0 && value.getAddress()!=null && !value.getAddress().equals(localAddress))
            notify(value.getAddress());
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    public InetSocketAddress getPredessor() {
        return predecessor;
    }

    public void setPredecessor(InetSocketAddress predecessor) {
        this.predecessor = predecessor;
    }


    public HashMap<Integer, Ipbind> getFingerTable() {
        return fingerTable;
    }

    public void setFingerTable(HashMap<Integer, Ipbind> fingerTable) {
        this.fingerTable = fingerTable;
    }

    public void printFingerTable(){
        System.out.println("START\tRANGE\tFINGER");
        for(int i=0;i<5;i++){
            System.out.println((id+(int)Math.pow(2,i))%32+"\t("+(id+(int)Math.pow(2,i))%32+
                   ", "+  (id+(int)Math.pow(2,i+1))%32+" )\t\t"+fingerTable.get(i).getId());
        }
    }

    public void printFiles(){
        System.out.println("................FILES...................");
        ArrayList<String> fl = new ArrayList<>();
        for (Map.Entry<Integer,ArrayList<String>> k:files.entrySet()){
            for(String x:k.getValue())
                fl.add(x);
        }
        Collections.sort(fl);
        for (String x:fl)
            System.out.println(x);
    }

    public void closeAllThreads(){
        listener.toDie();
        fixFingers.toDie();
    }

    public HashMap<Integer, ArrayList<String>> getFiles() {
        return files;
    }

    public void setFiles(HashMap<Integer, ArrayList<String>> files) {
        this.files = files;
    }
}
