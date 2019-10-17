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
	
	public ServerService(int port, int role) throws RemoteException {
		server = new Server(port,role);
	}
	
	public String request(String s, int id) throws RemoteException{
		return server.request(s, id);
	}

	@Override
	public boolean AppendEntriesRPC(int term, int leaderID, int prevLogIndex, int prevLogTerm, String entry,
			int leaderCommit) throws RemoteException {
		
		return server.receiveAppendEntry(term,leaderID, prevLogIndex, prevLogTerm, entry, leaderCommit);
	}

	@Override
	public int getPrevLogIndex() throws RemoteException {
		
		return server.getPrevLogIndex();
	}
	
	public void run() {
		server.run();
	}
}
