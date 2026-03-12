import {ILogger} from "../../Services";
import {RedisConfig} from "../../consts/Consts";
import {base64ToByteArray, byteArrayToBase64, mergeByteArray, uint32ToByteArray} from "../../utils/String";
import AesEncryption from "../encrypt/AesEncryption";
import IAutoExpireMap from "../utils/IAutoExpireMap";
import {ethers} from "ethers";
import {randNumber} from "../../utils/Random";

type WalletAddress = string;
type Nonce = [rand: number, iv: string];

export default class BscWalletService {
    constructor(
        logger: ILogger,
        signSecret: string,
        signPadding: string,
        displayText: string,
        nonces: IAutoExpireMap<WalletAddress, Nonce>,
    ) {
        this._logger = logger.clone('[WALLET]');
        this._encryptor = new AesEncryption();
        this._encryptor.importKey(signSecret);
        this._nonces = nonces;
        this._signPadding = signPadding;
        this._displayText = displayText;

    }

    private readonly _logger: ILogger;
    private readonly _nonces: IAutoExpireMap<WalletAddress, Nonce>;
    private readonly _encryptor: AesEncryption;
    private readonly _signPadding: string;
    private readonly _displayText: string;

    public async generateNonceData(walletAddress: WalletAddress): Promise<string> {
        const n = randNumber();
        const iv = this._encryptor.generateIV();
        await this._nonces.add(walletAddress, [n, iv], RedisConfig.K_NONCE_EXPIRED);

        // merge bytes n + iv;
        return byteArrayToBase64(mergeByteArray(uint32ToByteArray(n), base64ToByteArray(iv)));
    }

    public async checkProof(walletAddress: WalletAddress, signatureBase64: string): Promise<boolean> {
        // Check if wallet address is valid
        if (!ethers.isAddress(walletAddress)) {
            throw new Error(`${walletAddress} Invalid address`);
        }

        const nonceCompact = await this._nonces.get(walletAddress);

        if (!nonceCompact) {
            throw new Error(`${walletAddress} Nonce not found`);
        }

        const originalMessage = this.generateStringToSign(nonceCompact);
        // Get the actual address that signed this message
        try {
            const recoveredAddress = ethers.verifyMessage(originalMessage, signatureBase64);

            // Compare it to the address that was provided
            const isValid = recoveredAddress.toLowerCase() === walletAddress.toLowerCase();

            if (!isValid) {
                this._logger.error(`Signature validation failed: expected ${walletAddress}, got ${recoveredAddress}`);
            }

            // Remove the nonce regardless of whether verification succeeded
            await this._nonces.remove(walletAddress);

            return isValid;
        } catch (e) {
            // Remove the nonce to prevent replay attacks
            await this._nonces.remove(walletAddress);

            this._logger.error(`Signature verification error: ${(e as Error).message}`);
            return false;
        }
    }

    private generateStringToSign(nonce: Nonce): string {
        const message = `${this._signPadding}${nonce[0]}`;
        const encryptedNonce = this._encryptor.encrypt(message, nonce[1]);
        return `${this._displayText} ${encryptedNonce}`;
    }
}
