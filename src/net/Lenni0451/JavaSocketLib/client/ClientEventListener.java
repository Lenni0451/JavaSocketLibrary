package net.Lenni0451.JavaSocketLib.client;

import net.Lenni0451.JavaSocketLib.packets.IPacket;

public interface ClientEventListener {
	
	public default void onPreConnect() {}
	public default void onConnectionEstablished() {}
	public default void onDisconnect() {}
	
	public default void onRawPacketReceive(final byte[] packet) {}
	public default void onPacketReceive(final IPacket packet) {}
	
}
