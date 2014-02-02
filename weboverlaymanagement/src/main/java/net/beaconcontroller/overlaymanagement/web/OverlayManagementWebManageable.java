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
        tabs.add(new Tab("Test","/wm/overlaymanager/test.do"));
        tenants = new ArrayList<Tenant>();
    }
    
    @RequestMapping("/test")
    public String test(Map<String, Object> model) {
        Layout layout = new OneColumnLayout();
        model.put("layout", layout);

        // Bundle Form
        model.put("title", "Add Bundle");
        layout.addSection(new JspSection("test.jsp", new HashMap<String, Object>(model)), TwoColumnLayout.COLUMN1);
        return BeaconViewResolver.SIMPLE_VIEW;
    }
    
    @RequestMapping(value = "/bundle/add", method = RequestMethod.POST)
    public View osgiBundleAdd(@RequestParam("file") String file, Map<String, Object> model) throws Exception {
        BeaconJsonView view = new BeaconJsonView();
        tenants.add(overlayManager.createTenant(file));
        
        view.setContentType("text/javascript");
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
           	for(Segment s : t.getSegments()){
           		if (sb.length() > 0)
                       sb.append(" ");
           		sb.append(s.getName() + " ");
           		
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
           	row.add(overlayManager.getTenantByDevice(d).getName());
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
