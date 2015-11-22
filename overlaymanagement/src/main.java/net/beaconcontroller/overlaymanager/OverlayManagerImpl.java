package net.beaconcontroller.overlaymanager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.overlaymanagementsystem.OverlayManagementSystem;

public class OverlayManagerImpl implements IOFMessageListener, IOverlayManager{
	protected enum UpdateType {
        TEN_CREATED, TEN_REMOVED, SEG_CREATED, SEG_REMOVED,
        LIST_UPDATE_ADD, LIST_UPDATE_REM, DEV_ADDED, DEV_REMOVED
    }
	
	protected class Update{
		public Overlay srcOverlay;
		public Overlay dstOverlay;
		public Device device;
		public UpdateType updateType;
		
		public Update(UpdateType type) {
            this.updateType = type;
        }
	}
		
	protected IBeaconProvider beaconProvider;
	protected static Logger logger = LoggerFactory.getLogger(OverlayManagerImpl.class);
	protected Set<IOverlayManagerAware> overlayManagerAware;
	protected Map<Device, Tenant> tenantMap;
	protected Map<Device, Segment> segMap;
	protected BlockingQueue<Update> updates;
	protected Thread updatesThread;
	protected volatile boolean shuttingDown = false;
	protected ReentrantReadWriteLock lock;
	
