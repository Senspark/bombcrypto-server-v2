import {ethers} from 'ethers';

function isAddress(address: string) {
  return ethers.utils.isAddress(address);
}

function hashMessage(types: string[], values: unknown[]): Uint8Array {
  return ethers.utils.arrayify(ethers.utils.solidityKeccak256(types, values));
}

export {
  isAddress,
  hashMessage,
};
