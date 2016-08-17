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

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.util.UniquelyNamedThread;

class ConnectorThread extends UniquelyNamedThread {
	public static void enqueueRequest(MoveRequest req, boolean priority) {
		synchronized (INSTANCE.lock) {
			if (!req.equals(INSTANCE.processingRequest)) {
				INSTANCE.nextRequest = req;
				INSTANCE.overrideRequest = priority;
				INSTANCE.lock.notifyAll();
			}
		}
	}

	public static boolean isOverrideRequested() {
		return INSTANCE.overrideRequest;
	}

	private static ConnectorThread INSTANCE = new ConnectorThread();

	static {
		INSTANCE.start();
	}

	private Object lock;
	private transient boolean overrideRequest;
	private MoveRequest nextRequest;
	private MoveRequest processingRequest;

	private ConnectorThread() {
		super("tools-move-ConnectorThread");
		lock = new Object();
		overrideRequest = false;
		nextRequest = null;
	}

	public boolean isAbortRequested() {
		return overrideRequest;
	}

	@Override
	public void run() {
		while (true) {
			MoveRequest req;
			boolean wasOverride;
			synchronized (lock) {
				processingRequest = null;
				while (nextRequest == null) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
				req = nextRequest;
				wasOverride = overrideRequest;
				nextRequest = null;
				overrideRequest = false;
				processingRequest = req;
			}

			try {
				MoveResult result = Connector.computeWires(req);
				if (result != null) {
					MoveGesture gesture = req.getMoveGesture();
					gesture.notifyResult(req, result);
				}
			} catch (Exception t) {
				t.printStackTrace();
				if (wasOverride) {
					MoveResult result = new MoveResult(req,
							new ReplacementMap(), req.getMoveGesture()
									.getConnections(), 0);
					req.getMoveGesture().notifyResult(req, result);
				}
			}
		}
	}
}
