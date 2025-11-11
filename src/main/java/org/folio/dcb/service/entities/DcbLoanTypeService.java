package org.folio.dcb.service.entities;

import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.folio.dcb.utils.DCBConstants.DCB_LOAN_TYPE_NAME;
import static org.folio.dcb.utils.DCBConstants.LOAN_TYPE_ID;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.LoanTypeClient;
import org.folio.dcb.client.feign.LoanTypeClient.LoanType;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbLoanTypeService implements DcbEntityService<LoanType> {

  private final LoanTypeClient loanTypeClient;

  @Override
  public Optional<LoanType> findDcbEntity() {
    var cqlQuery = exactMatchByName(DCB_LOAN_TYPE_NAME);
    var librariesByQuery = loanTypeClient.findByQuery(cqlQuery);
    return findFirstValue(librariesByQuery);
  }

  @Override
  public LoanType createDcbEntity() {
    log.debug("createDcbEntity:: Creating DCB loan type");
    var loanType = getDcbLoanType();
    var createdLoanType = loanTypeClient.createLoanType(loanType);
    log.info("createDcbEntity:: DCB loan type created");
    return createdLoanType;
  }

  @Override
  public LoanType getDefaultValue() {
    return getDcbLoanType();
  }

  private static LoanType getDcbLoanType() {
    return LoanType.builder()
      .id(LOAN_TYPE_ID)
      .name(DCB_LOAN_TYPE_NAME)
      .build();
  }
}
