import java.io.*;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

public class Helper {

/*
Defining Global Factory Methods
 */

    public static String requestResponseHandler(InetSocketAddress socketAddress, String request) {
        try {
            Socket clientSocket = new Socket(socketAddress.getAddress(), socketAddress.getPort());
            DataOutputStream outToPeer = new DataOutputStream(clientSocket.getOutputStream());
            outToPeer.writeBytes(request+"\n\n");
            return inputToString(clientSocket.getInputStream());
        }catch (Exception exc){
            System.out.println("Execute Function failed for Request : "+request);
            exc.printStackTrace();
        }
        return null;
    }

    public static String inputToString(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String y = "";
        try {
            y = bufferedReader.readLine();
            while ( y!= null && !y.isEmpty()) {
                line += y;
                y=bufferedReader.readLine();
            }
        } catch (IOException e) {
            System.out.println("Input TO String Method");
            e.printStackTrace();
            return null;
        }
        return line;
    }

    public static int calculateRelativeId(int id,int base){
        int rem = id-base;
        int range = 5;
        if(rem<=0){
            rem+=(1<<range);
        }
        return rem;
    }


    public static int getIdFromAddress(InetSocketAddress localAddress) {
        String mess = localAddress.getAddress() + "" + localAddress.getPort();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(mess.getBytes("UTF-8"));
            byte res[] = md.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i=0;i<res.length;i++) {
                hexString.append(Integer.toHexString(0xFF & res[i]));
            }
            return convertToValue(hexString.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public static int getIdFromName(String name){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(name.getBytes("UTF-8"));
            byte res[] = md.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i=0;i<res.length;i++) {
                hexString.append(Integer.toHexString(0xFF & res[i]));
            }
            return convertToValue(hexString.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return 1;

    }

    private static int convertToValue(String mess) {
        int res = 1;
        for (int i = 0; i < mess.length(); i++) {
            if (Character.isDigit(mess.charAt(i)))
                res = res *10+ Character.valueOf(mess.charAt(i));
        }
        if (res<0)
            res*=-1;
        int ret=1,k=32;
        while (res>0){
            if (res<32)
                break;
            res=res/k;
            k--;
            if (k<10)
                k=32;
    }
    if (res>0)
        return res;
        return ret;
}

public static InetSocketAddress getInetSocketAddressFromResponse(String suc){
        return new InetSocketAddress(suc.substring(0,suc.indexOf(":")),
                Integer.parseInt(suc.substring(suc.indexOf(":")+1)));
}

    public static String sendFiles(int pId, int nId, Node localNode) {
    String flist = "";
        Set<Integer> keys = localNode.getFiles().keySet();
        ArrayList<Integer> keysList = new ArrayList<>();
        for (int k:keys){
            if (pId<nId && (k>pId && k<=nId)) {
                ArrayList<String> arr = localNode.getFiles().get(k);
                for (String x : arr) {
                    flist += x + "$";
                    File f = new File(localNode.getPath()+"/"+x);
                    f.delete();
                }
                keysList.add(k);
            }
            else if (pId>nId && (k>pId && k<=nId)){
                ArrayList<String> arr = localNode.getFiles().get(k);
                for (String x:arr) {
                    flist += x + "$";
                    File f = new File(localNode.getPath()+"/"+x);
                    f.delete();
                }
            keysList.add(k);
            }
        }
        for (int k:keysList) {
            localNode.getFiles().remove(k);
        }
        return flist;
    }
}
