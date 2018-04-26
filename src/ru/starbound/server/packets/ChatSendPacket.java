package ru.starbound.server.packets;

import java.io.IOException;

public class ChatSendPacket extends Packet {
	public String text;
	public int flag;

	public ChatSendPacket() {
		super(11);
	}
	
	public ChatSendPacket(int packetType, byte[] packetBody, boolean compressed) throws IOException {
		super(11, packetBody, compressed);
	}
	
	@Override
	public void load() throws IOException {
		text = stream.readString();
		flag = stream.readByte();
	}
}
