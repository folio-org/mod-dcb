package org.folio.dcb.service.entities;

import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.folio.dcb.utils.DCBConstants.CODE;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_TYPE_ID;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_TYPE_SOURCE;
import static org.folio.dcb.utils.DCBConstants.NAME;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.InstanceTypeClient;
import org.folio.dcb.client.feign.InstanceTypeClient.InstanceType;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbInstanceTypeService implements DcbEntityService<InstanceType> {

  private final InstanceTypeClient instanceTypeClient;

  @Override
  public Optional<InstanceType> findDcbEntity() {
    var instancesByQuery = instanceTypeClient.findByQuery(exactMatchByName(NAME));
    return findFirstValue(instancesByQuery);
  }

  @Override
  public InstanceType createDcbEntity() {
    log.debug("createDcbEntity:: Creating a new DCB Instance Type");
    var instanceType = getDcbInstanceType();

    instanceTypeClient.createInstanceType(instanceType);
    log.info("createDcbEntity:: DCB Instance Type created");

    return instanceType;
  }

  @Override
  public InstanceType getDefaultValue() {
    return getDcbInstanceType();
  }

  private static InstanceType getDcbInstanceType() {
    return InstanceType.builder()
      .id(INSTANCE_TYPE_ID)
      .code(CODE)
      .name(NAME)
      .source(INSTANCE_TYPE_SOURCE)
      .build();
  }
}
