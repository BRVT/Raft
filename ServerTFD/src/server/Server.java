package server;


import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import enums.STATE;
import server.constants.Constants;

public class Server implements IServer {
	
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
	public Server(){
			//5 threads ou 5 servers????
	}
	
//	public void startServer(int port, int role) {
//	
//        try {
//        	this.port = port;
//            IServer serv = this;
//        	Naming.rebind("rmi://localhost/server", serv);
//        	
//        	
//            state = STATE.values()[role];
//            //depois de aceitar request o server vai ver se eh lider
//            //se sim executa operacao Request
//            //se nao manda o porto do lider( atraves dum get port que existe na classe)
//        } catch (Exception e) {
//            System.out.println("Error: can't get inet address " + e.getMessage());
//        }
//
//	}

	@Override
	public String request(String s, int id) throws RemoteException {
		if(this.isLeader()) {
			//fazer operaçao como lider
			return s;
		}
		else {
			for(Integer port : Constants.PORTS_FOR_SERVER_REGISTRIES) {
				//nao sei se eh aqui se no raft library
				return String.valueOf(this.port);
			}
		}
		return s;
		
	}
	public int getPort() {
		return this.port;
	}
	
	public boolean isLeader() {
		return this.state.equals(STATE.LEADER);

	}
}
