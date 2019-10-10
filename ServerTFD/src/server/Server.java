package server;


import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import enums.STATE;

public class Server extends UnicastRemoteObject implements IServer {
	
	private static String address;
	private static int port;
	private STATE state;
	private int term;
	private int id;
	
	private static final long serialVersionUID = -7438376157658248593L;
	private static final String HOSTNAME_PROPERTY = "java.rmi.server.hostname";
	private static final String REGISTRY_NAME = "rmi://" + address + "/server" + String.valueOf(port);
	
	
	/**
	 * 
	 */
	public Server() throws RemoteException{
		//does nothing
		
	}
	
	public void startServer(int port, int role) {
	
        try {
        	this.port = port;
        	
        	address = System.getProperty("java.rmi.server.hostname");
        	
            Registry registry = LocateRegistry.createRegistry(port);
            
            registry.rebind(REGISTRY_NAME, this);
            
            state = STATE.values()[role];
            
        } catch (Exception e) {
            System.out.println("Error: can't get inet address.");
        }

	}

	@Override
	public String request(String s, int id) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
}
