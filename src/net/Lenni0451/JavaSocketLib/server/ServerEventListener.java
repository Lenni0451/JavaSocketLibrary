package net.Lenni0451.JavaSocketLib.server;

import net.Lenni0451.JavaSocketLib.packets.IPacket;

public abstract class ServerEventListener {
	
	public void onSocketPreConnect(final ClientConnection client) {}
	public void onSocketConnectionEstablished(final ClientConnection client) {}
	public void onSocketDisconnect(final ClientConnection client) {}
	
	public void onEncryptionResponse(final ClientConnection client) {}
	public void onRawPacketReceive(final ClientConnection client, final byte[] packet) {}
	public void onPacketReceive(final ClientConnection client, final IPacket packet) {}
	
	public void onServerClose() {}
	
}
