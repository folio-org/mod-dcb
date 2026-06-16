package org.folio.dcb.utils;

import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;

@UtilityClass
public class TransactionDetailsUtil {

  public static boolean statusesNotEqual(TransactionStatus.StatusEnum statusOne,
      TransactionStatus.StatusEnum statusTwo) {
    return !Objects.equals(statusOne, statusTwo);
  }

  public static boolean rolesNotEqual(DcbTransaction.RoleEnum roleOne, DcbTransaction.RoleEnum roleTwo) {
    return !Objects.equals(roleOne, roleTwo);
  }
}
