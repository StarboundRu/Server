package ru.starbound.server.packets;

import java.io.IOException;

import ru.starbound.server.stream.PacketStreamReader;

public class HandshakeResponsePacket extends Packet {
	public HandshakeResponsePacket() {
		super(9);
	}
	
	@Override
	public void read(PacketStreamReader stream) throws IOException {
		String s1 = stream.readString();
		String s2 = stream.readString();
		String s3 = stream.readString();
		
		int i1 = stream.readInt();
		
		System.out.printf(" < HandshakeResponsePacket(%s, %s, %s, %d)\n", s1, s2, s3, i1);
	}
}
