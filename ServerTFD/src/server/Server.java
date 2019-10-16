package server;

import java.lang.reflect.Array;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import DTO.LogEntry;
import DTO.LogEntry.Entry;
import enums.STATE;
import server.constants.Constants;
import server.domain.Entrada;
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

	private FollowerCommunication first;
	private FollowerCommunication second;
	private FollowerCommunication third;
	private FollowerCommunication fourth;


	private Map<Integer, Integer> answers;
	private int nAnswers;

	public Server(int port, int role){

		this.port = port;
		this.term = 0;
		this.state = STATE.values()[role];

		this.pendentEntries = false;
		this.pendentEntry = new ArrayList<>(); 

		this.servers_state_off = new ArrayList<Integer>();
		this.servers = new HashMap<>();

		this.answers =  new HashMap<>();
		this.log = new LogEntry();
		log.createFile(this.port);
		this.nAnswers = 0;
		//------------------------------------------------------------------
		//Todos deviam ter acesso aos registries+stubs dos outros servers 
		//(pensando no futuro, em caso de eleicao)
		//------------------------------------------------------------------
		//addRegistries();
		//------------------------------------------------------------------

		if(state.equals(STATE.LEADER)) {
			leaderWork();

		}
		else if(state.equals(STATE.FOLLOWER)){
			timer = new Timer();
			timer.schedule(new RemindTask(), 1000);
		}
	}
	public void leaderWork() {

		CountDownLatch latch = new CountDownLatch(2); 

		ArrayList<Integer> ports = new ArrayList<>();

		for (Integer integer : Constants.PORTS_FOR_SERVER_REGISTRIES) {
			if(integer != this.port)
				ports.add(integer);
		}
		first = new FollowerCommunication(5000, latch,  
				ports.get(0)); 


		second = new FollowerCommunication(5000, latch,  
				ports.get(1)); 
		third = new FollowerCommunication(5000, latch,  
				ports.get(2)); 
		fourth = new FollowerCommunication(5000, latch,  
				ports.get(3)); 

		first.start();
		second.start();
		third.start();
		fourth.start();
		
		while(true) {
			synchronized (answers) {
				if(answers.size() >= 2){
					int count = 0;
					for (Integer i : answers.values()) {
						if(i == 0) count ++;
					}
					if(count >= 2) log.commitEntry();
				}
			}

		}
		//verificar se nao eh redundante com a criacao do timer heartbeat a seguir
		//		for (Map.Entry<IServerService, Integer> server : servers.entrySet()) {
		//			try {
		//				System.out.println(server.getKey().AppendEntriesRPC(0, getPort(), 0, 0, null, 0));
		//			} catch (RemoteException e) {
		//				System.err.println("Erro a enviar primeiro heartbeat para o porto:" + server.getValue());
		//				continue;
		//			}
		//		}

		//		//thread do hearbeat
		//		heartbeat = new Timer();
		//		heartbeat.schedule(new RemindTaskHeartBeat(),
		//				0,        //initial delay
		//				1000);  //subsequent rate

		//		//timer para verificar state da rede
		//		timer_state = new Timer();
		//		timer_state.schedule(new ServerStateTask(), 10000);

	}



	class FollowerCommunication extends Thread
	{ 
		//dealy que vai ser para retentar comunicao
		private int delay; 
		private CountDownLatch latch; 
		private Registry r;
		private IServerService iServer;
		private int portF;
		private int verify;
		private Entry lastEntry;
		public FollowerCommunication(int delay, CountDownLatch latch, 
				int port) 
		{ 
			this.portF = port;
			this.lastEntry = log.getLastEntry();
			connect();

			this.delay = delay; 
			this.latch = latch; 
		} 

		@Override
		public void run() 
		{ 
			try
			{ 
				while(true) {
					while(verify == 1) {
						Thread.sleep(delay); 
						connect();
					}

					Thread.sleep(1000);
					Entry e = log.getLastEntry();
					
					if( e == (null) || e.equals(lastEntry) ){
						verify = sendHeartBeat(iServer, null);
					}else {
						verify = sendHeartBeat(iServer, e.toString());
						
						synchronized(answers) {
							answers.put(portF, verify);
							nAnswers ++;
						}
						if(nAnswers < 4) {
							this.wait(5000);
							nAnswers = 0;
						}else {
							nAnswers = 0;
							this.notifyAll();

						}
					}



				}


			} 
			catch (InterruptedException e) 
			{ 
				e.printStackTrace(); 
			} 
		}

		public void connect() {
			try {
				System.out.println("ENTROU CRL " + this.portF);
				r = LocateRegistry.getRegistry(portF);
				iServer =  (IServerService) r.lookup(ADDRESS);
				verify = 0;
			}catch (RemoteException | NotBoundException e) {
				verify = 1;

			} 

		}
	} 

	/**
	 * Vai enviar um heartbeat, para prevenir eleicao e "manter-se vivo"
	 */
	//	class RemindTaskHeartBeat extends TimerTask {
	//		public void run() {
	//			sendHeartBeat();
	//		}
	//
	//	}

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
			synchronized(s){
				log.writeLog(s.split("_")[1] , this.term, false, s.split("_")[0] );
			}


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
	 * flag = 0 -> envia vazio | 1-> envia cenas
	 * Hearbeat enviado pelo leader. Envia AppendEntries vazio, se nao houver requests para enviar.
	 * 0 = correu tudo bem
	 * 1 = ta off
	 * 2 =  term < currentTerm
	 * 3 = log doesn’t contain an entry at prevLogIndex whose term matches prevLogTerm
	 * @return
	 */
	public int sendHeartBeat(IServerService server, String entry) {

		try {
			server.AppendEntriesRPC(term, getPort(), log.getPrevLogIndex(),
					log.getPrevLogTerm(), entry, log.getCommitIndex());
		} catch (RemoteException e1) {
			return 1;
		}




		return 0;

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
//	//classe para dar commit
//	public void commitEntry(String s) {
//		log.commitEntry(s);
//	}

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
