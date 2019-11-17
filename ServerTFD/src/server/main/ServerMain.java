package server.main;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import server.constants.Constants;
import server.service.ServerService;

public class ServerMain {

	public static void main(String[] args) {
		ServerService server;
//		if(args.length != 1){
//			System.err.println("Uso errado");
//			System.exit(0);
//		}
		
		int port = Integer.parseInt(args[0]);
		
		try{
			if(args.length == 1) {
				server = new ServerService(port);
			}else {
				server = new ServerService(port, Integer.parseInt(args[1]));
			}
			
			
			Registry reg = LocateRegistry.createRegistry(port);
			
			reg.rebind(Constants.ADDRESS, server);
			
			System.out.println("Server estah a escutar no porto " + args[0]);
			
			server.run();
			
		}catch(Exception e){
			System.err.println(e.getMessage());
		
		}
	
	}
}
