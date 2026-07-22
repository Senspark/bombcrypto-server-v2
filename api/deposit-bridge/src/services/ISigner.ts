import {IBridgeRequest, IBridgeResult} from "../consts/Messages";

// Stateless withdraw signer. Reads the source-chain deposited counter (confirmed depth, bounded, cached),
// signs an EIP-191 authorization, and returns it. No tx submission, no DB, no held funds.
export default interface ISigner {
    readonly signerAddress: string;

    signWithdraw(req: IBridgeRequest): Promise<IBridgeResult>;
}
