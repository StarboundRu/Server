package ru.starbound.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import ru.starbound.server.packets.ChatReceivePacket;
import ru.starbound.server.packets.ChatSendPacket;
import ru.starbound.server.packets.ClientConnectPacket;
import ru.starbound.server.packets.Packet;
import ru.starbound.server.packets.ProtocolVersionPacket;
import ru.starbound.server.stream.PacketStream;

public class Client extends Thread {
	private Server server;
	private SocketChannel clientSocket;
	//private PacketStream clientBuffer = new PacketStream();
	//private PacketStream serverBuffer = new PacketStream();
    private ClientPacketRunner clientRunner = new ClientPacketRunner();
    private ServerPacketRunner serverRunner = new ServerPacketRunner();
	
	//private ArrayList<ByteBuffer> clientQueue = new ArrayList<>();
	//private ArrayList<ByteBuffer> serverQueue = new ArrayList<>();
	
	private boolean isAdmin = false;
    private boolean isBanned = false;
    private boolean isReady = false;
	
	private String name;
	
	public Client(Server server, SocketChannel client) {
		this.server = server;
		clientSocket = client;
		try {
			name = client.getRemoteAddress().toString();
		} catch (IOException e) {
			e.printStackTrace();
			name = "Unknown";
		}
		
		System.out.printf("%-20s  *  connected\n", name);
	}
	
	public String getClientName() {
		return name;
	}
	
	public String getClientIp() {
		return clientSocket.socket().getInetAddress().getHostAddress();
	}

    private abstract class PacketRunner {
        private PacketStream stream = new PacketStream();
        private ArrayList<Packet> queue = new ArrayList<>();

        public PacketRunner() {

        }
        public abstract void onPacketReceive(Packet packet);
        public void queuePacket(Packet packet) {
            queue.add(packet);
        }
        public Packet getQueuedPacket() {
            if (queue.size() == 0) return null;
            Packet packet = queue.get(0);
            queue.remove(0);
            return packet;
        }
        public void read(ByteBuffer buffer) throws IOException {
            stream.read(buffer);
            Packet packet;
            while ((packet = stream.getPacket()) != null) {
                onPacketReceive(packet);
            }
        }
    }

