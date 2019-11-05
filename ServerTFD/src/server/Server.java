package server;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import domain.LogEntry;
import domain.LogEntry.Entry;
import enums.STATE;
import server.constants.Constants;
import server.service.IServerService;

public class Server implements IServer {
	
	private int port;
	private int leaderPort;
	private STATE state;
	private int term;
	private int votedFor;

	private Timer timer;

	private ArrayList<String> pendentEntry = new ArrayList<>();
	private LogEntry log = new LogEntry();
	private Map<Integer, Integer> answers = new HashMap<>();

	private FollowerCommunication first;
	private FollowerCommunication second;
	private FollowerCommunication third;
	private FollowerCommunication fourth;
	
	private List<FollowerCommunication> followers = Arrays.asList(first,second,third,fourth);

	private int nAnswers;

	public Server(int port){

		this.port = port;
		this.term = 0;
		this.state = STATE.FOLLOWER;

		log.createFile(this.port);
	}

	/**
	 * Inicializa o server ---REVER---
	 */
	public void run() {
		if(state.equals(STATE.LEADER)) {
			leaderWork();

		}
		else if(state.equals(STATE.FOLLOWER)){
			Random r = new Random();
			int i = r.nextInt(4) + 2;
			
			timer = new Timer();
			timer.schedule(new RemindTask(this), i*1000);
			
			//como eh que o RemindTask vai saber e atualizar os atributos? passamos o this no construtor?
			//
		}

	}
	
	/**
	 * Funcao que contem o bulk do trabalho realizado pelo Leader
	 */
	@SuppressWarnings("unused")
	public void leaderWork() {

		CountDownLatch latch = new CountDownLatch(2); 

		ArrayList<Integer> ports = new ArrayList<>();

		for (Integer integer : Constants.PORTS_FOR_SERVER_REGISTRIES) {
			if(integer != this.port)
				ports.add(integer);
		}
		
		int j = 0;
		for (FollowerCommunication f : followers) {
			
			f = new FollowerCommunication(5000, latch,  
					ports.get(j));
			j++;
		}
		
		for (FollowerCommunication f : followers) {
			f.start();
		}

		while(true) {
			synchronized (answers) {
				if(answers.size() >= 2){
					int count = 0;
					for (Integer i : answers.values()) {
						if(i == 0) count ++;
					}
					if(count >= 2) {
						log.commitEntry();
					}
					
					answers = new HashMap<>();
				}
			}
		}
	}

	/**
	 * Faz reset a um timer (?) - devia ser static e receber um timer?
	 */
	public void resetTimer() {

		timer = new Timer();
		timer.schedule(new RemindTask(this), 5*100);
	}
	
	public void voteFor(int id) {
		this.votedFor = id;
	}
	
	public void cleanVote() {
		this.votedFor = 0;
	}
	
	public void changeState(STATE state) {
		
		this.state = state;
	}
	
	public void increaseTerm() {
		this.term ++;
	}

	public int getPrevLogIndex() {
		return log.getPrevLogIndex();
	}
	
	public int getPort() {
		return this.port;
	}
	
	public int getLeaderPort() {
		return leaderPort;
	}

	public STATE getState() {
		return state;
	}

	public int getTerm() {
		return term;
	}

	public int getVotedFor() {
		return votedFor;
	}
	
