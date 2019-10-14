package client.main;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import server.service.IServerService;

public class SimpleClient {

	private static final String folRes = "&";



	public static void main(String[] args) {
		IServerService simple;

		try {

			simple = locateAux(1234, "rmi://localhost/server");
			String uniqueID = UUID.randomUUID().toString().split("-")[0];

			Scanner s = new Scanner(System.in);

			System.out.println("Insira String: ");
			String request = s.nextLine();
			int id = 0;
			while(request != "quit") {
				String teste = uniqueID+"|"+id + "_" +request;
				
				
				String reply = simple.request(teste,id);
				String[] array = reply.split(" ");
				if(array[0].equals(folRes)) {
					
					
					simple = locateAux(Integer.parseInt(array[1]), "rmi://localhost/server");
					
					reply = simple.request(request, id);
				}
				
				System.out.println("Resposta : \n"+reply);
				System.out.println("Insira String: ");
				
				id++;
				
				
				request = s.nextLine();
			}
			s.close();
		}catch (Exception e) {

			System.err.print(e.getMessage());

		}


	}



	public static IServerService locateAux(int port, String name) throws RemoteException, NotBoundException {

		Registry reg = LocateRegistry.getRegistry(port);

		return (IServerService) reg.lookup(name);

	}


}
