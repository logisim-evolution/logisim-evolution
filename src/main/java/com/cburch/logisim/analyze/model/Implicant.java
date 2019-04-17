/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.analyze.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Implicant implements Comparable<Implicant> {
  private static class TermIterator implements Iterable<Implicant>, Iterator<Implicant> {
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

    public void remove() {}
  }

  static List<Implicant> computeMinimal(int format, AnalyzerModel model, String variable) {
    TruthTable table = model.getTruthTable();
    int column = model.getOutputs().bits.indexOf(variable);
    if (column < 0) return Collections.emptyList();

    Entry desired = format == AnalyzerModel.FORMAT_SUM_OF_PRODUCTS ? Entry.ONE : Entry.ZERO;
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
    if (!knownFound) return null;

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
            Implicant opp = new Implicant(imp.unknowns, imp.values ^ j);
            Entry oppEntry = current.get(opp);
            if (oppEntry != null) {
              toRemove.add(imp);
              toRemove.add(opp);
              Implicant i = new Implicant(opp.unknowns | j, opp.values);
              Entry e;
              if (oppEntry == Entry.DONT_CARE && detEntry == Entry.DONT_CARE) {
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
      if (covered.contains(required)) continue;
      int row = required.getRow();
      Implicant essential = null;
      for (Implicant imp : primes) {
        if ((row & ~imp.unknowns) == imp.values) {
          if (essential == null) essential = imp;
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
    for (Iterator<Implicant> it = primes.iterator(); it.hasNext(); ) {
      Implicant implicant = it.next();
      ContainsDontCare = false;
      for (Implicant term : implicant.getTerms()) {
        if (table.getOutputEntry(term.getRow(), column).equals(Entry.DONT_CARE))
          ContainsDontCare = true;
      }
      if (!ContainsDontCare) {
        primesNoDontCare.add(implicant);
      }
    }

    // Now we determine again the essential primes of this reduced set
    for (Implicant required : toCover) {
      if (covered.contains(required)) continue;
      int row = required.getRow();
      Implicant essential = null;
      for (Implicant imp : primesNoDontCare) {
        if ((row & ~imp.unknowns) == imp.values) {
          if (essential == null) essential = imp;
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
      for (Iterator<Implicant> it = primes.iterator(); it.hasNext(); ) {
        Implicant imp = it.next();
        int count = 0;
        for (Implicant term : imp.getTerms()) {
          if (toCover.contains(term)) ++count;
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

  public static Expression toExpression(
      int format, AnalyzerModel model, List<Implicant> implicants) {
    if (implicants == null) return null;
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
  static List<Implicant> MINIMAL_LIST = Arrays.asList(new Implicant[] {MINIMAL_IMPLICANT});

  final int unknowns, values;

  private Implicant(int unknowns, int values) {
    this.unknowns = unknowns;
    this.values = values;
  }

  public int compareTo(Implicant o) {
    if (this.values < o.values) return -1;
    if (this.values > o.values) return 1;
    if (this.unknowns < o.unknowns) return -1;
    if (this.unknowns > o.unknowns) return 1;
    return 0;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Implicant)) return false;
    Implicant o = (Implicant) other;
    return this.unknowns == o.unknowns && this.values == o.values;
  }

  public int getRow() {
    if (unknowns != 0) return -1;
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

  public Expression toProduct(TruthTable source) {
    Expression term = null;
    int cols = source.getInputColumnCount();
    for (int i = cols - 1; i >= 0; i--) {
      if ((unknowns & (1 << i)) == 0) {
        Expression literal = Expressions.variable(source.getInputHeader(cols - 1 - i));
        if ((values & (1 << i)) == 0) literal = Expressions.not(literal);
        term = Expressions.and(term, literal);
      }
    }
    return term == null ? Expressions.constant(1) : term;
  }

  public Expression toSum(TruthTable source) {
    Expression term = null;
    int cols = source.getInputColumnCount();
    for (int i = cols - 1; i >= 0; i--) {
      if ((unknowns & (1 << i)) == 0) {
        Expression literal = Expressions.variable(source.getInputHeader(cols - 1 - i));
        if ((values & (1 << i)) != 0) literal = Expressions.not(literal);
        term = Expressions.or(term, literal);
      }
    }
    return term == null ? Expressions.constant(1) : term;
  }

  static SortedMap<Implicant, String> computePartition(AnalyzerModel model) {
    // The goal is to find a minimal partitioning of each of the regions (of
    // the {0,1}^n hypercube) defined by the truth table output entries
    // (string of zero, one, dont_care, error, etc.). Similar to K-maps, we
    // can fairly easily find all the prime implicants, but unlike K-maps,
    // we can't have overlap, so a different algorithm is called for. Maybe
    // something from set-covering or binary-partition-trees? It's not even
    // obvious what the complexity of this problem is. We'll just go with a
    // simple greedy algorithm and hope for the best: sort the prime
    // implicants, keep accepting non-overlapping ones until we have covered
    // the region.
    TruthTable table = model.getTruthTable();
    int maxval = (1 << table.getInputColumnCount()) - 1;
    // Determine the set of regions and the first-cut implicants for each
    // region.
    HashMap<String, HashSet<Implicant>> regions = new HashMap<>();
    for (int i = 0; i < table.getVisibleRowCount(); i++) {
      String val = table.getVisibleOutputs(i);
      int idx = table.getVisibleRowIndex(i);
      int dc = table.getVisibleRowDcMask(i);
      Implicant imp = new Implicant(dc, idx);
      HashSet<Implicant> region = regions.get(val);
      if (region == null) {
        region = new HashSet<>();
        regions.put(val, region);
      }
      region.add(imp);
    }
    // For each region...
    TreeMap<Implicant, String> ret = new TreeMap<>();
    for (Map.Entry<String, HashSet<Implicant>> it : regions.entrySet()) {
      String val = it.getKey();
      HashSet<Implicant> base = it.getValue();

      // Work up to more general implicants.
      HashSet<Implicant> all = new HashSet<>();
      HashSet<Implicant> current = base;
      while (current.size() > 0) {
        HashSet<Implicant> next = new HashSet<>();
        for (Implicant imp : current) {
          all.add(imp);
          for (int j = 1; j <= maxval; j *= 2) {
            if ((imp.unknowns & j) != 0) continue;
            Implicant opp = new Implicant(imp.unknowns, imp.values ^ j);
            if (!all.contains(opp)) continue;
            Implicant i = new Implicant(opp.unknowns | j, opp.values);
            next.add(i);
          }
        }
        current = next;
      }

      ArrayList<Implicant> sorted = new ArrayList<>(all);
      Collections.sort(sorted, sortByGenerality);
      ArrayList<Implicant> chosen = new ArrayList<>();
      for (Implicant imp : sorted) {
        if (disjoint(imp, chosen)) {
          chosen.add(imp);
          ret.put(imp, val);
        }
      }
    }

    // todo in caller: convert implicant to Row and val back to Entry[]
    return ret;
  }

  private static boolean disjoint(Implicant imp, ArrayList<Implicant> chosen) {
    for (Implicant other : chosen) {
      int dc = imp.unknowns | other.unknowns;
      if ((imp.values & ~dc) == (other.values & ~dc)) return false;
    }
    return true;
  }

  private static final CompareGenerality sortByGenerality = new CompareGenerality();

  private static class CompareGenerality implements Comparator<Implicant> {
    public int compare(Implicant i1, Implicant i2) {
      int diff = (i2.getUnknownCount() - i1.getUnknownCount());
      if (diff != 0) return diff;
      return (i1.values & ~i1.unknowns) - (i2.values & ~i2.unknowns);
    }
  }
}
