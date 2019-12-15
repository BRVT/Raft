package domain;

import java.util.ArrayList;
import java.util.List;

import javafx.util.Pair;

public class TableManager {
	
	private static TableManager singleton;
	private List<Pair<String,String>> table ;
	
	private TableManager() {
		this.table = new ArrayList<Pair<String , String>>();
	}
	
	public static TableManager getInstance() {
		if(singleton instanceof TableManager) {
			return singleton;
		}
		singleton = new TableManager();
		return singleton;
		
	}

	public String getValue(String key) {
		for (Pair<String, String> pair : table) {
			if(pair.getKey().equals(key)) 
				return pair.getValue();		
		}
		return null;
	}

	public void removePair(String key) {
		for (Pair<String, String> pair : table) {
			if(pair.getKey().equals(key)) {
				table.remove(pair);
				return;
			}			
		}
	}
	
	public void putPair(String key, String value) {
		Pair <String, String> auxPair = new Pair<String, String>(key, value);
		removePair(key); // se existir elimina
		table.add(auxPair);
	}
	
	public List<Pair<String,String>> getList(){
		return table;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (Pair<String, String> pair : table) {
			sb.append("Key: " + pair.getKey());
			sb.append(" Value: " + pair.getValue());
			sb.append("\n");
		}
		
		return sb.toString();
	}

	public void cas(String key, String oldValue, String newValue) {
		for (Pair<String, String> pair : table) {
			if(pair.getKey().compareTo(key) == 0 && pair.getValue().compareTo(oldValue) == 0) {
				table.remove(pair);
				table.add(new Pair<>(key, newValue));
			}
		}
	}
}
