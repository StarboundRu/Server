package ru.starbound.server.packets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import ru.starbound.server.stream.PacketStream;
import ru.starbound.server.stream.PacketStreamReader;
import ru.starbound.server.stream.PacketStreamWriter;

public class Packet {
	private int packetType;
	private byte[] packetBody;
	private byte[] compressedBody;
	private boolean compressed;
	
	protected PacketStream stream;
	
	public Packet(int packetType) {
		this.packetType = packetType;
		
		stream = new PacketStream();
		needUpdateBuffer = true;
	}
	
	public Packet(int packetType, byte[] packetBody, boolean compressed) throws IOException {
		this.compressed = compressed;
		this.packetType = packetType;
		this.packetBody = compressed ? null : packetBody;
		this.compressedBody = compressed ? packetBody : null;
		
		if (compressed) {
			this.compressedBody = packetBody;
			this.uncompress();
		}
		else {
			this.packetBody = packetBody;
			this.compressedBody = null;
		}
		
		stream = new PacketStream(this.packetBody, this.packetBody.length);
		this.load();
	}
	
//	public Packet(int packetType, byte[] packetBody, byte[] compressedBody) {
//		this.packetType = packetType;
//		this.packetBody = packetBody;
//		this.compressedBody = compressedBody;
//	}
	
	public int getPacketType() {
		return packetType;
	}
	
