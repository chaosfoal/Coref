/*******************************************************************************
 * Copyright 2014,2015 Institute of Mathematics and Computer Science, University of Latvia
 * Author: Artūrs Znotiņš
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package lv.coref.rules;

import java.util.List;

import lv.coref.data.Mention;
import lv.coref.lv.Constants.PronType;

public class SubClause extends Rule {

	public String getName() {
		return "SUBCLAUSE";
	}

	public boolean filter(Mention m, Mention a) {
		if (!m.isPronoun() || a.isPronoun())
			return false;
		PronType pronType = m.getPronounType();
		if (pronType != PronType.RELATIVE && pronType != PronType.INTERROGATIVE) return false;
		
		int ms = m.getFirstToken().getPosition();
		int me = m.getLastToken().getPosition();
		int as = a.getFirstToken().getPosition();
		int ae = a.getLastToken().getPosition();
		if ((as >= ms && as <= me) || (ms >= as && ms <= ae))
			return false; // intersects, nested

		int d = ms - ae;
		if (d > 3) return false;

		return true;
	}

	public double score(Mention m, Mention a) {
		double prob = 1.0;
		if (!m.getGender().equals(a.getGender()))
			prob *= 0.5;
		if (!m.getNumber().equals(a.getNumber()))
			prob *= 0.5;
		if (!m.getCategory().weakEquals(a.getCategory()))
			prob *= 0.5;

		return prob;
	}

	public List<Mention> getPotentialAntecedents(Mention m) {
		return m.getPotentialAntecedents(-1, 0, 1);
	}

	public static void main(String[] args) {

	}

}
