package com.pedromonteiro.picpaydesafiobackend.transaction;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.pedromonteiro.picpaydesafiobackend.wallet.Wallet;
import com.pedromonteiro.picpaydesafiobackend.wallet.WalletRepository;
import com.pedromonteiro.picpaydesafiobackend.wallet.WalletType;

@Service
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionService(TransactionRepository transactionRepository, WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    public Transaction create(Transaction transaction) {
        // validar
        

        //criar transacao
        var newTransaction = transactionRepository.save(transaction);

        //debitar da carteira
        var wallet = walletRepository.findById(transaction.payer()).get();
        walletRepository.save(wallet.debit(transaction.value()));

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
            .orElseThrow())
        .orElseThrow();
  }

    private Boolean isTransactionValid(Transaction transaction, Wallet payer) {
        return payer.type() == WalletType.COMUM &&
            payer.balance().compareTo(transaction.value()) >= 0 &&
            !payer.id().equals(transaction.payee()) ? true : null;
    }
    
}
