package server;


import java.rmi.RemoteException;

import enums.STATE;
import server.constants.Constants;

public class Server implements IServer {
	
	private static String address;
	private static int port;
	private static int leader = 1234;
	private STATE state;

	/**
	 * @param role 
	 * 
	 */
	public Server(int role){
			this.state = STATE.values()[role];
	}
	

	@Override
	public String request(String s, int id) throws RemoteException {
		if(this.isLeader()) {
			//fazer operaçao como lider
			System.out.println("vou mandar o resultado do request");
			return s;
		}
		else {
			//devolve porto do 
			System.out.println("vou mandar o porto do lider");
			return "& " + String.valueOf(leader);
		}
		
	}
	public int getPort() {
		return this.port;
	}
	
	public boolean isLeader() {
		return this.state.equals(STATE.LEADER);

	}
}
