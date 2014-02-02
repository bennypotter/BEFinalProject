<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="net.beaconcontroller.core.IOFSwitch, net.beaconcontroller.devicemanager.Device, net.beaconcontroller.devicemanager.Device, net.beaconcontroller.packet.*, org.openflow.util.HexString, java.net.*"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<div class="section">
  <div class="section-header">${title}</div>
  <div class="section-content">
    <table id="deviceoverlays" class="tableSection">
      <thead>
        <tr>
          <th>Id</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach items="${switches}" var="sw" varStatus="status">
          <%  Device sw = (Device)pageContext.findAttribute("sw"); 
              pageContext.setAttribute("hexId", HexString.toHexString(sw.getDataLayerAddress()));
          %>
          <tr>
            <td><c:out value="${hexId}"/></td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>

<script type="text/javascript" charset="utf-8">
    (function() {
        new DataTableWrapper('table-switches', null, {}, false, false); 
    })();
</script>