package net.beaconcontroller.overlaymanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.overlaymanager.Overlay;
import net.beaconcontroller.overlaymanager.Segment;

public class Tenant extends Overlay {
	protected Map<Long, Segment> segments;
	
	public Tenant(String name) {
		super(name);	
		segments = new HashMap<Long, Segment>();
	}
	
	public Tenant(long id, String name) {
		super(id, name);	
		segments = new HashMap<Long, Segment>();
	}
	
	public Tenant(String name, ArrayList<Device> devices, ArrayList<Overlay> allowList) {
		super(name,devices,allowList);
		segments = new HashMap<Long, Segment>();
	}
	
	public Tenant(long id, String name, ArrayList<Device> devices,
			ArrayList<Overlay> allowList) {
		super(id, name, devices, allowList);
		segments = new HashMap<Long, Segment>();
	}
	
	public Map<Long, Segment> getSegments(){
		return segments;
	}
}
