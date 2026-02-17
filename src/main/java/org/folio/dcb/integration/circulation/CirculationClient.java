package org.folio.dcb.integration.circulation;

import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.CheckInRequest;
import org.folio.dcb.domain.dto.CheckOutRequest;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.Loan;
import org.folio.dcb.domain.dto.LoanCollection;
import org.folio.dcb.domain.dto.RenewByIdRequest;
import org.folio.dcb.domain.dto.RenewByIdResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange("circulation")
public interface CirculationClient {

  @PostExchange("/requests")
  CirculationRequest createRequest(@RequestBody CirculationRequest circulationRequest);

  @GetExchange("/requests")
  ResultList<CirculationRequest> findByQuery(
    @RequestParam("query") String query,
    @RequestParam("limit") Integer limit);

  @PostExchange("/check-in-by-barcode")
  void checkInByBarcode(@RequestBody CheckInRequest checkInRequest);

  @PostExchange("/check-out-by-barcode")
  void checkOutByBarcode(@RequestBody CheckOutRequest checkOutRequest);

  @PutExchange("/requests/{requestId}")
  CirculationRequest updateRequest(@PathVariable String requestId,
    @RequestBody CirculationRequest circulationRequest);

  @GetExchange("/loans")
  LoanCollection fetchLoanByQuery(@RequestParam("query") String query);

  @PostExchange("/renew-by-id")
  RenewByIdResponse renewById(@RequestBody RenewByIdRequest renewByIdRequest);

  @PutExchange("/loans/{loanId}")
  void updateLoan(@PathVariable String loanId, @RequestBody Loan requestBody);
}
