package server.service;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import server.Server;

public class ServerService extends UnicastRemoteObject implements IServerService {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8238574734864073746L;
	
	private Server server;
	
	public ServerService() throws RemoteException {
		server = new Server();
	}
	
	public String request(String s, int id) throws RemoteException{
		return server.request(s, id);
	}
}
