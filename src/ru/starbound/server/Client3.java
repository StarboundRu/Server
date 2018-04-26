package ru.starbound.server;

import java.nio.channels.SocketChannel;

public class Client3 extends Thread {
	private Server3 server;
	private SocketChannel clientChannel;
	private SocketChannel serverChannel;
	
	public Client3(Server3 server) {
		this.server = server;
	}

	@Override
	public void run() {
		server.addClient(this);
		
		server.removeClient(this);
	}
	
	public void setClientChannel(SocketChannel channel) {
		clientChannel = channel;
		server.addChannel(this, channel);
	}
	
	public SocketChannel getClientChannel() {
		return clientChannel;
	}
	
	public void setServerChannel(SocketChannel channel) {
		serverChannel = channel;
		server.addChannel(this, channel);
	}
	
	public SocketChannel getServerChannel() {
		return serverChannel;
	}
}
