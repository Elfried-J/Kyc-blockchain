package com.example.kycchain;

import com.example.kycchain.core.Blockchain;
import com.example.kycchain.core.KYCService;
import com.example.kycchain.model.Block;
import com.example.kycchain.model.tx.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KYC-on-Blockchain deep test suite.
 *
 * Scenarios
 *  1) End-to-end happy path for many customers, multi-institution shares, multi-tx blocks
 *  2) Impersonation attempt: BankB claims to be BankA (domain rule must fail)
 *  3) In-chain payload tamper: modify mined tx data -> chain must invalidate
 *  4) Signature swap across independent tx -> chain must invalidate
 *  5) Replay of a valid txId -> chain stays cryptographically valid, audit flags duplicate
 *  6) Difficulty and link assertions beyond chain.isValid() (hash prefix, link consistency)
 *  7) Randomized fuzzing: generate many customers/flows and inject random adverse edits
 *  8) Deterministic rebuild of chain contents (fresh mining) must still validate logically
 *  9) Re-mining after tamper: fixing PoW alone should not fix a broken signature
 * 10) Micro-benchmarks at multiple difficulties with latency stats (avg, p50, p90, p99)
 * 11) AI attestation end-to-end with hash binding and tamper detection
 */
public class Main {

    private static int passed = 0, failed = 0;

    private static void check(boolean cond, String msg) {
        if (cond) {
            passed++;
            System.out.println("  PASS: " + msg);
        } else {
            failed++;
            System.out.println("  FAIL: " + msg);
        }
    }

    private static void must(boolean cond, String msg) {
        if (!cond) {
            System.out.println("  FATAL: " + msg);
            throw new AssertionError(msg);
        }
        passed++;
        System.out.println("  PASS: " + msg);
    }

    private static record Env(Blockchain chain, KYCService svc) {}
    private static Env env(int difficulty) {
        Blockchain chain = new Blockchain(difficulty);
        KYCService svc = new KYCService(chain);
        return new Env(chain, svc);
    }

    public static void main(String[] args) {
        System.out.println("===== KYC Chain Comprehensive Test Suite =====");

        scenario1_happyPath_multiCustomers_multiTxBlocks();
        scenario2_impersonation_fails();
        scenario3_payloadTamper_detected();
        scenario4_signatureSwap_detected();
        scenario5_replay_detected_by_audit();
        scenario6_extra_difficulty_and_link_assertions();
        scenario7_randomized_fuzzing_with_adversarial_edits();
        scenario8_rebuild_chain_from_txs();
        scenario9_pow_remining_does_not_fix_signature_break();
        scenario10_latency_microbenchmarks();
        scenario11_ai_attestation_end_to_end();

        System.out.println("\n===== SUMMARY =====");
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        if (failed == 0) {
            System.out.println("All scenarios passed.");
        } else {
            System.out.println("Some checks failed; inspect logs above.");
        }
    }

    private static void scenario1_happyPath_multiCustomers_multiTxBlocks() {
        System.out.println("\n--- Scenario 1: Happy path, multi-customers, multi-tx blocks ---");
        Env e = env(3);
        KYCService svc = e.svc();
        Blockchain chain = e.chain();

        svc.register("BankA", "BANK");
        svc.register("InsurerX", "INSTITUTION");
        svc.register("BrokerY", "INSTITUTION");

        int N = 10;
        List<Transaction> pending = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            String cust = "Cust" + (i + 1);
            svc.register(cust, "CUSTOMER");

            String raw = cust + " KYC v1: passport=..., address=..., selfie=..., ts=" + System.currentTimeMillis();
            KYCUploadTx up = svc.createUploadTx(cust, "kyc://" + cust.toLowerCase() + "/v1", raw);
            pending.add(up);

            KYCVerifyTx ver = svc.createVerifyTx("BankA", cust, up.getTxId(), true, "All docs valid.");
            pending.add(ver);

            pending.add(svc.createShareTx("BankA", "InsurerX", cust, ver.getTxId()));
            pending.add(svc.createShareTx("BankA", "BrokerY",  cust, ver.getTxId()));

            if (pending.size() >= 8) {
                chain.addBlock(new ArrayList<>(pending));
                pending.clear();
            }
        }
        if (!pending.isEmpty()) chain.addBlock(pending);

        must(chain.isValid(), "Chain validates after multi-tx batching");

