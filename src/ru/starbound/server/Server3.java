package ru.starbound.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Server3 extends Thread {
	private int port;
	
	public Server3(int port) {
		this.port = port;
	}
	
	@Override
	public void run() {
		ServerSocketChannel server;
		try {
			server = ServerSocketChannel.open();
			server.configureBlocking(false);
			server.socket().bind(new InetSocketAddress(port));
			
			Selector selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT);
			
			while (true) {
				selector.select();
				
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> i = keys.iterator();
				while (i.hasNext()) {
					SelectionKey key = i.next();
					
					if (key.isAcceptable()) {
						SocketChannel channel = server.accept();
						
						System.out.printf("Accept client %s\n", channel.getRemoteAddress().toString());
						channel.configureBlocking(false);
						
						Client3 client = new Client3(this);
						client.setClientChannel(channel);
						client.start();
					}
					
					i.remove();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<Client3> clients = new ArrayList<Client3>();
	public void addClient(Client3 client) {
		synchronized (clients) {
			clients.add(client);
			
			addChannel(client, client.getClientChannel());
			addChannel(client, client.getServerChannel());
		}
	}
	
	public void removeClient(Client3 client) {
		synchronized (clients) {
			clients.remove(client);
		}
	}
	
	private HashMap<SocketChannel, Client3> channels = new HashMap<>();
	public void addChannel(Client3 client, SocketChannel channel) {
		synchronized (channels) {
			channels.put(channel, client);
		}
	}
}