	public boolean isLeader() {
		return this.state.equals(STATE.LEADER);
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
			return "& " + String.valueOf(leaderPort);
		}

	}

	/**
	 * Hearbeat enviado pelo leader. Envia AppendEntries vazio, se nao houver requests para enviar.
	 * 0 = correu tudo bem
	 * 1 = ta off
	 * 2 =  term < currentTerm
	 * 3 = log doesnt contain an entry at prevLogIndex whose term matches prevLogTerm
	 * @param server - stub do follower
	 * @param entry - entry a ser enviada
	 * @param election - 0 para enviar AppendEntriesRPC, 1 para enviar RequestVoteRPC
	 * @return
	 */
	public int sendHeartBeat(IServerService server, String entry, int election) {
		try {
			if(election == 0) {
				server.AppendEntriesRPC(term, getPort(), log.getPrevLogIndex(),
						log.getPrevLogTerm(), entry, log.getCommitIndex());
			}else if(election == 1) {
				server.RequestVoteRPC(getTerm(), getPort(), log.getPrevLogIndex(), log.getPrevLogTerm());
			}
			
		} catch (RemoteException e1) {
			return 1;
		}
		return 0;

	}
  

	/**
	 * Recebe AppendEntriesRPC do Leader
	 * @param term - termo do Leader
	 * @param leaderID - id do Leader (porto)
	 * @param prevLogIndex - prevLogIndex do Leader
	 * @param prevLogTerm - prevLogTerm do Leader
	 * @param entry - entry do Leader
	 * @param leaderCommit - ultimo commit do Leader
	 * @return true se tudo correu bem || false se ocorreu uma falha
	 */
	public boolean receiveAppendEntry(int term, int leaderID, int prevLogIndex, int prevLogTerm, String entry, int leaderCommit) {
		this.leaderPort = leaderID;
		if(entry == null) 
			resetTimer();
		else {
			System.out.println(this.port);
			return log.writeLog(entry.split(":")[1] , this.term, false, entry.split(":")[4] ) ;
		}
		return true;
	}
	
	/**
	 * Recebe RequestVoteRPC de um Candidate
	 * @param term - termo do candidato
	 * @param id - id do candidato (porto)
	 * @param prevLogIndex - prevLogIndex do candidato
	 * @param prevLogTerm - prevLogTerm do candidato
	 * @return true se concorda com nova leadership, false se nao concorda
	 */
	public boolean receiveRequestVote(int term, int id, int prevLogIndex, int prevLogTerm) {
		//ler paper para saber o que fazer aqui
		return true;
	}
	
	/**
	 * Para o leader comunicar com os followers
	 */
	class FollowerCommunication extends Thread{ 
		//dealy que vai ser para retentar comunicao
		private int delay; 
		private CountDownLatch latch; 
		private Registry r;
		private IServerService iServer;
		private int portF;
		private int verify;
		private Entry lastEntry;
		
		public FollowerCommunication(int delay, CountDownLatch latch, int port) { 
			this.portF = port;
			this.lastEntry = log.getLastEntry();
			connect();

			this.delay = delay; 
			this.latch = latch; 
		} 

		@Override
		public void run(){ 
			try{ 
				while(true) {
					while(verify == 1) {
						Thread.sleep(delay); 
						connect();
					}
					Thread.sleep(1000);
					Entry e = log.getLastEntry();


					if( e == (null) || e.equals(lastEntry) ){
						verify = sendHeartBeat(iServer, null, 0);
					}else {
						System.out.println("Veio entry " + e.toString());
						ArrayList <Entry> array = log.getLastEntriesSince(lastEntry);
						for (Entry entry : array) {
							System.out.println("for each da thread " + entry.toString());
							verify = sendHeartBeat(iServer, entry.toString(), 0);
						}
						
						//Verificar isto!
						//O verify nao verifica todas as entries enviadas
						//Ex: a Entry 1 da verify 1 (erro) e a entry 2 dah fixe, logo dah commit ah 1 na mesma?
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
			catch (InterruptedException e){ 
				e.printStackTrace(); 
			} 
		}

		public void connect() {
			try {
				r = LocateRegistry.getRegistry(portF);
				iServer =  (IServerService) r.lookup(Constants.ADDRESS);
				verify = 0;
			}catch (RemoteException | NotBoundException e) {
				verify = 1;
			} 
		}
	} 

	/**
	 * Para o follower verificar se o leader morreu ou nao, e comecar eleicao
	 */
	class RemindTask extends TimerTask {
		
		private Server server;
		private ArrayList<Integer> trueResponses = new ArrayList<>();
		
		public RemindTask(Server server) {
			this.server = server;
		}

		public void run() {
			timer.cancel();
			
			//increases term -> this.term++;
			server.increaseTerm();
			
			//changes state -> state = STATE.CANDIDATE;
			server.changeState(STATE.CANDIDATE);
			
			//changes votedFor -> votedFor = this.id;
			server.voteFor(server.getPort());
			
			//starts election timer
			//Timer dentro de timer?? Metemos o CASO 3 noutro timer?
			
			//sends RequestVoteRPC -> RequestVoteRPC(this.term, this.id, this.lasLogIndex, this.lastLogTerm)
			//thread para isto??
			
			//waits responses
			//copiar o synchronized do FollowerCommunication
			
			//count das responses true
			
			//CASO 1
			//if #responses > 2
			if(trueResponses.size() > Constants.MAJORITY) {
				//changes state -> state = STATE.LEADER
				server.changeState(STATE.LEADER);
				//sends heartbeats -> leaderwork();
				server.leaderWork();
			}
				
			//CASO 2
			//receives AppendEntriesRPC from another server
			//How to check this??
			
			//if leader.term > this.term
				//changes state -> state = STATE.FOLLOWER;
			//else
				//continues candidate
				//????????????????????????????
			
			//CASO 3
			//election timer ends (F)
			//timeout random~
				//timer = new Timer();
				//Random r = new Random();
				//timer.schedule(new RemindTask(), (r.nextInt(3) + 2) * 1000);
			
			
			//time out --> eleicao

			timer = new Timer();
			timer.schedule(new RemindTask(server), 5*100);
		}

	}


}
