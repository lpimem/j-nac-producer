import edu.memphis.cs.netlab.nacapp.Global;
import edu.memphis.cs.netlab.nacapp.NACNode;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.encrypt.EncryptError;
import net.named_data.jndn.encrypt.Producer;
import net.named_data.jndn.encrypt.ProducerDb;
import net.named_data.jndn.encrypt.Sqlite3ProducerDb;
import net.named_data.jndn.util.Blob;

import java.util.LinkedList;
import java.util.List;

public class TemperatureReader extends NACNode implements Producer.OnEncryptedKeys, EncryptError.OnError {
	private Producer producer;
	private LinkedList<Data> cKeys;

	public TemperatureReader(Name prefix, Name dataType, String sqliteDBPath) throws ProducerDb.Error {
		super();
		Name appPrefix = new Name(prefix);
		appPrefix.append(Global.SAMPLE.replace("/", ""));
		appPrefix.append(dataType);
		super.init(appPrefix);
		ProducerDb db = new Sqlite3ProducerDb(sqliteDBPath);
		producer = new Producer(prefix, dataType, m_face, m_keychain, db);
	}

	@Override
	public void onEncryptedKeys(List list) {
		for (Object ckeyObj : list) {
			if (null == ckeyObj) {
				continue;
			}
			Data cKey = (Data) ckeyObj;
			if (this.cKeys == null) {
				this.cKeys = new LinkedList<>();
			}
			this.cKeys.add(cKey);
			System.out.println("Added c-key: " + cKey.getName().toUri());
		}
		if (list.size() < 1) {
			System.err.println("Error: EncryptedKeys list is empty");
		}
	}

	@Override
	public void onError(EncryptError.ErrorCode errorCode, String s) {
		Global.LOGGER.warning(String.format("%s, %s", errorCode, s));
	}

	public Data searchLocalCKey(Name n) {
		System.out.println(
			String.format("Searching c-key for %s (%d keys in store)",
				n.toUri(), null == cKeys ? 0 : cKeys.size()));
		if (null != cKeys) {
			for (Data d : cKeys) {
				System.out.println("\t-> " + d.getName().toUri());
				if (n.isPrefixOf(d.getName())) {
					return d;
				}
			}
		}
		System.out.println("C-Key not found.");
		return null;
	}

	public void queryTemperature(double timeslot, OnData handler) {
		Data d = new Data();
		final double temp = 75.0;
		try {
			Name keyName = producer.createContentKey(timeslot, new Producer.OnEncryptedKeys() {
				@Override
				public void onEncryptedKeys(List keys) {
					TemperatureReader.this.onEncryptedKeys(keys);
					try {
						Global.LOGGER.info("    -> Producing onEncryptedKeys. ");
						producer.produce(d, timeslot, new Blob(String.valueOf(temp)));
						handler.onData(null, d);
					} catch (Throwable e) {
						throw new RuntimeException(e);
					}
				}
			}, this);
			if (null != searchLocalCKey(keyName)) {
				Global.LOGGER.info("    -> Producing on existing local c-key. ");
				producer.produce(d, timeslot, new Blob(String.valueOf(temp)));
				handler.onData(null, d);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

//	public void queryCKey(double timeslot, OnData onCKey) {
//		try {
//			Name n = producer.createContentKey(timeslot, new Producer.OnEncryptedKeys() {
//				@Override
//				public void onEncryptedKeys(List list) {
//					TemperatureReader.this.onEncryptedKeys(list);
//					if (list.size() < 1) {
//						Global.LOGGER.warning("No content key created.");
//						return;
//					}
//					Data d = (Data) list.get(0);
//					onCKey.onData(null, d);
//				}
//			});
//			Data d = searchLocalCKey(n);
//			onCKey.onData(null, d);
//		} catch (Exception error) {
//			Global.LOGGER.warning("queryCKey() -> " + error.getMessage());
//		}
//	}
}