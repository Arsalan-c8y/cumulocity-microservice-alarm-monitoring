package cumulocity.monitoring.alarm.service;

import java.util.Iterator;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cumulocity.model.event.CumulocityAlarmStatuses;
import com.cumulocity.model.event.CumulocitySeverities;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.alarm.AlarmApi;
import com.cumulocity.sdk.client.alarm.AlarmFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AlarmService {

	@Autowired
	private AlarmApi alarmApi;

	public void createAlarm(String sourceId, String alarmType, String alarmText, CumulocitySeverities severity) {

		try {
			log.info("Creating an alarm");
			ManagedObjectRepresentation source = new ManagedObjectRepresentation();
			source.setId(GId.asGId(sourceId));
			AlarmRepresentation alarm = new AlarmRepresentation();
			alarm.setSource(source);
			alarm.setText(alarmText);
			alarm.setType(alarmType);
			alarm.setDateTime(new DateTime());
			alarm.setSeverity(severity.toString());
			alarm.setStatus(CumulocityAlarmStatuses.ACTIVE.toString());

			AlarmRepresentation resp = alarmApi.create(alarm);
			log.info(resp.toJSON());
		} catch (Exception e) {
			log.error("Unable to create alarm");
			log.error(e.getMessage());
		}

	}

	public void clearAlarmForSourceForType(String sourceId, String alarmType) {
		try {
			log.info("Clear alarm for source: {} and type: {}", sourceId, alarmType);
			Iterator<AlarmRepresentation> it = getAlarmsForSourceForTypeIt(sourceId, alarmType);
			while (it.hasNext()) {
				AlarmRepresentation alarm = it.next();
				clearAlarm(alarm);
			}
		} catch (Exception e) {
			log.error("An error occurred while trying to clear alarm");
			log.error(e.getMessage());
		}
	}

	public Iterator<AlarmRepresentation> getAlarmsForSourceForTypeIt(String sourceId, String type) {

		AlarmFilter af = new AlarmFilter().bySource(GId.asGId(sourceId)).bySeverity(CumulocitySeverities.CRITICAL)
				.byType(type);
		return alarmApi.getAlarmsByFilter(af).get().allPages().iterator();

	}

	private void clearAlarm(AlarmRepresentation alarm) {
		try {
			alarm.setStatus(CumulocityAlarmStatuses.CLEARED.toString());
			AlarmRepresentation resp = alarmApi.update(alarm);
			log.info("Alarm cleared: " + resp.toJSON());
		} catch (Exception e) {
			log.error("Unable to update alarm");
			log.error(e.getMessage());
		}
	}

}
