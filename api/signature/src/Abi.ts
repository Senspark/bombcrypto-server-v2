// Minimal ABI fragments used by the signature service. Only the function
// fragments actually invoked via BlockchainCenterApi are kept — sending full
// contract ABIs over HTTP on every call was wasteful.

const CHECK_USER_CLAIM_ABI = [{
  inputs: [
    {internalType: 'address', name: 'user', type: 'address'},
    {internalType: 'uint256', name: 'eventID', type: 'uint256'},
  ],
  name: 'checkUserClaim',
  outputs: [{internalType: 'bool', name: '', type: 'bool'}],
  stateMutability: 'view',
  type: 'function',
}];

const CHECK_EXISTED_NFT_AIRDROP_ABI = [{
  inputs: [],
  name: 'checkExistedNFTAirdrop',
  outputs: [{internalType: 'uint256[]', name: '', type: 'uint256[]'}],
  stateMutability: 'view',
  type: 'function',
}];

const GET_TOTAL_CLAIM_ABI = [{
  inputs: [
    {internalType: 'address', name: 'user', type: 'address'},
    {internalType: 'uint256', name: 'tokenType', type: 'uint256'},
  ],
  name: 'getTotalClaim',
  outputs: [{internalType: 'uint256', name: '', type: 'uint256'}],
  stateMutability: 'view',
  type: 'function',
}];

const ERC721_BALANCE_OF_ABI = [{
  inputs: [{internalType: 'address', name: 'owner', type: 'address'}],
  name: 'balanceOf',
  outputs: [{internalType: 'uint256', name: '', type: 'uint256'}],
  stateMutability: 'view',
  type: 'function',
}];

export {
  CHECK_USER_CLAIM_ABI,
  CHECK_EXISTED_NFT_AIRDROP_ABI,
  GET_TOTAL_CLAIM_ABI,
  ERC721_BALANCE_OF_ABI,
};
