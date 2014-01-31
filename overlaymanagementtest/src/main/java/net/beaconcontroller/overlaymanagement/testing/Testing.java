package net.beaconcontroller.overlaymanagement.testing;

import java.io.IOException;
import java.util.List;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.devicemanager.Device;

public class Testing implements IOFMessageListener, ITestRunable {
	
	protected static Logger logger = LoggerFactory.getLogger(Testing.class);
	protected IBeaconProvider beaconProvider;
	 
    public IBeaconProvider getBeaconProvider() {
        return beaconProvider;
    }
 
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }
 
    public void startUp() {
    	beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }
 
    public void shutDown() {
    	beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
    }
 
    public String getName() {
        return "testing";
    }
 
    public Command receive(IOFSwitch sw, OFMessage msg) {
    	logger.info("AAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		return Command.CONTINUE;
    }

	@Override
	public boolean runTest(int i, List<Device> devices) {
		logger.info("HELLO HELOOOOOOO");
		return false;
	}


}
