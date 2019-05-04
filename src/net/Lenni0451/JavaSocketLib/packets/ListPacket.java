package net.Lenni0451.JavaSocketLib.packets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListPacket extends SimplePacket {
	
	private final List<Object> list;
	
	public ListPacket() {
		this.list = new ArrayList<>();
	}
	
	public ListPacket(final Object... objects) {
		this();
		
		Collections.addAll(this.list, objects);
	}
	
	public List<Object> getList() {
		return this.list;
	}
	
	@Override
	public void writePacketData(List<Object> list) {
		list.addAll(this.list);
	}

	@Override
	public void readPacketData(List<Object> list) {
		this.list.addAll(list);
	}
	
}
