package net.Lenni0451.JavaSocketLib.server;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;

import net.Lenni0451.JavaSocketLib.packets.IPacket;
import net.Lenni0451.JavaSocketLib.utils.RSACrypter;

public class ClientConnection {
	
	private final SocketServer server;
	private final Socket socket;
	private final InetAddress address;
	private final DataInputStream dataInputStream;
	private final DataOutputStream dataOutputStream;

	private PrivateKey decryptionKey = null;
	private PublicKey encryptionKey = null;
	
	private long latency = -1;
	private boolean terminated = false;
	
	public ClientConnection(final SocketServer server, final Socket socket) {
		this.server = server;
		this.socket = socket;
		this.address = socket.getInetAddress();
		try {
			this.dataInputStream = new DataInputStream(socket.getInputStream());
			this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
		} catch (Exception e) {
			this.terminateConnection();
			throw new IllegalStateException("Socket is closed or not ready yet", e);
		}
	}
	
	public Socket getSocket() {
		return this.socket;
	}
	
	public InetAddress getAddress() {
		return this.address;
	}
	
	public DataInputStream getInputStream() {
		return this.dataInputStream;
	}
	
	public DataOutputStream getOutputStream() {
		return this.dataOutputStream;
	}
	
	public boolean terminateConnection() {
		this.terminated = true;
		try {
			this.dataInputStream.close();
			this.dataOutputStream.close();
			this.socket.close();
			return true;
		} catch (Exception e) {}
		return false;
	}
	
	public void updateLatency(final long latency) {
		this.latency = latency;
	}
	
	public long getLatency() {
		return this.latency;
	}
	
	
	public void setDecryptionKey(final PrivateKey decryptionKey) {
		if(this.encryptionKey != null) {
			throw new IllegalStateException("Decryption key is already set");
		}
		
		this.decryptionKey = decryptionKey;
	}
	
	public PrivateKey getDecryptionKey() {
		return this.decryptionKey;
	}
	
	public void setEncryptionKey(final PublicKey encryptionKey) {
		if(this.encryptionKey != null) {
			throw new IllegalStateException("Encryption key is already set");
		}
		
		this.encryptionKey = encryptionKey;
	}
	
	public PublicKey getEncryptionKey() {
		return this.encryptionKey;
	}

	
	public void sendRawPacket(byte[] data) throws IOException {
		if(this.terminated) {
			throw new IllegalStateException("Client connection has been terminated");
		}
		
		if(this.encryptionKey != null) {
			try {
				data = RSACrypter.encrypt(this.encryptionKey, data);
			} catch (Exception e) {
				new IOException("Could not encrypt packet data for client " + this.socket.getInetAddress().getHostAddress(), e).printStackTrace();
			}
		}
		this.dataOutputStream.writeInt(data.length);
		this.dataOutputStream.write(data);
	}
	
	public void sendPacket(final IPacket packet) {
		if(this.terminated) {
			throw new IllegalStateException("Client connection has been terminated");
		}
		
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeUTF(this.server.getPacketRegister().getPacketLabel(packet.getClass()));
			packet.writePacketData(dos);
			this.sendRawPacket(baos.toByteArray());
		} catch (Exception e) {
			new IOException("Could not serialize packet for " + this.address.getHostAddress(), e).printStackTrace();
			return;
		}
	}
	
}
