/*
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

package com.cburch.logisim.gui.icons;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class AnimationTimer extends TimerTask implements PropertyChangeListener {

  private final List<AnimationListener> listeners;
  private final ArrayList<Component> parents;
  private boolean animate;

  public AnimationTimer() {
    listeners = new ArrayList<>();
    parents = new ArrayList<>();
    animate = AppPreferences.ANIMATED_ICONS.getBoolean();
    AppPreferences.ANIMATED_ICONS.addPropertyChangeListener(this);
  }

  public void registerListener(AnimationListener l) {
    if (l != null) listeners.add(l);
  }

  public void removeListener(AnimationListener l) {
    if (l == null) return;
    listeners.remove(l);
  }

  public void addParent(Component parent) {
    if (!parents.contains(parent)) parents.add(parent);
  }

  public void removeParent(Component parent) {
    parents.remove(parent);
  }

  @Override
  public void run() {
    if (!animate) return;
    for (AnimationListener l : listeners) l.animationUpdate();
    for (Component c : parents) c.repaint();
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    boolean lastanimate = animate;
    animate = AppPreferences.ANIMATED_ICONS.getBoolean();
    if (lastanimate && !animate) {
      for (AnimationListener l : listeners) l.resetToStatic();
      for (Component c : parents) c.repaint();
    }
  }

  /**
   * @deprecated AnimatedIcon class is deprecated and will be removed in next release.
   */
  public interface AnimationListener {
    /**
     * @deprecated AnimatedIcon class is deprecated and will be removed in next release.
     */
    void animationUpdate();

    /**
     * @deprecated AnimatedIcon class is deprecated and will be removed in next release.
     */
    void resetToStatic();
  }
}
