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

	private Map<Integer, Integer> votes = new HashMap<>();
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
		this.nAnswers = 0;

		log.createFile(this.port);
	}

	/**
	 * Inicializa o server ---REVER---
	 */
	public void run() {
		Random r = new Random();
		//inicializar os servers de forma ogual (retirar o que esta so no lider)

		int count =0;

		int i = r.nextInt(3) + 1;

		timer = new Timer();
		RemindTask rt = new RemindTask(this);
		timer.schedule(rt, i*10000);
		//como eh que o RemindTask vai saber e atualizar os atributos? passamos o this no construtor?
		//



	}

	/**
	 * Funcao que contem o bulk do trabalho realizado pelo Leader
	 */
	
	public void leaderWork() {

		ArrayList<Integer> ports = new ArrayList<>();

		for (Integer integer : Constants.PORTS_FOR_SERVER_REGISTRIES) {
			if(integer != this.port)
				ports.add(integer);
		}

		int j = 0;
		for (FollowerCommunication f : followers) {
			
			f = new FollowerCommunication(5000, false, ports.get(j));
			f.start();
			j++;
		}

		while(true) {
			synchronized (answers) {
				if(answers.size() > 2){
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
		Random r = new Random();
		//inicializar os servers de forma ogual (retirar o que esta so no lider)

		int count =0;

		int i = r.nextInt(3) + 1;
		
		timer = new Timer();
		timer.schedule(new RemindTask(this), i*10000);
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

	public void setTerm(int term) {
		this.term = term;

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
		int flag;
		try {
			if(election == 0) {
				flag = server.AppendEntriesRPC(term, getPort(), log.getPrevLogIndex(),
						log.getPrevLogTerm(), entry, log.getCommitIndex());

				if(flag > 1) {
					setTerm(flag);
					changeState(STATE.FOLLOWER);
				}
			}else if(election == 1 ) {
				System.out.println("vem aos votos : " + getPort());
				return server.RequestVoteRPC(getTerm(), getPort(), log.getPrevLogIndex(), log.getPrevLogTerm());
			}

		} catch (RemoteException e) {
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
	public int receiveAppendEntry(int term, int leaderID, int prevLogIndex, int prevLogTerm, String entry, int leaderCommit) {
		
		
		
		int ret = -1;
		if(term < this.getTerm()) {
			ret = this.getTerm();
		}else {
			timer.cancel();
			this.leaderPort = leaderID;
			if(entry == null) { 			
				ret = 0;
			}else {
				System.out.println(this.port);
				
				ret = log.writeLog(entry.split(":")[1] , this.term, false, entry.split(":")[4] ) ? 0 : 1 ;
				
			}
			resetTimer();
		}
		return ret;
	}

	/**
	 * Recebe RequestVoteRPC de um Candidate
	 * @param term - termo do candidato
	 * @param id - id do candidato (porto)
	 * @param prevLogIndex - prevLogIndex do candidato
	 * @param prevLogTerm - prevLogTerm do candidato
	 * @return -1 se ja votou, 0 se concorda com nova leadership, termo se nao concorda
	 */

	public int receiveRequestVote(int term, int id, int prevLogIndex, int prevLogTerm) {
		System.out.println("recebi voto : " + id);
		//ler paper para saber o que fazer aqui
		if (votedFor != 0) {
			System.out.println("nega voto");
			return -1;
		}
		else if (this.term > term) {
			//passa a candidato e inicia voto?
			return this.term;
		}
		else {
			System.out.println("olaaaa");
			votedFor = id;

			this.term = term;
			return 0;
		}
	}

	/**
	 * Para o leader comunicar com os followers
	 */
	class FollowerCommunication extends Thread { 
		//dealy que vai ser para retentar comunicao
		private int delay; 
		private Registry r;
		private IServerService iServer;
		private int portF;
		private int verify;
		private Entry lastEntry;
		private boolean forElection;


		public FollowerCommunication(int delay, boolean forElection, int port) { 
			this.portF = port;
			this.lastEntry = log.getLastEntry();
			connect();
			this.forElection = forElection;
			this.delay = delay; 
			System.out.println(portF + " ! " + this.getName());
		} 

		public void setElect(boolean ele) {
			forElection = ele;
		}

		@Override
		public void run(){ 
			try{ 
				if (!forElection) {
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
								
								verify = sendHeartBeat(iServer, entry.toString(), 0);
							}

							//Verificar isto!
							//O verify nao verifica todas as entries enviadas
							//Ex: a Entry 1 da verify 1 (erro) e a entry 2 dah fixe, logo dah commit ah 1 na mesma?
							synchronized(answers) {
								System.out.println(nAnswers + " <------------");
								answers.put(portF, verify);
								nAnswers ++;

								if(nAnswers < 3) {
									try {
										answers.wait(8000);
										nAnswers = 0;
									}catch (IllegalMonitorStateException i) {
										
									}
								}else {
									nAnswers = 0;
									answers.notifyAll();
								}
							}
							this.lastEntry = e;
						}
					}
				}
				else {
					if(verify != 1)
						verify = sendHeartBeat(iServer, null, 1);

					synchronized(votes) {
						votes.put(portF, verify);
						nAnswers++;

						if(nAnswers < 3) {
							System.out.println("esperou : " + nAnswers);
							votes.wait(5000);

							nAnswers = 0;
						}else {
							System.out.println("deu unlock");
							votes.notifyAll();
							nAnswers = 0;
							forElection = false;

						}
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
		private boolean finished;

		public RemindTask(Server server) {
			this.server = server;
			this.finished = false;
		}

		public void setFinished(boolean f) {
			this.finished = f;
		}
		public boolean getFinished() {
			return finished;
		}
		public void run() {

			if(votedFor == 0 || votedFor == leaderPort) {

				System.out.println("entrou");

				startVote();

				electionWork();

				System.out.println("saiu do election");

			}
			timer.cancel();

			//CASO 3
			//election timer ends (F)
			//timeout random~
			//timer = new Timer();
			//Random r = new Random();
			//timer.schedule(new RemindTask(), (r.nextInt(3) + 2) * 1000);


			//time out --> eleicao

			//}
		}

		public void startVote() {
			//increases term -> this.term++;
			server.increaseTerm();

			//changes state -> state = STATE.CANDIDATE;
			server.changeState(STATE.CANDIDATE);

			//changes votedFor -> votedFor = this.id;
			server.voteFor(server.getPort());
			votes.put(server.getPort(), 0);
		}

		public void electionWork() {



			ArrayList<Integer> ports = new ArrayList<>();

			for (Integer integer : Constants.PORTS_FOR_SERVER_REGISTRIES) {
				if(integer != this.server.getPort())
					ports.add(integer);
			}

			int j = 0;
			for (FollowerCommunication f : followers) {
				System.out.println("foreach");
				f = new FollowerCommunication(5000, true, ports.get(j));
				f.start();
				j++;
			}


			while(!finished) {
				synchronized (votes) {
					if(votes.size() > 2){
						
						int count = 0;
						for (Integer i : votes.values()) {
							if(i == 0) count ++;
							System.out.println(votes.keySet() + " | " + i + " | " + count);
						}
						if(count > 2) {

							server.changeState(STATE.LEADER);
							server.leaderPort = server.port;
							System.out.println("ja tem votos? -> " + server.getState());

							finished = true;
							leaderWork();
						}

						votes = new HashMap<>();
					}

				}

			}
		}
	}


}
