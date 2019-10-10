import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Scanner;

import server.IServer;

public class SimpleClient {

	public static void main(String[] args) {
		IServer simple;
		Scanner s = new Scanner(System.in);
		
		System.out.println("Insira String: ");
		String request = s.nextLine();
		try {
				
			simple = (IServer) Naming.lookup("rmi://localhost/server");
			
			
			int id = 0;
			String reply = simple.request(request, id);
			System.out.println("Resposta : \n"+reply);
			
		}catch (RemoteException e) {
			
			System.err.print(e.getMessage());
			
		}catch(MalformedURLException e) {
			
			System.err.print(e.getMessage());
			
		}catch( NotBoundException e) {
			
			System.err.print(e.getMessage());
			
		}
	}
	
	

}