	private void uncompress() {
		try {
			Inflater inflater = new Inflater();
			inflater.setInput(compressedBody);
			
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedBody.length);
			
			byte[] buffer = new byte[1024];
			while (!inflater.finished()) {
				int count = inflater.inflate(buffer);
				outputStream.write(buffer, 0, count);
				if (count == 0) break;
				//System.out.printf("     decompressed chunk %d bytes\n", count);
			}
			outputStream.close();
			this.packetBody = outputStream.toByteArray();
			
			//System.out.printf("     decompressed to %d bytes\n", packetBody.length);
			
			//FileOutputStream fos = new FileOutputStream("d:/temp/starbound/uncompressed.bin");
			//fos.write(body);
			//fos.close();
			
		} catch (DataFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//public ByteBuffer asByteBuffer() {
	//	ByteBuffer buffer = ByteBuffer.allocate(capacity)
	//}
	
	public void read(PacketStreamReader stream) throws IOException {
		
	}
	public void write(PacketStreamWriter stream) throws IOException {
		stream.writeVlqU(packetType);
		byte[] body = compressedBody != null ? compressedBody : packetBody;
		stream.writeVlqI(body.length);
		stream.writeBytes(body);
	}
	
	
//	private static int readVlqU(InputStream stream) throws IOException {
//		int res = 0;
//		while (true) {
//			int b = stream.read();
//			res = (res << 7) | (b & 0x7f);
//			if ((b & 0x80) == 0) {
//				break;
//			}
//		}
//		return res;
//	}
//	private static int readVlqI(InputStream stream) throws IOException {
//		int res = readVlqU(stream);
//		if ((res & 1) == 1) {
//			res = - (res >> 1);
//		}
//		else {
//			res = res >> 1;
//		}
//		return res;
//	}
	
	public void send(OutputStream stream) throws IOException {
		write(new PacketStreamWriter(stream));
	}
	
	public void load() throws IOException {
	}
	
	private void writeVlqI(ByteBuffer buffer, int value) {
		if (value < 0) {
			value = -value;
			value = (value << 1) | 1;
		}
		else {
			value <<= 1;
		}
		writeVlqU(buffer, value);
	}
	public void writeVlqU(ByteBuffer buffer, int value) {
		if (value == 0) {
			buffer.put((byte) 0);
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
			
			//System.out.printf("  > %02x\n", b);
			buffer.put((byte) b);
		}
	}
	
	protected boolean needUpdateBuffer = false;
	protected void save() throws IOException {
		
	}
	
	public ByteBuffer asByteBuffer() throws IOException {
		if (needUpdateBuffer) save();
		
		
		if (compressed) {
			ByteBuffer buffer = ByteBuffer.allocate(compressedBody.length + 12);
			writeVlqU(buffer, packetType);
			
			writeVlqI(buffer, -compressedBody.length);
			buffer.put(compressedBody, 0, compressedBody.length);
			buffer.flip();
			return buffer;
		}
		else {
			ByteBuffer buffer = ByteBuffer.allocate(stream.length() + 12);
			writeVlqU(buffer, packetType);
			
			writeVlqI(buffer, stream.length());
			buffer.put(stream.getBytes(), 0, stream.length());
			buffer.flip();
			return buffer;
		}
		
	}
	
//	public static Packet load(InputStream stream) throws IOException {
//		System.out.printf(" < read packet\n");
//		int packetType = readVlqU(stream);
//		int bodyLength = readVlqI(stream);
//		byte[] body = null;
//		byte[] compressed = null;
//		
//		System.out.printf(" < Packet [%d] with [%d] body length\n", packetType, bodyLength);
//		
//		if (bodyLength < 0) {
//			bodyLength = -bodyLength;
//			compressed = new byte[bodyLength];
//			int totalReaded = 0;
//			while (totalReaded < bodyLength) {
//				int readed = stream.read(compressed, totalReaded, bodyLength - totalReaded);
//				if (readed >= 0) {
//					totalReaded += readed;
//				}
//			}
//			
//			System.out.printf("     load compressed %d bytes (%d readed)\n", compressed.length, totalReaded);
//
//			// unpack
//			try {
//				Inflater inflater = new Inflater();
//				inflater.setInput(compressed);
//				
//				ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressed.length);
//				
//				byte[] buffer = new byte[1024];
//				while (!inflater.finished()) {
//					int count = inflater.inflate(buffer);
//					outputStream.write(buffer, 0, count);
//					if (count == 0) break;
//					//System.out.printf("     decompressed chunk %d bytes\n", count);
//				}
//				outputStream.close();
//				body = outputStream.toByteArray();
//				
//				System.out.printf("     decompressed to %d bytes\n", body.length);
//				
//				//FileOutputStream fos = new FileOutputStream("d:/temp/starbound/uncompressed.bin");
//				//fos.write(body);
//				//fos.close();
//				
//			} catch (DataFormatException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//		}
//		else {
//			System.out.printf("     load plain %d bytes\n", bodyLength);
//			
//			body = new byte[bodyLength];
//			stream.read(body);
//		}
//
//		Packet packet = null;
//		switch (packetType) {
//			case 7: {
//				packet = new ClientConnectPacket();
//				break;
//			}
//			case 9: {
//				packet = new HandshakeResponsePacket();
//				break;
//			}
//			default: {
//				System.out.printf(" < Unknown packet. Skip.\n");
//				packet = new Packet(packetType, body, compressed);
//			}
//		}
//		
//		ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
//		packet.read(new PacketStreamReader(inputStream));
//		
//		return packet;
//	}
}

/*

1: ProtocolVersionPacket
2: ConnectResponsePacket
3: PacketBase<_PacketType_3>
4: HandshakeChallengePacket
5: ChatReceivePacket
6: UniverseTimeUpdatePacket
7: ClientConnectPacket
8: PacketBase<_PacketType_8>
9: HandshakeResponsePacket
10: WarpCommandPacket
11: ChatSendPacket
12: ClientContextUpdatePacket
13: WorldStartPacket
14: WorldStopPacket
15: TileArrayUpdatePacket
16: TileUpdatePacket
17: TileLiquidUpdatePacket
18: TileDamageUpdatePacket
19: TileModificationFailurePacket
20: GiveItemPacket
21: WeatherUpdatePacket
22: SwapInContainerResultPacket
23: SkyUpdatePacket
25: ModifyTileListPacket
26: DamageTilePacket
27: DamageTileGroupPacket
28: RequestDropPacket
29: SpawnEntityPacket
30: EntityInteractPacket
24: EntityInteractResultPacket
31: ConnectWirePacket
32: DisconnectAllWiresPacket
33: OpenContainerPacket
34: CloseContainerPacket
35: SwapInContainerPacket
36: ItemApplyInContainerPacket
37: StartCraftingInContainerPacket
38: StopCraftingInContainerPacket
39: BurnContainerPacket
40: ClearContainerPacket
41: WorldClientStateUpdatePacket
42: EntityCreatePacket
43: EntityUpdatePacket
44: EntityDestroyPacket
45: DamageNotificationPacket
46: StatusEffectRequestPacket
47: UpdateWorldPropertiesPacket
48: HeartbeatPacket

*/
