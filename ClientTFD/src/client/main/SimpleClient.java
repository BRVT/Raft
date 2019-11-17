package client.main;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

import server.service.IServerService;

public class SimpleClient {

	private static final String folRes = "&";



	public static void main(String[] args) {
		IServerService simple;

		try {
			//escolher porto random
			Random r = new Random();
			
			int index = r.nextInt(5);
			
			int[] ports = {1234,1235,1236,1237,1238};
			
			
			simple = locateAux(ports[index], "rmi://localhost/server");
			String uniqueID = UUID.randomUUID().toString().split("-")[0];

			
			Scanner s = new Scanner(System.in);

			System.out.println("Insira String: ");
			String request = s.nextLine();
			int id = 0;
//			String enviar = args[0];
//			
//			for (int i = 0; i < 5; i++) {
//				System.out.println(simple.request( uniqueID+"|"+id + "_" +args[0]+String.valueOf(i), i));
//			}
//			
			
			while(!request.equals("quit")) {
				String teste = uniqueID+"|"+id + "_" +request;
				
				
				String reply = simple.request(teste,id);
				String[] array = reply.split(" ");
				if(array[0].equals(folRes)) {
					
					
					simple = locateAux(Integer.parseInt(array[1]), "rmi://localhost/server");
					
					reply = simple.request(teste, id);
				}
				
				System.out.println("Resposta : \n"+reply);
				System.out.println("Insira String: ");
				
				id++;
				request = s.nextLine();
			}
			s.close();
		}catch (Exception e) {
			System.err.print("Morreu" + e.getMessage());
		}
	}

	public static IServerService locateAux(int port, String name) throws RemoteException, NotBoundException {
		Registry reg = LocateRegistry.getRegistry(port);
		return (IServerService) reg.lookup(name);
	}
}
