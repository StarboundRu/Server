package ru.starbound.server.packets;

import java.io.FileOutputStream;
import java.io.IOException;

import ru.starbound.server.stream.PacketStreamReader;
import ru.starbound.server.stream.PacketStreamWriter;

public class ClientConnectPacket extends Packet {
	public byte[] bytes1;
	//public Variant variant;
	//public UUID uuid;
	public byte[] uuid;
	public String characterName;
	public String race;
	public byte[] playerFile;
	
	public ClientConnectPacket() {
		super(7);
	}
	
	public ClientConnectPacket(int packetType, byte[] packetBody, boolean compressed) throws IOException {
		super(7, packetBody, compressed);
	}
	
	@Override
	public void load() throws IOException {
		bytes1 = stream.readByteArray();
		stream.readVariant();
		uuid = stream.readUuid();
		characterName = stream.readString();
		race = stream.readString();
		playerFile = stream.readByteArray();
	}

	@Override
	public void read(PacketStreamReader stream) throws IOException {
		//int flag = stream.readByte();
		
		//System.out.printf(" < read flag [%d]\n", flag);
		
		bytes1 = stream.readByteArray();
		System.out.printf(" < read %d bytes\n", bytes1.length);
		
		stream.readVariant();
		uuid = stream.readUuid();
		
		if (uuid != null) {
			StringBuffer result = new StringBuffer();
			for (byte b:uuid) {
			    result.append(String.format("%02x", b));
			}
			String uuidString = result.toString();
			
			System.out.printf(" < read uuid: %s\n", uuidString);
		}
		
		characterName = stream.readString();
		race = stream.readString();
		
		System.out.printf(" < character %s race %s\n", characterName, race);
		
		playerFile = stream.readByteArray();
		
		FileOutputStream fos = new FileOutputStream("d:/temp/starbound/playerFile.bin");
		fos.write(playerFile);
		fos.close();
	}

	@Override
	public void write(PacketStreamWriter stream) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
}
