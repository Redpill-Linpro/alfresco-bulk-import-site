package se.redpill.alfresco.repo.bulkimportsite.mock;

import org.alfresco.repo.transaction.RetryingTransactionHelper;

public class RetryingTransactionHelperMock extends RetryingTransactionHelper {
  @Override
  public <R> R doInTransaction(RetryingTransactionCallback<R> cb, boolean readOnly) {
    R execute = null;
    try {
      execute = cb.execute();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
    return execute;
  }

  @Override
  public <R> R doInTransaction(RetryingTransactionCallback<R> cb, boolean readOnly, boolean requiresNew) {
    R execute = null;
    try {
      execute = cb.execute();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
    return execute;
  }
}
