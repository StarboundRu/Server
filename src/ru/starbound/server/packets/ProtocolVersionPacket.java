package ru.starbound.server.packets;

import java.io.IOException;

import ru.starbound.server.stream.PacketStreamReader;
import ru.starbound.server.stream.PacketStreamWriter;


public class ProtocolVersionPacket extends Packet {
	public int version;
	
	public ProtocolVersionPacket(int packetType, byte[] packetBody, boolean compressed) throws IOException {
		super(1, packetBody, compressed);
	}
	public ProtocolVersionPacket(int version) {
		super(1);
		this.version = version;
	}
	
	@Override
	public void load() throws IOException {
		version = stream.readInt();
	}
	
	@Override
	public void read(PacketStreamReader stream) throws IOException {
		version = stream.readInt();
	}

	@Override
	public void write(PacketStreamWriter stream) throws IOException {
		stream.writeByte(0x07);
		stream.writeByte(0x08);
		stream.writeInt(version);
		
//		stream.writeByte(0x00);
//		stream.writeByte(0x00);
//		stream.writeByte(0x01);
//		stream.writeByte(0x68);
	}
	
}
