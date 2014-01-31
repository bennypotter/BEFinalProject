package net.beaconcontroller.overlaymanager;

import java.util.ArrayList;
import java.util.List;

import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.overlaymanager.Overlay;
import net.beaconcontroller.overlaymanager.Segment;

public class Tenant extends Overlay {
	protected List<Segment> segments;
	
	public Tenant(String name) {
		super(name);	
		segments = new ArrayList<Segment>();
	}
	
	public Tenant(String name, ArrayList<Device> devices, ArrayList<Overlay> allowList) {
		super(name,devices,allowList);
		segments = new ArrayList<Segment>();
	}
	
	public Tenant(long id, String name, ArrayList<Device> devices,
			ArrayList<Overlay> allowList) {
		super(id, name, devices, allowList);
		segments = new ArrayList<Segment>();
	}
	
	public List<Segment> getSegments(){
		return segments;
	}
}
