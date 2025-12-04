package org.folio.dcb.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ResultListTest {

  @Test
  void asSinglePage_positive_varargsInput() {
    var result = ResultList.asSinglePage(1, 2, 3);
    assertThat(result.getResult()).isEqualTo(List.of(1, 2, 3));
    assertThat(result.getTotalRecords()).isEqualTo(3);
  }

  @Test
  void asSinglePage_positive_listInput() {
    var result = ResultList.asSinglePage(List.of(1, 2, 3));
    assertThat(result.getResult()).isEqualTo(List.of(1, 2, 3));
    assertThat(result.getTotalRecords()).isEqualTo(3);
  }
}
