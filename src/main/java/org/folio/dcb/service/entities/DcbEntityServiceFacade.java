package org.folio.dcb.service.entities;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.folio.dcb.client.feign.CancellationReasonClient.CancellationReason;
import org.folio.dcb.client.feign.HoldingsStorageClient.Holding;
import org.folio.dcb.client.feign.LoanTypeClient.LoanType;
import org.folio.dcb.client.feign.LocationsClient.LocationDTO;
import org.folio.dcb.config.DcbFeatureProperties;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DcbEntityServiceFacade {

  private final DcbHoldingService dcbHoldingService;
  private final DcbLoanTypeService dcbLoanTypeService;
  private final DcbCalendarService dcbCalendarService;
  private final DcbLocationService dcbLocationService;
  private final DcbFeatureProperties dcbFeatureProperties;
  private final DcbServicePointService dcbServicePointService;
  private final DcbCancellationReasonService dcbCancellationReasonService;

  /**
   * Creates all DCB umbrella entities if they do not already exist.
   * <p>
   * This method orchestrates the creation of all necessary DCB entities in the following order:
   * <ol>
   *   <li>Location Units (DCB institution, campus and library) and DCB Location</li>
   *   <li>DCB Calendar with DCB Service Point</li>
   *   <li>DCB Holding with Instance, Holding Source and Instance type</li>
   *   <li>Cancellation Reason</li>
   *   <li>Loan Type</li>
   * </ol>
   * Each entity is created only if it doesn't already exist in the system.
   * </p>
   */
  public void createAll() {
    log.debug("createAll:: creating DCB umbrella entities if not exist");
    dcbLocationService.findOrCreateEntity();
    dcbHoldingService.findOrCreateEntity();
    dcbCancellationReasonService.findOrCreateEntity();
    dcbLoanTypeService.findOrCreateEntity();
    dcbCalendarService.findOrCreateEntity();
    log.debug("createAll:: DCB umbrella entities creation process completed");
  }

  /**
   * Finds or creates a DCB holding entity.
   *
   * @return the DCB holding entity
   */
  public Holding findOrCreateHolding() {
    return getOrCreateEntity(
      dcbHoldingService::findOrCreateEntity,
      dcbHoldingService::getDefaultValue
    );
  }

  /**
   * Finds or creates a DCB location entity.
   *
   * @return the DCB location DTO
   */
  public LocationDTO findOrCreateLocation() {
    return getOrCreateEntity(
      dcbLocationService::findOrCreateEntity,
      dcbLocationService::getDefaultValue
    );
  }

  /**
   * Finds or creates a DCB cancellation reason.
   *
   * @return the DCB location DTO
   */
  public CancellationReason findOrCreateCancellationReason() {
    return getOrCreateEntity(
      dcbCancellationReasonService::findOrCreateEntity,
      dcbCancellationReasonService::getDefaultValue
    );
  }

  /**
   * Finds or creates a DCB loan type entity.
   *
   * @return the DCB loan type
   */
  public LoanType findOrCreateLoanType() {
    return getOrCreateEntity(
      dcbLoanTypeService::findOrCreateEntity,
      dcbLoanTypeService::getDefaultValue
    );
  }

  /**
   * Finds or creates a DCB Service Point entity.
   *
   * @return the DCB loan type
   */
  public ServicePointRequest findOrCreateServicePoint() {
    return getOrCreateEntity(
      dcbServicePointService::findOrCreateEntity,
      dcbServicePointService::getDefaultValue
    );
  }

  /**
   * Generic method to retrieve or create an entity based on runtime verification settings.
   *
   * @param <T>                  - the type of entity to retrieve or create
   * @param valueSupplier        - supplier that provides the actual entity
   * @param defaultValueSupplier - supplier that provides a default entity value
   * @return the entity from the - appropriate supplier based on runtime verification configuration
   */
  private <T> T getOrCreateEntity(Supplier<T> valueSupplier, Supplier<T> defaultValueSupplier) {
    if (dcbFeatureProperties.isDcbEntitiesRuntimeVerificationEnabled()) {
      return valueSupplier.get();
    }

    return defaultValueSupplier.get();
  }
}
