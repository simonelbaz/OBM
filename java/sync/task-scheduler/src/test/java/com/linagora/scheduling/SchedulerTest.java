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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Fail;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Atomics;
import com.google.common.util.concurrent.SettableFuture;
import com.linagora.scheduling.ScheduledTask.Listener;
import com.linagora.scheduling.ScheduledTask.State;

public class SchedulerTest {

	private Scheduler testee;
	private DateTime now;
	private TestDateTimeProvider dateTimeProvider;

	public static class TestDateTimeProvider implements DateTimeProvider {
		
		private DateTime current;

		public TestDateTimeProvider(DateTime start) {
			this.current = start;
		}
		
		public void setCurrent(DateTime current) {
			this.current = current;
		}
		
		@Override
		public DateTime now() {
			return current;
		}
	}
	
	@Before
	public void setup() {
		now = DateTime.parse("2014-12-2T11:35");
		dateTimeProvider = new TestDateTimeProvider(now);
		testee = Scheduler.builder().resolution(10, TimeUnit.MILLISECONDS).timeProvider(dateTimeProvider).start();
	}
	
	@After
	public void shutdown() throws Exception {
		testee.stop();
	}
	
	@Test
	public void stopShouldBeCallableMoreThanOnce() throws Exception {
		testee.stop();
		testee.stop();
	}
	
	@Test
	public void startShouldBeCallableMoreThanOnce() {
		testee.start();
	}
	
	@Test
	public void schedulerShouldBuildWithoutAnyParam() throws Exception {
		try (Scheduler scheduler = Scheduler.builder().start()) {
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void schedulerShouldNotBuildWithNegativeResolution() throws Exception {
		try (Scheduler scheduler = Scheduler.builder().resolution(-1, TimeUnit.SECONDS).start()) {
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void schedulerShouldNotBuildWithZeroResolution() throws Exception {
		try (Scheduler scheduler = Scheduler.builder().resolution(0, TimeUnit.SECONDS).start()) {
		}
	}
	
	@Test(expected=NullPointerException.class)
	public void schedulerShouldNotBuildWithNullResolution() throws Exception {
		try (Scheduler scheduler = Scheduler.builder().resolution(1, null).start()) {
		}
	}
	@Test(expected=NullPointerException.class)
	public void schedulerShouldNotBuildWithNullTimeProvider() throws Exception {
		try (Scheduler scheduler = Scheduler.builder().timeProvider(null).start()) {
		}
	}
	
	@Test(expected=NullPointerException.class)
	public void scheduleShouldThrowOnNullTask() {
		assertThat(testee.schedule((Task)null));
	}
	
	@Test(expected=NullPointerException.class)
	public void scheduleAtShouldThrowOnNullDateTime() {
		assertThat(testee.schedule(dummyTask()).at(null));
	}
	
	@Test(expected=TaskInThePastException.class)
	public void scheduleAtShouldThrowOnTaskScheduledInThePast() {
		assertThat(testee.schedule(dummyTask()).at(now.minusHours(1)));
	}
	
	@Test
	public void scheduleAtShouldRegisterATaskForNextHour() {
		DateTime targetTime = now.plusHours(1);
		Task task = dummyTask();
		ScheduledTask actual = testee.schedule(task).at(targetTime);
		assertThat(actual.scheduledTime()).isEqualTo(targetTime);
		assertThat(actual.task()).isEqualTo(task);
		assertThat(actual.state()).isEqualTo(ScheduledTask.State.WAITING);
	}
	
	@Test(expected=NullPointerException.class)
	public void scheduleInShouldThrowOnTaskScheduledInThePast() {
		testee.schedule(dummyTask()).in(null);
	}
	
	@Test(expected=NullPointerException.class)
	public void scheduleShouldThrowOnNullListener() {
		testee.schedule(dummyTask()).addListener(null);
	}
	
	@Test
	public void scheduleInShouldRegisterATaskForNextHour() {
		DateTime targetTime = now.plusHours(1);
		Task task = dummyTask();
		ScheduledTask actual = testee.schedule(task).in(Period.hours(1));
		assertThat(actual.scheduledTime()).isEqualTo(targetTime);
		assertThat(actual.task()).isEqualTo(task);
		assertThat(actual.state()).isEqualTo(ScheduledTask.State.WAITING);
	}
	
	@Test
	public void scheduleInShouldRunATaskInOneHour() throws Exception {
		DateTime targetTime = now.plusHours(1);
		Task task = dummyTask(Duration.millis(10));
		TestListener testListener = new TestListener();
		ScheduledTask actual = testee.schedule(task).addListener(testListener).in(Period.hours(1));
		assertThat(actual.state()).isEqualTo(ScheduledTask.State.WAITING);
		dateTimeProvider.setCurrent(targetTime);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.RUNNING);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.TERMINATED);
	}
	
	@Test
	public void scheduleInShouldNotRunACanceldedTaskInOneHour() throws Exception {
		DateTime targetTime = now.plusHours(1);
		Task task = dummyTask();
		TestListener testListener = new TestListener();
		ScheduledTask actual = testee.schedule(task).addListener(testListener).in(Period.hours(1));
		assertThat(actual.state()).isEqualTo(ScheduledTask.State.WAITING);
		actual.cancel();
		assertThat(actual.state()).isEqualTo(ScheduledTask.State.CANCELED);
		dateTimeProvider.setCurrent(targetTime);
		try {
			testListener.getNextState(100, TimeUnit.MILLISECONDS);
			Fail.failBecauseExceptionWasNotThrown(TimeoutException.class);
		} catch (TimeoutException e) {
		}
	}

	@Test
	public void cancelShouldNotAbortARunningTask() throws Exception {
		Task task = dummyTask(Duration.millis(100));
		TestListener testListener = new TestListener();
		ScheduledTask actual = testee.schedule(task).addListener(testListener).at(now);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.RUNNING);
		actual.cancel();
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.TERMINATED);
	}
	
