package org.obm.push.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.obm.push.SyncedCollectionStoreService;
import org.obm.push.backend.BackendSession;
import org.obm.push.backend.Sync;
import org.obm.push.data.CalendarDecoder;
import org.obm.push.data.ContactDecoder;
import org.obm.push.data.EmailDecoder;
import org.obm.push.data.IDataDecoder;
import org.obm.push.data.TaskDecoder;
import org.obm.push.exception.PartialException;
import org.obm.push.exception.ProtocolException;
import org.obm.push.store.BodyPreference;
import org.obm.push.store.CollectionNotFoundException;
import org.obm.push.store.FilterType;
import org.obm.push.store.IApplicationData;
import org.obm.push.store.ISyncStorage;
import org.obm.push.store.MSEmailBodyType;
import org.obm.push.store.PIMDataType;
import org.obm.push.store.SyncCollection;
import org.obm.push.store.SyncCollectionChange;
import org.obm.push.store.SyncCollectionOptions;
import org.obm.push.store.SyncStatus;
import org.obm.push.utils.DOMUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SyncDecoder {

	private static final Logger logger = LoggerFactory.getLogger(SyncDecoder.class);
	
	private final ISyncStorage store;
	private final SyncedCollectionStoreService syncedCollectionStoreService;
	private final Map<PIMDataType, IDataDecoder> decoders;

	@Inject
	private SyncDecoder(SyncedCollectionStoreService syncedCollectionStoreService, ISyncStorage store) {
		this.store = store;
		this.syncedCollectionStoreService = syncedCollectionStoreService;
		this.decoders = ImmutableMap.<PIMDataType, IDataDecoder>builder()
				.put(PIMDataType.CONTACTS, new ContactDecoder())
				.put(PIMDataType.CALENDAR, new CalendarDecoder())
				.put(PIMDataType.EMAIL, new EmailDecoder())
				.put(PIMDataType.TASKS, new TaskDecoder())
				.build();
	}

	public Sync decodeSync(Document doc, BackendSession bs) throws PartialException, ProtocolException {
		Sync ret = new Sync();
		Element root = doc.getDocumentElement();
		ret.setWait(getWait(root));

		Boolean isPartial = getPartial(root);
		if (isPartial) {
			logger.info("Partial element has been found. Collection(s) will be from cache");
		}
		NodeList nl = root.getElementsByTagName("Collection");
		for (int i = 0; i < nl.getLength(); i++) {
			Element col = (Element) nl.item(i);
			SyncCollection collec = getCollection(bs, col, isPartial);
			ret.addCollection(collec);
		}
		if (ret.getCollections().size() == 0) {
			throw new PartialException();
		}
		syncedCollectionStoreService.put(bs.getCredentials(), bs.getDevice(), ret.getCollections());
		return ret;
	}

	private boolean getPartial(Element root) {
		return root.getElementsByTagName("Partial").getLength() > 0;
	}

	private Integer getWait(Element root) {
		Integer ret = 0;
		String wait = DOMUtils.getElementText(root, "Wait");
		if (wait != null && wait.length() > 0) {
			try {
				ret = Integer.parseInt(wait);
			} catch (NumberFormatException e) {
			}
		}
		return ret;
	}

	private SyncCollection getCollection(BackendSession bs, Element col,
			boolean isPartial)
			throws PartialException, ProtocolException{
		SyncCollection collection = new SyncCollection();
		Integer collectionId = getCollectionId(col);
		if (collectionId == null) {
			throw new ProtocolException("CollectionId can't be null");
		}
		SyncCollection lastSyncCollection = syncedCollectionStoreService.get(bs.getCredentials(), bs.getDevice(), collectionId);
		if (isPartial && lastSyncCollection == null) {
			throw new PartialException();
		}
		try {
			collection.setCollectionId(collectionId);
			String collectionPath = store.getCollectionPath(collectionId);
			collection.setCollectionPath(collectionPath);
			PIMDataType dataType = store.getDataClass(collectionPath);
			collection.setDataType(dataType);
			collection.setDataClass(DOMUtils.getElementText(col, "Class"));
			collection.setSyncKey(DOMUtils.getElementText(col, "SyncKey"));

			Element wse = DOMUtils.getUniqueElement(col, "WindowSize");
			if (wse != null) {
				collection.setWindowSize(Integer.parseInt(wse.getTextContent()));
			}
			
			SyncCollectionOptions options = getUpdatedOptions(lastSyncCollection, col);
			collection.setOptions(options);
			
			appendCommand(col, collection);
		} catch (CollectionNotFoundException e) {
			collection.setError(SyncStatus.OBJECT_NOT_FOUND);
		}
		// TODO sync supported
		// TODO sync <getchanges/>

		return collection;
	}

	private SyncCollectionOptions getUpdatedOptions(SyncCollection lastSyncCollection,
			Element collectionElement) {
		SyncCollectionOptions options = null;
		if(lastSyncCollection != null){
			options = lastSyncCollection.getOptions();
		}
		if(options == null){
			options = new SyncCollectionOptions();
		}
		
		Element optionsElement = DOMUtils.getUniqueElement(collectionElement, "Options");
		if (optionsElement != null) {
			String filterTypeElement = DOMUtils.getElementText(optionsElement, "FilterType");
			if (filterTypeElement != null) {
				options.setFilterType(FilterType.getFilterType(filterTypeElement));
			}
			
			String mimeSupport = DOMUtils.getElementText(optionsElement, "MIMESupport");
			if (mimeSupport != null) {
				options.setMimeSupport(Integer.parseInt(mimeSupport));
			}
			
			String mimeTruncation = DOMUtils.getElementText(optionsElement,	"MIMETruncation");
			if (mimeTruncation != null) {
				options.setMimeTruncation(Integer.parseInt(mimeTruncation));
			}
			
			String conflict = DOMUtils.getElementText(optionsElement, "Conflict");
			if (conflict != null) {
				options.setConflict(Integer.parseInt(conflict));
			}

			String truncation = DOMUtils.getElementText(optionsElement, "Truncation");
			if (truncation != null) {
				options.setTruncation(Integer.parseInt(truncation));
			}
			
			String deletesAsMoves = DOMUtils.getElementText(optionsElement,	"DeletesAsMoves");
			if(deletesAsMoves != null){
				if ("0".equals(deletesAsMoves)) {
					options.setDeletesAsMoves(false);
				} else {
					options.setDeletesAsMoves(true);
				}
			}

			NodeList bodyPreferences = optionsElement.getElementsByTagName("BodyPreference");
			if (bodyPreferences != null) {
				for (int i = 0; i < bodyPreferences.getLength(); i++) {
					Element bodyPreference = (Element) bodyPreferences.item(i);
					String truncationSize = DOMUtils.getElementText(
							bodyPreference, "TruncationSize");
					String type = DOMUtils.getElementText(bodyPreference,
							"Type");
					BodyPreference bp = new BodyPreference();
					// nokia n900 sets type without truncationsize
					if (truncationSize != null) {
						bp.setTruncationSize(Integer.parseInt(truncationSize));
					}
					bp.setType(MSEmailBodyType.getValueOf(Integer
							.parseInt(type)));
					options.addBodyPreference(bp);
				}
			}
		}
		return options;
	}

	private Integer getCollectionId(Element col) {
		Element fid = DOMUtils.getUniqueElement(col, "CollectionId");
		if (fid != null) {
			try {
				return Integer.parseInt(fid.getTextContent());
			} catch (NumberFormatException e) {
			}
		}
		return null;
	}

	private void appendCommand(Element col, SyncCollection collection) {
		Element perform = DOMUtils.getUniqueElement(col, "Commands");
		if (perform != null) {
			NodeList fetchs = perform.getElementsByTagName("Fetch");
			List<String> fetchIds = new LinkedList<String>();
			for (int i = 0; i < fetchs.getLength(); i++) {
				Element fetch = (Element) fetchs.item(i);
				fetchIds.add(DOMUtils.getElementText(fetch, "ServerId"));
			}
			collection.setFetchIds(fetchIds);
			// get our sync state for this collection
			NodeList mod = perform.getChildNodes();
			for (int j = 0; j < mod.getLength(); j++) {
				Element modification = (Element) mod.item(j);
				collection.addChange(getChange(collection, modification));
			}
		}
	}

	private SyncCollectionChange getChange(SyncCollection collection,
			Element modification) {
		String modType = modification.getNodeName();
		String serverId = DOMUtils.getElementText(modification, "ServerId");
		String clientId = DOMUtils.getElementText(modification, "ClientId");
		Element syncData = DOMUtils.getUniqueElement(modification,
				"ApplicationData");
		IDataDecoder dd = getDecoder(collection.getDataType());
		IApplicationData data = null;
		if (dd != null) {
			if (syncData != null) {
				data = dd.decode(syncData);
			}
		} else {
			logger.error("no decoder for " + collection.getDataType());
			if (modType.equals("Fetch")) {
				logger.info("adding id to fetch " + serverId);
				collection.getFetchIds().add(serverId);
			}
		}
		return new SyncCollectionChange(serverId, clientId, modType, data,
				collection.getDataType());
	}

	protected IDataDecoder getDecoder(PIMDataType dataClass) {
		return decoders.get(dataClass);
	}

}
