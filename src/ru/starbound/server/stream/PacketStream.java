package ru.starbound.server.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import ru.starbound.server.packets.*;

public class PacketStream {
	private byte[] data = null;
	private int dataLength = 0;
	private int dataPos = 0;
	
	private static int MAX_PACKET_SIZE = 1024 * 128;
	
	public PacketStream() {
		
	}
	public PacketStream(byte[] data, int dataLength) {
		this.data = data;
		this.dataLength = dataLength;
	}
	
	public int length() {
		return dataLength;
	}
	
	public void reset() {
		dataPos = 0;
		dataLength = 0;
	}
	
	public byte[] getBytes() {
		return data;
	}
	
	public void read(ByteBuffer buffer) throws IOException {
		int len = buffer.remaining();
		grow(len);
		
		buffer.get(data, dataPos, len);
		dataPos += len;
	}
	
//	public class PacketLoader {
//		private int packetType;
//		private int bodySize;
//		
//		public PacketLoader() {
//			int lastPos = dataPos;
//			try {
//				packetType = readVlqU();
//				bodySize = Math.abs(readVlqI());
//			}
//			catch (IOException e) {
//			}
//			dataPos = lastPos;
//		}
//	}
	
	public int checkReadyPacket() {
		if (data == null || dataLength == 0) return 0;
		int result = 0;
		
		int lastPos = dataPos;
		try {
			dataPos = 0;
			int packetType = readVlqU();
			int bodySize = Math.abs(readVlqI());
			
			if (dataLength >= dataPos + bodySize) result = packetType;
		}
		catch (IOException e) {
		}
		dataPos = lastPos;
		return result;
	}
	
	public int skipPacket() {
		if (checkReadyPacket() == 0) return 0;
		
		int lastPos = dataPos;
		try {
			dataPos = 0;
			int packetType = readVlqU();
			int bodySize = readVlqI();
			if (bodySize < 0) {
				bodySize = -bodySize;
			}
			
			dataPos += bodySize;
			for (int i = 0, j = dataPos; j < dataLength; i++,j++) {
				data[i] = data[j];
			}
			dataLength -= dataPos;
			dataPos = dataLength;
			
			return packetType;
		}
		catch (IOException e) {
			dataPos = lastPos;
			return 0;
		}
	}
	
