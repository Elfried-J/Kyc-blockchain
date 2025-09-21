package com.example.kycchain.core;
import com.example.kycchain.model.Block;
import com.example.kycchain.model.tx.KYCShareTx;
import com.example.kycchain.model.tx.KYCUploadTx;
import com.example.kycchain.model.tx.KYCVerifyTx;
import com.example.kycchain.model.tx.Transaction;

import java.util.ArrayList;
import java.util.List;

public class Blockchain {
    private final List<Block> chain = new ArrayList<>();
    private final int difficulty;

    public Blockchain(int difficulty) {
        this.difficulty = difficulty;
        chain.add(createGenesis());
    }

    private Block createGenesis() {
        Block genesis = new Block(0, "0", List.of());
        genesis.hash = genesis.computeHash(); // no mine
        return genesis;
    }

    public Block lastBlock() { return chain.get(chain.size() - 1); }

    public Block addBlock(List<Transaction> txs) {
        Block b = new Block(chain.size(), lastBlock().hash, txs);
        b.mine(difficulty);
        chain.add(b);
        return b;
    }

    public List<Block> getBlocks() { return chain; }

    public boolean isValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block prev = chain.get(i - 1);
            Block curr = chain.get(i);

            // Check link
            if (!curr.prevHash.equals(prev.hash)) return false;

            // Check PoW & hash integrity
            String recomputed = curr.computeHash();
            if (!recomputed.equals(curr.hash)) return false;
            if (!curr.hash.startsWith("0".repeat(difficulty))) return false;

            // Verify each transaction signature and minimal domain rules
            for (Transaction tx : curr.transactions) {
                if (!tx.verifySignature()) return false;

                if (tx instanceof KYCUploadTx u) {
                    if (!tx.getSignerName().equals(u.getCustomerId())) return false;
                } else if (tx instanceof KYCVerifyTx v) {
                    if (!tx.getSignerName().equals(v.getBankId())) return false;
                } else if (tx instanceof KYCShareTx s) {
                    if (!tx.getSignerName().equals(s.getFromEntity())) return false;
                }
            }
        }
        return true;
    }
}
