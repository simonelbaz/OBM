/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
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
package com.linagora.scheduling;

import java.util.Collection;
import java.util.concurrent.DelayQueue;

import com.google.common.collect.ImmutableSet;

public interface TaskQueue<T extends Task> {

	Collection<ScheduledTask<T>> poll();
	
	void put(ScheduledTask<T> task);
	
	boolean remove(ScheduledTask<T> task);
	
	void clear();

	boolean hasAnyTask();
		
	public static class DelayedQueue<T extends Task> implements TaskQueue<T> {

		private final DelayQueue<ScheduledTask<T>> queue;

		public DelayedQueue() {
			queue = new DelayQueue<ScheduledTask<T>>();
		}
		
		@Override
		public Collection<ScheduledTask<T>> poll() {
			ScheduledTask<T> scheduledTask = queue.poll();
			if (scheduledTask != null) {
				return ImmutableSet.of(scheduledTask);
			}
			return ImmutableSet.of();
		}

		@Override
		public void put(ScheduledTask<T> task) {
			queue.put(task);
		}

		@Override
		public boolean remove(ScheduledTask<T> task) {
			return queue.remove(task);
		}

		@Override
		public void clear() {
			queue.clear();
		}

		@Override
		public boolean hasAnyTask() {
			return !queue.isEmpty();
		}

	}
}
