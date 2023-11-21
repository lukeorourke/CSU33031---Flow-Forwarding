import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class ClientReceiver {
    private DatagramSocket socket;
    public static final byte[] identifier = new byte[]{1, 2, 3, 4};

    public ClientReceiver(String routerAddress) throws IOException {
        // Bind the socket to a specific port to listen for incoming packets
        socket = new DatagramSocket(Server.port);

        // Send a packet to the router with the identifier as the header
        sendPacketToRouter(routerAddress);
    }

    private void sendPacketToRouter(String routerAddress) throws IOException {
        // Create the packet with the identifier as the header
        byte[] buffer = ByteBuffer.allocate(Header.HeaderSize + identifier.length).put(Header.RECEIVERHELLO).put(identifier).array();
        InetAddress address = InetAddress.getByName(routerAddress);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, Server.port);

        // Send the packet
        socket.send(packet);
    }



    public void listen() {
        try {
            while (true) {
                // Buffer to receive incoming packets
                byte[] buffer = new byte[65535];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Receive packet
                socket.receive(packet);
                System.out.println("Received packet from: " + packet.getAddress());

                // Process the received packet
                processPacket(packet);

                // Construct and send a response packet
                sendResponse(packet.getAddress(), packet.getPort());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    private void processPacket(DatagramPacket packet) {
        // Implement packet processing logic here
        byte[] data = packet.getData();
        ByteBuffer header = ByteBuffer.wrap(data,0, Header.HeaderSize);
        byte packetType= header.get();
        switch(packetType) {
            case Header.PUB:
                System.out.println("received pub");
                break;
            case Header.HELLO:
                System.out.println("received hello");
                break;
            default:
                System.out.println("Rcv: Unknown packet type");
        }
    }

    private void sendResponse(InetAddress returnAddr, int returnPort) throws IOException {
        // Implement response packet sending logic here
        String responseData = "Response message";
        byte header = Header.RESPONSE;
        byte[] data = responseData.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(Header.HeaderSize + data.length);
        buffer.put(header);
        buffer.put(data);
        DatagramPacket responsePacket = new DatagramPacket(buffer.array(), buffer.array().length, returnAddr, returnPort);

        socket.send(responsePacket);
        System.out.println("Sent response to: " + returnAddr);
    }

    public static void main(String[] args) {
        try {
            String routerHostname = args[0];
            InetAddress routerAddress = InetAddress.getByName(routerHostname);
            ClientReceiver receiver = new ClientReceiver(routerAddress.getHostAddress());
            receiver.listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
