package com.senspark.game.data.model.config;

import java.util.List;

public class HeroUpgradePower {
	private int rare;
	private List<Integer> powers;

	public int getRare() {
		return rare;
	}

	public void setRare(int rare) {
		this.rare = rare;
	}

	public List<Integer> getPowers() {
		return powers;
	}

	public void setPowers(List<Integer> powers) {
		this.powers = powers;
	}
}
