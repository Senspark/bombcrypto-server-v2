export interface IDecodedDetails {
  id: number;
  index: number;
  rarity: number;
  rarity_name: string;
  level: number;
  color: number;
  skin: number;
  stamina: number;
  speed: number;
  bomb_skin: number;
  bomb_count: number;
  bomb_power: number;
  bomb_range: number;
  abilities: number[];
  abilities_s: number[];
  block_number: number;
  randomize_ability_counter: number;
  shield_level: number;
  reset_shield_counter: number;
}

export interface IExtendedDecodedDetails extends IDecodedDetails {
  details: string;
  block_details: string;
  token_transfers: string;
  stake_amount: number; // legacy
  stake_bcoin: number;
  stake_sen: number;
}

export interface IShortDetails {
  details: string;
  id: number;
  stake_amount: number; // legacy
  stake_bcoin: number;
  stake_sen: number;
}