import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Chord {

    public static void main(String args[]) throws UnknownHostException {
        Scanner sc = new Scanner(System.in);
        System.out.println("--------------------------------------------------------------------");
        System.out.println("|            WELCOME TO THE CHORD APPLICATION                      |");
        System.out.println("--------------------------------------------------------------------");
        System.out.println("Do You Wish To :-\n1. Create\n2. Join");
        int c = sc.nextInt();
        Node node;
        if (c == 1) {
            System.out.println("Please Enter Port");
            int port = sc.nextInt();
            System.out.println("Creating First Node Of Chord Ring.....");
            InetSocketAddress localAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), port);
            node = new Node(localAddress);
            node.create();
            System.out.println("LAUNCHED....................");
        } else {
            System.out.println("Please Enter local Port");
            int port = sc.nextInt();
            System.out.println("Please Enter IP of KnownNode");
            String ip = sc.next();
            System.out.println("Please Enter Port Of KnownNode");
            int port1 = sc.nextInt();
            InetSocketAddress knownAddress = new InetSocketAddress(ip, port1);
            InetSocketAddress localAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), port);
            node = new Node(localAddress);
            node.join(knownAddress);
        }
        boolean run = true;
        while (run) {
            System.out.println("What Would You Like To Do :-- ");
            System.out.println("1. Find Local IP and ID");
            System.out.println("2. Find Successor");
            System.out.println("3. Find Predessor");
            System.out.println("4. Files Contained");
            System.out.println("5. Finger Table");
            System.out.println("6. Exit Ring");
            int choice = sc.nextInt();
            switch (choice){
                case 1:
                    System.out.println("Local IP: "+node.getLocalAddress());
                    System.out.println("Local ID: "+node.getId());
                    break;
                case 2:
                    System.out.println("Successor IP: "+node.findLocalSuccessor().getAddress());
                    System.out.println("Successor ID: "+node.findLocalSuccessor().getId());
                    break;
                case 3:
                    System.out.println("Predecessor IP: "+node.getPredessor());
                    System.out.println("Predecessor ID: "+Helper.getIdFromAddress(node.getPredessor()));
                    break;
                case 4:
                    node.printFiles();
                    break;
                case 5:
                    node.printFingerTable();
                    break;
                case 6:
                    node.gracefulExit();
                    System.out.println("Exiting.....");
                    System.exit(1);
                    return;
                default:
                    System.out.println("Not Valid");
            }
        }
    }
}
