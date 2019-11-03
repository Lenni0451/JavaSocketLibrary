package net.Lenni0451.JavaSocketLib.packets;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.Lenni0451.JavaSocketLib.packets.impl.PingPacket;
import net.Lenni0451.JavaSocketLib.server.ClientConnection;

public class PacketRegister {
	
	private final Map<String, Class<? extends IPacket>> packetTypes;
	private final List<Object> packetExecutor;
	
	public PacketRegister() {
		this.packetTypes = new HashMap<>();
		this.packetExecutor = new ArrayList<>();
		
		this.registerDefaults();
	}
	
	private void registerDefaults() {
		//System packets:
		this.packetTypes.put('\0' + "PingPacket", PingPacket.class);
		
		//Other packets
		this.packetTypes.put("ListPacket", ListPacket.class);
	}
	
	
	public void addPacket(final String label, final Class<? extends IPacket> packetClass) {
		if(label.startsWith("\0")) {
			throw new IllegalArgumentException("Can not add a system packet");
		}
		
		this.packetTypes.put(label, packetClass);
	}
	
	public void removePacket(final String label) {
		if(label.startsWith("\0")) {
			throw new IllegalArgumentException("Can not remove a system packet");
		}
		
		this.packetTypes.remove(label);
	}
	
	public Class<? extends IPacket> getPacketClass(final String label) {
		return this.packetTypes.get(label);
	}

	public String getPacketLabel(Class<? extends IPacket> packetClass) {
		for(Map.Entry<String, Class<? extends IPacket>> entry : this.packetTypes.entrySet()) {
			if(entry.getValue().equals(packetClass)) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	
	public void addPacketExecutor(final Object packetExecutor) {
		this.packetExecutor.add(packetExecutor);
	}
	
	public void removePacketExecutor(final Object packetExecutor) {
		this.packetExecutor.remove(packetExecutor);
	}
	
	public void callPacketExecutor(final IPacket packet) {
		this.callPacketExecutor(null, packet);
	}
	
	public void callPacketExecutor(final ClientConnection clientConnection, final IPacket packet) {
		for(Object packetExecutor : this.packetExecutor) {
			try {
				Class<?> executorClass = ((packetExecutor instanceof Class) ? ((Class<?>) packetExecutor) : (packetExecutor.getClass()));
				Object executorInstance = ((packetExecutor instanceof Class) ? (null) : (packetExecutor));
				for(Method method : executorClass.getMethods()) {
					if(executorInstance == null && !Modifier.isStatic(method.getModifiers())) {
						continue;
					}
					Class<?>[] parameter = method.getParameterTypes();
					if(parameter.length == 1 && parameter[0].equals(packet.getClass())) {
						method.invoke(executorInstance, packet);
					} else if(parameter.length == 2 && parameter[0].equals(ClientConnection.class) && parameter[1].equals(packet.getClass())) {
						method.invoke(executorInstance, clientConnection, packet);
					}
				}
			} catch (NullPointerException e) {
				;
			} catch (Throwable t) {
				new Exception("Unhandled exception in packet executor", t).printStackTrace();
			}
		}
	}
	
}
