package net.Lenni0451.JavaSocketLib.server;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.Lenni0451.JavaSocketLib.packets.IPacket;
import net.Lenni0451.JavaSocketLib.packets.PacketRegister;
import net.Lenni0451.JavaSocketLib.packets.impl.PingPacket;
import net.Lenni0451.JavaSocketLib.utils.RSACrypter;

public class SocketServer {
	
	private final int port;
	private ServerSocket socket;
	private int maxPacketSize = 32767;
	private boolean useEncryption;
	
	private Thread socketAcceptor;
	private Timer timer;
	private TimerTask pingTimer;
	
	private Map<ClientConnection, Thread> clients;
	private List<ServerEventListener> eventListener;
	
	private PacketRegister packetRegister;
	
	public SocketServer(final int port) {
		this(port, true);
	}
	
	public SocketServer(final int port, final boolean useEncryption) {
		this.port = port;
		this.useEncryption = useEncryption;
		
		this.clients = new HashMap<>();
		this.eventListener = new ArrayList<>();
		this.packetRegister = new PacketRegister();
	}
	
	public void bind() throws IOException {
		if(this.socket != null && this.socket.isBound()) {
			throw new IllegalStateException("Server socket is already bound to port " + this.port);
		}
		
		this.socket = new ServerSocket();
		this.socket.bind(new InetSocketAddress(this.port));
		this.socketAcceptor = new Thread(() -> {
			while(!this.socketAcceptor.isInterrupted() && this.socket.isBound()) {
				try {
					Socket socket = this.socket.accept();
					ClientConnection clientConnection = new ClientConnection(this, socket);
					{ //Call event
						for(ServerEventListener serverEventListener : this.eventListener.toArray(new ServerEventListener[0])) {
							try {
								serverEventListener.onSocketPreConnect(clientConnection);
								if(!this.useEncryption) {
									serverEventListener.onSocketConnectionEstablished(clientConnection);
								}
							} catch (Throwable t) {
								new Exception("Unhandled exception in server event listener", t).printStackTrace();
							}
						}
					}
					
					Thread clientListener;
					this.clients.put(clientConnection, clientListener = new Thread() {
						@Override
						public void run() {
							DataInputStream dataInputStream = clientConnection.getInputStream();
							while(!this.isInterrupted() && socket.isConnected() && !socketAcceptor.isInterrupted()) {
								try {
									int packetLength = dataInputStream.readInt();
									if(packetLength > maxPacketSize) {
										System.err.println("Client packet is over max size of " + maxPacketSize);
										try {
											dataInputStream.skipBytes(packetLength);
										} catch (Exception e) {
											new IOException("Could not skip bytes for too large packet " + clientConnection.getAddress().getHostAddress(), e).printStackTrace();
											break;
										}
										continue;
									}
									byte[] packet = new byte[packetLength];
									dataInputStream.read(packet);
									
									onRawPacketReceive(clientConnection, packet);
								} catch (EOFException | SocketException | SocketTimeoutException e) {
									;
								} catch (Exception e) {
									new IOException("Could not receive packet for client " + clientConnection.getAddress().getHostAddress(), e).printStackTrace();
									break;
								}
							}
							onClientDisconnect(clientConnection);
						}
					});
					clientListener.start();
					this.onClientConnect(clientConnection);
				} catch (Exception e) {
					if(e instanceof EOFException || (e instanceof SocketException && e.getMessage().equalsIgnoreCase("Socket closed"))) {
						;
					} else {
						new IOException("Unable to accept client socket", e).printStackTrace();
					}
				}
			}
		});
		this.socketAcceptor.start();
		
		this.timer = new Timer();
		this.timer.schedule(this.pingTimer = new TimerTask() {
			@Override
			public void run() {
				for(ClientConnection clientConnection : clients.keySet()) {
					if((clientConnection.getEncryptionKey() != null && clientConnection.getDecryptionKey() != null) || !useEncryption) {
						clientConnection.sendPacket(new PingPacket(System.currentTimeMillis()));
					}
				}
			}
		}, 0, 10000);
	}
	
	public void stop() throws IOException {
		this.socketAcceptor.interrupt();
		this.socket.close();
		
		//Cleanup
		this.pingTimer.cancel();
		for(ClientConnection clientConnection : this.clients.keySet()) {
			clientConnection.terminateConnection();
		}
		this.clients.clear();
		
		{ //Call event
			for(ServerEventListener serverEventListener : this.eventListener.toArray(new ServerEventListener[0])) {
				try {
					serverEventListener.onServerClose();
				} catch (Throwable t) {
					new Exception("Unhandled exception in server event listener", t).printStackTrace();
				}
			}
		}
	}
	
