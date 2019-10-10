package server.main;

import server.Server;

public class ServerMain {

	public static void main(String[] args) {
		Server server;
		if(args.length != 2){
			System.err.println("Uso errado");
		}
		
		int port = Integer.parseInt(args[0]);
		int role = Integer.parseInt(args[1]);
		
		try{
			server = new Server();
			server.startServer(port, role);
			System.out.println("Server estah a escutar no porto " + args[0]);
		}catch(Exception e){
			System.err.println(e.getMessage());
		
		}

		
	}

}
