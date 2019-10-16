package server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

import DTO.LogEntry;
import enums.STATE;
import server.constants.Constants;
import server.service.IServerService;

public class Server implements IServer {
	private static final String ADDRESS = "rmi://localhost/server";
	private static final int N_SERVERS = 4;

	private int port;
	private int leader;

	private STATE state;
	private int term;

	private Timer timer;
	private Timer heartbeat;
	private Timer timer_state;

	private boolean pendentEntries;
	private ArrayList<String> pendentEntry;

	private LogEntry log;
	
	private ArrayList<Integer> nextIndex;
	private ArrayList<Integer> matchIndex;

	private Map<IServerService, Integer> servers;

	private ArrayList<Integer> servers_state_off;

	public Server(int port, int role){

		this.port = port;
		this.term = 0;
		this.state = STATE.values()[role];

		this.pendentEntries = false;
		this.pendentEntry = new ArrayList<>(); 

		this.servers_state_off = new ArrayList<Integer>();
		this.servers = new HashMap<>();

		this.log = new LogEntry();
		log.createFile(this.port);
		
		//------------------------------------------------------------------
		//Todos deviam ter acesso aos registries+stubs dos outros servers 
		//(pensando no futuro, em caso de eleicao)
		//------------------------------------------------------------------
		addRegistries();
		//------------------------------------------------------------------
		
		if(state.equals(STATE.LEADER)) {
			
			//verificar se nao eh redundante com a criacao do timer heartbeat a seguir
			for (Map.Entry<IServerService, Integer> server : servers.entrySet()) {
				try {
					System.out.println(server.getKey().AppendEntriesRPC(0, getPort(), 0, 0, null, 0));
				} catch (RemoteException e) {
					System.err.println("Erro a enviar primeiro heartbeat para o porto:" + server.getValue());
					continue;
				}
			}
			
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
	
	/**
	 * Vai enviar um heartbeat, para prevenir eleicao e "manter-se vivo"
	 */
	class RemindTaskHeartBeat extends TimerTask {
		public void run() {
			sendHeartBeat();
		}

	}
	
	/**
	 * Para o follower verificar se o leader morreu ou nao
	 */
	class RemindTask extends TimerTask {
		public void run() {
			System.out.println("Time's up!");
			timer.cancel(); 

			//time out --> eleicao

			timer = new Timer();
			timer.schedule(new RemindTask(), 5*100);
		}

	}

	/**
	 * Timer dos servers para verificar se mais alguem voltou a vida desde a ultima verificacao
	 */
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

				servers.put((IServerService) r.lookup(ADDRESS),port);

			} catch (Exception e) {
				return false;
			} 

			return true;
		}

	}
	
	/**
	 * Faz reset a um timer (?) - devia ser static e receber um timer?
	 */
	public void resetTimer() {
		timer.cancel(); 
		timer = new Timer();
		timer.schedule(new RemindTask(), 5*100);
	}

	/**
	 * Funcao partilhada pelo ClientRMI, permite troca de mensagens com o client
	 * e o retorno de respostas.
	 * @return String de resposta, se for o leader, ou o porto do leader, se for um follower
	 */
	public String request(String s, int id) throws RemoteException {
		if(this.isLeader()) {

			log.writeLog(s.split("_")[1] , this.term, false, s.split("_")[0] );

			this.pendentEntry.add(s);
			return s.split("_")[1] ;
		}
		else {
			//devolve porto do leader
			System.out.println("Sou follower, vou enviar o porto do Leader");
			return "& " + String.valueOf(leader);
		}

	}
	
	/**
	 * Getter do porto
	 * @return porto do server 
	 */
	public int getPort() {
		return this.port;
	}
	
	/**
	 * Verificador de leadership
	 * @return true - eh leader false - cc
	 */
	public boolean isLeader() {
		return this.state.equals(STATE.LEADER);

	}
	
	/**
	 * Dah increase ao term do server, apenas usado em eleicoes
	 */
	public void increaseTerm() {
		this.term ++;
	}
	
	/**
	 * Retorna um novo valor para um timer
	 * @return
	 */
	public double newTimer() {
		Random r = new Random();
		double randomValue = 5.0 + (10.0 - 5.0) * r.nextDouble();
		return randomValue;
	}

	/**
	 * Hearbeat enviado pelo leader. Envia AppendEntries vazio, se nao houver requests para enviar.
	 * PRECISA DE SER ALTERADO
	 * @return
	 */
	public boolean sendHeartBeat() {
		int not_written = 0;
		boolean verify;
		
		for (Map.Entry<IServerService, Integer>  server : servers.entrySet()) {
			try {
				if(this.pendentEntry.size()>0) {
					verify = server.getKey().AppendEntriesRPC(term, getPort(), log.getPrevLogIndex(),
							log.getPrevLogTerm(), pendentEntry.get(0), log.getCommitIndex());
					
					if(!verify) {
						server.getKey();
						not_written++;
					}
				}else {
					server.getKey().AppendEntriesRPC(term, getPort(), 0, 0, null, 0);
				}
			} catch (Exception e) {
				servers.remove(server.getKey());
				servers_state_off.add(server.getValue());
				continue;
			}
		}

		if(not_written > (N_SERVERS/2)) {
			return false;
		}else {
			if(pendentEntry.size() >0) {
				commitEntry(pendentEntry.get(0));
				pendentEntry.remove(0);
			}
			return true;
		}


	}
	
	class resendLogEntries implements Runnable {
		private IServerService follower;
		
		public resendLogEntries(IServerService follower) {
			this.follower = follower;
		}
		
		public void run() {
			try {
				int indexFollower = follower.getPrevLogIndex();
				
				List<String> entriesPEnviar = pendentEntry.subList(indexFollower, log.getPrevLogIndex());
				
				for (String string : entriesPEnviar) {
					follower.AppendEntriesRPC(term, getPort(), indexFollower++,
							log.getPrevLogTerm(), string, log.getCommitIndex());
				}
			}catch (RemoteException e) {
				System.err.println("F");
			}
		}
	}
	//classe para dar commit
	public void commitEntry(String s) {
		log.commitEntry(s);
	}
	
	/**
	 * Para cada porto, vai guardar as registries e stubs ativas, para mais tarde comunicar através de JavaRMI
	 */
	private void addRegistries() {
		
		for (Integer port : Constants.PORTS_FOR_SERVER_REGISTRIES) {

			if(port != this.port) {
				Registry r = null;
				
				try {
					r = LocateRegistry.getRegistry(port);
					servers.put((IServerService) r.lookup(ADDRESS),port);

				}catch (Exception e) {
					servers_state_off.add(port);
					continue;
				}
			}
		}
	}
	
	/**
	 * 
	 * @param term
	 * @param leaderID
	 * @param prevLogIndex
	 * @param prevLogTerm
	 * @param entry
	 * @param leaderCommit
	 * @return
	 */
	public boolean receiveAppendEntry(int term, int leaderID, int prevLogIndex, int prevLogTerm,
			String entry, int leaderCommit) {

		if(entry == null) 
			resetTimer();
		else {
			return log.writeLog(entry.split("_")[1] , this.term, false, entry.split("_")[0] ) ;
		}
		return true;

	}

	public int getPrevLogIndex() {
		
		return log.getPrevLogIndex();
	}
}
