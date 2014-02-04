<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.osgi.framework.Bundle, net.beaconcontroller.util.BundleState,
                 java.util.List, net.beaconcontroller.util.BundleAction"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>

<div class="section">
  <div class="section-header">${title}</div>
  <div class="section-content">
    <form method="post" action="/wm/overlaymanager/allowlist" class="beaconAjaxDialogForm">
      <table>
        <tr>
          <td>Source Tenant ID: </td>
          <td><input type="input" name="srcTen"/></td>
          <td>Source Segment ID: </td>
          <td><input type="input" name="srcSeg"/></td>
        </tr>
        <tr>
        <td>Destination Tenant ID:</td>
        <td><input type="input" name="dstTen"/></td>
        <td>Destination Segment ID:</td>
        <td><input type="input" name="dstSeg"/></td>
        </tr>
        <tr>
        <td></td>
        	<td>Tenant:
        	<input type="radio" name="overlayCheck" value="Tenant">
        	</td>
        	<td>Segment:
        	<input type="radio" name="overlayCheck" value="Segment">
        	</td>
        </tr>
        <tr>
          <td colspan="2">
            <input type="submit" value="Add"/>
          </td>
        </tr>
      </table>
    </form>
  </div>
</div>