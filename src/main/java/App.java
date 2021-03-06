import edu.memphis.cs.netlab.nacapp.Global;
import edu.memphis.cs.netlab.nacapp.InterestHandler;
import net.named_data.jndn.*;
import net.named_data.jndn.encrypt.ProducerDb;
import net.named_data.jndn.util.Common;

import java.util.Date;

/*
 * This Java source file was generated by the Gradle 'init' task.
 */
public class App {
	public String getGreeting() {
		return "Hello world.";
	}

	private static class TemperatureHandler implements InterestHandler {
		private TemperatureReader node;

		TemperatureHandler(TemperatureReader node) {
			this.node = node;
		}

		@Override
		public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
							   InterestFilter filter) {
			double timeslot = Common.dateToMillisecondsSince1970(new Date());
			node.queryTemperature(timeslot, new OnData() {
				@Override
				public void onData(Interest interest, Data data) {
					node.putData(data);
				}
			});
		}

		@Override
		public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
		}

		@Override
		public String path() {
			return "/";
		}
	}

	private static class TemperatureCKeyHandler implements InterestHandler {
		private TemperatureReader node;

		TemperatureCKeyHandler(TemperatureReader node) {
			this.node = node;
		}

		@Override
		public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
							   InterestFilter filter) {
			Name ckeyName = interest.getName();
			Data ckey = node.searchLocalCKey(ckeyName);
			if (null == ckey){
				Global.LOGGER.warning("CKEY for " + ckeyName.toUri() + " not found");
			} else {
				node.putData(ckey);
			}
		}

		@Override
		public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
		}

		@Override
		public String path() {
			return "/C-KEY";
		}
	}

	private static void registerPrefixes(TemperatureReader node, Runnable onSuccess) {
		TemperatureHandler temperatureHandler = new TemperatureHandler(node);
		TemperatureCKeyHandler cKeyHandler = new TemperatureCKeyHandler(node);
		node.registerPrefixes(new InterestHandler[]{temperatureHandler, cKeyHandler}, onSuccess);
	}

	public static void main(String[] args) {
		Name prefix = new Name(Global.LOCAL_HOME + "/SAMPLE/location/bedroom");
		TemperatureReader node;
		try {
			node = new TemperatureReader(
				new Name(Global.LOCAL_HOME),
				new Name("/location/bedroom/temperature"),
				":memory:"
			);

			registerPrefixes(node, new Runnable() {
				@Override
				public void run() {
					Global.LOGGER.info("Start serving... ");
				}
			});

			node.startFaceProcessing();
		} catch (ProducerDb.Error error) {
			throw new RuntimeException(error);
		}
	}
}
