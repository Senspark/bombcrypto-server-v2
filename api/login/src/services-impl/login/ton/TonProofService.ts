import {sha256} from "@ton/crypto";
import {Address, Cell, contractAddress, loadStateInit} from "@ton/ton";
import {Buffer} from "buffer";
import nacl from 'tweetnacl';
import {CheckProofRequestDto} from "../../../dto/CheckProofRequest.dto";
import {requestPublicKey, tryParsePublicKey} from "../../../utils/TonUtils";
import {IDependencies} from "../../../DependenciesInjector";
import {ILogger} from "../../../Services";
import JwtService from "../../JwtService";

const {randomBytes, sign} = nacl; // import like that for typescript build
const tonProofPrefix = 'ton-proof-item-v2/';
const tonConnectPrefix = 'ton-connect';
const validAuthTime = 15 * 60; // 15 minute

/**
 * https://github.com/ton-connect/demo-dapp-with-react-ui/blob/master/src/server/services/ton-proof-service.ts
 */
export class TonProofService {
    constructor(
        private readonly _dep: IDependencies,
        private readonly _jwtService: JwtService,
    ) {
        this.#allowedDomains = _dep.envConfig.allowedDomains;
        this.#logger = _dep.logger.clone('[TON_PROOF]');
    }

    readonly #allowedDomains: string[];
    readonly #logger: ILogger;

    /**
     * Generate a random payload.
     */
    public generatePayload(): string {
        return Buffer.from(randomBytes(32)).toString('hex');
    }

    public async generatePayloadToken(): Promise<string> {
        const payload = this.generatePayload();
        return await this._jwtService.createPayloadToken({payload: payload});
    }

    public async verifyPayloadToken(payloadToken: string): Promise<boolean> {
        const result = await this._jwtService.verifyToken(payloadToken)
        return result != null;
    }

    /**
     * Reference implementation of the checkProof method:
     * https://github.com/ton-blockchain/ton-connect/blob/main/requests-responses.md#address-proof-signature-ton_proof
     * https://docs.ton.org/develop/dapps/ton-connect/sign
     */
    public async checkProof(payload: CheckProofRequestDto, getWalletPublicKey: (address: string) => Promise<Buffer | null>): Promise<boolean> {
        try {
            const stateInit = loadStateInit(Cell.fromBase64(payload.proof.state_init).beginParse());

            // 1. First, try to obtain public key via get_public_key get-method on smart contract deployed at Address.
            // 2. If the smart contract is not deployed yet, or the get-method is missing, you need:
            //  2.1. Parse TonAddressItemReply.walletStateInit and get public key from stateInit. You can compare the walletStateInit.code
            //  with the code of standard wallets contracts and parse the data according to the found wallet version.
            let publicKey = tryParsePublicKey(stateInit)
                ?? await requestPublicKey(payload.proof.state_init)
                ?? await getWalletPublicKey(payload.address);
            if (!publicKey) {
                return false;
            }

            // 2.2. Check that TonAddressItemReply.publicKey equals to obtained public key
            const wantedPublicKey = Buffer.from(payload.public_key, 'hex');
            if (!publicKey.equals(wantedPublicKey)) {
                return false;
            }

            // 2.3. Check that TonAddressItemReply.walletStateInit.hash() equals to TonAddressItemReply.address. .hash() means BoC hash.
            const wantedAddress = Address.parse(payload.address);
            const address = contractAddress(wantedAddress.workChain, stateInit);
            if (!address.equals(wantedAddress)) {
                return false;
            }

            if (this.#allowedDomains.length > 0) {
                const included = this.#allowedDomains.findIndex(d => d.includes(payload.proof.domain.value)) > 0;
                if (!included) {
                    this.#logger.error(`Domain ${payload.proof.domain.value} is not allowed`);
                    return false;
                }
            }

            const now = Math.floor(Date.now() / 1000);
            if (now - validAuthTime > payload.proof.timestamp) {
                return false;
            }

            const message = {
                workchain: address.workChain,
                address: address.hash,
                domain: {
                    lengthBytes: payload.proof.domain.lengthBytes,
                    value: payload.proof.domain.value,
                },
                signature: Buffer.from(payload.proof.signature, 'base64'),
                payload: payload.proof.payload,
                stateInit: payload.proof.state_init,
                timestamp: payload.proof.timestamp
            };

            const wc = Buffer.alloc(4);
            wc.writeUInt32BE(message.workchain, 0);

            const ts = Buffer.alloc(8);
            ts.writeBigUInt64LE(BigInt(message.timestamp), 0);

            const dl = Buffer.alloc(4);
            dl.writeUInt32LE(message.domain.lengthBytes, 0);

            // message = utf8_encode("ton-proof-item-v2/") ++
            //           Address ++
            //           AppDomain ++
            //           Timestamp ++
            //           Payload
            const msg = Buffer.concat([
                Buffer.from(tonProofPrefix),
                wc,
                message.address,
                dl,
                Buffer.from(message.domain.value),
                ts,
                Buffer.from(message.payload),
            ]);

            const msgHash = Buffer.from(await sha256(msg));

            // signature = Ed25519Sign(privkey, sha256(0xffff ++ utf8_encode("ton-connect") ++ sha256(message)))
            const fullMsg = Buffer.concat([
                Buffer.from([0xff, 0xff]),
                Buffer.from(tonConnectPrefix),
                msgHash,
            ]);

            const result = Buffer.from(await sha256(fullMsg));

            return sign.detached.verify(result, message.signature, publicKey);
        } catch (e) {
            return false;
        }
    }
}
