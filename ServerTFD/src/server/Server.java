package server;


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
import server.service.IServerService;

public class Server implements IServer {
	private static final String ADDRESS = "rmi://localhost/server";


	private int port;
	private int leader;

	private STATE state;
	private int term;

	private Timer timer;



	private ArrayList<String> pendentEntry;

	private LogEntry log;




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

		this.pendentEntry = new ArrayList<>(); 


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


	}

	public void run() {
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

					answers = new HashMap<>();
				}
			}

		}

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
						System.out.println("Veio entry " + e.toString());
						ArrayList <Entry> array = log.getLastEntriesSince(lastEntry);
						for (Entry entry : array) {
							System.out.println("for each da thread " + entry.toString());
							verify = sendHeartBeat(iServer, entry.toString());
						}


						synchronized(answers) {
							answers.put(portF, verify);
							nAnswers ++;

							if(nAnswers < 4) {

								answers.wait(5000);
								nAnswers = 0;
							}else {
								nAnswers = 0;
								answers.notifyAll();

							}
						}
						this.lastEntry = e;

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

				r = LocateRegistry.getRegistry(portF);
				iServer =  (IServerService) r.lookup(ADDRESS);
				verify = 0;
			}catch (RemoteException | NotBoundException e) {
				verify = 1;

			} 

		}
	} 


	/**
	 * Para o follower verificar se o leader morreu ou nao
	 */
	class RemindTask extends TimerTask {
		public void run() {


			//time out --> eleicao

			timer = new Timer();
			timer.schedule(new RemindTask(), 5*100);
		}

	}



	/**
	 * Faz reset a um timer (?) - devia ser static e receber um timer?
	 */
	public void resetTimer() {

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
	 * flag = 0 -> envia vazio | 1-> envia cenas
	 * Hearbeat enviado pelo leader. Envia AppendEntries vazio, se nao houver requests para enviar.
	 * 0 = correu tudo bem
	 * 1 = ta off
	 * 2 =  term < currentTerm
	 * 3 = log doesnt contain an entry at prevLogIndex whose term matches prevLogTerm
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
		this.leader = leaderID;
		if(entry == null) 
			resetTimer();
		else {
			System.out.println(this.port);
			return log.writeLog(entry.split(":")[1] , this.term, false, entry.split(":")[4] ) ;
		}
		return true;

	}

	public int getPrevLogIndex() {

		return log.getPrevLogIndex();
	}

}
