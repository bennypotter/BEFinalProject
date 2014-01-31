package net.beaconcontroller.overlaymanager;

import java.util.ArrayList;

import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.overlaymanager.Overlay;
import net.beaconcontroller.overlaymanager.Tenant;

public class Segment extends Overlay {
protected Tenant tenant;
	
	public Segment(String name, Tenant tenant) {
		super(name);
		this.tenant = tenant;
	}
	
	public Segment(String name, ArrayList<Device> devices, 
			ArrayList<Overlay> allowList, Tenant tenant) {
		super(name,devices,allowList);
		this.tenant = tenant;
	}
	
	public Segment(long id, String name, ArrayList<Device> devices,
			ArrayList<Overlay> allowList, Tenant tenant) {
		super(id, name, devices, allowList);
		this.tenant = tenant;
	}
	
	public Tenant getTenant(){
		return tenant;
	}
	
	public void setTenant(Tenant tenant){
		this.tenant = tenant;
	}
}
