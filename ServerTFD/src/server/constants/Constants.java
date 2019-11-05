package server.constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Constants {
	
	/**
	 * List of ports to use
	 */
	public static final String ADDRESS = "rmi://localhost/server";
	
	public static List<Integer> PORTS_FOR_SERVER_REGISTRIES = Arrays.asList(1234, 1235, 1236, 1237, 1238);
	
	public static int LEADER_PORT = 1234;
	
	
}
