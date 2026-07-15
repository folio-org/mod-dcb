package org.folio.dcb.domain.mapper;

import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.DcbPatron;
import org.folio.dcb.domain.dto.DcbPickup;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TransactionMapperTest {

  @InjectMocks
  private TransactionMapper transactionMapper;

    private static Stream<Arguments> provideIncompleteDcbTransactions() {
    DcbItem validItem = DcbItem.builder().id("item-id").barcode("item-barcode").build();
    DcbPatron validPatron = DcbPatron.builder().id("patron-id").barcode("patron-barcode").build();
    DcbPickup validPickup = DcbPickup.builder().servicePointId("sp-id").libraryCode("lib-code").build();
    return Stream.of(
      Arguments.of((DcbTransaction) null),
      Arguments.of(DcbTransaction.builder().item(null).patron(validPatron).pickup(validPickup).build()),
      Arguments.of(DcbTransaction.builder().item(validItem).patron(null).pickup(validPickup).build()),
      Arguments.of(DcbTransaction.builder().item(validItem).patron(validPatron).pickup(null).build())
    );
  }

    @ParameterizedTest
  @MethodSource("provideIncompleteDcbTransactions")
  void testMapToEntityShouldReturnNullWhenRequiredNestedObjectsAreMissing(DcbTransaction dcbTransaction) {
    // TestMate-b53b895f93dcfd0a7ea23b049adfc896
    // Given
    String transactionId = "550e8400-e29b-41d4-a716-446655440000";
    // When
    TransactionEntity result = transactionMapper.mapToEntity(transactionId, dcbTransaction);
    // Then
    assertThat(result).isNull();
  }

}