	public Packet getPacket() {
		if (checkReadyPacket() == 0) return null;
		
		int lastPos = dataPos;
		try {
			dataPos = 0;
			int packetType = readVlqU();
			int bodySize = readVlqI();
			boolean compressed = false;
			if (bodySize < 0) {
				bodySize = -bodySize;
				compressed = true;
			}
			
			byte[] packetBody = Arrays.copyOfRange(data, dataPos, dataPos + bodySize);
			
			Packet packet = null;
			
			try {
				switch (packetType) {
					case 1: packet = new ProtocolVersionPacket(packetType, packetBody, compressed); break;
					case 5: packet = new ChatReceivePacket(packetType, packetBody, compressed); break;
					case 7: packet = new ClientConnectPacket(packetType, packetBody, compressed); break;
					case 11: packet = new ChatSendPacket(packetType, packetBody, compressed); break;
					default: packet = new Packet(packetType, packetBody, compressed); 
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
			dataPos += bodySize;
			
			for (int i = 0, j = dataPos; j < dataLength; i++,j++) {
				data[i] = data[j];
			}
			dataLength -= dataPos;
			dataPos = dataLength;
			
			return packet;
		}
		catch (IOException e) {
			dataPos = lastPos;
			return null;
		}
	}
	
	public int readByte() throws IOException {
		if (dataPos >= dataLength) {
			throw new IOException("Exceed buffer limit");
		}
		int b = data[dataPos++];
		//System.out.printf("<   %02x\n", (byte)b);
		return b;
	}
	
	public int readInt() throws IOException {
		if (dataPos + 4 > dataLength) {
			throw new IOException("Exceed buffer limit");
		}
		int result = (data[dataPos] << 24) |
			 (data[dataPos+1] << 16) | 
			 (data[dataPos+2] << 8) | 
			 (data[dataPos+3]);
		dataPos += 4;
		
		//System.out.printf("READ int %d\n", result);
		
		return result;
	}
	
	public int readVlqU() throws IOException {
		int res = 0;
		while (true) {
			int b = readByte();
			res = (res << 7) | (b & 0x7f);
			if ((b & 0x80) == 0) {
				break;
			}
		}
		return res;
	}
	public int readVlqI() throws IOException {
		int res = readVlqU();
		if ((res & 1) == 1) {
			res = - (res >> 1);
		}
		else {
			res = res >> 1;
		}
		return res;
	}
	
	public byte[] readBytes(int len) throws IOException {
		if (dataPos + len > dataLength) {
			throw new IOException("Exceed buffer limit");
		}
		
		byte[] b = Arrays.copyOfRange(data, dataPos, dataPos + len);
		dataPos += len;
		return b;
	}
	
	public byte[] readByteArray() throws IOException {
		int len = readVlqU();
		return readBytes(len);
	}
	
	public void readVariant() throws IOException {
		int vType = readByte();
		//System.out.printf(" < read %d variant type\n", vType);
		switch (vType) {
			case 1: break;
//			case 2:	readDouble(); break;
//			case 3: readBool(); break;
//			case 4: readVlqI(); break;
//			case 5: readString(); break;
//			case 6: readList(); break;
//			case 7: readMap(); break;
			default: 
				System.out.printf(" < variant %d unsupported\n", vType);
		}
	}
	
	public boolean readBool() throws IOException {
		return (readByte() > 0);
	}
	
	public byte[] readUuid() throws IOException {
		if (readBool()) {
			return readBytes(16);
		}
		return null;
	}
	
	public String readString() throws IOException {
		return new String(readByteArray(), "UTF8");
	}
	
	private void grow(int len) throws IOException {
		int newLength = dataLength + len;
		
		// create buffer if need
		if (data == null) {
			int newBufferLength = 8;
			while (newBufferLength < newLength) newBufferLength <<= 1;
			if (newBufferLength > MAX_PACKET_SIZE) {
				throw new IOException("Packet too large");
			}
			data = new byte[newBufferLength];
		}
		// resize data if need
		else if (data.length < newLength) {
			int newBufferLength = data.length * 2;
			while (newBufferLength < newLength) newBufferLength <<= 1;
			if (newBufferLength > MAX_PACKET_SIZE) {
				throw new IOException("Packet too large");
			}
			data = Arrays.copyOf(data, newBufferLength);
		}
		
		dataLength += len;
	}
	
	public void writeByte(int value) throws IOException {
		grow(1);
		data[dataPos++] = (byte) value;
	}
	
	public void writeInt(int value) throws IOException {
		grow(4);
		data[dataPos++] = (byte) ((value >> 24) & 0xff);
		data[dataPos++] = (byte) ((value >> 16) & 0xff);
		data[dataPos++] = (byte) ((value >> 8) & 0xff);
		data[dataPos++] = (byte) ((value) & 0xff);
	} 
	
	public void writeVlqU(int value) throws IOException {
		if (value == 0) {
			writeByte(0);
			return;
		}
		
		int tCnt = 0;
		//while ((value & (0x7f << (7 * tCnt))) > 0) tCnt++;
		if (value == 0) tCnt = 1;
		else {
			while ((value >> (7 * tCnt)) > 0) tCnt++;
		}
		
		while (tCnt > 0) {
			tCnt--;
			
			int b = (value >> (7 * tCnt)) & 0x7f;
			if (tCnt > 0) b |= 0x80;
			
			writeByte(b);
		}
	}
	
	public void writeByteArray(byte[] bytes) throws IOException {
		if (bytes == null || bytes.length == 0) {
			writeVlqU(0);			
		}
		else {
			writeVlqU(bytes.length);
			grow(bytes.length);
			
			System.arraycopy(bytes, 0, data, dataPos, bytes.length);
			dataPos += bytes.length;
		}
	}
	
	public void writeString(String value) throws IOException {
		writeByteArray(value.getBytes("UTF8"));
	}
}
