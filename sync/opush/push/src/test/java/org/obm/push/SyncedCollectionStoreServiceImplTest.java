package org.obm.push;

import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.obm.push.impl.Credentials;
import org.obm.push.store.SyncCollection;

import com.google.common.collect.Lists;

public class SyncedCollectionStoreServiceImplTest {

	private ObjectStoreManager objectStoreManager;
	private SyncedCollectionStoreServiceImpl syncedCollectionStoreServiceImpl;
	private Credentials credentials;
	
	@Before
	public void init() {
		this.objectStoreManager = new ObjectStoreManager();
		this.syncedCollectionStoreServiceImpl = new SyncedCollectionStoreServiceImpl(objectStoreManager);
		this.credentials = new Credentials("login@domain", "password");
	}
	
	@Test
	public void get() {
		SyncCollection syncCollection = syncedCollectionStoreServiceImpl.get(credentials, getFakeDeviceId(), 1);
		Assert.assertNull(syncCollection);
	}
	
	@Test
	public void put() {
		syncedCollectionStoreServiceImpl.put(credentials, getFakeDeviceId(), buildListCollection(1));
		SyncCollection syncCollection = syncedCollectionStoreServiceImpl.get(credentials, getFakeDeviceId(), 1);
		Assert.assertNotNull(syncCollection);
		Assert.assertEquals(new Integer(1), syncCollection.getCollectionId());
	}
	
	@Test
	public void putUpdatedCollection() {
		Collection<SyncCollection> cols = buildListCollection(1);
		cols.iterator().next().setCollectionPath("PATH1");
		syncedCollectionStoreServiceImpl.put(credentials, getFakeDeviceId(), cols);
		cols.iterator().next().setCollectionPath("PATH1CHANGE");
		syncedCollectionStoreServiceImpl.put(credentials, getFakeDeviceId(), cols);
		
		SyncCollection syncCollection = syncedCollectionStoreServiceImpl.get(credentials, getFakeDeviceId(), 1);
		Assert.assertNotNull(syncCollection);
		Assert.assertEquals(new Integer(1), syncCollection.getCollectionId());
		Assert.assertEquals("PATH1CHANGE", syncCollection.getCollectionPath());
	}
	
	@Test
	public void putList() {
		syncedCollectionStoreServiceImpl.put(credentials, getFakeDeviceId(), buildListCollection(1,2,3));
		SyncCollection syncCollection1 = syncedCollectionStoreServiceImpl.get(credentials, getFakeDeviceId(), 1);
		SyncCollection syncCollection2 = syncedCollectionStoreServiceImpl.get(credentials, getFakeDeviceId(), 2);
		SyncCollection syncCollection3 = syncedCollectionStoreServiceImpl.get(credentials, getFakeDeviceId(), 3);
		
		Assert.assertNotNull(syncCollection1);
		Assert.assertEquals(new Integer(1), syncCollection1.getCollectionId());
		Assert.assertNotNull(syncCollection2);
		Assert.assertEquals(new Integer(2), syncCollection2.getCollectionId());
		Assert.assertNotNull(syncCollection3);
		Assert.assertEquals(new Integer(3), syncCollection3.getCollectionId());
	}

	private Collection<SyncCollection> buildListCollection(Integer... ids) {
		List<SyncCollection> cols = Lists.newLinkedList();
		for(Integer id : ids){
			SyncCollection col = new SyncCollection();
			col.setCollectionId(id);
			cols.add(col);
		}
		return cols;
	}
	
	private Device getFakeDeviceId(){
		return new Device("DevType", "DevId", null);
	}
	
}
