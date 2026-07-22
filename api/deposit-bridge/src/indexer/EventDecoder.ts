import {Interface} from "ethers";
import {BRIDGE_INDEXER_ABI, BRIDGE_LEGACY_EVENT_ABI} from "../consts/Consts";
import {ICenterLog} from "../services/IBlockchainCenter";

export interface IDepositEvent {
    kind: "Deposit";
    user: string;
    token: string;
    amount: bigint;         // amount moved by THIS deposit
    depositedTotal: bigint; // cumulative deposited[user][token] AFTER this deposit
    block: number;
    txHash: string;
    logIndex: number;
}

export interface IWithdrawEvent {
    kind: "Withdraw";
    user: string;
    token: string;
    amount: bigint;         // gross withdrawn by THIS withdraw (legacy `gross` == current `amount`)
    net: bigint;            // payout after fee
    withdrawnTotal: bigint; // cumulative withdrawn[user][token] AFTER this withdraw
    block: number;
    txHash: string;
    logIndex: number;
}

export type BridgeEvent = IDepositEvent | IWithdrawEvent;

// Decodes raw center logs into typed Deposit/Withdraw events. The cumulative *Total fields are the authoritative
// on-chain counters (replaying logs in any order still converges via the repo's GREATEST). The per-event
// amount/net + tx coordinates feed the itemized ledger (§18). Two Withdraw signatures are matched by topic0: the
// current Phase 9 event and the legacy Phase 8 one (emitted by both old `withdraw` and the removed relayer
// `withdrawTo`) — so historical timelines are complete.
export default class EventDecoder {
    readonly #iface = new Interface(BRIDGE_INDEXER_ABI);
    readonly #legacyIface = new Interface(BRIDGE_LEGACY_EVENT_ABI);
    readonly depositTopic: string;
    readonly withdrawTopic: string;
    readonly withdrawLegacyTopic: string;

    constructor() {
        this.depositTopic = this.#iface.getEvent("Deposit")!.topicHash;
        this.withdrawTopic = this.#iface.getEvent("Withdraw")!.topicHash;
        this.withdrawLegacyTopic = this.#legacyIface.getEvent("Withdraw")!.topicHash;
    }

    // Topic filter for getLogs. Includes the legacy Withdraw topic too, so a getLogs-based backfill over old
    // blocks would still catch relayer-era withdraws — the live tail only ever produces the current shapes.
    eventTopics(): (string | string[])[] {
        return [[this.depositTopic, this.withdrawTopic, this.withdrawLegacyTopic]];
    }

    decode(log: ICenterLog): BridgeEvent | null {
        const topic0 = log.topics[0];
        const coords = {block: log.blockNumber, txHash: log.transactionHash, logIndex: log.logIndex};

        if (topic0 === this.depositTopic) {
            const parsed = this.#iface.parseLog({topics: log.topics, data: log.data});
            if (!parsed) return null;
            return {
                kind: "Deposit",
                user: parsed.args.user as string,
                token: parsed.args.token as string,
                amount: parsed.args.amount as bigint,
                depositedTotal: parsed.args.depositedTotal as bigint,
                ...coords,
            };
        }
        if (topic0 === this.withdrawTopic) {
            const parsed = this.#iface.parseLog({topics: log.topics, data: log.data});
            if (!parsed) return null;
            return {
                kind: "Withdraw",
                user: parsed.args.user as string,
                token: parsed.args.token as string,
                amount: parsed.args.amount as bigint,
                net: parsed.args.net as bigint,
                withdrawnTotal: parsed.args.withdrawnTotal as bigint,
                ...coords,
            };
        }
        if (topic0 === this.withdrawLegacyTopic) {
            const parsed = this.#legacyIface.parseLog({topics: log.topics, data: log.data});
            if (!parsed) return null;
            return {
                kind: "Withdraw",
                user: parsed.args.user as string,
                token: parsed.args.token as string,
                amount: parsed.args.gross as bigint, // legacy `gross` is the same quantity as current `amount`
                net: parsed.args.net as bigint,
                withdrawnTotal: parsed.args.withdrawnTotal as bigint,
                ...coords,
            };
        }
        return null;
    }
}
