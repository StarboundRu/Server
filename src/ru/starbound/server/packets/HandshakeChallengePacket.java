package ru.starbound.server.packets;

import java.io.IOException;

import ru.starbound.server.stream.PacketStreamWriter;

public class HandshakeChallengePacket extends Packet {
	public HandshakeChallengePacket() {
		super(4);
	}
	
	@Override
	public void write(PacketStreamWriter stream) throws IOException {
		stream.writeByte(0x04);
		stream.writeByte(0x44); // length
		stream.writeString("");
		stream.writeString("JYBUuhpwURxYLOaiNASJpFgmdmELPV5R");
		// 044400204a59425575687077555278594c4f61694e41534a7046676d646d454c50563552
	}
}
