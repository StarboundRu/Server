package ru.starbound.server.packets;

import java.io.IOException;

public class ChatReceivePacket extends Packet {
	private int contextFlag;
	private String context;
	private int flag;
	private String from;
	private String text;

	
	public ChatReceivePacket(int contextFlag, String context, int flag, String from, String text) {
		super(5);
		this.contextFlag = contextFlag;
		this.context = context;
		this.flag = flag;
		this.from = from;
		this.text = text;
	}
	
	public ChatReceivePacket(int packetType, byte[] packetBody, boolean compressed) throws IOException {
		super(5, packetBody, compressed);
	}
	
	public void setString(String value) {
		needUpdateBuffer = true;
		text = value;
	}
	
	@Override
	public void load() throws IOException {
		//stream.readMessageContext();
		contextFlag = stream.readByte();
		context = stream.readString();
		
		flag = stream.readInt();
		from = stream.readString();
		text = stream.readString();
		
		//System.out.printf("ChatReceivePacket (%d, %s, %d, %s, %s)\n", flag, context, flag2, from, text);
	}
	
	@Override
	public void save() throws IOException {
		stream.reset();
		stream.writeByte(contextFlag);
		stream.writeString(context);
		
		stream.writeInt(flag);
		stream.writeString(from);
		stream.writeString(text);
	}
}
