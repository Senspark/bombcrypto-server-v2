import {RequestKind} from "./Consts";

// SV_DEPBRIDGE_REQUEST_STR — SmartFox → signer. Withdraw-sign only (withdraw = MAX, no amount input).
export interface IBridgeRequest {
    correlationId: string;
    kind: RequestKind;      // "withdraw"
    uid: number;
    wallet: string;         // user EOA
    rewardType: string;     // BCOIN_BRIDGE | SEN_BRIDGE
    withdrawChain: string;  // BSC | POLYGON — the chain the user will relay the withdraw ON
}

// AP_DEPBRIDGE_RESULT_STR — signer → SmartFox (matched by correlationId).
export interface IBridgeResult {
    correlationId: string;
    code: number;            // 0 ok, 100 error
    errorMessage?: string;   // client-safe text only (never raw exception/RPC strings)
    signature?: string;      // EIP-191 over (user, tokenW, otherDeposited, deadline, bridgeW)
    otherDeposited?: string; // source-chain deposited counter, wei as numeric string
    deadline?: number;       // unix seconds — operational window (§4.4)
    bridgeAddress?: string;  // withdraw-chain bridge (address(this) in the packed message)
    tokenAddress?: string;   // withdraw-chain token
    chain?: string;          // echo of the withdraw chain
}
