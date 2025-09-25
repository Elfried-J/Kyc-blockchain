YC Blockchain Demo

A blockchain prototype for Know Your Customer (KYC) processes, designed to show how institutions can share and verify identity data securely without ever exposing personal documents on-chain. This project demonstrates cryptographic integrity, proof-of-work mining, domain rules for participants, and even an optional AI attestation layer for realistic pre-screening of KYC files.

Description

This project explores how blockchain can be applied to KYC in a lightweight, transparent, and privacy-preserving way. In many banking and financial contexts, customers are asked to repeatedly provide their KYC documents—passport, proof of address, or biometric data—to different institutions. This leads to duplication, long verification cycles, and potential privacy risks.

The KYC Blockchain Demo solves this problem by recording only hashes and pointers to off-chain data, instead of storing raw sensitive information on-chain. Every action—upload, verification, and sharing—is represented as a signed transaction, and each transaction is grouped into a mined block. The blocks are linked together using cryptographic hashes, ensuring immutability and tamper-evidence.

The workflow is simple yet powerful:

Customer Upload: The customer commits a hash of their KYC package along with a pointer to the raw data, signed with their private key.

Bank Verification: A bank reviews the submission, may consult an AI pre-screening service, and signs a verification transaction. The AI’s decision is represented off-chain as a signed attestation, while the blockchain stores its hash and the model version.

Sharing: The verified KYC stamp is shared securely with another institution, removing the need for repetitive document collection.

Every step is auditable, and any tampering—such as payload changes, impersonation attempts, or swapped signatures—invalidates the chain. Eleven test scenarios in the project demonstrate resilience against real-world attack vectors such as replay, tampering, and re-mining. The result is a compact but thorough model for blockchain-based KYC integrity.

Getting Started
Dependencies

Before running the program, ensure you have the following prerequisites installed on your machine:

Java 17 or later: The project uses modern Java language features and requires at least version 17.

Maven 3.8 or later: Used for building and packaging the project.

Operating System: Works on Windows, macOS, or Linux.

No external libraries: All cryptographic primitives (SHA-256, ECDSA) and core logic are implemented using standard Java libraries.

Optional: A terminal or IDE such as IntelliJ IDEA for running and exploring the project.

How to run the program

Clone the repository
Open a terminal and clone the repository into your local environment:

git clone https://github.com/your-username/kyc-blockchain.git
cd kyc-blockchain


Build the project with Maven
Use Maven to clean and package the project:

mvn clean package


Run the JAR file
After building, run the packaged application from the target directory:

java -jar target/kyc-blockchain-1.0-SNAPSHOT.jar


Observe the test scenarios
The program will execute eleven scenarios end-to-end. These include:

Happy path with multiple customers and multi-transaction blocks

Impersonation attempts (banks pretending to be others)

Payload tampering

Signature swaps across transactions

Replay detection

Randomized fuzzing with adversarial edits

Chain rebuilds and re-mining after tamper

Performance benchmarks with proof-of-work difficulty

AI-assisted verification with attestation binding

Each scenario prints clear PASS or FAIL results to the console and ends with a summary. A successful run looks like:

===== SUMMARY =====
Passed: 84   Failed: 0
All scenarios passed.

Help

If you encounter issues running the project, consider the following troubleshooting tips:

Java not recognized: Make sure Java 17+ is installed and added to your system PATH. Check by running java -version.

Maven not recognized: Ensure Maven is installed and available in your PATH. Check with mvn -v.

Build errors: Run mvn clean before rebuilding to clear old artifacts.

Scenarios fail: If any scenario reports a failure, double-check that you have not modified transaction classes or cryptographic utilities, since even a one-character change can invalidate signatures.

IDE setup: If using IntelliJ IDEA, import the project as a Maven project and mark src/main/java as a source root.

For further exploration:

Experiment with the difficulty parameter in Blockchain.java to see how mining latency changes.

Add your own participants or modify the AI module (ai/) to simulate different verification strategies.

Extend the audit logic to persist results to disk or integrate with a database.

Closing Notes

The KYC Blockchain Demo is not just an academic exercise. It captures the challenges real institutions face when handling repeated KYC checks and shows how blockchain principles can be applied responsibly—protecting privacy, ensuring provenance, and enabling trust across multiple organizations.

By running and reading through the code, you’ll see how cryptographic signatures, canonical transaction formats, and proof-of-work can be combined into a working system. The addition of the AI attestation demonstrates how modern ML services can integrate with blockchain in a verifiable but privacy-preserving way.

This project is a foundation for exploring blockchain-based compliance, integrity systems, and secure data exchange. It’s lean, auditable, and extensible—built to spark discussion, inspire contributions, and show how integrity and privacy can coexist.
