package net.Lenni0451.JavaSocketLib.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import net.Lenni0451.JavaSocketLib.packets.IPacket;
import net.Lenni0451.JavaSocketLib.packets.PacketRegister;
import net.Lenni0451.JavaSocketLib.packets.impl.PingPacket;
import net.Lenni0451.JavaSocketLib.utils.RSACrypter;

public class SocketClient {
	
	private String ip;
	private int port;
	private Socket socket;
	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;
	private int maxPacketSize = 32767;
	
	private Thread packetListener;
	private List<ClientEventListener> eventListener;
	
	private boolean useEncryption;
	private PublicKey encryptionKey;
	private PrivateKey decryptionKey;
	
	private PacketRegister packetRegister;

	public SocketClient(final String ip, final int port) {
		this.ip = ip;
		this.port = port;
		this.useEncryption = true;
		
		this.eventListener = new ArrayList<>();
		this.packetRegister = new PacketRegister();
	}
	
	public void connect() throws IOException {
		if(this.isConnected()) {
			throw new IllegalStateException("Client socket is already connected to address " + this.ip);
		}
		
		this.socket = new Socket();
		this.socket.setTcpNoDelay(true);
		this.socket.connect(new InetSocketAddress(this.ip, this.port));
		this.dataInputStream = new DataInputStream(this.socket.getInputStream());
		this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
		this.packetListener = new Thread(() -> {
			while(!this.packetListener.isInterrupted() && this.socket.isConnected()) {
				try {
					int packetLength = this.dataInputStream.readInt();
					if(packetLength > this.maxPacketSize) {
						System.err.println("Server packet is over max size of " + this.maxPacketSize);
						try {
							dataInputStream.skipBytes(packetLength);
						} catch (Exception e) {
							new IOException("Could not skip bytes for too large packet", e).printStackTrace();
							break;
						}
						continue;
					}
					byte[] packet = new byte[packetLength];
					dataInputStream.read(packet);
					
					this.onPacketReceive(packet);
				} catch (EOFException | SocketException | SocketTimeoutException e) {
					;
				} catch (Exception e) {
					new IOException("Could not receive packet", e).printStackTrace();
					break;
				}
			}
			this.onDisconnect();
		});
		this.packetListener.start();
		
		{ //Call event
			for(ClientEventListener clientEventListener : this.eventListener.toArray(new ClientEventListener[0])) {
				try {
					clientEventListener.onPreConnect();
					if(!this.useEncryption) {
						clientEventListener.onConnectionEstablished();
					}
				} catch (Throwable t) {
					new Exception("Unhandled exception in client event listener", t).printStackTrace();
				}
			}
		}
	}
	
	public void disconnect() {
		try {
			this.socket.shutdownInput();
			this.socket.close();
		} catch (Exception e) {}
	}
	
	public boolean isConnected() {
		return this.socket != null && this.socket.isConnected() && this.packetListener.isAlive() && !this.packetListener.isInterrupted();
	}
	
	public void addEventListener(final ClientEventListener clientEventListener) {
		this.eventListener.add(clientEventListener);
	}
	
	public void setMaxPacketSize(final int maxPacketSize) {
		this.maxPacketSize = maxPacketSize;
	}
	
	public int getMaxPacketSize() {
		return this.maxPacketSize;
	}
	
	public boolean isUsingEncryption() {
		return this.useEncryption;
	}
	
	public PacketRegister getPacketRegister() {
		return this.packetRegister;
	}
	
	
	private void onDisconnect() {
		try {
			this.socket.close();
		} catch (Exception e) {}
		this.packetListener.interrupt();
		
		this.encryptionKey = null;
		this.decryptionKey = null;
		
		{ //Call event
			for(ClientEventListener clientEventListener : this.eventListener.toArray(new ClientEventListener[0])) {
				try {
					clientEventListener.onDisconnect();
				} catch (Throwable t) {
					new Exception("Unhandled exception in client event listener", t).printStackTrace();
				}
			}
		}
	}
	
	private void onPacketReceive(byte[] packet) {
		if(this.encryptionKey == null && this.useEncryption) {
			try {
				if(packet.length == 1) {
					this.useEncryption = false;
					return;
				}
				KeyPair keyPair = RSACrypter.generateKeyPair(2048);
				this.sendRawPacket(keyPair.getPublic().getEncoded());
				this.decryptionKey = keyPair.getPrivate();
				this.encryptionKey = RSACrypter.initPublicKey(packet);
				
				{ //Call event
					for(ClientEventListener clientEventListener : this.eventListener.toArray(new ClientEventListener[0])) {
						try {
							clientEventListener.onConnectionEstablished();
						} catch (Throwable t) {
							new Exception("Unhandled exception in client event listener", t).printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				new IOException("Could not create encryption key from server", e).printStackTrace();
				this.disconnect();
			}
			return;
		}
		
		if(this.decryptionKey != null && this.useEncryption) {
			try {
				packet = RSACrypter.decrypt(this.decryptionKey, packet);
			} catch (Exception e) {
				new IOException("Could not encrypt packet data", e).printStackTrace();
			}
		}
		
		try {
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet));
			String packetLabel = dis.readUTF();
			Class<? extends IPacket> packetClass = this.packetRegister.getPacketClass(packetLabel);
			IPacket packetObject = packetClass.newInstance();
			packetObject.readPacketData(dis);
			
			if(packetObject instanceof PingPacket) {
				this.sendPacket(packetObject);
				return;
			}
			
			{ //Call event
				for(ClientEventListener clientEventListener : this.eventListener.toArray(new ClientEventListener[0])) {
					try {
						clientEventListener.onPacketReceive(packetObject);
					} catch (Throwable t) {
						new Exception("Unhandled exception in client event listener", t).printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			{ //Call event
				for(ClientEventListener clientEventListener : this.eventListener.toArray(new ClientEventListener[0])) {
					try {
						clientEventListener.onRawPacketReceive(packet);
					} catch (Throwable t) {
						new Exception("Unhandled exception in client event listener", t).printStackTrace();
					}
				}
			}
		}
	}
	
	
	public void sendRawPacket(byte[] data) throws IOException {
		if(!this.isConnected()) {
			throw new IllegalStateException("Client is not connected to a server");
		}
		
		if(this.encryptionKey != null && this.useEncryption) {
			try {
				data = RSACrypter.encrypt(this.encryptionKey, data);
			} catch (Exception e) {
				new IOException("Could not decrypt packet data", e).printStackTrace();
			}
		}
		this.dataOutputStream.writeInt(data.length);
		this.dataOutputStream.write(data);
	}
	
	public void sendPacket(final IPacket packet) {
		if(!this.isConnected()) {
			throw new IllegalStateException("Client is not connected to a server");
		}
		
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeUTF(this.packetRegister.getPacketLabel(packet.getClass()));
			packet.writePacketData(dos);
			this.sendRawPacket(baos.toByteArray());
		} catch (Exception e) {
			new IOException("Could not serialize packet", e).printStackTrace();
			return;
		}
	}
	
}
