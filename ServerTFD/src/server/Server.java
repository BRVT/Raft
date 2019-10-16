package server;


import java.rmi.RemoteException;
import java.util.*;

import enums.STATE;
import server.constants.Constants;

public class Server implements IServer {
	
	private static String address;
	private static int port;
	private static int leader = 1234;
	private STATE state;
	private int term;

	private Timer timer;
	private Timer heartbeat;
	
	/**
	 * @param role 
	 * 
	 */
	public Server(int role){
		this.term = 0;
		this.state = STATE.values()[role];
		
		//timer do server
		timer = new Timer();
        timer.schedule(new RemindTask(), 5*1000);
        
        
        //heartbeat
        if(state.equals(STATE.LEADER)) {
        	//thread do hearbeat
        	heartbeat = new Timer();
        	heartbeat.schedule(new RemindTaskHeartBeat(),
                           0,        //initial delay
                           50);  //subsequent rate
        }
	}
	
	class RemindTaskHeartBeat extends TimerTask {
        int numWarningBeeps = 3;
        public void run() {
            sendHeartBeat();
        }
		
    }
	
    class RemindTask extends TimerTask {
        public void run() {
            System.out.println("Time's up!");
            timer.cancel(); 
            
            increaseTerm();
            
            timer = new Timer();
            timer.schedule(new RemindTask(), 5*1000);
        }
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
		System.out.println("Bip");
	}
}
