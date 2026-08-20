package cn.siyes.training.spring.annotation.service;

import cn.siyes.training.spring.annotation.exception.TransferException;
import cn.siyes.training.spring.annotation.repository.AccountRepository;
import cn.siyes.training.spring.annotation.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AccountService {
  private final AccountRepository accountRepository;
  private final AuditLogRepository auditLogRepository;

//  如果类只有一个构造器，可以不写 @Autowired，Spring 会自动使用它进行构造器注入。
  public AccountService(AccountRepository accountRepository, AuditLogRepository auditLogRepository) {
    this.accountRepository = accountRepository;
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional(
      propagation = Propagation.REQUIRED,
      isolation = Isolation.READ_COMMITTED,
      rollbackFor = Exception.class
  )
  public void transfer(long formAccountId,
                       long toAccountId,
                       BigDecimal amount) throws TransferException {
    if (formAccountId == toAccountId) {
      throw new TransferException("两个账户不能相同");
    }

    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new TransferException("转账金额必须大于0");
    }

    if (accountRepository.findBalance(formAccountId).compareTo(amount) < 0) {
      throw new TransferException("余额不足");
    }

    if (accountRepository.debit(formAccountId, amount) != 1) {
      throw new TransferException("转出失败");
    }

    if (accountRepository.credit(toAccountId, amount) != 1) {
      throw new TransferException("转入失败");
    }

    if (auditLogRepository.insert(formAccountId, toAccountId, amount) != 1) {
      throw new TransferException("日志写入失败");
    }

  }

  public void print() {}
}
