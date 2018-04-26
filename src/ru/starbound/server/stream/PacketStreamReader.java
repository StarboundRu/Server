package ru.starbound.server.stream;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PacketStreamReader {
	private DataInputStream in;
	public PacketStreamReader(InputStream in) throws IOException {
		this.in = new DataInputStream(in);
	}
	
	public byte[] readBytes(int length) throws IOException {
		byte[] b = new byte[length];
		in.read(b);
		return b;
	}
	
	public int readByte() throws IOException {
		return in.read();
	}
	public int readInt() throws IOException {
		return in.readInt();
	}
	public int readVlqU() throws IOException {
		int res = 0;
		//int step = 0;
		while (true) {
			int b = in.read();
			System.out.printf(" < VLQ %02x\n", b);
			
			res = (res << 7) | (b & 0x7f);
			//res |= (b & 0x7f) << step;
			if ((b & 0x80) == 0) {
				break;
			}
			//step += 7;
		}
		return res;
	}
	
	public int readVlqI() throws IOException {
		int res = 0;
		//int step = 0;
		while (true) {
			int b = in.read();
			System.out.printf(" < VLQ %02x\n", b);
			
			res = (res << 7) | (b & 0x7f);
			//res |= (b & 0x7f) << step;
			if ((b & 0x80) == 0) {
				break;
			}
			//step += 7;
		}
		if ((res & 1) == 1) {
			res = - (res >> 1);
		}
		else {
			res = res >> 1;
		}
		return res;
	}
	
	
	public byte[] readByteArray() throws IOException {
		int len = readVlqU();
		byte[] b = new byte[len];
		in.read(b);
		return b;
	}
	
	public void readVariant() throws IOException {
		int vType = readByte();
		System.out.printf(" < read %d variant type\n", vType);
		switch (vType) {
			case 1: break;
//			case 2:	readDouble(); break;
//			case 3: readBool(); break;
//			case 4: readVlqI(); break;
//			case 5: readString(); break;
//			case 6: readList(); break;
//			case 7: readMap(); break;
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
		return new String(readByteArray());
	}
	
	public int available() throws IOException {
		return in.available();
	}
}
