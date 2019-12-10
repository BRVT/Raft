package server;

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import enums.STATE;
import server.constants.Constants;

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

	public static void mySleep (int val) {
		try { 
			TimeUnit.SECONDS.sleep(val);
		} catch (InterruptedException e) {

		}
	}

	public void run() {
		System.out.println("Comeca eleicao");

		while(!finished) {
			startVote();
			electionWork();
			if(server.getState().equals(STATE.FOLLOWER))
				Thread.currentThread().interrupt();
			mySleep(20);
		}	

		server.cancelTimer();

	}

	public void startVote() {

		//increases term -> this.term++;
		server.increaseTerm();

		//changes state -> state = STATE.CANDIDATE;
		server.setState(STATE.CANDIDATE);

		//changes votedFor -> votedFor = this.id;
		server.setVoteFor(server.getPort());


	}

	public void electionWork() {
		server.resetVotes();
		ArrayList<Integer> ports = new ArrayList<>();

		for (Integer integer : Constants.PORTS_FOR_SERVER_REGISTRIES) {
			if(integer != this.server.getPort())
				ports.add(integer);
		}
		
		int j = 0;
		for (FollowerCommunication f : server.getFollowers()) {
			f = new FollowerCommunication(server, 5000, true, ports.get(j));
			f.start();
			j++;
		}
		
		synchronized (server.getVotes()) {
			server.addVote(server.getPort(), 0);
			
			if(server.getVotes().size() > 2){

				int count = 0;
				for (Integer i : server.getVotes().values()) {
					if(i == 0) count ++;
					
				}

				if(count > 2) {
					server.setState(STATE.LEADER);
					server.setLeaderPort(server.getPort());
					System.out.println("SOU O LIDER-> " + server.getLeaderPort());

					finished = true;
					server.leaderWork();
				}

				server.resetVotes();
			}
		}
	}
}
