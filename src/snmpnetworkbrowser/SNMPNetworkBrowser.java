package snmpnetworkbrowser;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.net.util.SubnetUtils;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SNMPNetworkBrowser {

    // <editor-fold defaultstate="collapsed" desc="variable declarations">
    //network related declarations
    private static InetAddress currentIp;
    private static InetAddress destIp;
    private static List<InetAddress> reachableIpList = new ArrayList<>();
    private static InetAddress netmask, network, broadcast;
    private static SubnetUtils subnetUtils;
    private static List<InetAddress> subnetIpList = new ArrayList<>();

    //SNMP related declarations
    // OID of MIB RFC 1213; Scalar Object = .iso.org.dod.internet.mgmt.mib-2.system.sysDescr.0
    private static String oidValue = ".1.3.6.1.2.1.1.1.0"; // ends with 0 for scalar object
    private static int snmpVersion = SnmpConstants.version2c;
    private static String community = "public";
    private static String snmpPort = "161";
    
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
        subnetUtils = new SubnetUtils(new StringBuilder(currentIp.toString()).deleteCharAt(0).toString()
                + "/"
                + networkInterface.getInterfaceAddresses().get(i).getNetworkPrefixLength());
        broadcast = Inet4Address.getByName(subnetUtils.getInfo().getBroadcastAddress());
        network = Inet4Address.getByName(subnetUtils.getInfo().getNetworkAddress());
        netmask = Inet4Address.getByName(subnetUtils.getInfo().getNetmask());

        System.out.println("This subnet has "
                + subnetUtils.getInfo().getAddressCount()
                + " addresses.");
        System.out.println("Converting "
                + subnetUtils.getInfo().getAddressCount()
                + " adresses to InetAddress types.");

        //get addresses and convert them to InetAddress type
        String[] allAddresses = subnetUtils.getInfo().getAllAddresses();
        long startTime = System.currentTimeMillis();
        for (i = 0; i < subnetUtils.getInfo().getAddressCount(); i++) {
            subnetIpList.add(InetAddress.getByName(allAddresses[i]));
        }
        long endTime = System.currentTimeMillis();
        System.out.println("That took " + (endTime - startTime) + " milliseconds");
        
        Iterator subnetIpIterator = subnetIpList.iterator();
        //</editor-fold>

        // <editor-fold defaultstate="collapsed" desc="Create the PDU object">
        // Create the PDU object
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oidValue)));
        pdu.setType(PDU.GET);
        pdu.setRequestID(new Integer32(1));
        //</editor-fold>

        while (subnetIpIterator.hasNext()) {

            destIp = (InetAddress) subnetIpIterator.next();
            
            //ping test
            if (destIp.isReachable(500)) {
                System.out.println(destIp + " is reachable");

                reachableIpList.add(destIp);

                // <editor-fold defaultstate="collapsed" desc="SNMP GET DEMO">
                
                System.out.println("SNMP GET Demo");
                startTime = System.currentTimeMillis();
                // Create TransportMapping and Listen
                TransportMapping transport = new DefaultUdpTransportMapping();
                transport.listen();

// Create Target Address object
                CommunityTarget comtarget = new CommunityTarget();
                comtarget.setCommunity(new OctetString(community));
                comtarget.setVersion(snmpVersion);
                comtarget.setAddress(new UdpAddress(new StringBuilder(destIp.toString()).deleteCharAt(0).toString()
                        + "/"
                        + snmpPort));
                comtarget.setRetries(0);
                comtarget.setTimeout(500);

// Create Snmp object for sending data to Agent
                Snmp snmp = new Snmp(transport);

                System.out.println("Sending Request to Agent "
                        + comtarget.getAddress());
                ResponseEvent response = snmp.get(pdu, comtarget);

// Process Agent Response
                if (response != null) {
                    System.out.println("Got Response from Agent");
                    PDU responsePDU = response.getResponse();

                    if (responsePDU != null) {
                        int errorStatus = responsePDU.getErrorStatus();
                        int errorIndex = responsePDU.getErrorIndex();
                        String errorStatusText = responsePDU.getErrorStatusText();

                        if (errorStatus == PDU.noError) {
                            System.out.println("Snmp Get Response = "
                                    + responsePDU.getVariableBindings());
                        } else {
                            System.out.println("Error: Request Failed");
                            System.out.println("Error Status = "
                                    + errorStatus);
                            System.out.println("Error Index = "
                                    + errorIndex);
                            System.out.println("Error Status Text = "
                                    + errorStatusText);
                        }
                    } else {
                        System.out.println("Error: Response PDU is null");
                    }
                } else {
                    System.out.println("Error: Agent Timeout... ");
                }

                snmp.close();
                endTime = System.currentTimeMillis();
                System.out.println("That took " + (endTime - startTime) + " milliseconds");
                //</editor-fold>
               
            } else {
                System.out.println(destIp
                        + " is not reachable");
            }
        }
        System.out.println("Found "
                + reachableIpList.size()
                + " online devices:");
        Iterator ipListIterator = reachableIpList.iterator();
        while (ipListIterator.hasNext()) {
            System.out.println(ipListIterator.next());
        }
    }
}
