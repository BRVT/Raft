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
	
	public ServerService(int role) throws RemoteException {
		server = new Server(role);
	}
	
	public String request(String s, int id) throws RemoteException{
		return server.request(s, id);
	}
}
