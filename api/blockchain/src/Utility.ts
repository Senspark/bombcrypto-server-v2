import {BigNumber, ethers} from 'ethers';
import {IDecodedDetails} from "./IDecodedDetails";

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function isAddress(address: string) {
  return ethers.utils.isAddress(address);
}

function isAddressEqual(lhs: string, rhs: string) {
  if (!isAddress(lhs)) {
    throw Error(`Invalid address: ${lhs}`);
  }
  if (!isAddress(rhs)) {
    throw Error(`Invalid address: ${rhs}`);
  }
  return lhs.toLowerCase() === rhs.toLowerCase();
}

function roundFloat(x: number, digits: number) {
  const p = Math.pow(10, digits);
  return Math.round(x * p) / p;
}

/**
 * https://stackoverflow.com/questions/2450954/how-to-randomize-shuffle-a-javascript-array
 */
function shuffleArray<T>(array: T[]) {
  for (let i = array.length - 1; i > 0; --i) {
    const j = Math.floor(Math.random() * (i + 1));
    const temp = array[i];
    array[i] = array[j];
    array[j] = temp;
  }
}

function weightedRandom(weights: number[]) {
  let totalWeight = 0;
  weights.forEach(item => totalWeight += item);
  const k = Math.random() * totalWeight;
  let total = 0;
  for (let i = 0; i < weights.length; ++i) {
    total += weights[i];
    if (k < total) {
      return i;
    }
  }
  return 0;
}

function weightedRandomSampling(_weights: number[], size: number) {
  const weights = [..._weights]; // Change to mutable.
  const result: number[] = [];
  for (let i = 0; i < size; ++i) {
    const index = weightedRandom(weights);
    weights[index] = 0;
    result.push(index);
  }
  return result;
}

function encodeHeroDetails(details: IDecodedDetails): string {
  let encoded = 0n;
  encoded |= BigInt(details.id);
  encoded |= BigInt(details.index) << 30n;
  encoded |= BigInt(details.rarity) << 40n;
  encoded |= BigInt(details.level) << 45n;
  encoded |= BigInt(details.color) << 50n;
  encoded |= BigInt(details.skin) << 55n;
  encoded |= BigInt(details.stamina) << 60n;
  encoded |= BigInt(details.speed) << 65n;
  encoded |= BigInt(details.bomb_skin) << 70n;
  encoded |= BigInt(details.bomb_count) << 75n;
  encoded |= BigInt(details.bomb_power) << 80n;
  encoded |= BigInt(details.bomb_range) << 85n;
  encoded |= BigInt(details.abilities.length) << 90n;
  for (let i = 0; i < details.abilities.length; ++i) {
    encoded |= BigInt(details.abilities[i]) << (95n + BigInt(i) * 5n);
  }
  encoded |= BigInt(details.block_number) << 145n
  encoded |= BigInt(details.randomize_ability_counter) << 175n
  encoded |= BigInt(details.abilities_s.length) << 180n
  for (let i = 0; i < details.abilities_s.length; ++i) {
    encoded |= BigInt(details.abilities_s[i]) << (185n + BigInt(i) * 5n);
  }
  encoded |= BigInt(details.shield_level) << 235n;
  encoded |= BigInt(details.reset_shield_counter) << 240n;
  return ethers.BigNumber.from(encoded).toString();
}

