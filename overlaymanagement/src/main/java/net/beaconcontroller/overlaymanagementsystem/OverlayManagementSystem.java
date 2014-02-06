package net.beaconcontroller.overlaymanagementsystem;

import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openflow.io.OFMessageInStream;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.factory.OFMessageFactory;
import org.openflow.util.HexString;
import org.openflow.util.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.io.OFMessageSafeOutStream;
import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.devicemanager.IDeviceManagerAware;
import net.beaconcontroller.overlaymanager.IOverlayManager;
import net.beaconcontroller.overlaymanager.IOverlayManagerAware;
import net.beaconcontroller.overlaymanager.Overlay;
import net.beaconcontroller.overlaymanager.Segment;
import net.beaconcontroller.overlaymanager.Tenant;
import net.beaconcontroller.routing.IRoutingEngine;
import net.beaconcontroller.routing.Link;
import net.beaconcontroller.routing.Route;

public class OverlayManagementSystem implements IOFMessageListener, IDeviceManagerAware, IOverlayManagerAware {

	
	protected static Logger logger = LoggerFactory.getLogger(OverlayManagementSystem.class);
	protected IBeaconProvider beaconProvider;
	protected IDeviceManager deviceManager;
	protected IRoutingEngine routingEngine;
	protected IOverlayManager overlayManager;
	protected Tenant defaultTenant;
	protected ReentrantReadWriteLock lock;
	
	
	/////////Testing only////////////
	List<Device> devices;
	int count = 0;	
	////////////////////////////////
	
	public void startUp() {
		defaultTenant = overlayManager.createTenant("Default Tenant");
		beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
		devices = new ArrayList<Device>();
		lock = new ReentrantReadWriteLock();
    }
	
	public void shutDown() {
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
    }	
	
	public IBeaconProvider getBeaconProvider() {
        return beaconProvider;
    }
	
