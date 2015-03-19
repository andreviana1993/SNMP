/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snmptest;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
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

public class SNMPtestMultipleDevices {

    private static InetAddress currentIp;

    private static List<InetAddress> ipList = new ArrayList<InetAddress>();

    private static boolean isReachable;

    private static String ipPrefix = "192.168.1.";

    private static Integer ipN;

    private static String port = "161";

// OID of MIB RFC 1213; Scalar Object = .iso.org.dod.internet.mgmt.mib-2.system.sysDescr.0
    private static String oidValue = ".1.3.6.1.2.1.1.1.0"; // ends with 0 for scalar object

    private static int snmpVersion = SnmpConstants.version2c;

    private static String community = "public";

    public static void main(String[] args) throws Exception {

        int i;
        NetworkInterface networkInterface = NetworkInterface.getByName("wlan0");
        System.out.println("Number of Interface Adresses:" 
                + networkInterface.getInterfaceAddresses().size());
        System.out.println("Searching for an IPv4 Address...");
        for (i = 0; i < networkInterface.getInterfaceAddresses().size();) {
            System.out.println("Interface Address #" 
                    + i);
            System.out.println(networkInterface.getDisplayName() 
                    + " --> "
                    + networkInterface.getInterfaceAddresses().get(i).getAddress() 
                    + "/"
                    + networkInterface.getInterfaceAddresses().get(i).getNetworkPrefixLength());
            if (networkInterface.getInterfaceAddresses().get(i).getAddress().getClass() == Inet6Address.class) {
                i++;
                System.out.println("Found an IPv6 Address. Skipping to the next Interface Address...");
            } else if (networkInterface.getInterfaceAddresses().get(i).getAddress().getClass() == Inet4Address.class) {
                System.out.println("Found an IPv4 Address.");
                System.out.println("Search was successful. Continuing...");
                break;
            }
        }
        
        currentIp = networkInterface.getInterfaceAddresses().get(i).getAddress();

        // Create the PDU object
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oidValue)));
        pdu.setType(PDU.GET);
        pdu.setRequestID(new Integer32(1));

        for (ipN = 1; ipN <= 254; ipN++) {

            InetAddress ipPing = Inet4Address.getByName(ipPrefix + ipN);
            isReachable = ipPing.isReachable(750);
                
            if (isReachable) {
                System.out.println(ipPing + " is reachable");

                ipList.add(ipPing);

                System.out.println("SNMP GET Demo");

                // Create TransportMapping and Listen
                TransportMapping transport = new DefaultUdpTransportMapping();
                transport.listen();
               
// Create Target Address object
                CommunityTarget comtarget = new CommunityTarget();
                comtarget.setCommunity(new OctetString(community));
                comtarget.setVersion(snmpVersion);
                comtarget.setAddress(new UdpAddress(new StringBuilder(ipPing.toString()).deleteCharAt(0).toString() 
                        + "/" 
                        + port));
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
            } else {
                System.out.println(ipPing 
                        + " is not reachable");
            }
        }
        System.out.println("Found " 
                + ipList.size() 
                + " online devices:");
        Iterator iter = ipList.iterator();
        while (iter.hasNext()) {
            System.out.println(iter.next());
        }
    }
}