<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="net.beaconcontroller.core.IOFSwitch, net.beaconcontroller.devicemanager.Device, net.beaconcontroller.packet.*, org.openflow.util.HexString, java.net.*"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<div class="section">
  <div class="section-header">${title}</div>
  <div class="section-content">
    <table id="table-switches" class="tableSection">
      <thead>
        <tr>
          <th>Id</th>
          <th>IP Address</th>
          <th>Port</th>
          <th>Connected</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach items="${switches}" var="sw" varStatus="status">
          <%  IOFSwitch sw = (IOFSwitch)pageContext.findAttribute("sw"); 
              pageContext.setAttribute("hexId", HexString.toHexString(sw.getId()));
              pageContext.setAttribute("remoteIp", IPv4.fromIPv4Address(sw.getSocketChannel().socket().getInetAddress().getAddress()));
              pageContext.setAttribute("remotePort", sw.get);
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