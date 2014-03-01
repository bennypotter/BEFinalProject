package net.beaconcontroller.overlaymanager;

import java.util.Map;

import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.overlaymanager.Overlay;
import net.beaconcontroller.overlaymanager.Segment;
import net.beaconcontroller.overlaymanager.Tenant;

public interface IOverlayManager {

	public Tenant getTenantById(long id);
	
	public Segment getSegmentById(long tenantId, long segmentId);
	
	public Map<Long, Tenant> getTenants();
	
	public Map<Long, Segment> getSegments();
	
	public Segment getSegmentByDevice(Long device);
	
	public Tenant getTenantByDevice(Long device);
	
	public void addDeviceToOverlay(Overlay overlay, Device device);
	
	public void removeDeviceFromOverlay(Overlay overlay, Device device);
	
	public Tenant createTenant(String name);
	
	public Segment createSegment(Tenant tenant, String name);
	
	public void deleteOverlay(Overlay overlay);
	
	public void addToList(Overlay srcOverlay, Overlay dstOverlay);
	
	public void removeFromList(Overlay srcOverlay, Overlay dstOverlay);
}
