package server;


import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

import enums.STATE;
import server.constants.Constants;
import server.service.IServerService;

public class Server implements IServer {
	private static final String ADRESS = "rmi://localhost/server";

	private int port;
	private int leader;

	private STATE state;
	private int term;

	private Timer timer;
	private Timer heartbeat;
	private Timer timer_state;


	private ArrayList<IServerService> servers;


	private ArrayList<Integer> servers_state_off;

	public Server(int port, int role){
		this.port = port;
		this.term = 0;
		this.state = STATE.values()[role];
		this.servers = new ArrayList<IServerService>();
		this.servers_state_off = new ArrayList<Integer>();



		//heartbeat
		if(state.equals(STATE.LEADER)) {

			if(term == 0) {
				addRegistries();

				for (IServerService server : servers) {
					try {

						System.out.println(server.AppendEntriesRPC(0, getPort(), 0, 0, null, 0));
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}

			//timer do server


			//thread do hearbeat
			heartbeat = new Timer();
			heartbeat.schedule(new RemindTaskHeartBeat(),
					0,        //initial delay
					20);  //subsequent rate

			//timer para verificar state da rede
			timer_state = new Timer();
			timer_state.schedule(new ServerStateTask(), 10000);


		}
		else if(state.equals(STATE.FOLLOWER)){
			timer = new Timer();
			timer.schedule(new RemindTask(), 1000);
		}

	}

	class AppendEntry implements Runnable{
		private int prevLogIndex;
		private int prevLogTerm;
		private String[] entries;
		private int leaderCommit;

		private IServerService server;

		public AppendEntry (IServerService server, int prevLogIndex, int prevLogTerm,
				String[] entries, int leaderCommit){

			this.server = server;
			this.prevLogIndex = prevLogIndex;
			this.prevLogTerm = prevLogTerm;
			this.entries = entries;
			this.leaderCommit = leaderCommit;
		}


		public void run() {
			try {
				server.AppendEntriesRPC(prevLogTerm, getPort(), prevLogIndex, prevLogTerm, entries, leaderCommit);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	} 
	class RemindTaskHeartBeat extends TimerTask {

		public void run() {
			sendHeartBeat();
		}

	}

	class RemindTask extends TimerTask {
		public void run() {
			System.out.println("Time's up!");
			timer.cancel(); 

			//time out --> eleicao

			timer = new Timer();
			timer.schedule(new RemindTask(), 5*100);
		}

	}


	class ServerStateTask extends TimerTask {
		public void run() {
			System.out.println("Time's up!");
			timer_state.cancel(); 

			//time out --> eleicao
			if(servers_state_off.size() > 0) {
				for (int i = 0; i < servers_state_off.size(); i++) {
					if(checkRegistry(servers_state_off.get(i))) {
						servers_state_off.remove(i);
					}
				}
			}
			timer_state = new Timer();
			timer_state.schedule(new ServerStateTask(), 10 * 1000);
		}

		public boolean checkRegistry(int port) {
			Registry r;
			try {
				r = LocateRegistry.getRegistry(port);

				servers.add((IServerService) r.lookup(ADRESS));

			} catch (Exception e) {
				return false;
			} 

			return true;
		}

	}

	public void resetTimer() {
		timer.cancel(); 
		timer = new Timer();
		timer.schedule(new RemindTask(), 5*100);
	}


	@Override
	public String request(String s, int id) throws RemoteException {
		if(this.isLeader()) {
			//fazer operaçao como lider
			System.out.println("vou mandar o resultado do request");
			return s;
		}
		else {
			//devolve porto do 
			System.out.println("vou mandar o porto do lider");
			return "& " + String.valueOf(leader);
		}

	}
	public int getPort() {
		return this.port;
	}

	public boolean isLeader() {
		return this.state.equals(STATE.LEADER);

	}

	public void increaseTerm() {
		this.term ++;
	}

	public double newTimer() {
		Random r = new Random();
		double randomValue = 5.0 + (10.0 - 5.0) * r.nextDouble();
		return randomValue;
	}

	public void sendHeartBeat() {

		for (IServerService server : servers) {
			try {

				server.AppendEntriesRPC(term, getPort(), 0, 0, null, 0);
			} catch (Exception e) {

				continue;
			}
		}
	}

	public void addRegistries() {
		for (Integer port : Constants.PORTS_FOR_SERVER_REGISTRIES) {

			if(port != this.port) {
				Registry r = null;
				try {
					r = LocateRegistry.getRegistry(port);

					servers.add((IServerService) r.lookup(ADRESS));

				}catch (Exception e) {
					servers_state_off.add(port);
					continue;
				}


			}


		}
	}

	public String receiveAppendEntry(int term, int leaderID, int prevLogIndex, int prevLogTerm,
			String[] entries, int leaderCommit) {

		if(prevLogTerm == 0 && entries == null && leaderCommit == 0) {
			System.out.println("heartbeat");
			resetTimer();
		}


		this.leader = leaderID;

		return "recebi " + this.port;

	}
}
