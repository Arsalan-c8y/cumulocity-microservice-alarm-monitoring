package cumulocity.monitoring.alarm.service;

import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.cumulocity.microservice.context.ContextService;
import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.microservice.subscription.model.MicroserviceSubscriptionAddedEvent;
import com.cumulocity.microservice.subscription.model.core.PlatformProperties;
import com.cumulocity.microservice.subscription.repository.application.ApplicationApi;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.event.CumulocitySeverities;
import com.cumulocity.rest.representation.application.ApplicationRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.PlatformParameters;
import com.cumulocity.sdk.client.RestConnector;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.InventoryFilter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MicroserviceMonitoringAlarmSample {

	private static final String APPLICATION_PREFIX = "c8y_Application_";

	@Value("${C8Y.bootstrap.tenant}")
	private String microserviceDeploymentTenant;

	private ManagedObjectRepresentation alarmCreationManagedObject;

	@Autowired
	private ApplicationApi applicationApi;

	@Autowired
	InventoryApi inventoryApi;

	@Autowired
	AlarmService alarmService;

	@Autowired
	private MicroserviceSubscriptionsService subscriptionsService;

	@Autowired
	private ContextService<MicroserviceCredentials> contextService;
	@Autowired
	private PlatformProperties platformProperties;

	@EventListener
	public void onSubscriptionAdded(final MicroserviceSubscriptionAddedEvent event) {
		final String tenantId = event.getCredentials().getTenant();

		subscriptionsService.runForTenant(tenantId, () -> {
			try {
				if (tenantId.equals(microserviceDeploymentTenant)) {
					ApplicationRepresentation microserviceRepresentation = getCurrentApplication();
					String applicationId = microserviceRepresentation.getId();
					log.info("Current application id:" + applicationId);
					alarmCreationManagedObject = getAssociatedManagedObject(applicationId);

					testAlarms();
				}

			} catch (Exception e) {
				log.info(e.getMessage());
			}
		});

	}

	public ApplicationRepresentation getCurrentApplication() {
		return contextService
				.callWithinContext((MicroserviceCredentials) platformProperties.getMicroserviceBoostrapUser(), () -> {
					return applicationApi.currentApplication().get();
				});
	}

	private void testAlarms() {
		// create an alarm on the microservice
		alarmService.createAlarm(alarmCreationManagedObject.getId().getValue(), "sampleAlarmType", "alarm text",
				CumulocitySeverities.CRITICAL);

		// clear the alarm

		alarmService.clearAlarmForSourceForType(alarmCreationManagedObject.getId().getValue(), "sampleAlarmType");

	}

	private ManagedObjectRepresentation getAssociatedManagedObject(String applicationId) {
		InventoryFilter filter = new InventoryFilter();
		filter.byType(APPLICATION_PREFIX + applicationId);
		Iterator<ManagedObjectRepresentation> i = inventoryApi.getManagedObjectsByFilter(filter).get().elements(1)
				.iterator();

		if (i.hasNext()) {
			return i.next();
		} else {
			throw new IllegalStateException(
					"Unable to determine microservice associated managed object. Can not continue");
		}

	}
}
