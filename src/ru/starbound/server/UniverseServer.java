package ru.starbound.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class UniverseServer extends Thread {
	private int port;
	
	public UniverseServer(int port) {
		this.port = port;
	}
	
	@Override
	public void run() {
		try {
			System.out.printf("Start listen on %d port\n", port);
			ServerSocket server = new ServerSocket(port);
			
			while (!isInterrupted()) {
				new StarClient(server.accept()).start();
			}
			
			while (true) {
				Socket client = server.accept();
				new Thread(new StarClient(client)).start();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
