package net.Lenni0451.JavaSocketLib.server;

import net.Lenni0451.JavaSocketLib.packets.IPacket;

public interface ServerEventListener {
	
	public default void onSocketPreConnect(final ClientConnection client) {}
	public default void onSocketConnectionEstablished(final ClientConnection client) {}
	public default void onSocketDisconnect(final ClientConnection client) {}
	
	public default void onRawPacketReceive(final ClientConnection client, final byte[] packet) {}
	public default void onPacketReceive(final ClientConnection client, final IPacket packet) {}
	
	public default void onServerClose() {}
	
}
