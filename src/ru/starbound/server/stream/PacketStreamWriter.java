package ru.starbound.server.stream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PacketStreamWriter {
	private DataOutputStream out;
	public PacketStreamWriter(OutputStream out) throws IOException {
		this.out = new DataOutputStream(out);
	}
	
	public void writeByte(int b) throws IOException {
		out.writeByte(b);
	}
	
	public void writeInt(int i) throws IOException {
		out.writeInt(i);
	}
	
	public void writeVlqU(int value) throws IOException {
		if (value == 0) {
			out.writeByte(0);
			return;
		}
		
		int tCnt = 0;
		while ((value & (0x7f << (7 * tCnt))) > 0) tCnt++;
		
		while (tCnt > 0) {
			tCnt--;
			
			int b = (value >> (7 * tCnt)) & 0x7f;
			if (tCnt > 0) b |= 0x80;
			
			out.writeByte(b);
		}
	}
	public void writeVlqI(int value) throws IOException {
		boolean negative = false;
		if (value < 0) {
			negative = true;
			value = -value;
		}
		
		value <<= 1;
		
		if (value == 0) {
			out.writeByte(0);
			return;
		}
		
		int tCnt = 0;
		while ((value & (0x7f << (7 * tCnt))) > 0) tCnt++;
		
		while (tCnt > 0) {
			tCnt--;
			
			int b = (value >> (7 * tCnt)) & 0x7f;
			if (tCnt > 0) b |= 0x80;
			else if (negative) b |= 0x01;
			
			out.writeByte(b);
		}
	}
	
	public void writeBytes(byte[] bytes) throws IOException {
		out.write(bytes);
	}
	
	public void writeByteArray(byte[] bytes) throws IOException {
		if (bytes == null || bytes.length == 0) {
			writeVlqU(0);			
		}
		else {
			writeVlqU(bytes.length);
			out.write(bytes);
		}
	}
	
	public void writeString(String string) throws IOException {
		writeByteArray(string.getBytes());
	}
}
