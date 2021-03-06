/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2014  Linagora
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version, provided you comply 
 * with the Additional Terms applicable for OBM connector by Linagora 
 * pursuant to Section 7 of the GNU Affero General Public License, 
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain 
 * the “Message sent thanks to OBM, Free Communication by Linagora” 
 * signature notice appended to any and all outbound messages 
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between 
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain 
 * from infringing Linagora intellectual property rights over its trademarks 
 * and commercial brands. Other Additional Terms apply, 
 * see <http://www.linagora.com/licenses/> for more details. 
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details. 
 *
 * You should have received a copy of the GNU Affero General Public License 
 * and its applicable Additional Terms for OBM along with this program. If not, 
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3 
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to 
 * OBM connectors. 
 * 
 * ***** END LICENSE BLOCK ***** */
package org.obm;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class DateUtils {

	/**
	 * Creates a Date object with the timezone offset for tzid applied. It must be in the format
	 * of ISO8601 with separators. Do not add a "Z" at the end, this will be done by the function.
	 *
	 * @param refDate		A String containing the reference date
	 * @param tzid			The timezone to return the date in
	 * @return				The converted date
	 */
	public static Date dateInZone(String dateAsString, String tzid) {
		return new DateTime(dateAsString, DateTimeZone.forID(tzid)).toDate();
	}

	public static Date date(String dateAsString) {
		return new DateTime(dateAsString).toDate();
	}

	public static Date dateUTC(String dateAsString) {
		return new DateTime(dateAsString, DateTimeZone.UTC).toDate();
	}
}
