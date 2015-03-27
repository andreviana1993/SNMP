/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snmpnetworkbrowser;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Vector;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 *
 * @author root
 */
public class NetworkDevice {

    private InetAddress ipAddress;
    private HashMap<String, Vector<? extends VariableBinding>> snmpResponse;
    private Boolean isReachable;
    //placeholders:
    private int ifNumber;

    public NetworkDevice(InetAddress ipAddress) {
        this.snmpResponse = new HashMap<>();
        this.ipAddress = ipAddress;
    }
    
    public Boolean isReachable() throws IOException {
        isReachable = ipAddress.isReachable(500);
        return isReachable;
    }
    
    public Boolean getReachableStatus(){
        return isReachable;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public Vector<? extends VariableBinding> getSnmpResponse(String oidValue) {
        return snmpResponse.get(oidValue);
    }
    
    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }
 
    public void setSnmpResponse(String oidValue, Vector<? extends VariableBinding> response) {
        this.snmpResponse.put(oidValue, response);
    }

    public void getSnmpDemo() throws IOException {
        System.out.println("SNMP GET Demo");
        long startTime = System.currentTimeMillis();
        // Create TransportMapping and Listen
        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();

// Create Target Address object
        CommunityTarget comtarget = new CommunityTarget();
        comtarget.setCommunity(new OctetString(SNMPNetworkBrowser.community));
        comtarget.setVersion(SNMPNetworkBrowser.snmpVersion);
        comtarget.setAddress(new UdpAddress(new StringBuilder(ipAddress.toString()).deleteCharAt(0).toString()
                + "/"
                + SNMPNetworkBrowser.snmpPort));
        comtarget.setRetries(0);
        comtarget.setTimeout(500);

// Create Snmp object for sending data to Agent
        Snmp snmp = new Snmp(transport);

        System.out.println("Sending Request to " + comtarget.getAddress());
        ResponseEvent response = snmp.get(SNMPNetworkBrowser.pdu, comtarget);

// Process Agent Response
        if (response != null) {
            System.out.println("Got Response from Agent");
            PDU responsePDU = response.getResponse();

            if (responsePDU != null) {
                int errorStatus = responsePDU.getErrorStatus();
                int errorIndex = responsePDU.getErrorIndex();
                String errorStatusText = responsePDU.getErrorStatusText();

                if (errorStatus == PDU.noError) {
                    System.out.println("Snmp Get Response = " + responsePDU.getVariableBindings());
                    snmpResponse.put(SNMPNetworkBrowser.oidValue, responsePDU.getVariableBindings());
                    System.out.println("Snmp Get Response (CLASS TYPE): "
                            + responsePDU.getVariableBindings().getClass());
                    System.out.println(this.getSnmpResponse(SNMPNetworkBrowser.oidValue));
                } else {
                    System.out.println("Error: Request Failed");
                    System.out.println("Error Status = " + errorStatus);
                    System.out.println("Error Index = " + errorIndex);
                    System.out.println("Error Status Text = " + errorStatusText);
                }
            } else {
                System.out.println("Error: Response PDU is null");
            }
        } else {
            System.out.println("Error: Agent Timeout... ");
        }

        snmp.close();
        long endTime = System.currentTimeMillis();
        System.out.println("That took " + (endTime - startTime) + " milliseconds");
    }
}
