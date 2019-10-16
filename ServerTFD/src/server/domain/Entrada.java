package server.domain;

import java.util.ArrayList;

public class Entrada {
	
	private int term;
	private int leaderID;
	private int prevLogIndex;
	private int prevLogTerm;
	private ArrayList <String> entries;
	private int leaderCommit;
	
	
	public Entrada(int term, int leaderID, int prevLogIndex, int prevLogTerm, ArrayList<String> entries,
			int leaderCommit) {
		this.term = term;
		this.leaderID = leaderID;
		this.prevLogIndex = prevLogIndex;
		this.prevLogTerm = prevLogTerm;
		this.entries = entries;
		this.leaderCommit = leaderCommit;
	}


	public int getTerm() {
		return term;
	}


	public int getLeaderID() {
		return leaderID;
	}


	public int getPrevLogIndex() {
		return prevLogIndex;
	}


	public int getPrevLogTerm() {
		return prevLogTerm;
	}


	public ArrayList<String> getEntries() {
		return entries;
	}


	public int getLeaderCommit() {
		return leaderCommit;
	}

}
