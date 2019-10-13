package server.service;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServerService extends Remote {
	
	public String request(String s, int id) throws RemoteException;

	public String AppendEntriesRPC(int term, int leaderID, int prevLogIndex, int prevLogTerm,
			String[] entries, int leaderCommit) throws RemoteException;
}
