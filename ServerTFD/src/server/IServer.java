package server;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServer extends Remote{
	
	public String request(String s, int id) throws RemoteException;
}
