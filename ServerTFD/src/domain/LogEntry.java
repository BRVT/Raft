package domain;

import java.io.*;
import java.util.*;
public class LogEntry {

	//Definir o que cada LogEntry vai ter
	// - num operacao
	// - id do client
	// - termo do server
	// - 
	private static final String BAR = System.getProperty("file.separator");

	private File f;
	private File dir;

	private int leaderID;
	private int prevLogIndex;
	private int prevLogTerm;
	private int commitIndex;

	private Entry lastEntry;

	private ArrayList<Entry> entries;


	public LogEntry() {
		this.entries = new ArrayList<>();
		this.prevLogIndex = 0;
		this.commitIndex = -1;
		this.lastEntry = null;
	}

	public void createFile(int port) {
		String dire = "src" + BAR +"server" +BAR +"file_server_"+String.valueOf(port);
		String file = dire + BAR + "log_" + String.valueOf(port)+".txt";
		// Use relative path for Unix systems


		this.dir = new File(dire);
		this.f = new File(file);
		if(!dir.exists()) {

			dir.mkdir();

			try {
				f.createNewFile();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}else {
			loadEntries();
		}
	}

	private void loadEntries() {

		FileInputStream fis;
		try {
			fis = new FileInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			String st = ""; 

			while ((st = br.readLine()) != null) {
				Entry e = Entry.setEntry(st);
				entries.add(e);
				if(e.isComitted()) {
					this.commitIndex ++;
				}
				prevLogIndex ++;

			}
			br.close();
		} catch (NumberFormatException | IOException e) {

			e.printStackTrace();
		} 
	}


	public boolean writeLog(String command, int term, boolean commited, String id_command) {

		Entry aux = new Entry(prevLogIndex,command,term,commited,id_command);
		if(entries.contains(aux)) {
			return false;
		}else {
			lastEntry =  aux;
			
			synchronized (entries) {
				entries.add(lastEntry);
			}

			prevLogIndex ++;
			
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(f,true));
				writer.write(lastEntry.toString());
				writer.newLine();
				writer.close();

				return true;

			} catch (IOException e) {
				return false;
			}

		}

	}


	public static class Entry{
		private int index;
		private String command;
		private int term;
		private boolean commited;
		private String clientIDCommand;


		public Entry(int index, String command, int term, boolean commited, String id_command) {
			this.index = index;
			this.command = command;
			this.term = term;
			this.commited = commited;
			this.clientIDCommand = id_command;
		}

		public String toString() {
			return (String.valueOf(index)+":"+command+":"+String.valueOf(term)+":"+
					Boolean.toString(commited)+":"+String.valueOf(clientIDCommand));
		}

		public static Entry setEntry(String s) {
			String[] array = s.split(":");

			return new Entry(Integer.parseInt(array[0]),array[1],Integer.parseInt(array[2]),Boolean.valueOf(array[3]),
					(array[4]));
		}

		public boolean isComitted() {
			return commited;
		}

		public String getClientIDCommand() {
			return clientIDCommand;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Entry other = (Entry) obj;
			if (clientIDCommand == null) {
				if (other.clientIDCommand != null)
					return false;
			} else if (!clientIDCommand.equals(other.clientIDCommand))
				return false;
			if (command == null) {
				if (other.command != null)
					return false;
			} else if (!command.equals(other.command))
				return false;
			if (commited != other.commited)
				return false;
			if (index != other.index)
				return false;
			if (term != other.term)
				return false;
			return true;
		}




		public void setCommitted() {
			this.commited = true;

			// temos que ir ao log mudar isto pah!!!!!!
		}

	}

	
	public void commitEntry() {
		//TODO
		commitIndex ++;

		System.out.println("ESTA COMITADO");
	}

	public int getPrevLogTerm() {
		return prevLogTerm;
	}

	public void setPrevLogTerm(int prevLogTerm) {
		this.prevLogTerm = prevLogTerm;
	}

	public int getPrevLogIndex() {
		return prevLogIndex;
	}

	public void setPrevLogIndex(int prevLogIndex) {
		this.prevLogIndex = prevLogIndex;
	}

	public int getCommitIndex() {
		return commitIndex;
	}

	public void setCommitIndex(int commitIndex) {
		this.commitIndex = commitIndex;
	}

	public Entry getLastEntry() {
		return lastEntry;
	}

	public ArrayList<Entry> getLastEntriesSince(Entry e) {

		ArrayList <Entry> array = new ArrayList<>();
		int flag = 0;
		if(e == null) {
			flag = 1;
		}
		for(Entry entry : entries) {

			if(flag == 1) {
				array.add(entry);
			}
			if((entry.equals(e))&& flag != 1) {
				flag = 1;
			}
		}
		return array;
	}

	public String getEntry(String string) {
		for (Entry entry : entries) {
			if(entry.command.split("-")[1].compareTo(string) == 0) {
				return entry.command.split("-")[2];
			}
		}
		return null;
		
	}

}
