package Model;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Entity implementation class for Entity: Condition
 * Represents a condition for an automation. 
 *
 */
/* name has explicit and in inverted comma, because condition is a mysql keyword */
@Entity(name = "\"Condition\"")
public class Condition implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	private String value;
	private boolean isAnd;

	@Enumerated(EnumType.STRING)
	private ConditionType type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Auto_ID")
	private Automation auto;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Thing_ID")
	private Thing thing;

	private static final long serialVersionUID = 1L;

	@Transient
	private boolean lastFulfillmentStatus = false;

	public Condition() {
		super();
	}

	public Condition(Automation auto, ConditionType type, Thing t, String value) {
		super();
		this.type = type;
		this.thing = t;
		this.value = value;
		this.auto = auto;
	}

	public int getId() {
		return this.id;
	}

	public boolean isAnd() {
		return isAnd;
	}

	public void setAnd(boolean isAnd) {
		this.isAnd = isAnd;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ConditionType getType() {
		return type;
	}
	
	public Automation getAutomation(){
		return auto;
	}

	public Thing getThing() {
		return thing;
	}
	
	public void setThing(Thing newThing){
		thing = newThing;
	}
	
	public void setType(ConditionType ct){
		type = ct;
	}

	/**
	 * check if the condition is fulfilled
	 * 
	 * @param newValue
	 *            the new received value
	 * @param topic
	 *            on the topic
	 * @return true = fulfilled
	 */
	public boolean fulfills(String newValue, String topic) {
		boolean retval = false;

		if (newValue == null) {
			return false;
		}
		/* check if the topic is matching the desired topic */
		if (thing.getMqttTopic().equals(topic)) {
			/* add data to instance */
			thing.addData(newValue);
		} else {
			/* topic is not matching, return last Status */
			System.out.println("topic is different, returning lastStatus");

			return lastFulfillmentStatus;

		}

		float newValueFloat = 0;
		float triggerFloat = 0;
		/*
		 * check if type conversion is required, not required for equal and not
		 * equal
		 */
		if (type != ConditionType.equal && type != ConditionType.notEqual) {
			newValueFloat = Float.parseFloat(newValue);
			triggerFloat = Float.parseFloat(value);
		}

		switch (type) {
		case equal:
			retval = newValue.equals(value);
			break;
		case notEqual:
			retval = !newValue.equals(value);
			break;
		case smaller:
			retval = newValueFloat < triggerFloat;
			break;
		case smallerThan:
			retval = newValueFloat <= triggerFloat;
			break;
		case greater:
			retval = newValueFloat > triggerFloat;
			break;
		case greaterThan:
			retval = newValueFloat >= triggerFloat;
			break;
		}
		lastFulfillmentStatus = retval;
		return retval;
	}

}
