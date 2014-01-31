package net.beaconcontroller.overlaymanager;

import java.util.Map;

import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.overlaymanager.Overlay;
import net.beaconcontroller.overlaymanager.Segment;
import net.beaconcontroller.overlaymanager.Tenant;

public interface IOverlayManager {

	public Tenant getTenantById(long id);
	
	public Segment getSegmentById(long id);
	
	public Map<Device, Tenant> getTenants();
	
	public Map<Device, Segment> getSegments();
	
	public Segment getSegmentByDevice(Device device);
	
	public Tenant getTenantByDevice(Device device);
	
	public void addDeviceToOverlay(Overlay overlay, Device device);
	
	public void removeDeviceFromOverlay(Overlay overlay, Device device);
	
	public Tenant createTenant(String name);
	
	public Segment createSegment(Tenant tenant, String name);
	
	public void deleteOverlay(Overlay overlay);
	
	public void addToList(Overlay srcOverlay, Overlay dstOverlay);
	
	public void removeFromList(Overlay srcOverlay, Overlay dstOverlay);
}
