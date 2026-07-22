import { defineConfig } from "vitest/config";

// Adversarial suite: an external attacker (roleless wallets in .env.test) probing the LIVE testnet
// contracts and the running ap-deposit-bridge signer over Redis. Nothing here touches backend source.
export default defineConfig({
    test: {
        globals: true,
        environment: "node",
        include: ["test/**/*.test.ts"],
        setupFiles: ["./test/setup.ts"],
        // Testnet round-trips (deposit + N-confirmation waits + relays) are slow.
        testTimeout: 240_000,
        hookTimeout: 240_000,
        // Shared, monotonic on-chain state + one nonce per wallet — files must not race each other.
        fileParallelism: false,
        pool: "forks",
        reporters: "verbose",
    },
});
