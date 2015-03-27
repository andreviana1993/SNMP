package snmpnetworkbrowser;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.net.util.SubnetUtils;
import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

public class SNMPNetworkBrowser {

    // <editor-fold defaultstate="collapsed" desc="variable declarations">
    //network related declarations
    private static InetAddress currentIp;
    private static ArrayList<NetworkDevice> networkDevices = new ArrayList<>();
    private static InetAddress netmask, network, broadcast;
    private static SubnetUtils subnetUtils;
    private static ArrayList<InetAddress> subnetIpList = new ArrayList<>();
    private static InetAddress lowAddress;
    private static InetAddress highAddress;

    //SNMP related declarations
    // OID of MIB RFC 1213; Scalar Object = .iso.org.dod.internet.mgmt.mib-2.system.sysDescr.0
    //public static String oidValue = ".1.3.6.1.2.1.1.1.0"; // ends with 0 for scalar object
    public static String oidValue = ".1.3.6.1.2.1.2.1.0"; // ends with 0 for scalar object
    public static final int snmpVersion = SnmpConstants.version2c;
    public static final String community = "public";
    public static final String snmpPort = "161";
    public static PDU pdu;
    
    //other declarations
    public static int reachableDevices=0;

    //</editor-fold>
    public static void main(String[] args) throws Exception {

        // <editor-fold defaultstate="collapsed" desc="get current ip from network interface">
        //get current ip from network interface
        int i;
        NetworkInterface networkInterface = NetworkInterface.getByName("wlan0");
        System.out.println("Number of Interface Adresses:"
                + networkInterface.getInterfaceAddresses().size());
        System.out.println("Searching for an IPv4 Address...");
        for (i = 0; i < networkInterface.getInterfaceAddresses().size();) {
            currentIp = networkInterface.getInterfaceAddresses().get(i).getAddress();
            System.out.println("Interface Address #"
                    + i);
            System.out.println(networkInterface.getDisplayName()
                    + " --> "
                    + currentIp
                    + "/"
                    + networkInterface.getInterfaceAddresses().get(i).getNetworkPrefixLength());
            if (currentIp.getClass() == Inet6Address.class) {
                i++;
                System.out.println("Found an IPv6 Address. Skipping to the next Interface Address...");
            } else if (currentIp.getClass() == Inet4Address.class) {
                System.out.println("Found an IPv4 Address.");
                System.out.println("Search was successful. Continuing...");
                break;
            }
        }
        // </editor-fold>

        // <editor-fold defaultstate="collapsed" desc="get subnet details">
        // get subnet details
        subnetUtils = new SubnetUtils(new StringBuilder(currentIp.toString()).deleteCharAt(0).toString() + "/"
                + networkInterface.getInterfaceAddresses().get(i).getNetworkPrefixLength());
        broadcast = Inet4Address.getByName(subnetUtils.getInfo().getBroadcastAddress());
        network = Inet4Address.getByName(subnetUtils.getInfo().getNetworkAddress());
        netmask = Inet4Address.getByName(subnetUtils.getInfo().getNetmask());
        lowAddress = Inet4Address.getByName(subnetUtils.getInfo().getLowAddress());
        highAddress = Inet4Address.getByName(subnetUtils.getInfo().getHighAddress());

        System.out.println("Subnet details:");
        System.out.println("Network address: " + network);
        System.out.println("Netmask: " + netmask);
        System.out.println("Broadcast: " + broadcast);
        System.out.println("LowAddress: " + lowAddress);
        System.out.println("HighAddress: " + highAddress);

        System.out.println("This subnet has "
                + subnetUtils.getInfo().getAddressCount()
                + " addresses.");
        System.out.println("Converting "
                + subnetUtils.getInfo().getAddressCount()
                + " adresses to NetworkDevices.");

        //get addresses and convert them to NetworkDevice class type
        String[] allAddresses = subnetUtils.getInfo().getAllAddresses();
        long startTime = System.currentTimeMillis();
        for (i = 0; i < subnetUtils.getInfo().getAddressCount(); i++) {
            networkDevices.add(new NetworkDevice(InetAddress.getByName(allAddresses[i])));

        }
        long endTime = System.currentTimeMillis();
        System.out.println("That took " + (endTime - startTime) + " milliseconds");

        Iterator subnetIpIterator = subnetIpList.iterator();
        //</editor-fold>

        // <editor-fold defaultstate="collapsed" desc="Create the PDU object">
        // Create the PDU object
        pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oidValue)));
        pdu.setType(PDU.GET);
        pdu.setRequestID(new Integer32(1));

        networkDevices.stream().map((_item) -> {
            return _item;
        }).forEach((NetworkDevice _item) -> {
            try {
                if (_item.isReachable()) {
                    System.out.println(_item.getIpAddress() + " is reachable");
                    reachableDevices++;
                    _item.getSnmpDemo();
                } else {
                    System.out.println(_item.getIpAddress() + " is not reachable");
                }
            } catch (IOException ex) {
                Logger.getLogger(SNMPNetworkBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        System.out.println("Found "
                + reachableDevices
                + " reachable devices:");

        networkDevices.stream().map((_item) -> {
            return _item;
        }).forEach((NetworkDevice _item) -> {
            if (_item.getReachableStatus()){
                System.out.println(_item.getIpAddress());
            }
        });
    }
}