	public void addEventListener(final ServerEventListener serverEventListener) {
		this.eventListener.add(serverEventListener);
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
	
	
	private void onClientConnect(final ClientConnection clientConnection) {
		if(this.useEncryption) {
			try {
				KeyPair keyPair = RSACrypter.generateKeyPair(2048);
				clientConnection.setDecryptionKey(keyPair.getPrivate());
				clientConnection.sendRawPacket(keyPair.getPublic().getEncoded());
			} catch (Exception e) {
				new IOException("Could not send encryption key to client " + clientConnection.getAddress().getHostAddress(), e).printStackTrace();
			}
		} else {
			try {
				clientConnection.sendRawPacket(new byte[] {0});
			} catch (Exception e) {
				new IOException("Could not send unencrypted info to client " + clientConnection.getAddress().getHostAddress(), e).printStackTrace();
			}
		}
	}
	
	private void onClientDisconnect(final ClientConnection clientConnection) {
		clientConnection.terminateConnection();
		this.clients.remove(clientConnection);
		
		{ //Call event
			for(ServerEventListener serverEventListener : this.eventListener.toArray(new ServerEventListener[0])) {
				try {
					serverEventListener.onSocketDisconnect(clientConnection);
				} catch (Throwable t) {
					new Exception("Unhandled exception in server event listener", t).printStackTrace();
				}
			}
		}
	}
	
	private void onRawPacketReceive(final ClientConnection clientConnection, byte[] packet) {
		if(clientConnection.getEncryptionKey() == null && this.useEncryption) {
			try {
				clientConnection.setEncryptionKey(RSACrypter.initPublicKey(packet));
				
				{ //Call event
					for(ServerEventListener serverEventListener : this.eventListener.toArray(new ServerEventListener[0])) {
						try {
							serverEventListener.onSocketConnectionEstablished(clientConnection);
						} catch (Throwable t) {
							new Exception("Unhandled exception in server event listener", t).printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				new IOException("Could not create encryption key from client " + clientConnection.getAddress().getHostAddress(), e).printStackTrace();
				clientConnection.terminateConnection();
			}
			return;
		}
		
		if(clientConnection.getDecryptionKey() != null && this.useEncryption) {
			try {
				packet = RSACrypter.decrypt(clientConnection.getDecryptionKey(), packet);
			} catch (Exception e) {
				new IOException("Could not decrypt packet data for client " + clientConnection.getAddress().getHostAddress(), e).printStackTrace();
			}
		}
		
		try {
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet));
			String packetLabel = dis.readUTF();
			Class<? extends IPacket> packetClass = this.packetRegister.getPacketClass(packetLabel);
			IPacket packetObject = packetClass.newInstance();
			packetObject.readPacketData(dis);
			
			if(packetObject instanceof PingPacket) {
				clientConnection.updateLatency(System.currentTimeMillis() - ((PingPacket) packetObject).getSystemTime());
				return;
			}
			
			{ //Call event
				for(ServerEventListener serverEventListener : this.eventListener.toArray(new ServerEventListener[0])) {
					try {
						serverEventListener.onPacketReceive(clientConnection, packetObject);
					} catch (Throwable t) {
						new Exception("Unhandled exception in server event listener", t).printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			{ //Call event
				for(ServerEventListener serverEventListener : this.eventListener.toArray(new ServerEventListener[0])) {
					try {
						serverEventListener.onRawPacketReceive(clientConnection, packet);
					} catch (Throwable t) {
						new Exception("Unhandled exception in server event listener", t).printStackTrace();
					}
				}
			}
		}
	}
	
	
	public ClientConnection[] getClients() {
		return this.clients.keySet().toArray(new ClientConnection[0]);
	}
	
	public void broadcastRawPacket(final byte[] packet) {
		for(ClientConnection clientConnection : this.clients.keySet()) {
			try {
				clientConnection.sendRawPacket(packet);
			} catch (Exception e) {
				new Exception("Could broadcast raw packet to client " + clientConnection.getAddress().getHostAddress(), e).printStackTrace();
			}
		}
	}
	
	public void broadcastPacket(final IPacket packet) {
		for(ClientConnection clientConnection : this.clients.keySet()) {
			try {
				clientConnection.sendPacket(packet);
			} catch (Exception e) {
				new Exception("Could broadcast packet to client " + clientConnection.getAddress().getHostAddress(), e).printStackTrace();
			}
		}
	}
	
}
