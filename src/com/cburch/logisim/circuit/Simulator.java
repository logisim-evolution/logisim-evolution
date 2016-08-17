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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.UniquelyNamedThread;

public class Simulator {

	class PropagationManager extends UniquelyNamedThread {

		private Propagator propagator = null;
		private PropagationPoints stepPoints = new PropagationPoints();
		private volatile int ticksRequested = 0;
		private volatile int stepsRequested = 0;
		private volatile boolean resetRequested = false;
		private volatile boolean propagateRequested = false;
		private volatile boolean complete = false;
		// These variables apply only if PRINT_TICK_RATE is set
		int tickRateTicks = 0;
		long tickRateStart = System.currentTimeMillis();

		private void doTick() {
			synchronized (this) {
				ticksRequested--;
			}
			propagator.tick();
		}

		public PropagationManager() {
			super("PropagationManager");
		}

		public Propagator getPropagator() {
			return propagator;
		}

		public synchronized void requestPropagate() {
			if (!propagateRequested) {
				propagateRequested = true;
				notifyAll();
			}
		}

		public synchronized void requestReset() {
			if (!resetRequested) {
				resetRequested = true;
				notifyAll();
			}
		}

		public synchronized void requestTick() {
			if (ticksRequested < 16) {
				ticksRequested++;
			}
			notifyAll();
		}

		@Override
		public void run() {
			while (!complete) {
				try {
				synchronized (this) {
					while (!complete && !propagateRequested && !resetRequested
							&& ticksRequested == 0 && stepsRequested == 0) {
						try {
							wait();
						} catch (InterruptedException e) {
						}
					}
				}

				if (resetRequested) {
					resetRequested = false;
					if (propagator != null) {
						propagator.reset();
					}
					firePropagationCompleted();
					propagateRequested |= isRunning;
				}

				if (propagateRequested || ticksRequested > 0
						|| stepsRequested > 0) {
					boolean ticked = false;
					propagateRequested = false;
					if (isRunning) {
						stepPoints.clear();
						stepsRequested = 0;
						if (propagator == null) {
							ticksRequested = 0;
						} else {
							ticked = ticksRequested > 0;
							if (ticked) {
								doTick();
							}
							do {
								propagateRequested = false;
								try {
									exceptionEncountered = false;
									propagator.propagate();
								} catch (UnsupportedOperationException thr) {
									exceptionEncountered = true;
									setIsRunning(false);
								} catch (Exception thr) {
									thr.printStackTrace();
									exceptionEncountered = true;
									setIsRunning(false);
								}
							} while (propagateRequested);
							if (isOscillating()) {
								setIsRunning(false);
								ticksRequested = 0;
								propagateRequested = false;
							}
						}
					} else {
						if (stepsRequested > 0) {
							if (ticksRequested > 0) {
								ticksRequested = 1;
								doTick();
							}

							synchronized (this) {
								stepsRequested--;
							}
							exceptionEncountered = false;
							try {
								stepPoints.clear();
								propagator.step(stepPoints);
							} catch (Exception thr) {
								thr.printStackTrace();
								exceptionEncountered = true;
							}
						}
					}
					if (ticked) {
						fireTickCompleted();
					}
					firePropagationCompleted();
				}
					} catch (Throwable e) {
						e.printStackTrace();
						exceptionEncountered = true;
						setIsRunning(false);
						javax.swing.SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								javax.swing.JOptionPane.showMessageDialog(null, "The simulator has crashed. Save your work and restart Logisim.");
							}
						});
					}
			}
		}

		public void setPropagator(Propagator value) {
			propagator = value;
		}

		public synchronized void shutDown() {
			complete = true;
			notifyAll();
		}
	}

	private boolean isRunning = true;
	private boolean isTicking = false;
	private boolean exceptionEncountered = false;
	private double tickFrequency = 1.0;
	private PropagationManager manager;
	private SimulatorTicker ticker;
	private ArrayList<SimulatorListener> listeners = new ArrayList<SimulatorListener>();

	public Simulator() {
		manager = new PropagationManager();
		ticker = new SimulatorTicker(manager);

		try {
			manager.setPriority(manager.getPriority() - 1);
			ticker.setPriority(ticker.getPriority() - 1);
		} catch (SecurityException e) {
		} catch (IllegalArgumentException e) {
		}

		manager.start();
		ticker.start();

		tickFrequency = 0.0;
		setTickFrequency(AppPreferences.TICK_FREQUENCY.get().doubleValue());
	}

	public void addSimulatorListener(SimulatorListener l) {
		listeners.add(l);
	}

	public void drawStepPoints(ComponentDrawContext context) {
		manager.stepPoints.draw(context);
	}

	void firePropagationCompleted() {
		SimulatorEvent e = new SimulatorEvent(this);
		for (SimulatorListener l : new ArrayList<SimulatorListener>(listeners)) {
			l.propagationCompleted(e);
		}
	}

	void fireSimulatorStateChanged() {
		SimulatorEvent e = new SimulatorEvent(this);
		for (SimulatorListener l : new ArrayList<SimulatorListener>(listeners)) {
			l.simulatorStateChanged(e);
		}
	}

	void fireTickCompleted() {
		SimulatorEvent e = new SimulatorEvent(this);
		for (SimulatorListener l : new ArrayList<SimulatorListener>(listeners)) {
			l.tickCompleted(e);
		}
	}

	public CircuitState getCircuitState() {
		Propagator prop = manager.getPropagator();
		return prop == null ? null : prop.getRootState();
	}

	public double getTickFrequency() {
		return tickFrequency;
	}

	public boolean isExceptionEncountered() {
		return exceptionEncountered;
	}

	public boolean isOscillating() {
		Propagator prop = manager.getPropagator();
		return prop != null && prop.isOscillating();
	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean isTicking() {
		return isTicking;
	}

	public void removeSimulatorListener(SimulatorListener l) {
		listeners.remove(l);
	}

	private void renewTickerAwake() {
		ticker.setAwake(isRunning && isTicking && tickFrequency > 0);
	}

	public void requestPropagate() {
		manager.requestPropagate();
	}

	public void requestReset() {
		manager.requestReset();
	}

	public void setCircuitState(CircuitState state) {
		manager.setPropagator(state.getPropagator());
		renewTickerAwake();
	}

	public void setIsRunning(boolean value) {
		if (isRunning != value) {
			isRunning = value;
			renewTickerAwake();
			/*
			 * DEBUGGING - comment out: if (!value) flushLog(); //
			 */
			fireSimulatorStateChanged();
		}
	}

	public void setIsTicking(boolean value) {
		if (isTicking != value) {
			isTicking = value;
			renewTickerAwake();
			fireSimulatorStateChanged();
		}
	}

	public void setTickFrequency(double freq) {
		if (tickFrequency != freq) {
			int millis = (int) Math.round(1000 / freq);
			int ticks;
			if (millis > 0) {
				ticks = 1;
			} else {
				millis = 1;
				ticks = (int) Math.round(freq / 1000);
			}

			tickFrequency = freq;
			ticker.setTickFrequency(millis, ticks);
			renewTickerAwake();
			fireSimulatorStateChanged();
		}
	}

	public void shutDown() {
		ticker.shutDown();
		manager.shutDown();
	}

	public void step() {
		synchronized (manager) {
			manager.stepsRequested++;
			manager.notifyAll();
		}
	}

	public void tick() {
		ticker.tickOnce();
	}

	public void tickMain(int count) {
		while (count > 0) {
			ticker.tickOnce();
			count--;
			try {
				Thread.sleep(50);
			} catch (InterruptedException ex) {
				Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE,
						null, ex);
			}
		}

	}

}
