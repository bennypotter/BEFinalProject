package net.beaconcontroller.overlaymanagement.web;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.openflow.util.HexString;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import net.beaconcontroller.web.IWebManageable;
import net.beaconcontroller.web.view.BeaconJsonView;
import net.beaconcontroller.web.view.BeaconViewResolver;
import net.beaconcontroller.web.view.Tab;
import net.beaconcontroller.web.view.layout.Layout;
import net.beaconcontroller.web.view.layout.OneColumnLayout;
import net.beaconcontroller.web.view.layout.TwoColumnLayout;
import net.beaconcontroller.web.view.section.JspSection;
import net.beaconcontroller.web.view.section.TableSection;
import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.devicemanager.Device;
import net.beaconcontroller.devicemanager.IDeviceManager;
import net.beaconcontroller.overlaymanager.IOverlayManager;
import net.beaconcontroller.overlaymanager.Segment;
import net.beaconcontroller.overlaymanager.Tenant;
import net.beaconcontroller.packet.Ethernet;

@Controller
@RequestMapping("/overlaymanager")
public class OverlayManagementWebManageable implements IWebManageable {

	protected IOverlayManager overlayManager;
	protected IDeviceManager deviceManager;
	protected IBeaconProvider beaconProvider;
    protected List<Tab> tabs;
    protected List<Tenant> tenants;
	
    public OverlayManagementWebManageable() {
        tabs = new ArrayList<Tab>();
        tabs.add(new Tab("Overview", "/wm/overlaymanager/overview.do"));
        tabs.add(new Tab("Device view", "/wm/overlaymanager/deviceview.do"));
        tabs.add(new Tab("Overlay Creation","/wm/overlaymanager/overlay_creation.do"));
        tabs.add(new Tab("Overlay Deletion","/wm/overlaymanager/overlay_deletion.do"));
        tenants = new ArrayList<Tenant>();
    }
    
