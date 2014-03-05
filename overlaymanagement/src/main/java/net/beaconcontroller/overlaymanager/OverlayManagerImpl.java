package net.beaconcontroller.overlaymanager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
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
import net.beaconcontroller.packet.Ethernet;

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
	protected Map<Long, Tenant> tenantMap;
	protected Map<Long, Segment> segMap;
	protected Map<Long, Overlay> idToOverlayMap;
	protected BlockingQueue<Update> updates;
	protected Thread updatesThread;
	protected volatile boolean shuttingDown = false;
	protected ReentrantReadWriteLock lock;
	protected Random idGen;//Generates random Ids for overlays
	
	public void startUp() {
		idGen = new Random();
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
        tenantMap = new HashMap<Long, Tenant>();
		segMap = new HashMap<Long,Segment>();
		idToOverlayMap = new HashMap<Long, Overlay>();
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
		lock.readLock().lock();
		try{
			return (Tenant)idToOverlayMap.get(id);
		} finally {
			lock.readLock().unlock();
		}
		
	}

	@Override
	public Segment getSegmentById(long tenantId, long segmentId) {
		lock.readLock().lock();
		try{
			return (Segment)((Tenant)idToOverlayMap.get(tenantId)).getSegments().get(segmentId);
		} finally {
			lock.readLock().unlock();
		}		
	}

	@Override
	public Map<Long,Tenant> getTenants() {
		lock.readLock().lock();
		try {
			return tenantMap;
		} finally {
			lock.readLock().unlock();
		}		
	}
	
	@Override
	public Map<Long, Segment> getSegments() {
		lock.readLock().lock();
		try {
			return segMap;
		} finally {
			lock.readLock().unlock();
		}	
	}

	@Override
	public Segment getSegmentByDevice(Long device) {
		lock.readLock().lock();
		try {			
			return segMap.get(device);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Tenant getTenantByDevice(Long device) {
		lock.readLock().lock();
		try {
			return tenantMap.get(device);			
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
			overlay.addDevice(device);//id device is removed and readded this could cause problems with comparison in canCaommunicate()
			Tenant t;
			if((t = tenantMap.get(Ethernet.toLong(device.getDataLayerAddress()))) != null){
				//String name = t.getName();
				removeDeviceFromOverlay(t, device);
			}
			if(overlay instanceof Segment){
				//if(segMap.get(Ethernet.toLong(device.getDataLayerAddress()))!=null){
					//removeDeviceFromOverlay(segMap.get(Ethernet.toLong(device.getDataLayerAddress())), device);
				//}
				segMap.put(Ethernet.toLong(device.getDataLayerAddress()), (Segment)overlay);
			}else if(overlay instanceof Tenant){
				
				tenantMap.put(Ethernet.toLong(device.getDataLayerAddress()), (Tenant)overlay);
				logger.info("{}",tenantMap.size());
				
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
				Segment s = segMap.remove(Ethernet.toLong(device.getDataLayerAddress()));
				s.removeDevice(device);
				//we must move device to new home in its tenant
				//addDeviceToOverlay(((Segment)overlay).getTenant(), device);
			}else if(overlay instanceof Tenant){
				Tenant t = tenantMap.remove(Ethernet.toLong(device.getDataLayerAddress()));
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
			//Need to be fixed...not truly a long
			Long id = (long) idGen.nextInt(Integer.MAX_VALUE)+1;
			Tenant t = new Tenant(id, name);
			idToOverlayMap.put(id, t);
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
			Long id = (long) idGen.nextInt(Integer.MAX_VALUE)+1;
			Segment s = new Segment(id, name, tenant);
			tenant.getSegments().put(id, s);
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
				/*for(Iterator<Map.Entry<Long, Segment>> it = ((Tenant)overlay).getSegments().entrySet().iterator(); it.hasNext();){
					Map.Entry<Long, Segment> tenantsSegEntry = it.next();
					for(Iterator<Map.Entry<Long, Segment>> itt = segMap.entrySet().iterator(); itt.hasNext();){
						Map.Entry<Long, Segment> segMapEntry = itt.next();
						if(tenantsSegEntry.getValue().getId() == segMapEntry.getValue().getId()){
							itt.remove();
							updateSegsStatus(tenantsSegEntry.getValue(), false);
						}
					}*/
				for(Iterator<Map.Entry<Long, Segment>> it = ((Tenant)overlay).getSegments().entrySet().iterator(); it.hasNext();){
					Map.Entry<Long, Segment> tenantsSegEntry = it.next();
					updateSegsStatus(tenantsSegEntry.getValue(), false);
				}
				tenantMap.remove(overlay.getId());
				updateTensStatus((Tenant)overlay, false);				
			}else if(overlay instanceof Segment){
				Tenant t = ((Segment)overlay).getTenant();				
				t.getSegments().remove(overlay.getId());
				updateSegsStatus((Segment)overlay, false);
				/*for(Iterator<Map.Entry<Long, Segment>> it = segMap.entrySet().iterator(); it.hasNext();){
					Map.Entry<Long, Segment> entry = it.next();
					if(overlay.equals(entry.getValue())){
						it.remove();
					}*/
			}			
			//overlay=null;
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
