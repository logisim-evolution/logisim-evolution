/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class Implicant implements Comparable<Implicant> {
  private static class TermIterator implements Iterable<Implicant>, Iterator<Implicant> {
    final Implicant source;
    int currentMask = 0;

    TermIterator(Implicant source) {
      this.source = source;
    }

    @Override
    public boolean hasNext() {
      return currentMask >= 0;
    }

    @Override
    public Iterator<Implicant> iterator() {
      return this;
    }

    @Override
    public Implicant next() {
      final var ret = currentMask | source.values;
      final var diffs = currentMask ^ source.unknowns;
      final var diff = diffs ^ ((diffs - 1) & diffs);
      if (diff == 0) {
        currentMask = -1;
      } else {
        currentMask = (currentMask & -diff) | diff;
      }
      return new Implicant(0, ret);
    }

    @Override
    public void remove() {
      // Do nothing.
    }
  }

  static List<Implicant> computeMinimal(int format, AnalyzerModel model, String variable) {
    final var table = model.getTruthTable();
    final var column = model.getOutputs().bits.indexOf(variable);
    if (column < 0) return Collections.emptyList();

    final var desired = format == AnalyzerModel.FORMAT_SUM_OF_PRODUCTS ? Entry.ONE : Entry.ZERO;
    final var undesired = desired == Entry.ONE ? Entry.ZERO : Entry.ONE;

    // determine the first-cut implicants, as well as the rows
    // that we need to cover.
    final var base = new HashMap<Implicant, Entry>();
    final var toCover = new HashSet<Implicant>();
    var knownFound = false;
    for (var i = 0; i < table.getRowCount(); i++) {
      final var entry = table.getOutputEntry(i, column);
      if (entry == undesired) {
        knownFound = true;
      } else if (entry == desired) {
        knownFound = true;
        final var imp = new Implicant(0, i);
        base.put(imp, entry);
        toCover.add(imp);
      } else {
        base.put(new Implicant(0, i), entry);
      }
    }
    if (!knownFound) return null;

    // work up to more general implicants, discovering
    // any prime implicants.
    final var primes = new HashSet<Implicant>();
    var current = base;
    while (current.size() > 1) {
      final var toRemove = new HashSet<Implicant>();
      final var next = new HashMap<Implicant, Entry>();
      for (final var curEntry : current.entrySet()) {
        final var imp = curEntry.getKey();
        final var detEntry = curEntry.getValue();
        for (var j = 1; j <= imp.values; j *= 2) {
          if ((imp.values & j) != 0) {
            final var opp = new Implicant(imp.unknowns, imp.values ^ j);
            final var oppEntry = current.get(opp);
            if (oppEntry != null) {
              toRemove.add(imp);
              toRemove.add(opp);
              final var i = new Implicant(opp.unknowns | j, opp.values);
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

      for (final var curEntry : current.entrySet()) {
        final var det = curEntry.getKey();
        if (!toRemove.contains(det) && curEntry.getValue() == desired) {
          primes.add(det);
        }
      }

      current = next;
    }

    // we won't have more than one implicant left, but it
    // is probably prime.
    for (final var curEntry : current.entrySet()) {
      final var imp = curEntry.getKey();
      if (current.get(imp) == desired) {
        primes.add(imp);
      }
    }

    // determine the essential prime implicants
    final var retSet = new HashSet<Implicant>();
    final var covered = new HashSet<Implicant>();
    for (final var required : toCover) {
      if (covered.contains(required)) continue;
      final var row = required.getRow();
      Implicant essential = null;
      for (final var implicant : primes) {
        if ((row & ~implicant.unknowns) == implicant.values) {
          essential = (essential == null) ? implicant : null;
        }
      }
      if (essential != null) {
        retSet.add(essential);
        primes.remove(essential);
        for (final var implicant : essential.getTerms()) {
          covered.add(implicant);
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
    boolean containsDontCare;
    final var primesNoDontCare = new HashSet<Implicant>();
    for (final var implicant : primes) {
      containsDontCare = false;
      for (final var term : implicant.getTerms()) {
        if (table.getOutputEntry(term.getRow(), column).equals(Entry.DONT_CARE))
          containsDontCare = true;
      }
      if (!containsDontCare) {
        primesNoDontCare.add(implicant);
      }
    }

    // Now we determine again the essential primes of this reduced set
    for (final var required : toCover) {
      if (covered.contains(required)) continue;
      final var row = required.getRow();
      Implicant essential = null;
      for (final var implicant : primesNoDontCare) {
        if ((row & ~implicant.unknowns) == implicant.values) {
          if (essential == null) {
            essential = implicant;
          } else {
            essential = null;
            break;
          }
        }
      }
      if (essential != null) {
        retSet.add(essential);
        primesNoDontCare.remove(essential);
        primes.remove(essential);
        for (final var implicant : essential.getTerms()) covered.add(implicant);
      }
    }
    toCover.removeAll(covered);

    /* When this did not do the job we use a greedy algorithm */
    while (!toCover.isEmpty()) {
      // find the implicant covering the most rows
      Implicant max = null;
      var maxCount = 0;
      var maxUnknowns = Integer.MAX_VALUE;
      for (final var it = primes.iterator(); it.hasNext(); ) {
        final var imp = it.next();
        var count = 0;
        for (final var term : imp.getTerms()) {
          if (toCover.contains(term)) ++count;
        }
        if (count == 0) {
          it.remove();
        } else if (count > maxCount) {
          max = imp;
          maxCount = count;
          maxUnknowns = imp.getUnknownCount();
        } else if (count == maxCount) {
          final var unk = imp.getUnknownCount();
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
        for (final var term : max.getTerms()) {
          toCover.remove(term);
        }
      }
    }

    // Now build up our sum-of-products expression
    // from the remaining terms
    final var ret = new ArrayList<Implicant>(retSet);
    Collections.sort(ret);
    return ret;
  }

  public static Expression toExpression(int format, AnalyzerModel model, List<Implicant> implicants) {
    if (implicants == null) return null;
    final var table = model.getTruthTable();
    if (format == AnalyzerModel.FORMAT_PRODUCT_OF_SUMS) {
      Expression product = null;
      for (final var implicant : implicants) {
        product = Expressions.and(product, implicant.toSum(table));
      }
      return product == null ? Expressions.constant(1) : product;
    } else {
      Expression sum = null;
      for (final var implicant : implicants) {
        sum = Expressions.or(sum, implicant.toProduct(table));
      }
      return sum == null ? Expressions.constant(0) : sum;
    }
  }

  static final Implicant MINIMAL_IMPLICANT = new Implicant(0, -1);
  static final List<Implicant> MINIMAL_LIST = Collections.singletonList(MINIMAL_IMPLICANT);

  final int unknowns;
  final int values;

  private Implicant(int unknowns, int values) {
    this.unknowns = unknowns;
    this.values = values;
  }

  @Override
  public int compareTo(Implicant o) {
    if (this.values < o.values) return -1;
    if (this.values > o.values) return 1;
    return Integer.compare(this.unknowns, o.unknowns);
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof Implicant o)
           ? this.unknowns == o.unknowns && this.values == o.values
           : false;
  }

  public int getRow() {
    return (unknowns != 0) ? -1 : values;
  }

  public Iterable<Implicant> getTerms() {
    return new TermIterator(this);
  }

  public int getUnknownCount() {
    var ret = 0;
    var n = unknowns;
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
    final var cols = source.getInputColumnCount();
    for (var i = cols - 1; i >= 0; i--) {
      if ((unknowns & (1 << i)) == 0) {
        var literal = Expressions.variable(source.getInputHeader(cols - 1 - i));
        if ((values & (1 << i)) == 0) literal = Expressions.not(literal);
        term = Expressions.and(term, literal);
      }
    }
    return term == null ? Expressions.constant(1) : term;
  }

  public Expression toSum(TruthTable source) {
    Expression term = null;
    final var cols = source.getInputColumnCount();
    for (var i = cols - 1; i >= 0; i--) {
      if ((unknowns & (1 << i)) == 0) {
        var literal = Expressions.variable(source.getInputHeader(cols - 1 - i));
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
    final var table = model.getTruthTable();
    final var maxval = (1 << table.getInputColumnCount()) - 1;
    // Determine the set of regions and the first-cut implicants for each
    // region.
    final var regions = new HashMap<String, HashSet<Implicant>>();
    for (var i = 0; i < table.getVisibleRowCount(); i++) {
      final var val = table.getVisibleOutputs(i);
      final var idx = table.getVisibleRowIndex(i);
      final var dc = table.getVisibleRowDcMask(i);
      final var imp = new Implicant(dc, idx);
      final var region = regions.computeIfAbsent(val, k -> new HashSet<Implicant>());
      region.add(imp);
    }
    // For each region...
    final var ret = new TreeMap<Implicant, String>();
    for (var it : regions.entrySet()) {
      final var val = it.getKey();
      final var base = it.getValue();

      // Work up to more general implicants.
      final var all = new HashSet<Implicant>();
      var current = base;
      while (!current.isEmpty()) {
        final var next = new HashSet<Implicant>();
        for (final var implicant : current) {
          all.add(implicant);
          for (int j = 1; j <= maxval; j *= 2) {
            if ((implicant.unknowns & j) != 0) continue;
            final var opp = new Implicant(implicant.unknowns, implicant.values ^ j);
            if (!all.contains(opp)) continue;
            final var i = new Implicant(opp.unknowns | j, opp.values);
            next.add(i);
          }
        }
        current = next;
      }

      final var sorted = new ArrayList<Implicant>(all);
      sorted.sort(sortByGenerality);
      final var chosen = new ArrayList<Implicant>();
      for (final var implicant : sorted) {
        if (disjoint(implicant, chosen)) {
          chosen.add(implicant);
          ret.put(implicant, val);
        }
      }
    }

    // TODO in caller: convert implicant to Row and val back to Entry[]
    return ret;
  }

  private static boolean disjoint(Implicant imp, ArrayList<Implicant> chosen) {
    for (final var other : chosen) {
      final var dc = imp.unknowns | other.unknowns;
      if ((imp.values & ~dc) == (other.values & ~dc)) return false;
    }
    return true;
  }

  private static final CompareGenerality sortByGenerality = new CompareGenerality();

  private static class CompareGenerality implements Comparator<Implicant> {
    @Override
    public int compare(Implicant i1, Implicant i2) {
      final var diff = (i2.getUnknownCount() - i1.getUnknownCount());
      if (diff != 0) return diff;
      return (i1.values & ~i1.unknowns) - (i2.values & ~i2.unknowns);
    }
  }
}
