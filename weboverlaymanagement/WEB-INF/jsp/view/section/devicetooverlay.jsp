<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="net.beaconcontroller.overlaymanager.IOverlayManager, net.beaconcontroller.overlaymanager.Tenant,
		net.beaconcontroller.overlaymanager.Segment, net.beaconcontroller.devicemanager.Device, 
		net.beaconcontroller.packet.*, org.openflow.util.HexString, java.net.*"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<div class="section">
  <div class="section-header">${title}</div>
  <div class="section-content">
    <table id="deviceoverlays" class="tableSection">
      <thead>
        <tr>
          <th>Device</th>
          <th>Owner</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach items="${switches}" var="sw" varStatus="status">
          <%  Device sw = (Device)pageContext.findAttribute("sw"); 
              pageContext.setAttribute("hexId", HexString.toHexString(sw.getDataLayerAddress()));
              IOverlayManager om = (IOverlayManager)pageContext.findAttribute("overlayManager");
              Tenant t = om.getTenantByDevice(sw);
              if(t != null){
              	pageContext.setAttribute("devOwner", t.getName());
              } else {
              	Segment s = om.getSegmentByDevice(sw);
              	pageContext.setAttribute("devOwner", s.getName());
              }
          %>
          <tr>
            <td><c:out value="${hexId}"/></td>
            <td><c:out value="${devOwner}"/></td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>

<script type="text/javascript" charset="utf-8">
    (function() {
        new DataTableWrapper('deviceoverlays', null, {}, false, false); 
    })();
</script>