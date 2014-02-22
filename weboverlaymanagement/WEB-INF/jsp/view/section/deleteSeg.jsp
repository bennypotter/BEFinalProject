<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.osgi.framework.Bundle, net.beaconcontroller.util.BundleState,
                 java.util.List, net.beaconcontroller.util.BundleAction"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>

<div class="section">
  <div class="section-header">${title}</div>
  <div class="section-content">
    <form method="post" action="/wm/overlaymanager/segment/add" class="beaconAjaxDialogForm">
      <table>
        <tr>
          <td>Segment Name: </td>
          <td><input type="input" name="segName"/></td>
        </tr>
        <tr>
        <td>Tenant Owner:</td>
        <td><input type="input" name="tenOwner"/></td>
        </tr>
        <tr>
          <td colspan="2">
            <input type="submit" value="Create"/>
          </td>
        </tr>
      </table>
    </form>
  </div>
</div>