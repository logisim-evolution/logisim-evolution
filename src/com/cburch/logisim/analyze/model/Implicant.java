/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.analyze.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Implicant implements Comparable<Implicant> {
	private static class TermIterator implements Iterable<Implicant>,
			Iterator<Implicant> {
		Implicant source;
		int currentMask = 0;

		TermIterator(Implicant source) {
			this.source = source;
		}

		public boolean hasNext() {
			return currentMask >= 0;
		}

		public Iterator<Implicant> iterator() {
			return this;
		}

		public Implicant next() {
			int ret = currentMask | source.values;
			int diffs = currentMask ^ source.unknowns;
			int diff = diffs ^ ((diffs - 1) & diffs);
			if (diff == 0) {
				currentMask = -1;
			} else {
				currentMask = (currentMask & ~(diff - 1)) | diff;
			}
			return new Implicant(0, ret);
		}

		public void remove() {
		}
	}

	static List<Implicant> computeMinimal(int format, AnalyzerModel model,
			String variable) {
		TruthTable table = model.getTruthTable();
		int column = model.getOutputs().indexOf(variable);
		if (column < 0)
			return Collections.emptyList();

		Entry desired = format == AnalyzerModel.FORMAT_SUM_OF_PRODUCTS ? Entry.ONE
				: Entry.ZERO;
		Entry undesired = desired == Entry.ONE ? Entry.ZERO : Entry.ONE;

		// determine the first-cut implicants, as well as the rows
		// that we need to cover.
		HashMap<Implicant, Entry> base = new HashMap<Implicant, Entry>();
		HashSet<Implicant> toCover = new HashSet<Implicant>();
		boolean knownFound = false;
		for (int i = 0; i < table.getRowCount(); i++) {
			Entry entry = table.getOutputEntry(i, column);
			if (entry == undesired) {
				knownFound = true;
			} else if (entry == desired) {
				knownFound = true;
				Implicant imp = new Implicant(0, i);
				base.put(imp, entry);
				toCover.add(imp);
			} else {
				Implicant imp = new Implicant(0, i);
				base.put(imp, entry);
			}
		}
		if (!knownFound)
			return null;

		// work up to more general implicants, discovering
		// any prime implicants.
		HashSet<Implicant> primes = new HashSet<Implicant>();
		HashMap<Implicant, Entry> current = base;
		while (current.size() > 1) {
			HashSet<Implicant> toRemove = new HashSet<Implicant>();
			HashMap<Implicant, Entry> next = new HashMap<Implicant, Entry>();
			for (Map.Entry<Implicant, Entry> curEntry : current.entrySet()) {
				Implicant imp = curEntry.getKey();
				Entry detEntry = curEntry.getValue();
				for (int j = 1; j <= imp.values; j *= 2) {
					if ((imp.values & j) != 0) {
						Implicant opp = new Implicant(imp.unknowns, imp.values
								^ j);
						Entry oppEntry = current.get(opp);
						if (oppEntry != null) {
							toRemove.add(imp);
							toRemove.add(opp);
							Implicant i = new Implicant(opp.unknowns | j,
									opp.values);
							Entry e;
							if (oppEntry == Entry.DONT_CARE
									&& detEntry == Entry.DONT_CARE) {
								e = Entry.DONT_CARE;
							} else {
								e = desired;
							}
							next.put(i, e);
						}
					}
				}
			}

			for (Map.Entry<Implicant, Entry> curEntry : current.entrySet()) {
				Implicant det = curEntry.getKey();
				if (!toRemove.contains(det) && curEntry.getValue() == desired) {
					primes.add(det);
				}
			}

			current = next;
		}

		// we won't have more than one implicant left, but it
		// is probably prime.
		for (Map.Entry<Implicant, Entry> curEntry : current.entrySet()) {
			Implicant imp = curEntry.getKey();
			if (current.get(imp) == desired) {
				primes.add(imp);
			}
		}

		// determine the essential prime implicants
		HashSet<Implicant> retSet = new HashSet<Implicant>();
		HashSet<Implicant> covered = new HashSet<Implicant>();
		for (Implicant required : toCover) {
			if (covered.contains(required))
				continue;
			int row = required.getRow();
			Implicant essential = null;
			for (Implicant imp : primes) {
				if ((row & ~imp.unknowns) == imp.values) {
					if (essential == null)
						essential = imp;
					else {
						essential = null;
						break;
					}
				}
			}
			if (essential != null) {
				retSet.add(essential);
				primes.remove(essential);
				for (Implicant imp : essential.getTerms()) {
					covered.add(imp);
				}
			}
		}
		toCover.removeAll(covered);

		// This is an unusual case, but it's possible that the
		// essential prime implicants don't cover everything.
		// In that case, greedily pick out prime implicants
		// that cover the most uncovered rows.
		// BUG: This algorithm does not always find the correct solution
		// Fix: We are first making a set that contains primes without the don't care set
		boolean ContainsDontCare;
		HashSet<Implicant> primesNoDontCare = new HashSet<Implicant>();
		for (Iterator<Implicant> it = primes.iterator(); it.hasNext();) {
			Implicant implicant = it.next();
			ContainsDontCare = false;
			for (Implicant term : implicant.getTerms()) {
				if (table.getOutputEntry(term.getRow(),column).equals(Entry.DONT_CARE))
					ContainsDontCare = true;
			}
			if (!ContainsDontCare) {
				primesNoDontCare.add(implicant);
			}
		}
		
		// Now we determine again the essential primes of this reduced set
		for (Implicant required : toCover) {
			if (covered.contains(required))
				continue;
			int row = required.getRow();
			Implicant essential = null;
			for (Implicant imp : primesNoDontCare) {
				if ((row & ~imp.unknowns) == imp.values) {
					if (essential == null)
						essential = imp;
					else {
						essential = null;
						break;
					}
				}
			}
			if (essential != null) {
				retSet.add(essential);
				primesNoDontCare.remove(essential);
				primes.remove(essential);
				for (Implicant imp : essential.getTerms()) {
					covered.add(imp);
				}
			}
		}
		toCover.removeAll(covered);

		/* When this did not do the job we use a greedy algorithm */
		while (!toCover.isEmpty()) {
			// find the implicant covering the most rows
			Implicant max = null;
			int maxCount = 0;
			int maxUnknowns = Integer.MAX_VALUE;
			for (Iterator<Implicant> it = primes.iterator(); it.hasNext();) {
				Implicant imp = it.next();
				int count = 0;
				for (Implicant term : imp.getTerms()) {
					if (toCover.contains(term))
						++count;
				}
				if (count == 0) {
					it.remove();
				} else if (count > maxCount) {
					max = imp;
					maxCount = count;
					maxUnknowns = imp.getUnknownCount();
				} else if (count == maxCount) {
					int unk = imp.getUnknownCount();
					if (unk > maxUnknowns) {
						max = imp;
						maxUnknowns = unk;
					}
				}
			}

			// add it to our choice, and remove the covered rows
			if (max != null) {
				retSet.add(max);
				primes.remove(max);
				for (Implicant term : max.getTerms()) {
					toCover.remove(term);
				}
			}
		}

		// Now build up our sum-of-products expression
		// from the remaining terms
		ArrayList<Implicant> ret = new ArrayList<Implicant>(retSet);
		Collections.sort(ret);
		return ret;
	}

	static Expression toExpression(int format, AnalyzerModel model,
			List<Implicant> implicants) {
		if (implicants == null)
			return null;
		TruthTable table = model.getTruthTable();
		if (format == AnalyzerModel.FORMAT_PRODUCT_OF_SUMS) {
			Expression product = null;
			for (Implicant imp : implicants) {
				product = Expressions.and(product, imp.toSum(table));
			}
			return product == null ? Expressions.constant(1) : product;
		} else {
			Expression sum = null;
			for (Implicant imp : implicants) {
				sum = Expressions.or(sum, imp.toProduct(table));
			}
			return sum == null ? Expressions.constant(0) : sum;
		}
	}

	static Implicant MINIMAL_IMPLICANT = new Implicant(0, -1);
	static List<Implicant> MINIMAL_LIST = Arrays
			.asList(new Implicant[] { MINIMAL_IMPLICANT });

	private int unknowns;

	private int values;

	private Implicant(int unknowns, int values) {
		this.unknowns = unknowns;
		this.values = values;
	}

	public int compareTo(Implicant o) {
		if (this.values < o.values)
			return -1;
		if (this.values > o.values)
			return 1;
		if (this.unknowns < o.unknowns)
			return -1;
		if (this.unknowns > o.unknowns)
			return 1;
		return 0;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Implicant))
			return false;
		Implicant o = (Implicant) other;
		return this.unknowns == o.unknowns && this.values == o.values;
	}

	public int getRow() {
		if (unknowns != 0)
			return -1;
		return values;
	}

	public Iterable<Implicant> getTerms() {
		return new TermIterator(this);
	}

	public int getUnknownCount() {
		int ret = 0;
		int n = unknowns;
		while (n != 0) {
			n &= (n - 1);
			ret++;
		}
		return ret;
	}

	@Override
	public int hashCode() {
		return (unknowns << 16) | values;
	}

	private Expression toProduct(TruthTable source) {
		Expression term = null;
		int cols = source.getInputColumnCount();
		for (int i = cols - 1; i >= 0; i--) {
			if ((unknowns & (1 << i)) == 0) {
				Expression literal = Expressions.variable(source
						.getInputHeader(cols - 1 - i));
				if ((values & (1 << i)) == 0)
					literal = Expressions.not(literal);
				term = Expressions.and(term, literal);
			}
		}
		return term == null ? Expressions.constant(1) : term;
	}

	private Expression toSum(TruthTable source) {
		Expression term = null;
		int cols = source.getInputColumnCount();
		for (int i = cols - 1; i >= 0; i--) {
			if ((unknowns & (1 << i)) == 0) {
				Expression literal = Expressions.variable(source
						.getInputHeader(cols - 1 - i));
				if ((values & (1 << i)) != 0)
					literal = Expressions.not(literal);
				term = Expressions.or(term, literal);
			}
		}
		return term == null ? Expressions.constant(1) : term;
	}
}
