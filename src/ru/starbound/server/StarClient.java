package ru.starbound.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import ru.starbound.server.packets.*;

public class StarClient extends Thread {
	private Socket clientSocket;
	
	private SocketThread serverThread;
	private SocketThread clientThread;
	
	public StarClient(Socket client) {
		clientSocket = client;
	}
	
	@Override
	public void run() {
		System.out.printf("Client start\n");
		Socket server = null;
		try {
			server = new Socket("127.0.0.1", 21025);
			//server = new Socket("server.starbound.ru", 21025);
			
		} catch (IOException e) {
			e.printStackTrace();

			try {
				clientSocket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		} finally {
			serverThread = new SocketThread(server);
			clientThread = new SocketThread(clientSocket);
			
			serverThread.setPeer(clientThread);
			clientThread.setPeer(serverThread);
			
			serverThread.setName("server");
			clientThread.setName("client");
			
			serverThread.start();
			clientThread.start();
			
			try {
				serverThread.join();
				clientThread.join();
			} catch (InterruptedException xc) {
			}
		}
		
		System.out.printf("Client end\n");
	}
	
	public void cleanup() {
		try {
			serverThread.interrupt();
			serverThread.join(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			clientThread.interrupt();
			clientThread.join(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private class SocketThread extends Thread {
		private boolean done = false;
		private Socket socket;
		
		private SocketThread peer;
		
		public SocketThread(Socket socket) {
			this.socket = socket;
		}
		
		public void setPeer(SocketThread peer) {
			this.peer = peer;
		}
		
		public boolean isDone() {
			return done;
		}
		
		private int readVlqU(InputStream stream) throws IOException {
			//System.out.printf("   Read VLQ ");
			int res = 0;
			while (true) {
				int b = stream.read();
				
				//System.out.printf(" %02x", b);
				
				res = (res << 7) | (b & 0x7f);
				if ((b & 0x80) == 0) {
					break;
				}
			}
			//System.out.printf(" = %d\n", res);
			return res;
		}
		private int readVlqI(InputStream stream) throws IOException {
			int res = readVlqU(stream);
			if ((res & 1) == 1) {
				res = - (res >> 1);
			}
			else {
				res = res >> 1;
			}
			//System.out.printf("     signed: %d\n", res);
			return res;
		}
		
		public void writeVlqU(OutputStream out, int value) throws IOException {
			if (value == 0) {
				out.write(0);
				return;
			}
			
			int tCnt = 0;
			while ((value & (0x7f << (7 * tCnt))) > 0) tCnt++;
			
			while (tCnt > 0) {
				tCnt--;
				
				int b = (value >> (7 * tCnt)) & 0x7f;
				if (tCnt > 0) b |= 0x80;
				
				out.write(b);
			}
		}
		public void writeVlqI(OutputStream out, int value) throws IOException {
			boolean negative = false;
			if (value < 0) {
				negative = true;
				value = -value;
			}
			
			//System.out.printf("   Write VLQ Signed %d = ", value);
			
			value <<= 1;
			
			if (value == 0) {
				out.write(0);
				return;
			}
			
			int tCnt = 0;
			while ((value & (0x7f << (7 * tCnt))) > 0) tCnt++;
			
			while (tCnt > 0) {
				tCnt--;
				
				int b = (value >> (7 * tCnt)) & 0x7f;
				if (tCnt > 0) b |= 0x80;
				else if (negative) b |= 0x01;
				
				//System.out.printf(" %02x", b);
				out.write(b);
			}
			//System.out.println();
		}
		
		private Object lock = new Object();
		public void writePacket(int packetType, int bodyLength, boolean compressed, byte[] body) throws IOException {
			OutputStream out = socket.getOutputStream();
			
			//synchronized (socket) {
				System.out.printf("   %-10s   Write packetType %d\n", getName(), packetType);
				writeVlqU(out, packetType);
				System.out.printf("   %-10s   Write bodyLength %d\n", getName(), bodyLength);
				writeVlqI(out, compressed ? - bodyLength : bodyLength);
//				byte body[] = bodyBuffer.toByteArray();
//				int writeBytes = 0;
//				while (writeBytes < bodyLength) {
//					out.write(body, writeBytes, 1024);
//					writeBytes += 1024;
//				}
				System.out.printf("   %-10s   Write body\n", getName());
				out.write(body, 0, bodyLength);
			//}
		}

		@Override
		public void run() {
			System.out.printf("  peer start\n");
			try {
				InputStream in = socket.getInputStream();
				OutputStream out = peer.socket.getOutputStream();
				peer.socket.setSendBufferSize(1024 * 8);
				
				socket.setSoTimeout(0);
				socket.setSoLinger(true, 60);
				
				ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream(64 * 1024);
				
				byte[] readBuffer = new byte[1024 * 8];
				int readCount;
				
				boolean packetMode = true;
				
				try {
					if (packetMode) {
						while (!isInterrupted()) {
							int packetType;
							int bodyLength;
							boolean compressed = false;
							bodyBuffer.reset();
							
							//synchronized (socket) {
								System.out.printf("   %-10s   Read packetType\n", getName());
								packetType = readVlqU(in);
								//System.out.printf("   %-10s        %02d\n", getName(), packetType);
								System.out.printf("   %-10s   Read bodyLength\n", getName());
								bodyLength = readVlqI(in);
								//System.out.printf("   %-10s        %d\n", getName(), bodyLength);

								if (bodyLength < 0) {
									bodyLength = -bodyLength;
									compressed = true;
								}
								
								int readBodyLength = 0;
								System.out.printf("   %-10s   Read body\n", getName());
								while (((readCount = in.read(readBuffer, 0, Math.min(readBuffer.length, bodyLength - readBodyLength))) > 0) && !isInterrupted()) {
									readBodyLength += readCount;
									//System.out.printf("   %-10s        %5d chunk %5d/%d\n", getName(), readCount, readBodyLength, bodyLength);
									bodyBuffer.write(readBuffer);
									
									if (readBodyLength == bodyLength) break;
								}
								//System.out.printf("   %-10s        %5d total\n", getName(), readBodyLength);
							//}
							
							System.out.printf(" < %-10s < Received packet [%02d] with [%d] body bytes\n", getName(), packetType, bodyLength);
							
							//peer.writePacket(packetType, readBodyLength, compressed, bodyBuffer.toByteArray().clone());
							
							//synchronized (peer.socket) {
								System.out.printf("   %-10s   Write packetType %d\n", getName(), packetType);
								writeVlqU(out, packetType);
								System.out.printf("   %-10s   Write bodyLength %d\n", getName(), bodyLength);
								writeVlqI(out, compressed ? - bodyLength : bodyLength);
	
								System.out.printf("   %-10s   Write body\n", getName());
								
								byte body[] = bodyBuffer.toByteArray();
								int writeBytes = 0;
								while (writeBytes < bodyLength) {
									out.write(body, writeBytes, 1024);
									writeBytes += 1024;
								}
								//out.write(bodyBuffer.toByteArray(), 0, bodyLength);
							//}
						}
					}
					else {
						while (((readCount = in.read(readBuffer)) > 0) && !isInterrupted()) {
							out.write(readBuffer, 0, readCount);
						}
					}
				} catch (SocketException e) {
					System.out.printf(" * %-10s * Socket error: %s\n", getName(), e.getMessage());
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					out.flush();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			
			done = true;
			try {
				if ((peer == null) || peer.isDone()) {
					// Cleanup if there is only one peer OR
					// if _both_ peers are done
					socket.close();
					if (peer != null) peer.socket.close();
				} else {
					// Signal the peer (if any) that we're done on this side
					// of the connection
					peer.interrupt();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				
			}
			
			System.out.printf("  peer end\n");
		}

		@Override
		public void interrupt() {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}			
			super.interrupt();
		}
		
		
	}

	/*
	private class ServerReader implements Runnable {
		public ServerReader() {
			try {
				server = new Socket("127.0.0.1", 21024);
			} catch (IOException e) {
				try {
					client.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
				return;
			}
		}
		
		@Override
		public void run() {
			try {
				InputStream is = server.getInputStream();
				OutputStream os = client.getOutputStream();

				while (true) {
					Packet.load(is).send(os);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			

		}
	}
	
	@Override
	public void oldrun() {
		new Thread(new ServerReader()).start();
		
		
		try {
			InputStream is = client.getInputStream();
			OutputStream os = server.getOutputStream();

			while (true) {
				Packet.load(is).send(os);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		Packet.read(socket, ProtocolVersionPacket.class);
//		
//		ProtocolVersionPacket version = new ProtocolVersionPacket();
//		
		System.out.printf("New connection \n");
		
		try {
			PacketStreamWriter out = new PacketStreamWriter(client.getOutputStream());
			
			
			Packet version = Packet.load(server.getInputStream());
			version.write(out);
			
			//System.out.printf(" > Version: %d\n", 0x268);
			//new ProtocolVersionPacket(0x268).write(out);
			
			
			Packet.load(client.getInputStream());
			
			new HandshakeChallengePacket().write(out);
			
			Packet.load(client.getInputStream());
			
			//while (stream.readInt() > 0) {
			//	System.out.printf(" < Available: %d\n", stream.available());
			//}
			
		} catch (IOException e) {
			e.printStackTrace();
			
			try {
				// close error socket
				client.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		try {
			// close error socket
			client.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	*/
	
}
