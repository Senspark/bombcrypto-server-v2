package com.senspark.game.data.model.config;

import java.util.List;

/**
 * Ganho de stamina por nivel, por raridade. Espelha {@link HeroUpgradePower}: a lista e indexada
 * por nivel-1, e cada entrada e o TOTAL acumulado naquele nivel, nao o incremento.
 */
public class HeroUpgradeStamina {
	private int rare;
	private List<Integer> staminas;

	public int getRare() {
		return rare;
	}

	public void setRare(int rare) {
		this.rare = rare;
	}

	public List<Integer> getStaminas() {
		return staminas;
	}

	public void setStaminas(List<Integer> staminas) {
		this.staminas = staminas;
	}
}
