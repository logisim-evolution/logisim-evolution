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

package com.cburch.logisim.circuit;
import com.cburch.logisim.util.UniquelyNamedThread;

class SimulatorTicker extends UniquelyNamedThread {
	private Simulator.PropagationManager manager;
	private int ticksPerTickPhase;
	private int millisPerTickPhase;

	private boolean shouldTick;
	private int ticksPending;
	private boolean complete;

	public SimulatorTicker(Simulator.PropagationManager manager) {
		super("SimulationTicker");
		this.manager = manager;
		ticksPerTickPhase = 1;
		millisPerTickPhase = 1000;
		shouldTick = false;
		ticksPending = 0;
		complete = false;
	}

	@Override
	public void run() {
		long lastTick = System.currentTimeMillis();
		while (true) {
			boolean curShouldTick = shouldTick;
			int millis = millisPerTickPhase;
			int ticks = ticksPerTickPhase;
			try {
				synchronized (this) {
					curShouldTick = shouldTick;
					millis = millisPerTickPhase;
					ticks = ticksPerTickPhase;
					while (!curShouldTick && ticksPending == 0 && !complete) {
						wait();
						curShouldTick = shouldTick;
						millis = millisPerTickPhase;
						ticks = ticksPerTickPhase;
					}
				}
			} catch (InterruptedException e) {
			}

			if (complete)
				break;

			int toTick;
			long now = System.currentTimeMillis();
			if (curShouldTick && now - lastTick >= millis) {
				toTick = ticks;
			} else {
				toTick = ticksPending;
			}

			if (toTick > 0) {
				lastTick = now;
				for (int i = 0; i < toTick; i++) {
					manager.requestTick();
				}
				synchronized (this) {
					if (ticksPending > toTick)
						ticksPending -= toTick;
					else
						ticksPending = 0;
				}
				// we fire tickCompleted in this thread so that other
				// objects (in particular the repaint process) can slow
				// the thread down.
			}

			try {
				long nextTick = lastTick + millis;
				int wait = (int) (nextTick - System.currentTimeMillis());
				if (wait < 1)
					wait = 1;
				if (wait > 100)
					wait = 100;
				Thread.sleep(wait);
			} catch (InterruptedException e) {
			}
		}
	}

	synchronized void setAwake(boolean value) {
		shouldTick = value;
		if (shouldTick)
			notifyAll();
	}

	public synchronized void setTickFrequency(int millis, int ticks) {
		millisPerTickPhase = millis;
		ticksPerTickPhase = ticks;
	}

	public synchronized void shutDown() {
		complete = true;
		notifyAll();
	}

	public synchronized void tickOnce() {
		ticksPending++;
		notifyAll();
	}
}
