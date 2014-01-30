package net.beaconcontroller.overlaymanager;

import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.overlaymanager.Overlay;
import net.beaconcontroller.overlaymanager.Segment;
import net.beaconcontroller.overlaymanager.Tenant;

public interface IOverlayManagerAware {
	
	public void tenantCreated(Tenant tenant);
	
	public void tenantRemoved(Tenant tenant);
	
	public void segmentCreated(Segment segment);
	
	public void segmentRemoved(Segment segment);
	
	public void allowListUpdate(Overlay srcOverlay, Overlay dstOverlay, boolean added);
	
	public void deviceAdded(Device device, Overlay overlay);
	
	public void deviceRemoved(Device device, Overlay overlay);
}
