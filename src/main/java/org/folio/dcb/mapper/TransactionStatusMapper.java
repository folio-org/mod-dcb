package org.folio.dcb.mapper;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionStatusMapper {
  TransactionStatusDto mapToDto(TransactionStatus transactions);
}
