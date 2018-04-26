package ru.starbound.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class Server extends Thread {
	private int port;
	
	public Server(int port) {
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
						
						String ip = channel.socket().getInetAddress().getHostAddress();
						if (haveBan(ip)) {
							channel.close();
						}
						else {
							System.out.printf("Accept client %s\n", channel.getRemoteAddress().toString());
							channel.configureBlocking(false);
							
							Client client = new Client(this, channel);
							addClient(client);
							client.start();
						}
					}
					
					i.remove();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<Client> clients = new ArrayList<>();
	
	public void addClient(Client client) {
		synchronized (clients) {
			clients.add(client);
		}
		
	}
	
	public void removeClient(Client client) {
		synchronized (clients) {
			clients.remove(client);
		}
	}

	public ArrayList<Client> getClients() {
		return clients;
	}
	
	
	private ArrayList<String> bans = new ArrayList<>();
	public void addBan(String ban) {
		synchronized (bans) {
			bans.add(ban);
		}
        synchronized (clients) {
            for (Client c: clients) {
                if (c.getClientIp().compareTo(ban) == 0 || c.getClientName().compareTo(ban) == 0) {
                    c.close();
                }
            }
        }
	}
	
	public void removeBan(String ban) {
		synchronized (bans) {
			bans.remove(ban);
		}
	}
	
	public boolean haveBan(String ban) {
		synchronized (bans) {
			return bans.contains(ban);
		}
	}
	
	public ArrayList<String> getBans() {
		return bans;
	}
}