	@Test
	public void cancelShouldNotFailWhenTerminatedTask() throws Exception {
		Task task = dummyTask(Duration.millis(100));
		TestListener testListener = new TestListener();
		ScheduledTask actual = testee.schedule(task).addListener(testListener).at(now);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.RUNNING);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.TERMINATED);
		actual.cancel();
	}
	
	@Test
	public void scheduleInShouldRunATaskInOneYearAnd2Hours() throws Exception {
		Task task = dummyTask(Duration.millis(10));
		TestListener testListener = new TestListener();
		ScheduledTask actual = testee.schedule(task).addListener(testListener).in(Period.years(1).plusHours(2));
		dateTimeProvider.setCurrent(now.plusYears(1));
		assertThat(actual.state()).isEqualTo(ScheduledTask.State.WAITING);
		dateTimeProvider.setCurrent(now.plusYears(1).plusHours(2));
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.RUNNING);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.TERMINATED);
	}
	
	@Test
	public void scheduleInShouldRunATaskOnceWhenHourJumpBack() throws Exception {
		Task task = dummyTask(Duration.millis(10));
		TestListener testListener = new TestListener();
		testee.schedule(task).addListener(testListener).in(Period.hours(2));
		dateTimeProvider.setCurrent(now.plusHours(2));
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.RUNNING);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.TERMINATED);
		dateTimeProvider.setCurrent(now.minusHours(2));
		dateTimeProvider.setCurrent(now.plusHours(2));
		try {
			testListener.getNextState(100, TimeUnit.MILLISECONDS);
			Fail.failBecauseExceptionWasNotThrown(TimeoutException.class);
		} catch (TimeoutException e) {
		}
	}
	
	@Test
	public void scheduleAtShouldRunATaskOnceWhenHourJumpBack() throws Exception {
		Task task = dummyTask(Duration.millis(10));
		TestListener testListener = new TestListener();
		testee.schedule(task).addListener(testListener).at(now.plusHours(2));
		dateTimeProvider.setCurrent(now.plusHours(2));
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.RUNNING);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.TERMINATED);
		dateTimeProvider.setCurrent(now.minusHours(2));
		dateTimeProvider.setCurrent(now.plusHours(2));
		try {
			testListener.getNextState(100, TimeUnit.MILLISECONDS);
			Fail.failBecauseExceptionWasNotThrown(TimeoutException.class);
		} catch (TimeoutException e) {
		}
	}
	
	@Test
	public void scheduleInShouldRunATaskOnceWhenHourJumpForward() throws Exception {
		Task task = dummyTask(Duration.millis(10));
		TestListener testListener = new TestListener();
		testee.schedule(task).addListener(testListener).in(Period.hours(2));
		dateTimeProvider.setCurrent(now.plusHours(3));
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.RUNNING);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.TERMINATED);
	}
	
	@Test
	public void scheduleAtShouldRunATaskOnceWhenHourJumpForward() throws Exception {
		Task task = dummyTask(Duration.millis(10));
		TestListener testListener = new TestListener();
		testee.schedule(task).addListener(testListener).at(now.plusHours(2));
		dateTimeProvider.setCurrent(now.plusHours(3));
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.RUNNING);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.TERMINATED);
	}
	
	@Test
	public void scheduleInShouldRunNothingAfterStop() throws Exception {
		Task task = dummyTask();
		TestListener testListener = new TestListener();
		testee.schedule(task).addListener(testListener).in(Period.hours(2));
		testee.stop();
		dateTimeProvider.setCurrent(now.plusHours(2));
		try {
			testListener.getNextState(100, TimeUnit.MILLISECONDS);
			Fail.failBecauseExceptionWasNotThrown(TimeoutException.class);
		} catch (TimeoutException e) {
		}
	}
	
	@Test
	public void listenerFailureShouldNotBreakNotification() throws Exception {
		DateTime targetTime = now.plusHours(1);
		Task task = dummyTask(Duration.millis(100));
		TestListener testListener = new TestListener();
		Future<State> scheduledFuture = testListener.getFutureState();
		testee.schedule(task).addListener(new Listener() {
			@Override
			public void scheduled() {
				throw new RuntimeException();
			}
			
			@Override
			public void running() {
				throw new RuntimeException();
			}
			
			@Override
			public void terminated() {
				throw new RuntimeException();
			}
		}).addListener(testListener).in(Period.hours(1));
		assertThat(scheduledFuture.get(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.WAITING);
		dateTimeProvider.setCurrent(targetTime);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.RUNNING);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.TERMINATED);
	}

	@Test
	public void failingTaskShouldNotifyListener() throws Exception {
		TestListener testListener = new TestListener();
		testee.schedule(failingTask(Duration.millis(100))).addListener(testListener).in(Period.hours(1));
		dateTimeProvider.setCurrent(now.plusHours(1));
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.RUNNING);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.FAILED);
		assertThat(testListener.failure).isExactlyInstanceOf(RuntimeException.class).hasMessage("failing task message");
	}
	
	@Test
	public void failedListenerShouldNotBreakNotification() throws Exception {
		TestListener testListener = new TestListener();
		testee.schedule(failingTask(Duration.millis(100))).addListener(new Listener() {
			@Override
			public void failed(Throwable failure) {
				throw new RuntimeException();
			}
		}).addListener(testListener).in(Period.hours(1));
		dateTimeProvider.setCurrent(now.plusHours(1));
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.RUNNING);
		assertThat(testListener.getNextState(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.FAILED);
	}
	
	@Test
	public void canceledListenerShouldNotBreakNotification() throws Exception {
		Task task = dummyTask(Duration.millis(100));
		TestListener testListener = new TestListener();
		ScheduledTask scheduled = testee.schedule(task).addListener(new Listener() {
			@Override
			public void canceled() {
				throw new RuntimeException();
			}
		}).addListener(testListener).in(Period.hours(1));
		Future<State> canceledFuture = testListener.getFutureState();
		scheduled.cancel();
		assertThat(canceledFuture.get(1500, TimeUnit.MILLISECONDS)).isEqualTo(ScheduledTask.State.CANCELED);
	}
	
	private Task dummyTask() {
		return dummyTask(Duration.standardSeconds(1));
	}
	
	private Task dummyTask(final Duration duration) {
		return new Task() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(duration.getMillis());
				} catch (InterruptedException e) {
					Throwables.propagate(e);
				}
			}
			
			@Override
			public String taskName() {
				return "dummy";
			}
		};
	}
	
	private Task failingTask(final Duration duration) {
		return new Task() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(duration.getMillis());
				} catch (InterruptedException e) {
				}
				throw new RuntimeException("failing task message");
			}
			
			@Override
			public String taskName() {
				return "failing task";
			}
		};
	}
	
	private static class TestListener extends Listener {

		AtomicReference<SettableFuture<State>> stateRef = Atomics.newReference(SettableFuture.<State>create());
		Throwable failure;

		Future<State> getFutureState() {
			return stateRef.get();
		}
		
		State getNextState(int timeout, TimeUnit unit) throws Exception {
			Stopwatch start = Stopwatch.createStarted();
			try {
				return stateRef.get().get(timeout, unit);
			} finally {
				System.out.println("next state in : " + start.elapsed(TimeUnit.MILLISECONDS));
			}
		}

		void notifyOnce(State state) {
			stateRef.getAndSet(SettableFuture.<State>create()).set(state);
		}

		@Override
		public void canceled() {
			notifyOnce(State.CANCELED);
		}
		
		@Override
		public void running() {
			notifyOnce(State.RUNNING);
		}

		@Override
		public void terminated() {
			notifyOnce(State.TERMINATED);
		}
		
		@Override
		public void failed(Throwable failure) {
			this.failure = failure;
			notifyOnce(State.FAILED);
		}
		
		@Override
		public void scheduled() {
			notifyOnce(State.WAITING);
		}
	}
}

