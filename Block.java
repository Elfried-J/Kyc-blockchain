package com.example.kycchain.model;

import com.example.kycchain.model.tx.Transaction;
import com.example.kycchain.util.CryptoUtils;
import java.util.List;
import java.util.stream.Collectors;

public class Block {
    public final int index;
    public final long timestamp;
    public final String prevHash;
    public final List<Transaction> transactions;
    public int nonce;
    public String hash;

    public Block(int index, String prevHash, List<Transaction> transactions) {
        this.index = index;
        this.prevHash = prevHash;
        this.transactions = transactions;
        this.timestamp = System.currentTimeMillis();
        this.nonce = 0;
        this.hash = computeHash();
    }

    public String txIdsConcat() {
        return transactions.stream().map(Transaction::getTxId).collect(Collectors.joining(","));
    }

    public String computeHash() {
        String header = index + "|" + timestamp + "|" + prevHash + "|" + nonce + "|" + txIdsConcat();
        return CryptoUtils.sha256Hex(header);
    }

    public void mine(int difficulty) {
        String target = "0".repeat(Math.max(0, difficulty));
        while (!hash.startsWith(target)) {
            nonce++;
            hash = computeHash();
        }
    }

    @Override public String toString() {
        return "Block#" + index + " hash=" + hash + " prev=" + prevHash + " txs=" + transactions.size();
    }
}