        String probe = "Cust" + ThreadLocalRandom.current().nextInt(1, N + 1);
        List<Transaction> audit = svc.auditForCustomer(probe);
        check(audit.size() == 4, "Audit for " + probe + " returns 4 transactions");
    }

    private static void scenario2_impersonation_fails() {
        System.out.println("\n--- Scenario 2: Impersonation fails (signer != bankId) ---");
        Env e = env(3);
        KYCService svc = e.svc();
        Blockchain chain = e.chain();

        svc.register("Alice", "CUSTOMER");
        svc.register("BankA", "BANK");
        svc.register("BankB", "BANK");

        KYCUploadTx up = svc.createUploadTx("Alice", "kyc://alice/1", "Alice raw v1");
        chain.addBlock(List.of(up));

        KYCVerifyTx forged = svc.createVerifyTx("BankB", "Alice", up.getTxId(), true, "pretend BankA");
        overwriteField(forged, "bankId", "BankA");
        chain.addBlock(List.of(forged));

        check(!chain.isValid(), "Validation fails because signer name does not match declared bankId");
    }

    private static void scenario3_payloadTamper_detected() {
        System.out.println("\n--- Scenario 3: In-chain payload tamper is detected ---");
        Env e = env(3);
        KYCService svc = e.svc();
        Blockchain chain = e.chain();

        svc.register("Alice", "CUSTOMER");
        svc.register("BankA", "BANK");

        KYCUploadTx up = svc.createUploadTx("Alice", "kyc://alice/1", "Alice v1");
        chain.addBlock(List.of(up));
        must(chain.isValid(), "Chain valid prior to tamper");

        overwriteField(up, "dataPointer", "kyc://alice/TAMPERED");
        check(!chain.isValid(), "Validation fails after payload tamper inside a mined block");
    }

    private static void scenario4_signatureSwap_detected() {
        System.out.println("\n--- Scenario 4: Signature swap attack is detected ---");
        Env e = env(3);
        KYCService svc = e.svc();
        Blockchain chain = e.chain();

        svc.register("Alice", "CUSTOMER");
        svc.register("Bob",   "CUSTOMER");
        svc.register("BankA", "BANK");

        KYCUploadTx upA = svc.createUploadTx("Alice", "kyc://alice/1", "Alice v1");
        KYCUploadTx upB = svc.createUploadTx("Bob",   "kyc://bob/1",   "Bob v1");
        chain.addBlock(List.of(upA, upB));
        must(chain.isValid(), "Chain valid before signature swap");

        String sigA = upA.getSignatureBase64();
        overwriteField(upA, "signatureB64", upB.getSignatureBase64());
        overwriteField(upB, "signatureB64", sigA);

        check(!chain.isValid(), "Validation fails after signature swap (signatures are bound to canonical data)");
    }

    private static void scenario5_replay_detected_by_audit() {
        System.out.println("\n--- Scenario 5: Replay detection by audit ---");
        Env e = env(2);
        KYCService svc = e.svc();
        Blockchain chain = e.chain();

        svc.register("Alice", "CUSTOMER");
        svc.register("BankA", "BANK");
        svc.register("InsurerX", "INSTITUTION");

        KYCUploadTx up = svc.createUploadTx("Alice", "kyc://alice/1", "Alice raw");
        chain.addBlock(List.of(up));
        KYCVerifyTx ver = svc.createVerifyTx("BankA", "Alice", up.getTxId(), true, "OK");
        chain.addBlock(List.of(ver));
        KYCShareTx share = svc.createShareTx("BankA", "InsurerX", "Alice", ver.getTxId());
        chain.addBlock(List.of(share));

        chain.addBlock(List.of(share));
        must(chain.isValid(), "Chain remains valid cryptographically with a replayed tx");

        Set<String> ids = new HashSet<>();
        boolean dup = false;
        for (Block b : chain.getBlocks()) {
            for (Transaction tx : b.transactions) {
                if (!ids.add(tx.getTxId())) dup = true;
            }
        }
        check(dup, "Audit detects duplicate txId (replay) even though PoW and signatures are intact");
    }

    private static void scenario6_extra_difficulty_and_link_assertions() {
        System.out.println("\n--- Scenario 6: Difficulty and link assertions ---");
        int difficulty = 3;
        Env e = env(difficulty);
        KYCService svc = e.svc();
        Blockchain chain = e.chain();

        svc.register("Alice", "CUSTOMER");
        svc.register("BankA", "BANK");

        for (int i = 0; i < 6; i++) {
            KYCUploadTx up = svc.createUploadTx("Alice", "kyc://alice/" + i, "Alice data " + i);
            chain.addBlock(List.of(up));
        }
        must(chain.isValid(), "Baseline validity");

        List<Block> blocks = chain.getBlocks();
        String prefix = "0".repeat(difficulty);
        boolean ok = true;
        for (int i = 1; i < blocks.size(); i++) {
            Block prev = blocks.get(i - 1);
            Block curr = blocks.get(i);
            if (!curr.prevHash.equals(prev.hash)) ok = false;
            if (!curr.hash.startsWith(prefix)) ok = false;
            if (!curr.hash.equals(curr.computeHash())) ok = false;
        }
        check(ok, "All non-genesis blocks satisfy link and hash-prefix constraints");
    }

    private static void scenario7_randomized_fuzzing_with_adversarial_edits() {
        System.out.println("\n--- Scenario 7: Randomized fuzzing with adversarial edits ---");
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        int trials = 30;
        int difficulty = 2;

        for (int t = 0; t < trials; t++) {
            Env e = env(difficulty);
            KYCService svc = e.svc();
            Blockchain chain = e.chain();

            svc.register("BankA", "BANK");
            svc.register("InsurerX", "INSTITUTION");
            int customers = rnd.nextInt(3, 8);

            List<KYCUploadTx> uploads = new ArrayList<>();
            for (int i = 0; i < customers; i++) {
                String name = "User" + t + "_" + i;
                svc.register(name, "CUSTOMER");
                KYCUploadTx up = svc.createUploadTx(name, "kyc://" + name + "/v1", "raw " + rnd.nextLong());
                uploads.add(up);
            }
            chain.addBlock(new ArrayList<>(uploads));

            List<KYCVerifyTx> verifications = new ArrayList<>();
            for (KYCUploadTx up : uploads) {
                verifications.add(svc.createVerifyTx("BankA", up.getCustomerId(), up.getTxId(), true, "OK"));
            }
            chain.addBlock(new ArrayList<>(verifications));

            List<Transaction> shares = new ArrayList<>();
            for (KYCVerifyTx v : verifications) {
                shares.add(svc.createShareTx("BankA", "InsurerX", v.getCustomerId(), v.getTxId()));
            }
            chain.addBlock(shares);

            must(chain.isValid(), "Trial " + t + ": valid before adversarial edit");

            int attack = rnd.nextInt(4);
            boolean expectInvalid = false;
            switch (attack) {
                case 0 -> {
                    KYCUploadTx target = uploads.get(rnd.nextInt(uploads.size()));
                    overwriteField(target, "dataPointer", "kyc://tampered/" + rnd.nextInt());
                    expectInvalid = true;
                }
                case 1 -> {
                    if (uploads.size() >= 2) {
                        KYCUploadTx a = uploads.get(0), b = uploads.get(1);
                        String sigA = a.getSignatureBase64();
                        overwriteField(a, "signatureB64", b.getSignatureBase64());
                        overwriteField(b, "signatureB64", sigA);
                        expectInvalid = true;
                    }
                }
                case 2 -> {
                    KYCVerifyTx v = verifications.get(0);
                    overwriteField(v, "bankId", "BankX");
                    expectInvalid = true;
                }
                case 3 -> {
                    expectInvalid = false;
                }
            }
            boolean valid = chain.isValid();
            check(expectInvalid ? !valid : valid,
                    "Trial " + t + ": post-edit validity matches expectation (attack=" + attack + ")");
        }
    }

    private static void scenario8_rebuild_chain_from_txs() {
        System.out.println("\n--- Scenario 8: Rebuild chain from existing txs ---");
        Env e = env(2);
        KYCService svc = e.svc();
        Blockchain chain = e.chain();

        svc.register("BankA", "BANK");
        svc.register("InsurerX", "INSTITUTION");
        svc.register("Alice", "CUSTOMER");

        KYCUploadTx up = svc.createUploadTx("Alice", "kyc://alice/1", "Alice raw");
        chain.addBlock(List.of(up));
        KYCVerifyTx ver = svc.createVerifyTx("BankA", "Alice", up.getTxId(), true, "OK");
        chain.addBlock(List.of(ver));
        KYCShareTx   sh  = svc.createShareTx("BankA", "InsurerX", "Alice", ver.getTxId());
        chain.addBlock(List.of(sh));
        must(chain.isValid(), "Original chain valid");

        Env e2 = env(2);
        Blockchain chain2 = e2.chain();
        chain2.addBlock(List.of(up));
        chain2.addBlock(List.of(ver));
        chain2.addBlock(List.of(sh));
        must(chain2.isValid(), "Rebuilt chain also validates with same tx objects");
    }

    private static void scenario9_pow_remining_does_not_fix_signature_break() {
        System.out.println("\n--- Scenario 9: Re-mining cannot fix broken signatures ---");
        int difficulty = 2;
        Env e = env(difficulty);
        KYCService svc = e.svc();
        Blockchain chain = e.chain();

        svc.register("BankA", "BANK");
        svc.register("Alice", "CUSTOMER");

        KYCUploadTx up = svc.createUploadTx("Alice", "kyc://alice/1", "Alice v1");
        chain.addBlock(List.of(up));
        KYCVerifyTx ver = svc.createVerifyTx("BankA", "Alice", up.getTxId(), true, "OK");
        chain.addBlock(List.of(ver));
        must(chain.isValid(), "Baseline valid prior to tamper");

        overwriteField(ver, "signatureB64", ver.getSignatureBase64() + "x");
        Block last = chain.getBlocks().get(chain.getBlocks().size() - 1);
        last.mine(difficulty);

        check(!chain.isValid(), "Even after re-mining, invalid signature keeps chain invalid");
    }

    private static void scenario10_latency_microbenchmarks() {
        System.out.println("\n--- Scenario 10: Latency micro-benchmarks ---");
        benchDifficulty(2, 30);
        benchDifficulty(3, 20);
    }

    private static void benchDifficulty(int difficulty, int N) {
        Env e = env(difficulty);
        KYCService svc = e.svc();
        Blockchain chain = e.chain();
        svc.register("Alice", "CUSTOMER");

        long[] ms = new long[N];
        for (int i = 0; i < N; i++) {
            String raw = "Alice v" + i + " " + ThreadLocalRandom.current().nextLong();
            KYCUploadTx up = svc.createUploadTx("Alice", "kyc://alice/" + i, raw);
            long t0 = System.nanoTime();
            chain.addBlock(List.of(up));
            long t1 = System.nanoTime();
            ms[i] = (t1 - t0) / 1_000_000;
        }
        must(chain.isValid(), "Benchmark chain valid at difficulty " + difficulty);

        Arrays.sort(ms);
        long avg = Arrays.stream(ms).sum() / N;
        long p50 = ms[N / 2];
        long p90 = ms[(int) Math.floor(0.9 * (N - 1))];
        long p99 = ms[Math.max(0, (int) Math.floor(0.99 * (N - 1)))];
        System.out.printf("  Difficulty %d: avg=%dms p50=%dms p90=%dms p99=%dms (N=%d)%n",
                difficulty, avg, p50, p90, p99, N);
    }

    private static void scenario11_ai_attestation_end_to_end() {
        System.out.println("\n--- Scenario 11: AI attestation end-to-end ---");
        Env e = env(2);
        KYCService svc = e.svc();
        Blockchain chain = e.chain();

        svc.register("Alice", "CUSTOMER");
        svc.register("BankA", "BANK");
        svc.register("InsurerX", "INSTITUTION");

        com.example.kycchain.ai.ModelRegistry registry = new com.example.kycchain.ai.ModelRegistry();
        com.example.kycchain.ai.SimpleKycAIService ai = new com.example.kycchain.ai.SimpleKycAIService("kyc-doc-face", "3.4.2");
        svc.attachAI(ai, registry);

        KYCUploadTx up = svc.createUploadTx("Alice", "kyc://alice/ai/v1", "passport=..., selfie=..., address=...");
        chain.addBlock(List.of(up));

        KYCVerifyTx verAI = svc.createVerifyTxWithAI("BankA", up, true, "AI-assisted OK");
        chain.addBlock(List.of(verAI));

        KYCShareTx sh = svc.createShareTx("BankA", "InsurerX", "Alice", verAI.getTxId());
        chain.addBlock(List.of(sh));

        must(chain.isValid(), "Chain valid with AI-assisted verification");
        check(verAI.getModelId().equals("kyc-doc-face") && verAI.getModelVersion().equals("3.4.2"),
                "Verify tx carries model id/version");
        String attHash = verAI.getAttestationHash();
        must(attHash != null && !attHash.isEmpty(), "Verify tx binds to attestation hash");

        var att = svc.getAttestationByHash(attHash);
        must(att != null, "Off-chain attestation retrievable by hash");
        check(registry.verify(att), "Registry accepts attestation signature and model binding");

        String forgedCanonical = att.canonical() + "#tamper";
        String forgedHash = com.example.kycchain.util.CryptoUtils.sha256Hex(forgedCanonical);
        check(!forgedHash.equals(attHash), "Different attestation content produces a different hash");

// Change a signed field we can reliably edit at runtime
        overwriteField(verAI, "bankId", "BankZ");
        check(!chain.isValid(), "Changing a signed field in the verify tx invalidates the chain");
    }

    private static void overwriteField(Object target, String fieldName, Object newVal) {
        try {
            Field f;
            Class<?> c = target.getClass();
            try {
                f = c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                f = c.getSuperclass().getDeclaredField(fieldName);
            }
            f.setAccessible(true);
            f.set(target, newVal);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed for field: " + fieldName, e);
        }
    }
}
