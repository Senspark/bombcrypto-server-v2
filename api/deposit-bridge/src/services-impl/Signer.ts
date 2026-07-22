import {Wallet, solidityPackedKeccak256, getBytes} from "ethers";
import ILogger from "../services/ILogger";
import IEnvConfig from "../services/IEnvConfig";
import ISigner from "../services/ISigner";
import DepositReader from "./DepositReader";
import {
    Chain, ClientErrors, CODE_ERROR, CODE_OK, INetworkConfig, oppositeChain, REWARD_TYPE_TO_TOKEN,
} from "../consts/Consts";
import {IBridgeRequest, IBridgeResult} from "../consts/Messages";
import {ValidationError} from "../consts/ServerError";
import SignerEvents from "../indexer/SignerEvents";

// Signing path (stateless). Resolves the three distinct addresses this cross-chain sign needs, reads the
// source-chain deposited counter through the isolated DepositReader, then signs EIP-191 over
//   (user, tokenWithdrawChain, otherDeposited, deadline, bridgeWithdrawChain)
// exactly matching DepositBridge._hashWithdraw. Fail-closed: any read/validation error → no signature.
export default class Signer implements ISigner {
    readonly #logger: ILogger;
    readonly #env: IEnvConfig;
    readonly #reader: DepositReader;
    readonly #configs: Map<Chain, INetworkConfig>;
    readonly #wallet: Wallet;
    readonly #events?: SignerEvents;
    readonly signerAddress: string;

    constructor(
        logger: ILogger, env: IEnvConfig, reader: DepositReader, configs: Map<Chain, INetworkConfig>,
        events?: SignerEvents,
    ) {
        this.#logger = logger.clone('[Signer]');
        this.#env = env;
        this.#reader = reader;
        this.#configs = configs;
        this.#events = events;
        if (!env.signerPrivateKey) throw new Error("SIGNER_PRIVATE_KEY is required");
        const pk = env.signerPrivateKey.startsWith('0x') ? env.signerPrivateKey : `0x${env.signerPrivateKey}`;
        this.#wallet = new Wallet(pk);
        this.signerAddress = this.#wallet.address;
    }

    async signWithdraw(req: IBridgeRequest): Promise<IBridgeResult> {
        try {
            const withdrawChain = this.#validate(req);
            const srcChain = oppositeChain(withdrawChain);      // deposit lives on the opposite chain
            const symbol = REWARD_TYPE_TO_TOKEN[req.rewardType]; // validated in #validate
            const tokenW = this.#config(withdrawChain).tokens[symbol]; // goes into the packed message
            const tokenS = this.#config(srcChain).tokens[symbol];      // to read deposited on the source
            const bridgeW = this.#config(withdrawChain).bridgeAddress; // = address(this) in the packed message

            const otherDeposited = await this.#reader.readOtherDeposited(srcChain, req.wallet, tokenS);
            if (otherDeposited <= 0n) {
                // Never deposited on the source chain → nothing could be withdrawn here. Reject with a friendly
                // message rather than hand out a signature the contract would only reject (saves the user gas).
                return this.#reject(req, ClientErrors.NOTHING_TO_WITHDRAW);
            }

            const deadline = Math.floor(Date.now() / 1000) + this.#env.signWindowSeconds;
            const raw = solidityPackedKeccak256(
                ["address", "address", "uint256", "uint256", "address"],
                [req.wallet, tokenW, otherDeposited, deadline, bridgeW],
            );
            const signature = await this.#wallet.signMessage(getBytes(raw));

            // In-process hand-off to the indexer to open a lifecycle record. Wrapped so a listener fault
            // can never affect the signing reply.
            try {
                this.#events?.emitSigned({
                    wallet: req.wallet, token: tokenW, symbol, chain: withdrawChain, otherDeposited, deadline,
                });
            } catch (e) {
                this.#logger.error(e);
            }

            return {
                correlationId: req.correlationId,
                code: CODE_OK,
                signature,
                otherDeposited: otherDeposited.toString(),
                deadline,
                bridgeAddress: bridgeW,
                tokenAddress: tokenW,
                chain: withdrawChain,
            };
        } catch (e) {
            // Full detail stays in the server log only — NEVER return raw exception/RPC text to the client.
            this.#logger.error(e);
            return this.#reject(req, ClientErrors.WITHDRAW_UNAVAILABLE);
        }
    }

    // Validate request shape and return the resolved withdraw chain.
    #validate(req: IBridgeRequest): Chain {
        if (!req?.uid || !req.wallet || !req.rewardType || !req.withdrawChain) {
            throw new ValidationError(ClientErrors.INVALID_REQUEST);
        }
        if (!REWARD_TYPE_TO_TOKEN[req.rewardType]) {
            throw new ValidationError(ClientErrors.INVALID_REQUEST);
        }
        if (req.withdrawChain !== Chain.BSC && req.withdrawChain !== Chain.POLYGON) {
            throw new ValidationError(ClientErrors.INVALID_REQUEST);
        }
        return req.withdrawChain as Chain;
    }

    #config(chain: Chain): INetworkConfig {
        const cfg = this.#configs.get(chain);
        if (!cfg) throw new Error(`no network config for ${chain}`);
        return cfg;
    }

    #reject(req: IBridgeRequest, errorMessage: string): IBridgeResult {
        return {correlationId: req?.correlationId, code: CODE_ERROR, errorMessage};
    }
}
