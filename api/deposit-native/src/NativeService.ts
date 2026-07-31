import {BigNumber, ethers} from 'ethers';
import {BlockchainCenterApi} from './BlockchainCenterApi';
import {NativeNetworkConfig} from './NativeConfig';
import ILogger from './services/ILogger';

const COUNTERS_ABI = [
  {
    inputs: [{internalType: 'address', name: '', type: 'address'}],
    name: 'deposited',
    outputs: [{internalType: 'uint256', name: '', type: 'uint256'}],
    stateMutability: 'view',
    type: 'function',
  },
  {
    inputs: [{internalType: 'address', name: '', type: 'address'}],
    name: 'withdrawn',
    outputs: [{internalType: 'uint256', name: '', type: 'uint256'}],
    stateMutability: 'view',
    type: 'function',
  },
];

export class NativeService {
  private readonly _wallet: ethers.Wallet;
  private readonly _logger: ILogger;

  constructor(
    private readonly _networks: Map<string, NativeNetworkConfig>,
    private readonly _center: BlockchainCenterApi,
    privateKey: string,
    logger: ILogger,
  ) {
    this._logger = logger.clone('[SIGN]');
    // Signer only — signMessage runs locally, no provider needed (reads go through the center).
    this._wallet = new ethers.Wallet(privateKey);
    // The address must hold SIGNER_ROLE on every DepositNative proxy. A withdraw that reverts with a
    // bad signature is diagnosed by comparing this line against the on-chain role.
    this._logger.info(`signer address ${this._wallet.address}`);
  }

  get signerAddress(): string {
    return this._wallet.address;
  }

  private cfg(network: string): NativeNetworkConfig {
    const c = this._networks.get(network);
    if (!c) {
      throw new Error(`network ${network} not configured`);
    }
    if (!c.depositNativeAddress) {
      // Distinct from "unknown network": the slot is simply still blank for this deploy.
      throw new Error(`network ${network} has no DepositNative address for this build (prod flag mismatch?)`);
    }
    return c;
  }

  // Exact wei counters, read at confirmed depth so a value about to reorg out can't be signed over.
  async getCounters(network: string, userAddress: string): Promise<{deposited: string; withdrawn: string}> {
    const c = this.cfg(network);
    const [deposited, withdrawn] = await Promise.all([
      this._center.callContract(c.centerNetwork, c.depositNativeAddress, COUNTERS_ABI, 'deposited', [userAddress], c.confirmations),
      this._center.callContract(c.centerNetwork, c.depositNativeAddress, COUNTERS_ABI, 'withdrawn', [userAddress], c.confirmations),
    ]);
    return {
      deposited: BigNumber.from(deposited ?? 0).toString(),
      withdrawn: BigNumber.from(withdrawn ?? 0).toString(),
    };
  }

  // The game server owns the wei ledger + row lock and supplies the already-computed `allowedCumulative`
  // (exact wei string). This only stamps a deadline and signs. Preimage must match
  // DepositNative._hashWithdraw: keccak256(abi.encodePacked(user, allowed, deadline, contract, chainId)).
  async signWithdraw(
    network: string,
    userAddress: string,
    allowedCumulative: string,
  ): Promise<{signature: string; deadline: number; contractAddress: string; chainId: number}> {
    const c = this.cfg(network);
    const deadline = Math.floor(Date.now() / 1000) + c.signWindowSeconds;
    const raw = ethers.utils.arrayify(ethers.utils.solidityKeccak256(
      ['address', 'uint256', 'uint256', 'address', 'uint256'],
      [userAddress, BigNumber.from(allowedCumulative), deadline, c.depositNativeAddress, c.chainId],
    ));
    const signature = await this._wallet.signMessage(raw);
    return {signature, deadline, contractAddress: c.depositNativeAddress, chainId: c.chainId};
  }
}
