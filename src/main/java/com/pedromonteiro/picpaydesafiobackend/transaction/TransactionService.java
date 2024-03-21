package com.pedromonteiro.picpaydesafiobackend.transaction;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pedromonteiro.picpaydesafiobackend.authorization.AuthorizerService;
import com.pedromonteiro.picpaydesafiobackend.exception.InvalidTransactionException;
import com.pedromonteiro.picpaydesafiobackend.wallet.Wallet;
import com.pedromonteiro.picpaydesafiobackend.wallet.WalletRepository;
import com.pedromonteiro.picpaydesafiobackend.wallet.WalletType;

@Service
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final AuthorizerService authorizerService;

    public TransactionService(TransactionRepository transactionRepository, WalletRepository walletRepository, AuthorizerService authorizerService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.authorizerService = authorizerService;
    }

    @Transactional
    public Transaction create(Transaction transaction) {
        // validar
        validate(transaction);

        //criar transacao
        var newTransaction = transactionRepository.save(transaction);

        //debitar da carteira
        var wallet = walletRepository.findById(transaction.payer()).get();
        walletRepository.save(wallet.debit(transaction.value()));

        //chamar serviços externos
        authorizerService.authorize(transaction);

        return newTransaction;
    }

    /*
     * the payer has a common wallet
     * the payer has enough balance
     * the payer is not the payee
     */
    private void validate(Transaction transaction) {
        walletRepository.findById(transaction.payee())
        .map(payee -> walletRepository.findById(transaction.payer())
            .map(
                payer -> isTransactionValid(transaction, payer))
            .orElseThrow(() -> new InvalidTransactionException("Invalid transaction - %s".formatted(transaction))))
        .orElseThrow(() -> new InvalidTransactionException("Invalid transaction - %s".formatted(transaction)));
  }

    private Boolean isTransactionValid(Transaction transaction, Wallet payer) {
        return payer.type() == WalletType.COMUM &&
            payer.balance().compareTo(transaction.value()) >= 0 &&
            !payer.id().equals(transaction.payee()) ? true : null;
    }
    
}