    private class ClientPacketRunner extends PacketRunner {
        private boolean firstTime12 = true;
        @Override
        public void onPacketReceive(Packet packet) {
            int packetType = packet.getPacketType();
            boolean ignorePacket = false;

            switch (packetType) {
                case 7: {
                    ClientConnectPacket xpacket = (ClientConnectPacket)packet;

                    isReady = true;

                    System.out.printf("%-20s <   ClientConnectPacket, character name: %s\n", name, xpacket.characterName);
                    System.out.flush();
                    name = xpacket.characterName;

                    if (server.haveBan(name)) {
                        isBanned = true;
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
                case 11: //
                {
                    ChatSendPacket xpacket = (ChatSendPacket)packet;
                    System.out.printf("%-20s <   Say: %s\n", name, xpacket.text, xpacket.text.substring(0, 1));

                    boolean processed = false;

                    if (xpacket.text.startsWith("/")) {
                        if (xpacket.text.compareTo("/admin iddqd") == 0) {
                            processed = true;

                            sendServerMessage("Доступ разрешён");
                            isAdmin = true;
                        }
                        else if (xpacket.text.compareTo("/players") == 0) {
                            processed = true;

                            ArrayList<Client> clients = server.getClients();
                            synchronized (clients) {
                                sendServerMessage("Всего игроков: " + Integer.toString(clients.size()));

                                if (isAdmin) {
                                    for (Client c : clients) {
                                        if (!c.isReady) continue;
                                        sendServerMessage("  " + c.getClientName() + " - " + c.getClientIp());
                                    }

                                    sendServerMessage("--------");
                                }
                            }
                        }
                        else if (isAdmin && xpacket.text.startsWith("/ban ")) {
                            processed = true;
                            String nick = xpacket.text.substring(5).trim();

                            server.addBan(nick);
                            sendServerMessage(nick + " забанен");
                        }
                        else if (isAdmin && xpacket.text.startsWith("/unban ")) {
                            processed = true;
                            String nick = xpacket.text.substring(7).trim();

                            server.removeBan(nick);

                            sendServerMessage(nick + " разбанен");
                        }
                        else if (isAdmin && xpacket.text.compareTo("/bans") == 0) {
                            processed = true;
                            ArrayList<String> bans = server.getBans();

                            synchronized (bans) {
                                sendServerMessage("Список банов:");

                                for (String ban : bans) {
                                    sendServerMessage("  " + ban);
                                }

                                sendServerMessage("--------");
                            }
                        }

                        if (!processed) {
                            sendServerMessage("Неизвестная команда: " + xpacket.text);
                        }

                        ignorePacket = true;
                    }

                }
                case 12: // ClientContextUpdatePacket
                {
                    if (firstTime12) {
                        sendServerMessage("Добро пожаловать на сервер Starbound.ru!");
                        sendServerMessage("На нашем сервере запрещёно гриферство. За нарушение этого правила вы можете быть забанены навсегда.");
                        firstTime12 = false;
                    }
                    break;
                }
            }
            if (!ignorePacket) {
                serverRunner.queuePacket(packet);
            }
        }

        private void sendServerMessage(String message) {
            queuePacket(new ChatReceivePacket(3, "", 0, "starbound.ru", message));
        }
    }
    private class ServerPacketRunner extends PacketRunner {
        @Override
        public void onPacketReceive(Packet packet) {
            int packetType = packet.getPacketType();
            switch (packetType) {
                case 1: // ProtocolVersionPacket
                {
                    ProtocolVersionPacket xpacket = (ProtocolVersionPacket)packet;
                    System.out.printf("%-20s   > ProtocolVersionPacket, version: %04x\n", name, xpacket.version);
                    break;
                }
                case 2: // ConnectResponsePacket
                {
                    //clientRunner.sendServerMessage("Welcome to Starbound.ru :)");
                }

                case 4: // HandshakeChallengePacket
                case 5: // ChatReceivePacket
                case 6: // UniverseTimeUpdatePacket
                case 12: // ClientContextUpdatePacket
                case 10:
                case 14:
                case 13: // WorldStartPacket
                case 15: // TileArrayUpdatePacket
                case 16: // TileUpdatePacket
                case 18: // TileDamageUpdatePacket
                case 23: // SkyUpdatePacket
                case 42: // EntityCreatePacket
                case 43: // EntityUpdatePacket
                case 44: // EntityDestroyPacket
                case 45:
                case 48: // HeartbeatPacket
                default: {
                    // do nothing
                    //System.out.printf("%-20s   > Unknown packet, %d\n", name, packetType);
                }
            }


            clientRunner.queuePacket(packet);
        }
    }

	@Override
	public void run() {
		try {
			
			//FileChannel buffer1 = new RandomAccessFile("buffer1.bin", "rw").getChannel();
			//FileChannel buffer2 = new RandomAccessFile("buffer2.bin", "rw").getChannel();
			//FileChannel buffer3 = new RandomAccessFile("buffer3.bin", "rw").getChannel();
			//FileChannel buffer4 = new RandomAccessFile("buffer4.bin", "rw").getChannel();
			
			Selector selector = Selector.open();
			
			//SocketChannel serverSocket = SocketChannel.open(new InetSocketAddress("server.starbound.ru", 21025));
			SocketChannel serverSocket = SocketChannel.open(new InetSocketAddress("127.0.0.1", 21025));
			serverSocket.configureBlocking(false);
			
			clientSocket.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			serverSocket.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			
			ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 8);
			boolean firstTime12 = true;
			
			while (selector.isOpen()) {
				selector.select();
				
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> i = keys.iterator();
				while (i.hasNext()) {
					SelectionKey key = i.next();
					i.remove();
					
					SocketChannel socket = (SocketChannel) key.channel();
					
					if (key.isWritable()) {
                        Packet packet;
                        if (socket == clientSocket) {
							while ((packet = clientRunner.getQueuedPacket()) != null) {
								ByteBuffer bf = packet.asByteBuffer();
								int needWrite = bf.remaining();

								int written = 0;
								while (bf.remaining() > 0) {
									written += socket.write(bf);
								}
								if (written < needWrite) {
									System.out.printf("  written %d / %d\n", written, needWrite);
								}
							}
						}
						else if (socket == serverSocket) {
                            while ((packet = serverRunner.getQueuedPacket()) != null) {
                                ByteBuffer bf = packet.asByteBuffer();
                                int needWrite = bf.remaining();

								int written = 0;
								while (bf.remaining() > 0) {
									written += socket.write(bf);
								}
								if (written < needWrite) {
									System.out.printf("  written %d / %d\n", written, needWrite);
								}
							}
						}
					}
					
					if (key.isReadable()) {
						
						
						readBuffer.clear();
						
						try {
							socket.read(readBuffer);
						}
						catch (IOException e) {
							key.cancel();
							selector.close();
							
							System.out.printf("CONNECTION CLOSED\n");
							continue;
						}
						
						readBuffer.flip();
						
						if (socket == clientSocket) {
                            clientRunner.read(readBuffer);
                        }
                        else {
                            serverRunner.read(readBuffer);
                        }


//							clientBuffer.read(readBuffer);
//
//							//readBuffer.rewind();
//							//buffer1.write(readBuffer);
//
//							Packet xpacket;
//							while ((xpacket = clientBuffer.getPacket()) != null) {
//
//                                onReadPacket(xpacket);
//
//								int packetType = xpacket.getPacketType();
//
//								//System.out.printf("%-20s <   [%s]\n", name, packetType);
//
//								boolean ignorePacket = false;
//
//								switch (packetType) {
//									case 7: {
//										ClientConnectPacket packet = (ClientConnectPacket)xpacket;
//										System.out.printf("%-20s <   ClientConnectPacket, character name: %s\n", name, packet.characterName);
//										System.out.flush();
//										name = packet.characterName;
//
//										if (server.haveBan(name)) {
//											clientSocket.close();
//										}
//
//										//ChatReceivePacket sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", "Hello ;)");
//										//clientSocket.write(sendPacket.asByteBuffer());
//
//										break;
//									}
//									case 11: //
//									{
//										ChatSendPacket packet = (ChatSendPacket)xpacket;
//										System.out.printf("%-20s <   ChatSendPacket, %s [%s]\n", name, packet.text, packet.text.substring(0, 1));
//
//										boolean processed = false;
//
//										if (packet.text.startsWith("/")) {
//											if (packet.text.compareTo("/admin iddqd") == 0) {
//												processed = true;
//
//												ChatReceivePacket sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", "Granted");
//												clientSocket.write(sendPacket.asByteBuffer());
//
//												isAdmin = true;
//											}
//											else if (packet.text.compareTo("/players") == 0) {
//												processed = true;
//
//												ArrayList<Client> clients = server.getClients();
//												synchronized (clients) {
//													ChatReceivePacket sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", "Total players: " + Integer.toString(clients.size()));
//													clientSocket.write(sendPacket.asByteBuffer());
//
//													if (isAdmin) {
//														for (Client c : clients) {
//															sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", "  " + c.getClientName() + " - " + c.getClientIp());
//															clientSocket.write(sendPacket.asByteBuffer());
//														}
//
//														sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", "--------");
//														clientSocket.write(sendPacket.asByteBuffer());
//													}
//												}
//											}
//											else if (isAdmin && packet.text.startsWith("/ban ")) {
//												processed = true;
//												String nick = packet.text.substring(5).trim();
//
//												server.addBan(nick);
//
//												ChatReceivePacket sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", nick + " banned");
//												clientSocket.write(sendPacket.asByteBuffer());
//											}
//											else if (isAdmin && packet.text.startsWith("/unban ")) {
//												processed = true;
//												String nick = packet.text.substring(7).trim();
//
//												server.removeBan(nick);
//
//												ChatReceivePacket sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", nick + " unbanned");
//												clientSocket.write(sendPacket.asByteBuffer());
//											}
//											else if (isAdmin && packet.text.compareTo("/bans") == 0) {
//												processed = true;
//												ArrayList<String> bans = server.getBans();
//
//												synchronized (bans) {
//													ChatReceivePacket sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", "Ban list:");
//													clientSocket.write(sendPacket.asByteBuffer());
//
//													for (String ban : bans) {
//														sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", "  " + ban);
//														clientSocket.write(sendPacket.asByteBuffer());
//													}
//
//													sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", "--------");
//													clientSocket.write(sendPacket.asByteBuffer());
//												}
//											}
//
//											if (!processed) {
//												ChatReceivePacket sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", "Unknown command " + packet.text);
//												clientSocket.write(sendPacket.asByteBuffer());
//											}
//
//											ignorePacket = true;
//										}
//
//									}
//									case 12: // ClientContextUpdatePacket
//									{
//										if (firstTime12) {
//											ChatReceivePacket sendPacket = new ChatReceivePacket(3, "", 0, "starbound.ru", "Welcome to Starbound.ru server!");
//											clientSocket.write(sendPacket.asByteBuffer());
//											firstTime12 = false;
//										}
//
//										break;
//									}
//									case 9:  // HandshakeResponsePacket
//									case 41: // WorldClientStateUpdatePacket
//									case 42: // EntityCreatePacket
//									case 43: // EntityUpdatePacket
//									case 44:
//									case 45:
//									case 48: // HeartbeatPacket
//									{
//										// ignore
//										break;
//									}
//									default:{
//										System.out.printf("%-20s <   Unknown packet, %d\n", name, packetType);
//									}
//								}
//
//								if (!ignorePacket) {
//									ByteBuffer bf = xpacket.asByteBuffer();
//									//serverSocket.write(bf);
//
//									serverQueue.add(bf);
//
//									//bf.rewind();
//									//buffer2.write(bf);
//								}
//							}
//
//							//if (buffer1.size() != buffer2.size()) {
//								//System.err.printf("BUFFERS 1 & 2 MISMATCH! %d != %d\n", buffer1.size() , buffer2.size());
//							//}
//
//						}
//						else {
//							serverBuffer.read(readBuffer);
//
//							//readBuffer.rewind();
//							//buffer3.write(readBuffer);
//
//							Packet xpacket;
//							while ((xpacket = serverBuffer.getPacket()) != null) {
//								int packetType = xpacket.getPacketType();
//								boolean ignorePacket = false;
//
//								switch (packetType) {
//									case 0: {
//										// no packet
//										break;
//									}
//									case 1: {
//										ProtocolVersionPacket packet = (ProtocolVersionPacket)xpacket;
//										System.out.printf("%-20s   > ProtocolVersionPacket, version: %04x\n", name, packet.version);
//										break;
//									}
//									case 2: // ConnectResponsePacket
//									case 4: // HandshakeChallengePacket
//									case 5: // ChatReceivePacket
//									case 6: // UniverseTimeUpdatePacket
//									case 12: // ClientContextUpdatePacket
//									case 10:
//									case 14:
//									case 13: // WorldStartPacket
//									case 15: // TileArrayUpdatePacket
//									case 16: // TileUpdatePacket
//									case 18: // TileDamageUpdatePacket
//									case 23: // SkyUpdatePacket
//									case 42: // EntityCreatePacket
//									case 43: // EntityUpdatePacket
//									case 44: // EntityDestroyPacket
//									case 45:
//									case 48: // HeartbeatPacket
//									{
//										//ignore
//										break;
//									}
//									default: {
//										System.out.printf("%-20s   > Unknown packet, %d\n", name, packetType);
//									}
//								}
//
//								if (!ignorePacket) {
//									ByteBuffer bf = xpacket.asByteBuffer();
//									//clientSocket.write(bf);
//
//									clientQueue.add(bf);
//
//									//bf.rewind();
//									//buffer4.write(bf);
//								}
//							}
//
//							//if (buffer3.size() != buffer4.size()) {
//								//System.err.printf("BUFFERS 3 & 4 MISMATCH! %d != %d\n", buffer3.size() , buffer4.size());
//							//}
//
//						}
					}
					
					
				}
			}
			serverSocket.close();
			clientSocket.close();
			
			//buffer1.close();
			//buffer2.close();
			//buffer3.close();
			//buffer4.close();
			
			System.out.printf("Client shutdown\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		server.removeClient(this);
	}
}

