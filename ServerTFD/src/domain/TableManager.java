package domain;

import java.util.ArrayList;
import java.util.List;

import javafx.util.Pair;

public class TableManager {

	private List<Pair<String,String>> table ;
	
	public TableManager() {
		this.table = new ArrayList<Pair<String , String>>();
		
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
			if(pair.getKey().equals(key)) 
				table.remove(pair);		
		}
		
	}
	
	public void putPair(String key, String value) {
		Pair <String, String> auxPair = new Pair<String, String>(key, value);
		removePair(key); // se existir elimina
		table.add(auxPair);
	}
	
	

}