	public void startUp() {
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
        tenantMap = new HashMap<Device, Tenant>();
		segMap = new HashMap<Device,Segment>();
		lock = new ReentrantReadWriteLock();
		this.updates = new LinkedBlockingQueue<Update>();
		//Tenant t = new Tenant("Test Tenant");
		//updateTensStatus(t, true);
		
		updatesThread = new Thread(new Runnable () {
            @Override
            public void run() {
                while (true) {
                    try {
                        Update update = updates.take();
                        if (overlayManagerAware != null) {
                            for (IOverlayManagerAware oma : overlayManagerAware) {
                                try {
                                    switch (update.updateType) {
                                        case TEN_CREATED:
                                        	oma.tenantCreated((Tenant)update.srcOverlay);
                                            break;
                                        case TEN_REMOVED:
                                        	oma.tenantRemoved((Tenant)update.srcOverlay);
                                            break;
                                        case SEG_CREATED:
                                            oma.segmentCreated((Segment)update.srcOverlay);
                                            break;
                                        case SEG_REMOVED:
                                        	oma.segmentRemoved((Segment)update.srcOverlay);
                                            break;
                                        case LIST_UPDATE_ADD:                                        	
                                            oma.allowListUpdate(update.srcOverlay, update.dstOverlay, true);
                                            break;
                                        case LIST_UPDATE_REM:
                                        	oma.allowListUpdate(update.srcOverlay, update.dstOverlay, false);
                                        	break;
                                        case DEV_ADDED:
                                        	oma.deviceAdded(update.device, update.srcOverlay);                                            
                                            break;
                                        case DEV_REMOVED:
                                        	oma.deviceRemoved(update.device, update.srcOverlay);
                                        	break;
                                    }
                                } catch (Exception e) {
                                    logger.error("Exception in callback", e);
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        logger.warn("OverlayManager Updates thread interupted", e);
                        if (shuttingDown)
                            return;
                    }
                }
            }}, "OverlayManager Updates");
        updatesThread.start();
        
    }
	
	public void shutDown() {
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
        shuttingDown = true;
		updatesThread.interrupt();
    }
	
	public IBeaconProvider getBeaconProvider() {
        return beaconProvider;
    }
	
	public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }	
	
	public void setOverlayManagerAware(Set<IOverlayManagerAware> overlayManagerAware){
		this.overlayManagerAware = overlayManagerAware;
	}
	
	/***************************************************************************
	    * Puts an update in queue for the Overlay.  Must be called from within the
	    * write lock. This is because the intended place where these functions are
	    * called is within a function that could be called by multiple threads.
	    ****************************************************************************/
	    
	    protected void updateTensStatus(Tenant tenant, boolean added) {
	        Update update;
	        if (added) {
	            update = new Update(UpdateType.TEN_CREATED);
	        } else {
	            update = new Update(UpdateType.TEN_REMOVED);
	        }
	        update.srcOverlay = tenant;
	        updates.add(update);
	    }
	    
	    protected void updateSegsStatus(Segment segment, boolean added) {
	        Update update;
	        if (added) {
	            update = new Update(UpdateType.SEG_CREATED);
	        } else {
	            update = new Update(UpdateType.SEG_REMOVED);
	        }
	        update.srcOverlay = segment;
	        updates.add(update);
	    }
		
	    protected void updateListStatus(Overlay srcOverlay, Overlay dstOverlay, boolean added) {
	        Update update;
	        if (added) {
	            update = new Update(UpdateType.LIST_UPDATE_ADD);
	        } else {
	            update = new Update(UpdateType.LIST_UPDATE_REM);
	        }
	        update.srcOverlay = srcOverlay;
	        update.dstOverlay = dstOverlay;
	        updates.add(update);
	    }
	    
	    protected void updateDevStatus(Device device, Overlay overlay, 
	    		boolean added){
	    	Update update;
	        if (added) {
	            update = new Update(UpdateType.DEV_ADDED);
	        } else {
	            update = new Update(UpdateType.DEV_REMOVED);
	        }
	        update.device = device;
	        update.srcOverlay = overlay;
	        updates.add(update);
	    }
	
	/************************IOFMessageListener*********************************/
	    /*****************TO BE DELETED*************************************/
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg) throws IOException {
		return Command.CONTINUE;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "overlayManager";
	}
	/***************************************************************************/

	/********************* IOverlayManager **************************/
	@Override
	public Tenant getTenantById(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Segment getSegmentById(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Device,Tenant> getTenants() {
		lock.readLock().lock();
		try {
			return tenantMap;
		} finally {
			lock.readLock().unlock();
		}		
	}
	
	@Override
	public Map<Device, Segment> getSegments() {
		lock.readLock().lock();
		try {
			return segMap;
		} finally {
			lock.readLock().unlock();
		}	
	}

	@Override
	public Segment getSegmentByDevice(Device device) {
		lock.readLock().lock();
		try {			
			return segMap.get(device);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Tenant getTenantByDevice(Device device) {
		lock.readLock().lock();
		try {
			Tenant t = tenantMap.get(device); 
			return t;
		} finally {
			lock.readLock().unlock();
		}
	}
	
	@Override
	public void addDeviceToOverlay(Overlay overlay, Device device){
		lock.writeLock().lock();
		try{
			//Device is added to the overlay and relvant map
			//is updated
			overlay.addDevice(device);
			if(overlay instanceof Segment){
				segMap.put(device, (Segment)overlay);
			}else if(overlay instanceof Tenant){
				tenantMap.put(device, (Tenant)overlay);
				
			}
			updateDevStatus(device,overlay,true);
		} finally {
			lock.writeLock().unlock();
		}
	}
		
	@Override
	public void removeDeviceFromOverlay(Overlay overlay, Device device){
		lock.writeLock().lock();
		try{
			
			if(overlay instanceof Segment){			
				Segment s = segMap.remove(device);
				s.removeDevice(device);
			}else if(overlay instanceof Tenant){
				Tenant t = tenantMap.remove(device);
				t.removeDevice(device);
			}
			updateDevStatus(device,overlay,false);
		} finally {
			//overlay.removeDevice(device);
			lock.writeLock().unlock();
		}
	}
	
	@Override
	public Tenant createTenant(String name){
		lock.writeLock().lock();
		try{
			Tenant t = new Tenant(name);
			updateTensStatus(t, true);
			return t;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	@Override
	public Segment createSegment(Tenant tenant, String name){
		lock.writeLock().lock();
		try{
			Segment s = new Segment(name, tenant);
			tenant.getSegments().add(s);
			updateSegsStatus(s, true);
			return s;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void deleteOverlay(Overlay overlay){
		//Along with creating an update, here we must also
		//do some general cleanup to the Hashmaps holding
		//Tenant and Segmnet to Device mappings
		lock.writeLock().lock();
		try{
			if(overlay instanceof Tenant){
				updateTensStatus((Tenant)overlay, false);
				for(Map.Entry entry:tenantMap.entrySet()){
					if(overlay.equals(entry.getValue())){
						tenantMap.remove(entry.getKey());
					}
				}
			}else if(overlay instanceof Segment){
				updateSegsStatus((Segment)overlay, false);
				for(Map.Entry entry : segMap.entrySet()){
					if(overlay.equals(entry.getValue())){
						segMap.remove(entry.getKey());
					}
				}
			}
			overlay=null;
		}finally {
			lock.writeLock().unlock();
		}
	}
	
	public void addToList(Overlay srcOverlay, Overlay dstOverlay){
		lock.writeLock().lock();
		try{
			srcOverlay.allowList.add(dstOverlay);
			updateListStatus(srcOverlay, dstOverlay, true);
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public void removeFromList(Overlay srcOverlay, Overlay dstOverlay){
		lock.writeLock().lock();
		try{
			srcOverlay.allowList.remove(dstOverlay);
			updateListStatus(srcOverlay, dstOverlay, false);
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	/*****************************************************************/

}