	public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }
	
	public IDeviceManager getDeviceManager() {
		return deviceManager;
	}
	
	public void setDeviceManager(IDeviceManager deviceManager){
		this.deviceManager = deviceManager;
	}
	
	public IRoutingEngine getRoutingEngine() {
		return routingEngine;
	}
	
	public void setRoutingEngine(IRoutingEngine routingEngine) {
		this.routingEngine = routingEngine;
	}
	
	public IOverlayManager getOverlayManager() {
		return overlayManager;
	}	
	
	public void setOverlayManager(IOverlayManager overlayManager) {
		this.overlayManager = overlayManager;
	}
	
	public void deleteRoutes(List<Device> devices) {		
		for(Device device : devices){
			OFMatch match = new OFMatch();
	        match.setDataLayerDestination(device.getDataLayerAddress());
	      //Perform an XOR to wildcard for any flows matching the 
	        //destination devices MAC address
	        match.setWildcards(OFMatch.OFPFW_ALL ^ OFMatch.OFPFW_DL_DST);
	        OFFlowMod fm = (OFFlowMod) device.getSw().getInputStream()
	                .getMessageFactory().getMessage(OFType.FLOW_MOD);
	        //Delete any flows matching wildcard
	        fm.setCommand(OFFlowMod.OFPFC_DELETE)
	            .setOutPort((short) OFPort.OFPP_NONE.getValue())
	            .setMatch(match)
	            .setActions(Collections.<OFAction> emptyList())
	            .setLength(U16.t(OFFlowMod.MINIMUM_LENGTH));
	        //Send the FlowMod to all switches
	        for (IOFSwitch outSw : beaconProvider.getSwitches().values()) {
	            try {            	
	                outSw.getOutputStream().write(fm);
	            } catch (IOException e) {
	                logger.error("Failure sending flow mod delete for moved device", e);
	            }
	        }
		}
	}
	
	
	public void constructPacket(OFMessageFactory factory, OFMatch match, Route route, 
			Device srcDevice, Device dstDevice, OFPacketIn pi/*int bufferId*/) {
		OFFlowMod fm = (OFFlowMod) factory.getMessage(OFType.FLOW_MOD);
	    OFActionOutput action = new OFActionOutput();
	    List<OFAction> actions = new ArrayList<OFAction>();
	    actions.add(action);
	    fm.setIdleTimeout((short)255)
        	.setBufferId(0xffffffff)
        	.setMatch(match.clone())
        	.setActions(actions)
        	.setLengthU(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH);	   
	    
	    IOFSwitch sw = beaconProvider.getSwitches().get(route.getId().getDst());
	    OFMessageSafeOutStream out = sw.getOutputStream();	        
	    short outport = dstDevice.getSwPort();
		((OFActionOutput)fm.getActions().get(0)).setPort(outport);
		String linkage = "";
	    //Above, building a flow for the destination device
	    for(int i = route.getPath().size()-1; i >=0; i--){       	
	    	//set input port
	    	String part = "";
			Link l = route.getPath().get(i);
			fm.getMatch().setInputPort(l.getInPort());
			part = "{"+outport+"}["+l.getDst()+"]"+"{"+l.getInPort()+"}---";
			linkage = linkage + part;
			//check we are not sending to ourself
			if(fm.getMatch().getInputPort() == ((OFActionOutput) fm
	                   .getActions().get(0)).getPort()){
				logger.info("Bad/Old flows detected from {} to {}", 
						HexString.toHexString(fm.getMatch().getDataLayerSource()),
						HexString.toHexString(dstDevice.getDataLayerAddress()));
				deviceMoved(dstDevice, null, null, null, null);
				return;
			}
			//send the flow
			try{
				out.write(fm);
				logger.info("flow sent to switch {}",sw.getId());
			}catch (IOException e){
				logger.info("Error sending flow", e);
			}
				
			fm = fm.clone();
			//if i > 0 then we are not at the source switch yet and must use
			//the current links out port as outport for next flow and get the
			//next sw in the path				
			outport = l.getOutPort();
			((OFActionOutput)fm.getActions().get(0)).setPort(outport);
			if(i>0){
				sw = beaconProvider.getSwitches().get(route.getPath().get(i-1).getDst());
			}else{
				sw = beaconProvider.getSwitches().get(route.getId().getSrc());
			}
			//Check the we have a valid switch
			if(sw ==null) {
				logger.info("Switch could not be found");
				return;
			}
			out = sw.getOutputStream();
	    }
	    if(route.getPath().size() == 0){
	    	((OFActionOutput)fm.getActions().get(0)).setPort(dstDevice.getSwPort());	
		}
		//Now we can set the original flow
		fm.setMatch(match)
			.setBufferId(pi.getBufferId());
		fm.getMatch().setInputPort(srcDevice.getSwPort());
		String part = "{"+outport+"}["+srcDevice.getSw().getId()+"]{"+srcDevice.getSwPort()+"}---";
		linkage = linkage + part;
		//check we are not sending to ourself
		if(fm.getMatch().getInputPort() == ((OFActionOutput) fm
                   .getActions().get(0)).getPort()){
			logger.info("Final Flow: Bad/Old flows detected from {} port {} -- port {}  to {}", 
					HexString.toHexString(fm.getMatch().getDataLayerSource()), ((OFActionOutput)fm.getActions().get(0)).getPort(),
					fm.getMatch().getInputPort(),
					HexString.toHexString(dstDevice.getDataLayerAddress()));
			deviceMoved(dstDevice, null, null, null, null);
			return;
		}
		
		//Create packet out (stops "specified buffer does not exist" error)
		//Think this is because the switch does not buffer for long and this 
		//algorithm take longer than time it is buffered for???
		/*OFActionOutput outAction;
		if(route.getPath().size() > 0){
			outAction = new OFActionOutput(outport);
		}else{
			outAction = new OFActionOutput(dstDevice.getSwPort());
		}

        OFPacketOut po = new OFPacketOut()
            .setBufferId(OFPacketOut.BUFFER_ID_NONE)
            .setInPort(fm.getMatch().getInputPort())
            .setActions(Collections.singletonList((OFAction)outAction))
        	.setPacketData(pi.getPacketData());
		*/
		//send the flow
		try{
			out.write(fm);
			
			//out.write(po);
		}catch (IOException e){
			logger.info("Error sending flow", e);
		}
		logger.info(linkage);
	}	
	
	/******************** IOFMessageListener ******************************/ 
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg) throws IOException {
		
		
		//1. Find out who the packet was from and to
		OFPacketIn pi = (OFPacketIn)msg;
		OFMatch match = OFMatch.load(pi.getPacketData(), pi.getInPort());
		
		//We use L3 addressing because ARP packets will not know the 
		//destination L2 address so we must identify the device by the 
		//IP the ARP packet is trying to resolve.
		Integer nwSrc = match.getNetworkSource();
		Integer nwDst = match.getNetworkDestination();
		//byte[] dlSrc = match.getDataLayerSource();
		//byte[] dlDst = match.getDataLayerDestination();
		Device dstDevice = deviceManager.getDeviceByNetworkLayerAddress(nwDst);
		Device srcDevice = deviceManager.getDeviceByNetworkLayerAddress(nwSrc);
		
		
		
		//deviceManager will return null for all devices on Mininet
		// because the devices are not as active as normal computers
		//they only send packets when commanded therefore Beacon cannot
		//learn the address as quickly as it normally would..."pingall"
		//sorts this
		if(dstDevice == null){
			//logger.info("Destination device is not know");
			return Command.CONTINUE;
		}else if(srcDevice == null){
			//logger.info("Source device is not know");
			
			return Command.CONTINUE;
		}
		
		//logger.info("{} to {}",HexString.toHexString(srcDevice.getDataLayerAddress()),
				//HexString.toHexString(dstDevice.getDataLayerAddress()));
		
		//2. Find out what overlay these device belong to
		Overlay dstOverlay = overlayManager.getTenantByDevice(dstDevice);
		Overlay srcOverlay = overlayManager.getTenantByDevice(srcDevice);
		
		if(dstOverlay == null){
			//logger.info("Destination Tenant is null");
		}else if (srcOverlay == null){
			//logger.info("Source Tenant is null");
		}
		
		if((dstOverlay == null) && (srcOverlay == null)){
			//Devices must reside in a segment
			srcOverlay = overlayManager.getSegmentByDevice(srcDevice);
			dstOverlay = overlayManager.getSegmentByDevice(dstDevice);
		}else if((dstOverlay == null) || (srcOverlay == null)){
			//This means one if the devices is not in a Tenant but possibly 
			//a segment and communication is not allowed
			//logger.info("One of the devices does not belong to a Tenant, dropping packet");
			return Command.CONTINUE;
		}
		 
		//3. Check if the overlay match in order to allow communication
		//   and if not have they authorised communication between eachother
		//Are both devices in the same Tenant
		boolean canTalk = false;
		if(dstOverlay.equals(srcOverlay)){
			//logger.info("Overlays are the same.....");
			canTalk = true;			
		}else{
			if((srcOverlay instanceof Tenant) && (dstOverlay instanceof Tenant)){
				//logger.info("Tenants are Not the same, checking allow list...");
				//	Check if they have allowed communication
				boolean srcTntCanTalk = srcOverlay.canCommunicate(dstOverlay);
				boolean dstTntCanTalk = dstOverlay.canCommunicate(srcOverlay);
				if((srcTntCanTalk) && (dstTntCanTalk)){
					//logger.info("Both Tenant have agreed communication...");
					canTalk = true;
				}					
			}else{
				//They are segments
				//logger.info("Segments are Not the same, checking allow list...");
				boolean srcSegCanTalk = srcOverlay.canCommunicate(dstOverlay);
				boolean dstSegCanTalk = dstOverlay.canCommunicate(srcOverlay);
				if((srcSegCanTalk) && (dstSegCanTalk)){
					//logger.info("Segments have agreed communication, checking tenants...");
					Tenant srcTenant = ((Segment)srcOverlay).getTenant();
					Tenant dstTenant =  ((Segment)dstOverlay).getTenant();
					boolean srcTnCanTalk = srcTenant.canCommunicate(dstTenant);
					boolean dstTnCanTalk = dstTenant.canCommunicate(srcTenant);
					if((srcTnCanTalk) && (dstTnCanTalk)){
						//logger.info("Tenants have agreed communication...");
						canTalk = true;
					}else{
						//logger.info("Tenants have not agreed communication");
					}
				}else{
					//logger.info("Segments have not agreed communication");
				}
			}			
		}
		
		if(canTalk){
			//logger.info("Devices are allowed to communicate");
			Route r = routingEngine.getRoute(srcDevice.getSw().getId(), dstDevice.getSw().getId());
			if(r == null){
				//logger.info("Route not found");
				return Command.CONTINUE;
			}else{
				//logger.info("Route has been found inputPort: {}",match.getInputPort());
				//When we build the route we need to make sure every switch has instruction to forward
				//the packet to the next switch so we do not have to repeat this process at the next switch.
				//This mean we must take each "Link" in a Route, find the dst Switch, and tell it which port
				//to send the packet out of...
				//logger.info("packet from switch {}",sw.getId());
				OFMessageInStream in = sw.getInputStream();
				constructPacket(in.getMessageFactory(), match, r, srcDevice, dstDevice, /*pi.getBufferId()*/ pi);	
				
				//Create packet out (stops "specified buffer does not exist" error)
				//Think this is because the switch does not buffer for long and this 
				//algorithm take longer than time it is buffered for???
				/*OFActionOutput outAction;
				if(r.getPath().size() > 0){
					outAction = new OFActionOutput(r.getPath().get(0).getOutPort());
				}else{
					outAction = new OFActionOutput(dstDevice.getSwPort());
				}

		        OFPacketOut po = new OFPacketOut()
		            .setBufferId(OFPacketOut.BUFFER_ID_NONE)
		            .setInPort(pi.getInPort())
		            .setActions(Collections.singletonList((OFAction)outAction))
		        	.setPacketData(pi.getPacketData());
		        //send the flow
				try{
					sw.getOutputStream().write(po);
				}catch (IOException e){
					logger.info("Error sending flow", e);
				}*/
			}
		}
		return Command.CONTINUE;
		
    }	

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "overlayManagementSystem";
	}
	boolean first= true;
	/*********************** IDeviceManagerAware ******************************/ 
	@Override
	public void deviceAdded(Device device) {		
		//Devices that have been added to the network must be placed
		//into the	 default zone
		
		lock.writeLock().lock();
		try{
		overlayManager.addDeviceToOverlay(defaultTenant, device);
		}finally{
			lock.writeLock().lock();
		}
		//Testing only
		/*devices.add(device);
		count++;
		if(count == 4){
			buildEnvironment();
		}*/
		/////////////////////
	}

	@Override
	public void deviceRemoved(Device device) {
		//Delete the flows currently associated with device		
	}

	@Override
	public void deviceMoved(Device device, IOFSwitch oldSw, Short oldPort,
			IOFSwitch sw, Short port) {
		List<Device>devices = new ArrayList<Device>();
		devices.add(device);
		deleteRoutes(devices);		
	}

	@Override
	public void deviceNetworkAddressAdded(Device device,
			Set<Integer> networkAddresses, Integer networkAddress) {
		//NOOP
		
	}

	@Override
	public void deviceNetworkAddressRemoved(Device device,
			Set<Integer> networkAddresses, Integer networkAddress) {
		//NOOP
		
	}

	/************************* IOverlayManagerAware *****************************/ 
	@Override
	public void tenantCreated(Tenant tenant) {
		logger.debug("Tenant: {} ID: {} has been created", tenant.getName(), tenant.getId());			
	}

	@Override
	public void tenantRemoved(Tenant tenant) {
		//Need to tell switches to removed routes
		//associated with any device in that tenant
		//Then take all devices in associated Segments and Tenant
		//and place in the Default Tenant zone
		List<Device> devices = new ArrayList<Device>();
		//Get all devices residing in the Tenant and remove them
		for(Device device : tenant.getDevices()){
			devices.add(device);
			tenant.removeDevice(device);
		}
		
		//Get all devices residing in segments and remove them
		for(Map.Entry entry: tenant.getSegments().entrySet()){
			segmentRemoved((Segment)entry.getValue());
		}
		//delete routes on the switches
		deleteRoutes(devices);
		logger.info("Tenant: {} ID: {} has been removed", tenant.getName(), tenant.getId());		
	}

	@Override
	public void segmentCreated(Segment segment) {
		logger.info("Segment: {} ID: {} has been created in Tenant: {} ID: {}",
				segment.getName(), segment.getId(), segment.getTenant().getName(), segment.getTenant().getId());			
	}

	@Override
	public void segmentRemoved(Segment segment) {
		//Need to tell switches to removed routes
		//associated with any device in that segment.
		//Then need to move device from segment to tenant owner
		List<Device> devices = new ArrayList<Device>();
		for(Device device : segment.getDevices()){
			devices.add(device);
			segment.removeDevice(device);
		}
		//delete routes
		deleteRoutes(devices);
		logger.info("Segment: {} has been removed from Tenant: {}",
				segment.getName(), segment.getTenant().getName());
	}

	@Override
	public void allowListUpdate(Overlay srcOverlay, Overlay dstOverlay,
			boolean added) {
		logger.info("ListUpdate: {} has given access to: {}", 
				srcOverlay.getName(), dstOverlay.getName());		
	}

	@Override
	public void deviceAdded(Device device, Overlay overlay) {
		//remove device from default zone
		if(!overlay.equals(defaultTenant)){
			overlayManager.removeDeviceFromOverlay(defaultTenant, device);
		}
		logger.info("Device: {} added to Overlay: {}",
				HexString.toHexString(device.getDataLayerAddress()), overlay.getName());		
	}

	@Override
	public void deviceRemoved(Device device, Overlay overlay) {
		List<Device> devices = new ArrayList<Device>();
		devices.add(device);
		deleteRoutes(devices);
		
		//check instanceof
		if(overlay instanceof Tenant){
			if(!overlay.equals(defaultTenant)){
				//if removed from Tenant device must be placed back in the
				//default instance
				overlayManager.addDeviceToOverlay(defaultTenant, device);
			}
		}else{
			//Device removed from segment must be placed into their Tenant owner
			overlayManager.addDeviceToOverlay(((Segment)overlay).getTenant(), device);			
		}			
		logger.info("Device: {} has been removed from: {}", 
					HexString.toHexString(device.getDataLayerAddress()), overlay.getName());		
	}
}
