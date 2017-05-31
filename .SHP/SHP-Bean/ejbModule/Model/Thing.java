package Model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

/**
 * Entity implementation class for Entity: Thing
 *
 */
@Entity
@NamedQueries({ 
	@NamedQuery(name = Thing.GET_ALL_THINGS, query = "SELECT Z FROM Thing z"),
	@NamedQuery(name = Thing.GET_ALL_SENSORS, query = "SELECT Z FROM Thing z"),
	@NamedQuery(name = Thing.GET_ALL_ACTORS, query = "SELECT Z FROM Thing z")
})
public class Thing implements Serializable {

	public static final String GET_ALL_THINGS = "Thing.getAll";
	public static final String GET_ALL_SENSORS = "Thing.getAllSensors";
	public static final String GET_ALL_ACTORS = "Thing.getAllActors";
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String name;
	private String mqttTopic;
	
	@OneToMany(mappedBy = "thing", fetch=FetchType.EAGER)
	private List<SensorData> data;
	

    @Enumerated(EnumType.STRING)
    private ThingType type;
	
	private static final long serialVersionUID = 1L;

	public Thing() {
		super();
	}   
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}   
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}   
	public String getMqttTopic() {
		return this.mqttTopic;
	}

	public void setMqttTopic(String mqttTopic) {
		this.mqttTopic = mqttTopic;
	}   
	public List<SensorData> getData() {
		return this.data;
	}

	public void addData(SensorData data) {
		this.data.add(data);
	}
   
	public ThingType getType(){
		return type;
	}
}
