package net.beaconcontroller.overlaymanager;

import java.util.ArrayList;

import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.overlaymanager.Overlay;

public class Overlay {
	protected ArrayList<Device> devices;
	protected ArrayList<Overlay> allowList;
	protected long id;
	protected String name;
	
	public Overlay(String name) {
		this.name = name;
		devices = new ArrayList<Device>();
		allowList = new ArrayList<Overlay>();
		id = 0;		
	}
	
	public Overlay(long id, String name) {
		this.name = name;
		devices = new ArrayList<Device>();
		allowList = new ArrayList<Overlay>();
		this.id = id;		
	}
	
	public Overlay(String name, ArrayList<Device> devices, ArrayList<Overlay> allowList) {
		this.name = name;
		this.devices = devices;
		this.allowList = allowList;
		id = 0;
	}
	
	public Overlay(long id, String name, ArrayList<Device> devices, ArrayList<Overlay> allowList) {
		this.name = name;
		this.devices = devices;
		this.allowList = allowList;
		this.id = id;
	}
	
	public long getId(){
		return id;
	}
	
	public ArrayList<Device> getDevices(){
		return devices;
	}
	
	public void setDevices(ArrayList<Device> devices){
		this.devices = devices;
	}
	
	public ArrayList<Overlay> getAllowList(){
		return allowList;
	}
	
	public void setAllowList(ArrayList<Overlay> allowList){
		this.allowList = allowList;
	}
	
	public String getName(){
		return name;
	}

	public void setName(String name){
		this.name = name;
	}
	
	public void addDevice(Device device){
		devices.add(device);
	}
	
	public void removeDevice(Device device){
		devices.remove(device);		
	}
	
	public boolean isMember(Device device){
		for(Device d:devices){
			if(d.equals(device)){
				return true;
			}
		}
		return false;
	}
	
	public boolean canCommunicate(Overlay o){
		for(Overlay overlay:allowList){
			if(overlay.equals(o)){
				return true;
			}
		}
		return false;
	}
}
