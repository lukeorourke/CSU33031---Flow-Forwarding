import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class TermClient {
	
	public static String stdmsg = "Hello Terminal World";

	public static void main(String[] args) {		
		DatagramSocket socket;
		
		System.out.println("--- Client start ---");
		
		Terminal terminal= new Terminal(args[0]);	
		
		try {
			socket= new DatagramSocket();

			// Create header
			byte packetType = Header.PUB;
			byte contentType = Header.VIDEO;
			int prodID = 0xabcdef; 
			int streamNo = 0x05;
			int streamID = (prodID<<8) | (streamNo&0xff);
			byte[] header = ByteBuffer.allocate(6).put(packetType).put(contentType).putInt(streamID).array();

			// Create buffer with header and payload
			byte[] payload = TermClient.stdmsg.getBytes();
			byte[] buffer = new byte[header.length+payload.length];
			System.arraycopy(header, 0, buffer,0, header.length);
			System.arraycopy(payload, 0, buffer, header.length, payload.length);

			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			InetSocketAddress dstAddress=new InetSocketAddress(args[1],Server.port);
			packet.setSocketAddress(dstAddress);
			socket.send(packet);
			terminal.println("Sent msg: "+stdmsg);

			// Wait for a possible reply
			buffer = new byte[65535];
			packet = new DatagramPacket(buffer, buffer.length);
			socket.receive(packet);

			terminal.println("Rcv: "+packet.getAddress()+":"+packet.getLength());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("--- Client finish ---");

	}
}
