import {EventEmitter} from "events";
import {Chain, TokenSymbol} from "../consts/Consts";

// Emitted by the Signer the instant it hands out a withdraw authorization. In-process only (same service,
// no Redis hop). The Indexer subscribes to open a lifecycle record; if nobody listens it is a cheap no-op,
// so the signing path never depends on the indexer being up.
export interface ISignedEvent {
    wallet: string;
    token: string;          // withdraw-chain token address
    symbol: TokenSymbol;
    chain: Chain;           // withdraw chain
    otherDeposited: bigint; // value authorized
    deadline: number;       // unix seconds
}

const SIGNED = "signed";

export default class SignerEvents {
    readonly #emitter = new EventEmitter();

    emitSigned(e: ISignedEvent): void {
        this.#emitter.emit(SIGNED, e);
    }

    onSigned(handler: (e: ISignedEvent) => void): void {
        this.#emitter.on(SIGNED, handler);
    }
}
