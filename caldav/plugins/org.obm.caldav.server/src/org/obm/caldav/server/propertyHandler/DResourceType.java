package org.obm.caldav.server.propertyHandler;


import org.obm.caldav.server.IProxy;
import org.obm.caldav.server.impl.DavRequest;
import org.obm.caldav.server.share.Token;
import org.obm.caldav.utils.DOMUtils;
import org.w3c.dom.Element;

/**
 *  Name:       resourcetype
 *  Purpose:    Specifies the nature of the resource.
 *  Description: The resourcetype property MUST be defined on all DAV
 * 				 compliant resources.  The default value is empty.
 *  
 *  <!ELEMENT resourcetype ANY >
 * 
 * @author adrienp
 *
 */
public class DResourceType extends DavPropertyHandler {

	public DResourceType(IProxy proxy) {
		super(proxy);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void appendPropertyValue(Element prop, Token t, DavRequest req) {
		DOMUtils.createElement(prop, "D:collection");
		DOMUtils.createElement(prop, "C:calendar");
	}


}
