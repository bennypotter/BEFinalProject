package net.beaconcontroller.overlaymanagement.testing;

import java.util.List;

import net.beaconcontroller.devicemanager.Device;

public interface ITestRunable {
	public boolean runTest(int i, List<Device> devices);
}
