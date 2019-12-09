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
	private static final String uniqueID = UUID.randomUUID().toString().split("-")[0];
	private static final String folRes = "&";
	private static IServerService simple;


	public static void main(String[] args) {


		try {
			//escolher porto random
			Random r = new Random();

			int index = r.nextInt(5);

			int[] ports = {1234,1235,1236,1237,1238};


			simple = locateAux(ports[index], "rmi://localhost/server");


			Scanner s = new Scanner(System.in);

			System.out.println("Insira String: ");
			String request = s.nextLine();
			int id = 0;			
			String operation = "";
			while(!request.equals("quit")) {

				switch ( operation = request.split(" ")[0]) {
				case "put":
					if(request.split(" ").length == 3) {
						String key = request.split(" ")[1];
						String value = request.split(" ")[2];
						doRequest("p:"+key+":"+value,id);
					}else {
						System.out.println("Insira no formato put <key> <value>");
					}
					break;
				case "get":
					if(request.split(" ").length == 2) {
						String key = request.split(" ")[1];
						doRequest("g:"+key,id);
					}else {
						System.out.println("Insira no formato get <key>");
					}
					break;

				case "del":
					if(request.split(" ").length == 2) {
						String key = request.split(" ")[1];
						doRequest("d:"+key,id);
					}else {
						System.out.println("Insira no formato del <key>");
					}
					break;

				case "list":
					doRequest("l:",id);
					break;

				case "cas":
					//?????????
					break;

				default:
					System.out.println("Insira um comando válido!");
					break;
				}

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

	public static void doRequest(String input, int id) {
		String teste = uniqueID+"|"+ id + "_" + input;

		try {
			String reply = simple.request(teste,id);
			String[] array = reply.split(" ");
			if(array[0].equals(folRes)) {


				simple = locateAux(Integer.parseInt(array[1]), "rmi://localhost/server");

				reply = simple.request(teste, id);
			}

			System.out.println("Resposta : \n"+reply);

		}catch(Exception e) {
			System.err.print("Morreu" + e.getMessage());
		}


	}
}