    @RequestMapping("/overlay_creation")
    public String overlay_creation(Map<String, Object> model) {
        Layout layout = new OneColumnLayout();
        model.put("layout", layout);

        // Tenant create Form
        model.put("title", "Create Tenant");
        layout.addSection(new JspSection("test.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);
        
        
        //Segment Create Form
        model.put("title", "Create Segment");
        layout.addSection(new JspSection("createSeg.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);
        //Segment Create Form
        model.put("title", "Add Device to Tenant");
        layout.addSection(new JspSection("addDeviceToTen.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);
        
        //Device Add to Ten Form
        model.put("title", "Add Device to Segment");
        layout.addSection(new JspSection("addDeviceToSeg.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);
        
      //Device Allow List Form
        model.put("title", "Add to Allow List");
        layout.addSection(new JspSection("allowlist.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);
        
        return BeaconViewResolver.SIMPLE_VIEW;
    }
    
    @RequestMapping("/overlay_deletion")
    public String overlay_deletion(Map<String, Object> model) {
        Layout layout = new OneColumnLayout();
        model.put("layout", layout);

        // Tenant create Form
        model.put("title", "Delete Tenant");
        layout.addSection(new JspSection("deleteTen.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);
        
        
        //Segment Create Form
        model.put("title", "Delete Segment");
        layout.addSection(new JspSection("deleteSeg.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);
        //Segment Create Form
        model.put("title", "Delete Device from Tenant");
        layout.addSection(new JspSection("deleteDeviceFromTen.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);
        
        //Device Add to Ten Form
        model.put("title", "Delete Device from Segment");
        layout.addSection(new JspSection("deleteDeviceFromSeg.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);
       
      //Device Allow List Form
        model.put("title", "Delete from Allow List");
        layout.addSection(new JspSection("removFromAllowList.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);
        
        return BeaconViewResolver.SIMPLE_VIEW;
    }
    
    
    
    @RequestMapping(value = "/tenant/add", method = RequestMethod.POST)
    public View createTenant(@RequestParam("file") String file, Map<String, Object> model) throws Exception {
        BeaconJsonView view = new BeaconJsonView();
        tenants.add(overlayManager.createTenant(file));
        
        view.setContentType("text/javascript");
        return view;
    }
    
    @RequestMapping(value = "/tenant/delete", method = RequestMethod.POST)
    public View deleteTenant(@RequestParam("tenId") String tenId, Map<String, Object> model) throws Exception {
        BeaconJsonView view = new BeaconJsonView();
        
        Tenant t = overlayManager.getTenantById(Long.parseLong(tenId));
        overlayManager.deleteOverlay(t);
        tenants.remove(t);        
        view.setContentType("text/javascript");
        return view;
    }
    
    @RequestMapping(value = "/segment/add", method = RequestMethod.POST)
    public View createSegment(@RequestParam("segName") String segName,@RequestParam("tenOwner") String owner,
    		Map<String, Object> model) throws Exception {
        BeaconJsonView view = new BeaconJsonView();
        for(Tenant t : tenants){
        	if(t.getName().equals(owner)){
        		overlayManager.createSegment(t, segName);
        		view.setContentType("text/javascript");
        		return view;
        	}
        }
        //logger.info here
        view.setContentType("text/javascript");
        return view;
    }
    
    @RequestMapping(value = "/segment/delete", method = RequestMethod.POST)
    public View deleteSegment(@RequestParam("segId") String segId,@RequestParam("tenId") String tenId,
    		Map<String, Object> model) throws Exception {
        BeaconJsonView view = new BeaconJsonView();
        Segment s = overlayManager.getSegmentById(Long.parseLong(tenId),Long.parseLong(segId));
        overlayManager.deleteOverlay(s);        
        view.setContentType("text/javascript");
        return view;
    }
    
    @RequestMapping(value = "/device/addtenant", method = RequestMethod.POST)
    public View deviceAddTenant(@RequestParam("mac") String dlAddr,@RequestParam("tenId") String tenID,
    		Map<String, Object> model) throws Exception {
        BeaconJsonView view = new BeaconJsonView();
        Device d = deviceManager.getDeviceByDataLayerAddress(HexString.fromHexString(dlAddr));
        
        long tenId = Long.parseLong(tenID);
        Tenant t = overlayManager.getTenantById(tenId);
        String s = t.getName();
        overlayManager.addDeviceToOverlay(t, d);
        view.setContentType("text/javascript");
        return view;
    }
    
    @RequestMapping(value = "/device/deletetenant", method = RequestMethod.POST)
    public View deviceDeleteTenant(@RequestParam("mac") String dlAddr,
    		Map<String, Object> model) throws Exception {
        BeaconJsonView view = new BeaconJsonView();
        Device d = deviceManager.getDeviceByDataLayerAddress(HexString.fromHexString(dlAddr));
        Long dlAddress = Ethernet.toLong(d.getDataLayerAddress());
        Tenant t = overlayManager.getTenantByDevice(dlAddress);
        overlayManager.removeDeviceFromOverlay(t, d);      
        view.setContentType("text/javascript");
        return view;
    }
    
    @RequestMapping(value = "/device/addsegment", method = RequestMethod.POST)
    public View deviceAddSegment(@RequestParam("mac") String dlAddr,@RequestParam("tenId") String tenID,
    		@RequestParam("segId") String segID, Map<String, Object> model) throws Exception {
        BeaconJsonView view = new BeaconJsonView();
        Device d = deviceManager.getDeviceByDataLayerAddress(HexString.fromHexString(dlAddr));
        long segId = Long.parseLong(segID);
        long tenId = Long.parseLong(tenID);
        Segment s = overlayManager.getSegmentById(tenId, segId);
        overlayManager.addDeviceToOverlay(s, d);
        view.setContentType("text/javascript");
        return view;
    }
    
    @RequestMapping(value = "/device/deletesegment", method = RequestMethod.POST)
    public View deviceDeleteSegment(@RequestParam("mac") String dlAddr, Map<String, Object> model) throws Exception {
        BeaconJsonView view = new BeaconJsonView();
        Device d = deviceManager.getDeviceByDataLayerAddress(HexString.fromHexString(dlAddr));
        Long dlAddress = Ethernet.toLong(d.getDataLayerAddress());
        Segment s = overlayManager.getSegmentByDevice(dlAddress);
        overlayManager.removeDeviceFromOverlay(s, d);
        view.setContentType("text/javascript");
        return view;
    }
    
    @RequestMapping(value = "/allowlist", method = RequestMethod.POST)
    public View addToAllowList(@RequestParam("srcTen") String srcTen, @RequestParam("dstTen") String dstTen,
    		@RequestParam("overlayCheck") String selection, @RequestParam("srcSeg") String srcSeg, 
    		@RequestParam("dstSeg") String dstSeg,	Map<String, Object>model) throws Exception {
    	BeaconJsonView view = new BeaconJsonView();
    	Long srcId = Long.parseLong(srcTen);
    	Long dstId = Long.parseLong(dstTen);
    	Tenant srcTenant = overlayManager.getTenantById(srcId);
    	Tenant dstTenant = overlayManager.getTenantById(dstId);
    	switch(selection){
    	case "Tenant":    		
        	overlayManager.addToList(srcTenant, dstTenant);
    		break;
    		
    	case "Segment":
    		Segment srcSegment = srcTenant.getSegments().get(Long.parseLong(srcSeg));
    		Segment dstSegment = dstTenant.getSegments().get(Long.parseLong(dstSeg));
    		overlayManager.addToList(srcSegment, dstSegment);
    		break;
    	}
    	
    	return view;
    }
    
    @RequestMapping(value = "/removefromallowlist", method = RequestMethod.POST)
    public View removeFromAllowList(@RequestParam("srcTen") String srcTen, @RequestParam("dstTen") String dstTen,
    		@RequestParam("overlayCheck") String selection, @RequestParam("srcSeg") String srcSeg, 
    		@RequestParam("dstSeg") String dstSeg,	Map<String, Object>model) throws Exception {
    	BeaconJsonView view = new BeaconJsonView();
    	Long srcId = Long.parseLong(srcTen);
    	Long dstId = Long.parseLong(dstTen);
    	Tenant srcTenant = overlayManager.getTenantById(srcId);
    	Tenant dstTenant = overlayManager.getTenantById(dstId);
    	switch(selection){
    	case "Tenant":    		
        	overlayManager.removeFromList(srcTenant, dstTenant);
    		break;
    		
    	case "Segment":
    		Segment srcSegment = srcTenant.getSegments().get(Long.parseLong(srcSeg));
    		Segment dstSegment = dstTenant.getSegments().get(Long.parseLong(dstSeg));
    		overlayManager.removeFromList(srcSegment, dstSegment);
    		break;
    	}
    	
    	return view;
    }
    
    @RequestMapping("/overview")
    public String overview(Locale locale, Map<String, Object> model) {
    	Layout layout = new TwoColumnLayout();
        model.put("layout", layout);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss z", locale);    	
    	
        List<String> columnNames = new ArrayList<String>();
        List<List<String>> cells = new ArrayList<List<String>>();
        
        columnNames.add("Tenant");
        columnNames.add("Segment");
        //columnNames.add("Authorised");
        cells = new ArrayList<List<String>>();
        for(Tenant t : tenants) {
           	List<String> row = new ArrayList<String>();
           	row.add(t.getName());
           	StringBuffer sb = new StringBuffer();
           	for(@SuppressWarnings("rawtypes") Map.Entry entry : t.getSegments().entrySet()){
           		if (sb.length() > 0)
                       sb.append(" ");
           		sb.append(((Segment)entry.getValue()).getName() + " ");
           		
           	}
           	row.add(sb.toString());
           	cells.add(row);
        
        }
        
        Map<String,String> tableOptions = new HashMap<String, String>();
        tableOptions.put("\"bFilter\"", "true");
        TableSection tableSection = new TableSection("Overlays", columnNames, cells, "overlay-domains", tableOptions);
        layout.addSection(tableSection, TwoColumnLayout.COLUMN1);
        
        // Switch List Table
        model.put("title", "Device Overlays");
        model.put("switches", deviceManager.getDevices());
        model.put("overlayManager", overlayManager);
        layout.addSection(new JspSection("devicetooverlay.jsp", model), TwoColumnLayout.COLUMN2);
        
       
        
        return BeaconViewResolver.SIMPLE_VIEW;
    }
    
    @RequestMapping("/deviceview")
    public String deviceview(Locale locale, Map<String, Object> model) {
    	
    	Layout layout = new TwoColumnLayout();
        model.put("layout", layout);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss z", locale);    	
    	
        List<String> columnNames = new ArrayList<String>();
        List<List<String>> cells = new ArrayList<List<String>>();
        
        columnNames.add("Device");
        columnNames.add("Tenant");
        columnNames.add("Segment");
        //columnNames.add("Authorised");
        cells = new ArrayList<List<String>>();
        for(Device d : deviceManager.getDevices()) {
           	List<String> row = new ArrayList<String>();
           	row.add(HexString.toHexString(d.getDataLayerAddress()));
           	Long dlAddress = Ethernet.toLong(d.getDataLayerAddress());
           	row.add(overlayManager.getTenantByDevice(dlAddress).getName());
           	//row.add(overlayManager.getSegmentByDevice(d).getName());
           	cells.add(row);        
        }
        
        Map<String,String> tableOptions = new HashMap<String, String>();
        tableOptions.put("\"bFilter\"", "true");
        TableSection tableSection = new TableSection("Devices", columnNames, cells, "devices-Ten", tableOptions);
        layout.addSection(tableSection, TwoColumnLayout.COLUMN1);
    	
    	return BeaconViewResolver.SIMPLE_VIEW;
    }
    
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Overlay Managment";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Tab> getTabs() {
		// TODO Auto-generated method stub
		return tabs;
	}
	
	@Autowired
    public void setOverlayManager(IOverlayManager overlayManager) {
        this.overlayManager = overlayManager;
    }
	
	@Autowired
    public void setDeviceManager(IDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }
	
	@Autowired
    public void setBeaconProvier(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }

}
