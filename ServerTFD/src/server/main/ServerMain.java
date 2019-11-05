package server.main;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import server.constants.Constants;
import server.service.ServerService;

public class ServerMain {

	public static void main(String[] args) {
		ServerService server;
		if(args.length != 2){
			System.err.println("Uso errado");
		}
		
		int port = Integer.parseInt(args[0]);
		
//		if(!checkPortValue(port)) {
//			System.err.println("Porto errado, use um destes:");
//			Constants.PORTS_FOR_SERVER_REGISTRIES.iterator().forEachRemaining(p -> System.out.println(p));
//		}
		
		int role = Integer.parseInt(args[1]);
		
		try{
			server = new ServerService(port, role);
			
			Registry reg = LocateRegistry.createRegistry(port);
			
			reg.rebind(Constants.ADDRESS, server);
			
			System.out.println("Vai criar registry");
			
			System.out.println("Server estah a escutar no porto " + args[0]);
			
			server.run();
			
		}catch(Exception e){
			System.err.println(e.getMessage());
		
		}
	
	}

	private static boolean checkPortValue(int port) {
		
		return Constants.PORTS_FOR_SERVER_REGISTRIES.contains(port);
		
	}
	
}
