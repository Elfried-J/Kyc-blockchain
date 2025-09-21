package com.example.kycchain.core;

import com.example.kycchain.ai.AIModelService;
import com.example.kycchain.ai.Attestation;
import com.example.kycchain.ai.ModelRegistry;
import com.example.kycchain.model.Participant;
import com.example.kycchain.model.tx.*;
import com.example.kycchain.util.CryptoUtils;

import java.util.*;

public class KYCService {
    private final Map<String, Participant> participants = new HashMap<>();
    private final Blockchain blockchain;
    private AIModelService ai;
    private ModelRegistry registry;
    private final Map<String, Attestation> attestationsByHash = new HashMap<>();

    public KYCService(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public void attachAI(AIModelService aiService, ModelRegistry modelRegistry) {
        this.ai = aiService;
        this.registry = modelRegistry;
        if (registry != null && aiService != null) {
            registry.register(aiService.getModelId(), aiService.getModelVersion(), aiService.getServicePubKeyB64());
        }
    }

    public Participant register(String name, String role) {
        Participant p = new Participant(name, role);
        participants.put(name, p);
        return p;
    }

    public Participant get(String name) {
        Participant p = participants.get(name);
        if (p == null) throw new IllegalArgumentException("Unknown participant: " + name);
        return p;
    }

    public KYCUploadTx createUploadTx(String customerName, String dataPointer, String rawKycData) {
        String dataHash = CryptoUtils.sha256Hex(rawKycData);
        KYCUploadTx tx = new KYCUploadTx(customerName, dataHash, dataPointer);
        Participant c = get(customerName);
        String pub = c.publicKeyB64();
        String sig = c.sign(tx.canonicalDataFor(pub));
        tx.signWith(c.getName(), pub, sig);
        return tx;
    }

    public KYCVerifyTx createVerifyTx(String bankName, String customerName, String uploadTxId, boolean decision, String notes) {
        KYCVerifyTx tx = new KYCVerifyTx(bankName, customerName, uploadTxId, decision, notes);
        Participant bank = get(bankName);
        String pub = bank.publicKeyB64();
        String sig = bank.sign(tx.canonicalDataFor(pub));
        tx.signWith(bank.getName(), pub, sig);
        return tx;
    }

    public KYCVerifyTx createVerifyTxWithAI(String bankName, KYCUploadTx uploadTx, boolean decision, String notes) {
        if (ai == null || registry == null) throw new IllegalStateException("AI service or registry not attached");
        Attestation att = ai.attest(uploadTx);
        String attHash = att.hashHex();
        if (!att.verifySignature()) throw new IllegalStateException("AI attestation has invalid signature");
        if (!registry.verify(att)) throw new IllegalStateException("AI attestation key not registered");
        attestationsByHash.put(attHash, att);
        KYCVerifyTx tx = new KYCVerifyTx(bankName, uploadTx.getCustomerId(), uploadTx.getTxId(), decision, notes,
                att.modelId, att.modelVersion, attHash);
        Participant bank = get(bankName);
        String pub = bank.publicKeyB64();
        String sig = bank.sign(tx.canonicalDataFor(pub));
        tx.signWith(bank.getName(), pub, sig);
        return tx;
    }

    public KYCShareTx createShareTx(String fromEntity, String toEntity, String customerName, String verifiedTxId) {
        KYCShareTx tx = new KYCShareTx(fromEntity, toEntity, customerName, verifiedTxId);
        Participant from = get(fromEntity);
        String pub = from.publicKeyB64();
        String sig = from.sign(tx.canonicalDataFor(pub));
        tx.signWith(from.getName(), pub, sig);
        return tx;
    }

    public Blockchain chain() { return blockchain; }

    public List<Transaction> auditForCustomer(String customerId) {
        List<Transaction> out = new ArrayList<>();
        blockchain.getBlocks().forEach(b -> b.transactions.forEach(tx -> {
            if (tx instanceof KYCUploadTx u && u.getCustomerId().equals(customerId)) out.add(tx);
            if (tx instanceof KYCVerifyTx v && v.getCustomerId().equals(customerId)) out.add(tx);
            if (tx instanceof KYCShareTx s && s.getCustomerId().equals(customerId)) out.add(tx);
        }));
        return out;
    }

    public Attestation getAttestationByHash(String hashHex) {
        return attestationsByHash.get(hashHex);
    }

    public ModelRegistry getRegistry() { return registry; }
}
