package org.obm.caldav.server.propertyHandler;

import org.obm.caldav.server.IProxy;
import org.obm.caldav.server.impl.DavRequest;
import org.obm.caldav.server.share.Token;
import org.obm.caldav.utils.DOMUtils;
import org.w3c.dom.Element;


/**
 * Name:  calendar-user-address-set
 * 
 * Namespace:  urn:ietf:params:xml:ns:caldav
 * 
 * Purpose:  	Identify the calendar addresses of the associated principal
 * 			 	resource.
 * 
 * Conformance:  	This property MAY be protected and SHOULD NOT be
 * 					returned by a PROPFIND allprop request (as defined in Section 14.2
 * 					of [RFC4918]).  Support for this property is REQUIRED.  This
 * 					property SHOULD be searchable using the DAV:principal-property-
 * 					search REPORT.  The DAV:principal-search-property-set REPORT
 * 					SHOULD identify this property as such.
 * 
 * Description:  This property is needed to map calendar user addresses
 * 				 in iCalendar data to principal resources and their associated
 * 				 scheduling Inbox and Outbox collections.  In the event that a user 
 * 				 has no well defined identifier for their calendar user address,
 * 				 the URI of their principal resource can be used.
 * Definition:
 * 		<!ELEMENT calendar-user-address-set (DAV:href*)>
 * 
 * 
 * @author adrienp
 *
 */
public class CCalendarUserAddressSet extends DavPropertyHandler {

	public CCalendarUserAddressSet(IProxy proxy) {
		super(proxy);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void appendPropertyValue(Element prop, Token t, DavRequest req) {
		DOMUtils.createElementAndText(prop, "D:href", "/"
				+ t.getLoginAtDomain() + "/events/");

	}

}