function decodeHeroDetails(_details: string): IDecodedDetails {
  const details = ethers.BigNumber.from(_details);
  const parse = (position: number, size: number) => {
    return details.shr(position).and((1 << size) - 1).toNumber();
  };
  const id = parse(0, 30);
  const index = parse(30, 10);
  const rarity = parse(40, 5);
  const level = parse(45, 5);
  const color = parse(50, 5);
  const skin = parse(55, 5);
  const stamina = parse(60, 5);
  const speed = parse(65, 5);
  const bombSkin = parse(70, 5);
  const bombCount = parse(75, 5);
  const bombPower = parse(80, 5);
  const bombRange = parse(85, 5);
  const ability = parse(90, 5);
  const abilities: number[] = [];
  for (let i = 0; i < ability; ++i) {
    abilities.push(parse(95 + i * 5, 5));
  }
  const blockNumber = parse(145, 30);
  const randomizeAbilityCounter = parse(175, 5);
  const abilityS = parse(180, 5);
  const abilitiesS: number[] = [];
  for (let i = 0; i < abilityS; ++i) {
    abilitiesS.push(parse(185 + i * 5, 5));
  }
  const shieldLevel = parse(235, 5);
  const resetShieldCounter = parse(240, 10);
  const RARITY_NAMES = [
    `Common`,
    `Rare`,
    `Super Rare`,
    `Epic`,
    `Legend`,
    `Super Legend`
  ];
  return {
    id,
    index,
    rarity,
    rarity_name: RARITY_NAMES[rarity],
    level,
    color,
    skin,
    stamina,
    speed,
    bomb_skin: bombSkin,
    bomb_count: bombCount,
    bomb_power: bombPower,
    bomb_range: bombRange,
    abilities,
    abilities_s: abilitiesS,
    block_number: blockNumber,
    randomize_ability_counter: randomizeAbilityCounter,
    shield_level: shieldLevel,
    reset_shield_counter: resetShieldCounter,
  };
}

function encodeHouseDetails(details: { id: number; index: number; rarity: number; recovery: number; capacity: number; block_number: number }): string {
  let encoded = 0n;
  encoded |= BigInt(details.id);
  encoded |= BigInt(details.index) << 30n;
  encoded |= BigInt(details.rarity) << 40n;
  encoded |= BigInt(details.recovery) << 45n;
  encoded |= BigInt(details.capacity) << 60n;
  encoded |= BigInt(details.block_number) << 65n;
  return ethers.BigNumber.from(encoded).toString();
}

function decodeHouseDetails(_details: string) {
  const details = ethers.BigNumber.from(_details);
  const parse = (position: number, size: number) => {
    return details.shr(position).and((1 << size) - 1).toNumber();
  };
  const id = parse(0, 30);
  const index = parse(30, 10);
  const rarity = parse(40, 5);
  const recovery = parse(45, 15);
  const capacity = parse(60, 5);
  const blockNumber = parse(65, 30);
  const RARITY_NAMES = [
    `Common`,
    `Rare`,
    `Super Rare`,
    `Epic`,
    `Legend`,
    `Super Legend`
  ];
  const RARITY_DISPLAY_NAMES = [
    `Tiny House`,
    `Mini House`,
    `Luxury House`,
    `Penthouse`,
    `Villa`,
    `Super Villa`,
  ];
  return {
    id,
    index,
    rarity,
    rarity_name: RARITY_NAMES[rarity],
    rarity_display_name: RARITY_DISPLAY_NAMES[rarity],
    recovery,
    capacity,
    block_number: blockNumber,
  };
}

function decodeFunctionInput(inputData: string, abi: string, functionName: string): BigNumber[] {
  const iface = new ethers.utils.Interface(abi);
  const data = iface.decodeFunctionData(iface.getFunction(functionName), inputData);
  const result: BigNumber[] = [];
  for (const key in data) {
    result.push(data[key]);
  }
  return result;
}

// https://stackoverflow.com/questions/46946380/fetch-api-request-timeout
async function fetchTimeout(url: string, ms: number) {
  const controller = new AbortController();
  const promise = fetch(url);
  const timeout = setTimeout(() => controller.abort(), ms);
  return promise.finally(() => clearTimeout(timeout));
}

function bnToNumber(value: number | null): number {
  if (!value) {
    return 0;
  }
  return formatNumber2(BigInt(value));
}

function formatNumber(value: BigNumber) {
  return parseFloat(ethers.utils.formatEther(value));
}

function formatNumber2(value: bigint) {
  return parseFloat(ethers.utils.formatEther(value));
}

export {
  sleep,
  isAddress,
  isAddressEqual,
  roundFloat,
  shuffleArray,
  weightedRandom,
  weightedRandomSampling,
  decodeHeroDetails,
  encodeHeroDetails,
  decodeHouseDetails,
  encodeHouseDetails,
  decodeFunctionInput,
  fetchTimeout,
  bnToNumber,
  formatNumber,
  formatNumber2,
};