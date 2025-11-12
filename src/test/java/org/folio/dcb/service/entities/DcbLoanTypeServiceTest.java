package org.folio.dcb.service.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.folio.dcb.utils.DCBConstants.DCB_LOAN_TYPE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.folio.dcb.client.feign.LoanTypeClient;
import org.folio.dcb.client.feign.LoanTypeClient.LoanType;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcbLoanTypeServiceTest {

  private static final String TEST_DCB_LOAN_TYPE_NAME = "DCB Can circulate";
  private static final String TEST_LOAN_TYPE_ID = "4dec5417-0765-4767-bed6-b363a2d7d4e2";

  @InjectMocks private DcbLoanTypeService dcbLoanTypeService;
  @Mock private LoanTypeClient loanTypeClient;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(loanTypeClient);
  }

  @Test
  void findDcbEntity_positive_shouldReturnLoanTypeWhenExists() {
    var foundLoanTypes = ResultList.asSinglePage(dcbLoanType());
    var expectedQuery = exactMatchByName(DCB_LOAN_TYPE_NAME);
    when(loanTypeClient.findByQuery(expectedQuery)).thenReturn(foundLoanTypes);

    var result = dcbLoanTypeService.findDcbEntity();

    assertThat(result).contains(dcbLoanType());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    var expectedQuery = exactMatchByName(DCB_LOAN_TYPE_NAME);
    when(loanTypeClient.findByQuery(expectedQuery)).thenReturn(ResultList.empty());

    var result = dcbLoanTypeService.findDcbEntity();

    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateLoanType() {
    var expectedLoanType = dcbLoanType();
    when(loanTypeClient.createLoanType(expectedLoanType)).thenReturn(expectedLoanType);

    var result = dcbLoanTypeService.createDcbEntity();

    assertThat(result).isEqualTo(dcbLoanType());
    verify(loanTypeClient).createLoanType(expectedLoanType);
  }

  @Test
  void getDefaultValue_positive_shouldReturnLoanTypeWithDefaultValues() {
    var result = dcbLoanTypeService.getDefaultValue();
    assertThat(result).isEqualTo(dcbLoanType());
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingLoanType() {
    var expectedQuery = exactMatchByName(DCB_LOAN_TYPE_NAME);
    var loanTypesResult = ResultList.asSinglePage(dcbLoanType());
    when(loanTypeClient.findByQuery(expectedQuery)).thenReturn(loanTypesResult);

    var result = dcbLoanTypeService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbLoanType());
    verify(loanTypeClient, never()).createLoanType(any());
  }

  private static LoanType dcbLoanType() {
    return LoanType.builder()
      .id(TEST_LOAN_TYPE_ID)
      .name(TEST_DCB_LOAN_TYPE_NAME)
      .build();
  }
}
