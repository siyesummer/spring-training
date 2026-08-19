package cn.siyes.training.spring.xml.service;

import cn.siyes.training.spring.xml.exception.TransferException;
import cn.siyes.training.spring.xml.repository.AccountRepository;
import cn.siyes.training.spring.xml.repository.AuditLogRepository;

import java.math.BigDecimal;

public class AccountService {
  private final AccountRepository accountRepository;
  private final AuditLogRepository auditLogRepository;

  public AccountService(AccountRepository accountRepository, AuditLogRepository auditLogRepository) {
    this.accountRepository = accountRepository;
    this.auditLogRepository = auditLogRepository;
  }

  public void sayHi() {
    System.out.println("你好啊");
  }

  public void transfer(long fromAccountId, long toAccountId, BigDecimal amount){
    System.out.println("transfer执行");
    if (fromAccountId == toAccountId) {
      throw new TransferException("付款账户和收款账户不能相同");
    }

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new TransferException("金额必须大于零");
    }

    final int debit = accountRepository.debit(fromAccountId, amount);
    if (debit != 1) {
      throw new TransferException("转出失败");
    }

    final int credit = accountRepository.credit(toAccountId, amount);
    if (credit != 1) {
      throw new TransferException("转入失败");
    }

    final int insert = auditLogRepository.insert(fromAccountId, toAccountId, amount);
    if (insert != 1) {
      throw new TransferException("转账日志写入失败");
    }

  }
}
