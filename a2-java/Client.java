import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

public class Client {
	public static String stdmsg = "Hello World";
	public static byte[] identifier;


	public static void main(String[] args) {
		DatagramSocket socket;

		System.out.println("--- Client start ---");

		try {
			Thread.sleep(10000);
			byte[] destIdentifier = new byte[]{1, 2, 3, 4};
			socket= new DatagramSocket();

			// Create header
			byte packetType = Header.PUB;

			// Create buffer with header and payload
			byte[] header = ByteBuffer.allocate(Header.HeaderSize).put(packetType).put(destIdentifier).array();
			byte[] payload = Server.helloMessage.getBytes();
			byte[] buffer = new byte[header.length + payload.length];
			System.arraycopy(header, 0, buffer, 0, header.length);
			System.arraycopy(payload, 0, buffer, header.length, payload.length);

			// Wait for a possible reply
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			InetSocketAddress dstAddress=new InetSocketAddress(args[0],Server.port);
			packet.setSocketAddress(dstAddress);
			socket.send(packet);
			System.out.println("Sent msg: "+stdmsg);


			buffer = new byte[65535];
			packet = new DatagramPacket(buffer, buffer.length);
			socket.receive(packet);

			System.out.println("Received Response: "+packet.getSocketAddress()+" =="+packet.getLength());
			socket.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("--- Client finish ---");
	}

	private static byte[] generateRandomIdentifier() {
		Random random = new Random();
		byte[] identifier = new byte[4];  // 4 bytes
		random.nextBytes(identifier);
		return identifier;
	}

	// Helper method to convert bytes to hex string (for readability)
	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
