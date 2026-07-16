package org.folio.dcb.domain.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TransactionMapperTest {

  @InjectMocks private TransactionMapper transactionMapper;

  @DisplayName("mapToEntity_parameterized")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("provideIncompleteDcbTransactions")
  void mapToEntity_parameterized(@SuppressWarnings("unused") String name,
    DcbTransaction dcbTransaction) {
    // TestMate-b53b895f93dcfd0a7ea23b049adfc896
    var transactionId = "550e8400-e29b-41d4-a716-446655440000";
    var result = transactionMapper.mapToEntity(transactionId, dcbTransaction);
    assertThat(result).isNull();
  }

  private static Stream<Arguments> provideIncompleteDcbTransactions() {
    var validItem = DcbItem.builder().id("item-id").barcode("item-barcode").build();
    var validPatron = DcbPatron.builder().id("patron-id").barcode("patron-barcode").build();
    var validPickup = DcbPickup.builder().servicePointId("sp-id").libraryCode("lib-code").build();
    return Stream.of(
      arguments("null transaction", null),
      arguments("valid item & patron & pickup ",
        DcbTransaction.builder().item(null).patron(validPatron).pickup(validPickup).build()),
      arguments("valid item & pickup & null patron",
        DcbTransaction.builder().item(validItem).patron(null).pickup(validPickup).build()),
      arguments("valid item & patron & null pickup",
        DcbTransaction.builder().item(validItem).patron(validPatron).pickup(null).build())
    );
  }
}
