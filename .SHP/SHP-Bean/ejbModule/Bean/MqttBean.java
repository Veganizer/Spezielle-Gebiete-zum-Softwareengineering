package Bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import Interface.HomeBeanRemote;
import Interface.MqttBeanRemote;
import Model.Automation;
import Model.Condition;
import Model.SensorData;
import Model.SystemConfig;
import Model.Thing;

/**
 * Startup Bean for Mqtt Client. Connects to the Broker and adds Data to the DB
 */

@Startup
@Singleton
@Remote(MqttBeanRemote.class)
public class MqttBean implements MqttCallback, MqttBeanRemote {
	public static MqttClient client;

	@PersistenceContext
	EntityManager em;

	@EJB
	HomeBeanRemote hb;

	private Map<String, Thing> things = new HashMap<String, Thing>();

	private Map<String, List<Automation>> autos = new HashMap<String, List<Automation>>();
	
	public MqttBean() {		

	}

	@PostConstruct
	public void init() {
		System.out.println("----> MQTT BEAN STARTED <----");
		SystemConfig sc = hb.getSystemConfig();
		System.out.println(sc.getMqttServer());
		autos = buildTopicAutomationMap();
		try {
			@SuppressWarnings("unchecked")
			List<Thing> thingsList = (List<Thing>) em.createNamedQuery(Thing.GET_ALL_THINGS).getResultList();

			client = new MqttClient("tcp://" + sc.getMqttServer(), "SHP" + new Random().nextInt(500000));
			client.connect();
			client.setCallback(this);

			for (Thing t : thingsList) {
				things.put(t.getMqttTopic(), t);
				client.subscribe(t.getMqttTopic());
			}

			System.out.println("!!!!CONNECTED!!!!");
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			System.out.println("!!!!MQTT EXCEPTION!!!!");
			e.printStackTrace();
		}

	}
	
	public void testi(){
		System.out.println("TEEEEEEEEEEEEEEEEEEEEST");
	}
	
	private Map<String, List<Automation>> buildTopicAutomationMap(){
		Map<String, List<Automation>> map = new HashMap<String, List<Automation>>();
		
		List<Automation> autolist =  em.createNamedQuery(Automation.GET_ALL_AUTOMATIONS).getResultList();
		/* loop all conditions in all automations */
		for(Automation a:autolist){
			for(Condition c:a.getConditions()){
				/* get the mqtt topic for the condition */
				String key = c.getThing().getMqttTopic();
				
				/* check if the topic is already in the map */
				if(map.containsKey(key)){
					List<Automation> tmpList = map.get(key);
					/* check if the automation is already in the list, if not, it needs to be added */
					if(!tmpList.contains(a)){
						tmpList.add(a);
						map.put(key,tmpList);
					}
				}else {
					/* the topic was not in the map, it needs to be added */
					List<Automation> newList = new ArrayList<Automation>();
					newList.add(a);
					map.put(key,newList);
				}
			}
		}
		return map;
	}

	

	@Override
	public void messageArrived(String topic, MqttMessage arg1) {
		System.out.println(topic + " " + new String(arg1.getPayload()));
		Thing databaseThing = things.get(topic);
		SensorData data = new SensorData(new String(arg1.getPayload()), databaseThing);
		
		hb.addData(data);
		/* warum ich den umweg �ber die hb gehe und nicht einfach em.persis(data) mache? weils nicht geht... 
		 * namedquerrys gehen, persist nicht :( */
		
		
		

		List<Automation> affectedAutos = autos.get(topic);
		if(affectedAutos == null){
			return;
		}
		
		for(Automation a: affectedAutos){
			System.out.println("affected: " + a.getName());
			if(a.fulfilled(data.getValue(), topic)){
				//FIRE!			
				System.out.println(a.getName() + " will fire!");
			}
		}
		
	}
	
	
	/** 
	 * check if the mqtt client is ready, if not, connect it
	 * @return false = client cant be connected
	 */
	private boolean checkMqttClient() {

		try {
			if (client == null) {
				client = new MqttClient("tcp://" + hb.getSystemConfig().getMqttServer(),
						"SHP" + new Random().nextInt(500000));
			}
			if (!client.isConnected()) {
				client.connect();
			}
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return client.isConnected();
	}
	
	/* (non-Javadoc)
	 * @see Interface.HomeBeanRemote#publish(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean publish(String t, String message) {
		if (!checkMqttClient()){
			return false;
		}
		try {						
			client.publish(t, new MqttMessage(message.getBytes()));
			System.out.println("done");
			return true;
		} catch (MqttException e) {
			e.printStackTrace();
		}
		return false;

	}



	
	/**
	 * Sleep 2 seconds
	 */
	private void sleep2k() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
	}


	@Override
	public void connectionLost(Throwable arg0) {
		System.out.println("DC!");
		sleep2k();
		while (!client.isConnected()) {
			System.out.println("connect...");
			try {				
				client.connect();
			} catch (MqttException e) {
				System.out.println("connect Failed!");
				sleep2k();				
			}
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
	}

}